package com.example.a78487.networktestonandroid;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * Created by 78487 on 2018/9/28.
 */
public class Receiver {
    private static final String TAG = "Receiver";
    public String LocalIP;
    public int DEFAULT_PORT=9002;

    private DatagramSocket socket;
    private DatagramSocket ACK_socket;

    public void server(){
        try {
            //System.out.println("Receiver starts");
            Log.i(TAG, "Listener stats, try to open socket");

            socket = new DatagramSocket(DEFAULT_PORT);
            DatagramSocket ACK_socket = new DatagramSocket();
            Log.i(TAG, "Socket done, waiting for packet");
            while(true){

                byte[] buf = new byte[2048];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                long cost1 = System.currentTimeMillis();
                byte[] data = packet.getData();
                unicast_packet arrival = new unicast_packet();
                arrival = arrival.bytes_to_packet(data);
                //System.out.println("receved " + arrival.getSeq()+ " dep = " + arrival.getdeparture());
                arrival.setProcessing_cost(System.currentTimeMillis() - cost1);

                DatagramPacket to_sent_back = new DatagramPacket(arrival.toByteArray(),
                        arrival.toByteArray().length, InetAddress.getByName(arrival.getFrom()), 9001);
                ACK_socket.send(to_sent_back);
                //System.out.println( arrival.getSeq() +" ACK sent");
                Log.i(TAG, arrival.getSeq() +" ACK sent");
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
    }
}
