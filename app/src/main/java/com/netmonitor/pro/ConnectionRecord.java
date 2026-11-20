package com.netmonitor.pro;
public class ConnectionRecord {
 public final long time; public final String protocol,ip; public final int port; public final long bytes;
 public ConnectionRecord(long t,String p,String i,int po,long b){time=t;protocol=p;ip=i;port=po;bytes=b;}
}