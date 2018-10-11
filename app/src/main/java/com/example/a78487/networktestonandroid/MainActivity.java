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
    public static String serverIP = "13.115.245.244"; //"13.233.125.32";
    //public static String serverIP = "192.168.1.105"; //"13.233.125.32";
    public static int serverPort = 9001;
    public static int packetLength = 1024;
    public static DatagramSocket clientSocket ;
    public static int localPort ;
    public static String localIP ;
    public static boolean onProgress = false;
    public static boolean dupTimeout = true; //默认超时
    public static boolean reqSentFlag = false;
    public static boolean oneWayMode = true;
    public static TextView cliInfo;
    public static TextView senInfo;
    public static TextView recInfo;

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

        //button.setText();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                Log.i(TAG, "Client Started");
                cliInfo = (TextView)findViewById(R.id.textview);
                    cliInfo.setText("System Info: Button clicked, trying to connect to server");
                    try {
                        clientSocket = new DatagramSocket(); //TODO 避免重复打开同一端口
                        ExecutorService exec = Executors.newCachedThreadPool();
                        Thread thread1=new Thread(new ClientReciever());
                        Thread thread2=new Thread(new ClientSender());
                        exec.execute(thread1);
                        exec.execute(thread2);
                        exec.shutdown();
                        Daemon daemon=new Daemon();
                    Thread daemoThread=new Thread(daemon);
                    daemoThread.setDaemon(true);
                    daemoThread.start();
                    onProgress = true;
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
                System.out.println("Client listener Online");
                //System.out.println("Client listening at: "+ localIP
                //					+ " : "+ localPort);
                while(true) {
                    byte[] buf = new byte[packetLength]; // The maxium size of UDP is 65507, 现实中有区别
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
                    }else if(arrival.getType() == 0) {//收到Data 自动进入dup模式 返回ACK
                        dupTimeout = false; //取消 超时 flag
                        oneWayMode = false;

                        //System.out.println("client enter duplex mode");
                        unicast_packet to_sent = arrival;
                        arrival.seType(1);
                        arrival.setArrival(System.currentTimeMillis());
                        buf = to_sent.toByteArray();
                        DatagramPacket tosent = new DatagramPacket(buf, buf.length,
                                InetAddress.getByName(serverIP), serverPort);
                        //Thread.sleep(2000);
                        clientSocket.send(tosent);
                        System.out.println(arrival.getSeq() + " ACK sent back");
                } else {
                    System.out.println("received a shit");
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
                while(true){
                    Thread.sleep(1000);
                    Log.i(TAG, "Client Sender idling, check flag ever 1s");
                    if (!reqSentFlag && onProgress) {//如果目前没有在进行任何活动 自动进入
                      System.out.println("New progress started and request is sending (20 times)");
                      Log.i(TAG, "New progress started and request is sending (20 times)");
                      for(int i = 0 ;i < 10 ; i++){
                        byte[] buf = new byte[packetLength];
                        unicast_packet to_sent = new unicast_packet(-1, -1);
                        //System.out.println("clent type = " + to_sent.getType());
                        buf = to_sent.toByteArray();
                        System.out.println("req sent to " + serverIP +" at " + serverPort);
                        DatagramPacket tosent = new DatagramPacket(buf, buf.length,
                                InetAddress.getByName(serverIP), serverPort); //192.168.202.191  192.168.109.1
                        clientSocket.send(tosent);
                        Thread.sleep(5);
                      }
                      reqSentFlag = true;
                    }                               //请求发送完毕 开始等待服务器回应 若无回应则进入单向模式
                    Log.i(TAG, "Sender: Connection request sent, waiting for response (6s)");

                    Thread.sleep(6000);

                    if(oneWayMode && onProgress && reqSentFlag){//若没有检测到服务器回应，进入单向模式
                        Log.i(TAG, "Sender: Entered One-way mode");
                        System.out.println("Client sender on one way mode, start send shit to server");
                        //cInfo.setText("Server lost, entering one-way mode, do not close this app.");
                        int i , interval;
                        for (interval = 0 ; interval <9 ; interval++){
                            for(i = 0 ;i<10000 ; i++) {
                                byte[] buf = new byte[packetLength];
                                unicast_packet to_sent = new unicast_packet(i, 0);
                                to_sent.setDeparture(System.currentTimeMillis());
                                buf = to_sent.toByteArray();
                                Log.i(TAG, i + " sent to " + serverIP + " at " + serverPort);
                                System.out.println(i + " sent to " + serverIP + " at " + serverPort);
                                DatagramPacket tosent = new DatagramPacket(buf, buf.length,
                                        InetAddress.getByName(serverIP), serverPort);
                                clientSocket.send(tosent);
                                Thread.sleep(interval);
                            }
                            Thread.sleep(1000);
                        }

                        Log.i(TAG, "Sender: one-way mode done, onProgress off");
                        System.out.println("Sender: one-way mode , onProgress off");
                        //cInfo.setText("Test Done, please kill this app.");
                        onProgress = false;
                    }
                }
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    static class Daemon implements Runnable {
        @Override
        public void run () {
            while (true) {
                try {
                    Thread.sleep(1000);
                    System.out.println("Deamon idling");
                    if (onProgress && !oneWayMode) { // connection built
                        System.out.println("Deamon find dup-way mode is on");
                        Thread.sleep(10000); // 从任务开始至首次检测超时的时间
                        while (!dupTimeout) { // check one way mode flag
                            System.out.println("Deamon: one-way still running");
                            dupTimeout = true; // 1/0逻辑与服务端相反
                            Thread.sleep(5000); //最大收包间隔，若五秒内没有再收到则超时
                        }
                        System.out.println("Deamon Client one-way timeout!!");
                        onProgress = false;
                        System.out.println("Server: Oneway mode ended, One way timeout, connection break");
                        //cInfo.setText("Test Done, please kill this app.");
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.out.println("Deamon is dead");
                    Log.e(TAG, "Deamon is dead");
                }
            }
        }
    }

    //-----------------------------------------------------------------------
}
