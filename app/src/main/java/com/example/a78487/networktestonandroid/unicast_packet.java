package com.example.a78487.networktestonandroid;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by 78487 on 2018/9/28.
 */

public class unicast_packet {
    private int seq; //
    private long departure;
    private long arrival;
    private long processing_cost; //so fucking helpful
    private String from;

    //System.currentTimeMillis()

    public int getSeq() {
        return seq;
    }
    public void setSeq(int p) {
        this.seq = p;
    }
    public long getdeparture() {
        return departure;
    }
    public void setDeparture(long s) {
        this.departure = s;
    }
    public long getArrival() {
        return arrival;
    }
    public void setArrival(long r) {
        this.arrival = r;
    }
    public long getProcessing_cost() {
        return processing_cost;
    }
    public void setProcessing_cost(long re) {
        this.processing_cost = re;
    }
    public String getFrom() {
        return from;
    }
    public void setFrom(String ss) {
        this.from = ss;
    }

    public byte[] toByteArray() throws UnknownHostException {
        ByteBuffer buffer = ByteBuffer.allocate(32);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(this.seq);	    	  // 4bytes 0-3
        buffer.putLong(this.departure); 	  // 8bytes 4-11
        buffer.putLong(this.arrival);    	  // 8bytes 12-19
        buffer.putLong(this.processing_cost); // 8bytes 20-27
        byte ip_in_bytes[] = InetAddress.getByName(this.from).getAddress();
        buffer.put(ip_in_bytes[0]);
        buffer.put(ip_in_bytes[1]);
        buffer.put(ip_in_bytes[2]);
        buffer.put(ip_in_bytes[3]);
        byte[] bytes = buffer.array();

        //for(int i = 0; i<32 ; i++) {
        //	System.out.println("all["+ i +"] =  "+ bytes[i]);
        //}

        //System.out.println("dep decoded is : " + longFrom8Bytes(bytes, 4, false));

        return bytes;
    }

    public static long longFrom8Bytes(byte[] input, int offset, boolean littleEndian){
        long value=0;
        // 循环读取每个字节通过移位运算完成long的8个字节拼装
        for(int  count=0;count<8;++count){
            int shift=(littleEndian?count:(7-count))<<3;
            value |=((long)0xff<< shift) & ((long)input[offset+count] << shift);
        }
        return value;
    }

    public unicast_packet bytes_to_packet (byte[] input) {
        unicast_packet result = new unicast_packet();

        int seq  = input[3] & 0xFF;
        seq |= ((input[2] << 8) & 0xFF00);
        seq |= ((input[1] << 16) & 0xFF0000);
        seq |= ((input[0] << 24) & 0xFF000000);

        long dep = longFrom8Bytes(input, 4, false);
        long cost = longFrom8Bytes(input, 20, false);

        String from_addr = (input[28] & 0xff)
                + "." + (input[29] & 0xff)
                + "." + (input[30] & 0xff)
                + "." + (input[31] & 0xff);
        result.setDeparture(dep);
        result.setProcessing_cost(cost);
        result.setSeq(seq);
        result.setFrom(from_addr);

        return result;
    }
}
