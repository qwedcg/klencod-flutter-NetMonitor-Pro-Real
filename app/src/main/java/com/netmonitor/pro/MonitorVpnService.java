package com.netmonitor.pro;

import android.app.*;import android.content.*;import android.net.*;import android.os.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;

public class MonitorVpnService extends VpnService {
 private static volatile boolean running=false; private ParcelFileDescriptor tun; private SocksServer socks;
 public static boolean running(){return running;}
 @Override public int onStartCommand(Intent intent,int flags,int startId){
  if(running)return START_STICKY;
  startForeground(9,notification());
  try{
   ArrayList<String> apps=intent==null?null:intent.getStringArrayListExtra("apps");
   Builder b=new Builder().setSession("NetMonitor Pro").setMtu(1500).addAddress("10.7.0.2",32).addRoute("0.0.0.0",0).addDnsServer("1.1.1.1");
   if(apps==null||apps.isEmpty())throw new IOException("No apps selected");
   for(String p:apps)try{b.addAllowedApplication(p);}catch(Exception ignored){}
   tun=b.establish();if(tun==null)throw new IOException("VPN establish failed");
   socks=new SocksServer(1080);socks.start();
   File cfg=new File(getCacheDir(),"hev-monitor.yaml");
   String y="tunnel:\n  mtu: 1500\n  ipv4: 10.7.0.1\n  ipv6: 'fc00::1'\n  icmp: 'off'\n"
     +"socks5:\n  address: 127.0.0.1\n  port: 1080\n  udp: 'udp'\n";
   try(FileOutputStream o=new FileOutputStream(cfg)){o.write(y.getBytes(StandardCharsets.UTF_8));}
   running=true;final int fd=tun.getFd();final String path=cfg.getAbsolutePath();
   new Thread(()->{try{TProxyService.TProxyStartService(path,fd);}catch(Throwable e){stopSelf();}},"tun2socks").start();
   return START_STICKY;
  }catch(Exception e){stopSelf();return START_NOT_STICKY;}
 }
 private Notification notification(){String ch="monitor";NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
  if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel(ch,"NetMonitor Pro",NotificationManager.IMPORTANCE_LOW));
  Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,ch):new Notification.Builder(this);
  return b.setContentTitle("NetMonitor Pro").setContentText("مراقبة الشبكة تعمل").setSmallIcon(R.drawable.ic_launcher).build();}
 @Override public void onDestroy(){stopTunnel();super.onDestroy();}@Override public void onRevoke(){stopTunnel();super.onRevoke();}
 private synchronized void stopTunnel(){if(!running)return;running=false;try{TProxyService.TProxyStopService();}catch(Throwable ignored){}if(socks!=null)socks.close();if(tun!=null)try{tun.close();}catch(Exception ignored){}tun=null;}
}