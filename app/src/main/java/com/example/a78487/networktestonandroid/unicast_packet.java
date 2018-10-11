package com.example.a78487.networktestonandroid;


import java.net.DatagramPacket;
import java.net.DatagramSocket;

import java.net.InetAddress;

import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


//import java.lang;
public class unicast_packet {
    public static final int  packetLength = 1024;

    private int seq = 0; //  if seq == -1, this is connection building packet

    private long departure = 0;
    private long arrival = 0;
    private long nakArrival = 0; //so fucking helpful
    private String from = "";
    private int type = 0;


    public unicast_packet() {



    }
    public unicast_packet(int t) {

        type = t;

    }
    public unicast_packet(int s, int t) {
        seq = s;
        type = t;
    }

    public unicast_packet(int s, long d, long a, long p, String f, int t) {
        seq = s;
        departure = d;
        arrival = a;
        nakArrival = p;
        from = f;
        type = t;
    }

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
    public long getNakArrival() {
        return nakArrival;
    }
    public void setNakArrival(long re) {
        this.nakArrival = re;
    }
    public String getFrom() {
        return from;
    }
    public void setFrom(String ss) {
        this.from = ss;
    }
    public int getType() {
        return type;
    }
    public void seType(int p) {
        this.type = p;
    }
    public byte[] toByteArray() throws UnknownHostException {
        ByteBuffer buffer = ByteBuffer.allocate(packetLength);
        buffer.order(ByteOrder.BIG_ENDIAN);

        buffer.putInt(this.seq);	    	  // 4bytes 0-3
        buffer.putInt(this.type); // 32 33 34 35

        buffer.putLong(this.departure); 	  // 8bytes 4-11 +4
        buffer.putLong(this.arrival);    	  // 8bytes 12-19 +4
        buffer.putLong(this.nakArrival); // 8bytes 20-27 +4
        byte ip_in_bytes[] = InetAddress.getByName(this.from).getAddress(); //+4
        buffer.put(ip_in_bytes[0]);
        buffer.put(ip_in_bytes[1]);
        buffer.put(ip_in_bytes[2]);
        buffer.put(ip_in_bytes[3]);


        //System.out.println("");
        byte[] bytes = buffer.array();

        //for(int i = 0; i<36 ; i++) {
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

        int temtype  = input[7] & 0xFF;
        temtype |= ((input[6] << 8) & 0xFF00);
        temtype |= ((input[5] << 16) & 0xFF0000);
        temtype |= ((input[4] << 24) & 0xFF000000);

        long dep = longFrom8Bytes(input, 4+4, false);
        long cost = longFrom8Bytes(input, 20+4, false);


        String from_addr = (input[28+4] & 0xff)
                + "." + (input[29+4] & 0xff)
                + "." + (input[30+4] & 0xff)
                + "." + (input[31+4] & 0xff);

        result.setDeparture(dep);
        result.setNakArrival(cost);
        result.setSeq(seq);
        result.setFrom(from_addr);
        result.seType(temtype);
        return result;
    }

    public void sendThisPacket(unicast_packet to_sent, DatagramSocket socket, String targetIP, int port){
        try {
            byte[] buf = to_sent.toByteArray();
            DatagramPacket packet = new DatagramPacket(buf, buf.length,
                    InetAddress.getByName(targetIP), port);
            socket.send(packet);
            System.out.println( seq +" sent");
            //return to_sent;

        } catch (Exception e) {
            e.printStackTrace();
        }
        //return null;
    }
}
