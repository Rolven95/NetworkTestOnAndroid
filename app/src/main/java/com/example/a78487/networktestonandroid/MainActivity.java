package com.example.a78487.networktestonandroid;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.IOException;
import java.net.DatagramPacket;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private Context context;

    //---------------------------------------

    public static String serverIP = "192.168.1.105"; //"13.233.125.32";
    public static int serverPort = 9001;
    public static int packetLength = 512;
    public static DatagramSocket clientSocket ;
    public static int localPort ;
    public static String localIP ;
    public static boolean reqSentFlag = false;
    public static boolean oneWayMode = true;
    //---------------------------------------

    public String getIP(){
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
                {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && (inetAddress instanceof Inet4Address))
                    {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        }
        catch (SocketException ex){
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_activity_main);
        Button button = (Button)findViewById(R.id.button);
        TextView ipAddr = (TextView)findViewById(R.id.textview);
        ipAddr.setText("Local IP: "+ this.getIP());
        TextView cInfo = (TextView)findViewById(R.id.cInfo);
        cInfo.setText("Showing Current Status");
        //button.setText();
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                Log.i(TAG, "Client Started");
                try {
                    clientSocket = new DatagramSocket();
                    //localPort = clientSocket.getLocalPort();
                    //localIP = InetAddress.getLocalHost().getHostAddress().toString();
                    ExecutorService exec = Executors.newCachedThreadPool();
                    Thread thread1=new Thread(new ClientReciever());
                    Thread thread2=new Thread(new ClientSender());
                    exec.execute(thread1);
                    exec.execute(thread2);
                    exec.shutdown();
                } catch (SocketException e) {
                    e.printStackTrace();
                }
            }
        });
    }



    ///----------------------------------------------------------------------
    static class ClientReciever implements Runnable{
        @Override
        public void run() {
            try {
                System.out.println("Clilent listener Online");
                //System.out.println("Client listening at: "+ localIP
                //					+ " : "+ localPort);
                while(true) {
                    byte[] buf = new byte[packetLength]; // The maxium size of UDP is 65507, 视线中
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    clientSocket.receive(packet);

                    byte[] data = packet.getData();
                    unicast_packet arrival = new unicast_packet();
                    arrival = arrival.bytes_to_packet(data);

                    if(arrival.getType() == -2) {//服务端收到req 返回打洞尝试
                        oneWayMode = false;
                        System.out.println("client in duplex mode");
                        for(int i = 0 ;i < 5 ; i++){
                            //byte[] buf = new byte[packetLength];
                            unicast_packet to_sent = new unicast_packet(-2);
                            //System.out.println("clent type = " + to_sent.getType());
                            buf = to_sent.toByteArray();
                            System.out.println("dup notif sent to  " + serverIP +":" + serverPort);
                            DatagramPacket tosent = new DatagramPacket(buf, buf.length,
                                    InetAddress.getByName(serverIP), serverPort); //192.168.202.191  192.168.109.1
                            clientSocket.send(tosent);
                        }
                    }else if(arrival.getType() == 0) {//收到Data 返回ACK
                        oneWayMode = false;
                        //System.out.println("client enter duplex mode");
                        unicast_packet to_sent = arrival;
                        arrival.seType(1);
                        arrival.setArrival(System.currentTimeMillis());
                        buf = to_sent.toByteArray();
                        DatagramPacket tosent = new DatagramPacket(buf, buf.length,
                                InetAddress.getByName(serverIP), serverPort); //192.168.202.191  192.168.109.1
                        //Thread.sleep(2000);
                        clientSocket.send(tosent);
                        System.out.println(arrival.getSeq() + " ACK sent back");
                } else {
                    System.out.println("recieved a shit");
                }
                }
            }catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    static class ClientSender implements Runnable{
        @Override
        public void run() {
            try {
                if (!reqSentFlag) {
                    for(int i = 0 ;i < 20 ; i++){
                        byte[] buf = new byte[packetLength];
                        unicast_packet to_sent = new unicast_packet(-1, -1);
                        //System.out.println("clent type = " + to_sent.getType());
                        buf = to_sent.toByteArray();
                        System.out.println("req sent to " + serverIP +" at " + serverPort);
                        DatagramPacket tosent = new DatagramPacket(buf, buf.length,
                                InetAddress.getByName(serverIP), serverPort); //192.168.202.191  192.168.109.1
                        clientSocket.send(tosent);
                        Thread.sleep(3);
                    }
                }
                Log.i(TAG, "Sender: Connection request sent, waiting for response (6s)");

                Thread.sleep(6000);

                if(oneWayMode) {
                    Log.i(TAG, "Sender: Entered One-way mode");
                    System.out.println("Client sender on one way mode, start send shit to server");
                    for(int i = 0 ;i<10000 ; i++){
                        byte[] buf = new byte[packetLength];
                        unicast_packet to_sent = new unicast_packet(i,0);
                        to_sent.setDeparture(System.currentTimeMillis());
                        buf = to_sent.toByteArray();
                        Log.d(TAG, i + " sent to " + serverIP +" at " + serverPort);
                        System.out.println(i + " sent to " + serverIP +" at " + serverPort);
                        DatagramPacket tosent = new DatagramPacket(buf, buf.length,
                                InetAddress.getByName(serverIP), serverPort); //192.168.202.191  192.168.109.1
                        clientSocket.send(tosent);
                        //Thread.sleep(10);
                    }
                }

            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }


    //-----------------------------------------------------------------------
}
