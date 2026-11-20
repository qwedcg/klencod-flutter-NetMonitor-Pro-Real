package com.netmonitor.pro;
import java.util.*; import java.util.concurrent.CopyOnWriteArrayList;
public final class MonitorStore {
 private static final CopyOnWriteArrayList<ConnectionRecord> DATA=new CopyOnWriteArrayList<>();
 private static volatile long packets=0,bytes=0;
 public static void add(String p,String ip,int port,long b){packets++;bytes+=Math.max(0,b);DATA.add(new ConnectionRecord(System.currentTimeMillis(),p,ip,port,b));if(DATA.size()>5000)DATA.remove(0);}
 public static List<ConnectionRecord> all(){return new ArrayList<>(DATA);}
 public static long packets(){return packets;} public static int ips(){HashSet<String>s=new HashSet<>();for(ConnectionRecord r:DATA)s.add(r.ip);return s.size();}
 public static int ports(){HashSet<Integer>s=new HashSet<>();for(ConnectionRecord r:DATA)s.add(r.port);return s.size();}
 public static void clear(){DATA.clear();packets=bytes=0;}
}