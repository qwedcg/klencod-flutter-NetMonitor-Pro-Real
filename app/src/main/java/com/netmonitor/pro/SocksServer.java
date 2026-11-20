package com.netmonitor.pro;

import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.util.concurrent.*;

public class SocksServer implements Closeable {
 private final ServerSocket server; private final ExecutorService pool=Executors.newCachedThreadPool(); private volatile boolean closed;
 public SocksServer(int port)throws IOException{server=new ServerSocket(port,64,InetAddress.getByName("127.0.0.1"));}
 public void start(){pool.execute(()->{while(!closed)try{Socket s=server.accept();pool.execute(()->handle(s));}catch(IOException e){if(!closed)e.printStackTrace();}});}
 private void handle(Socket s){try{
  InputStream in=s.getInputStream();OutputStream out=s.getOutputStream();byte[] h=new byte[2];readFully(in,h);
  if((h[0]&255)!=5){s.close();return;} int n=h[1]&255;byte[] methods=new byte[n];readFully(in,methods);
  out.write(new byte[]{5,0});out.flush();
  byte[] rq=new byte[4];readFully(in,rq);int cmd=rq[1]&255;int atyp=rq[3]&255;
  String host;int port;
  if(atyp==1){byte[] a=new byte[4];readFully(in,a);host=InetAddress.getByAddress(a).getHostAddress();}
  else if(atyp==3){int l=in.read();if(l<0)throw new EOFException();byte[] a=new byte[l];readFully(in,a);host=new String(a,StandardCharsets.UTF_8);}
  else if(atyp==4){byte[] a=new byte[16];readFully(in,a);host=InetAddress.getByAddress(a).getHostAddress();}
  else throw new IOException("ATYP");
  byte[] pb=new byte[2];readFully(in,pb);port=((pb[0]&255)<<8)|(pb[1]&255);
  if(cmd==1){tcp(s,in,out,host,port);} else if(cmd==3){udpAssociate(s,out);} else {fail(out);s.close();}
 }catch(Exception e){try{s.close();}catch(Exception ignored){}}}
 private void tcp(Socket c,InputStream ci,OutputStream co,String host,int port)throws Exception{
  Socket r=new Socket();r.connect(new InetSocketAddress(host,port),7000);r.setTcpNoDelay(true);
  co.write(new byte[]{5,0,0,1,0,0,0,0,0,0});co.flush();
  
  Thread a=new Thread(()->copy(ci,r,host,port));Thread b=new Thread(()->copy(r,c,host,port));a.start();b.start();a.join();try{r.close();}catch(Exception ignored){}try{c.close();}catch(Exception ignored){}
 }
 private void copy(InputStream in,Socket out,String host,int port){try{OutputStream o=out.getOutputStream();byte[]b=new byte[16384];int n;while((n=in.read(b))!=-1){o.write(b,0,n);o.flush();MonitorStore.add("TCP",host,port,n);}}catch(Exception ignored){}}
 private void udpAssociate(Socket control,OutputStream co)throws Exception{
  DatagramSocket relay=new DatagramSocket(0,InetAddress.getByName("127.0.0.1"));int rp=relay.getLocalPort();
  co.write(new byte[]{5,0,0,1,127,0,0,1,(byte)(rp>>8),(byte)rp});co.flush();
  Thread t=new Thread(()->udpLoop(relay));t.start();
  try{while(control.getInputStream().read()!=-1){}}finally{relay.close();t.interrupt();}
 }
 private void udpLoop(DatagramSocket relay){
  byte[]buf=new byte[65535];
  while(!relay.isClosed()&&!Thread.currentThread().isInterrupted())try{
   DatagramPacket p=new DatagramPacket(buf,0,buf.length);relay.receive(p);int len=p.getLength();
   if(len<10)continue;int atyp=buf[3]&255;int pos=4;String host;
   if(atyp==1){host=InetAddress.getByAddress(new byte[]{buf[pos],buf[pos+1],buf[pos+2],buf[pos+3]}).getHostAddress();pos+=4;}
   else if(atyp==3){int l=buf[pos++]&255;if(pos+l+2>len)continue;host=new String(buf,pos,l,StandardCharsets.UTF_8);pos+=l;}
   else continue;
   int port=((buf[pos]&255)<<8)|(buf[pos+1]&255);pos+=2;int payload=pos;int plen=len-pos;
   MonitorStore.add("UDP",host,port,plen);
   DatagramSocket ds=new DatagramSocket();try{
    ds.setSoTimeout(2000);DatagramPacket q=new DatagramPacket(buf,payload,plen,InetAddress.getByName(host),port);ds.send(q);
    byte[]rb=new byte[65535];DatagramPacket rr=new DatagramPacket(rb,rb.length);ds.receive(rr);
    byte[] head=udpHeader(rr.getAddress(),rr.getPort());byte[] all=new byte[head.length+rr.getLength()];System.arraycopy(head,0,all,0,head.length);System.arraycopy(rr.getData(),rr.getOffset(),all,head.length,rr.getLength());
    DatagramPacket back=new DatagramPacket(all,all.length,p.getAddress(),p.getPort());relay.send(back);
   }catch(Exception ignored){}finally{ds.close();}
  }catch(Exception e){break;}
 }
 private byte[] udpHeader(InetAddress a,int port)throws Exception{byte[]ip=a.getAddress();byte[]h=new byte[4+1+ip.length+2];h[0]=0;h[1]=0;h[2]=0;h[3]=(byte)(ip.length==4?1:4);System.arraycopy(ip,0,h,4,ip.length);int x=4+ip.length;h[x]=(byte)(port>>8);h[x+1]=(byte)port;return h;}
 private static void fail(OutputStream o)throws IOException{o.write(new byte[]{5,7,0,1,0,0,0,0,0,0});o.flush();}
 private static void readFully(InputStream i,byte[]b)throws IOException{int p=0,n;while(p<b.length&&(n=i.read(b,p,b.length-p))>0)p+=n;if(p<b.length)throw new EOFException();}
 public void close(){closed=true;try{server.close();}catch(Exception ignored){}pool.shutdownNow();}
}