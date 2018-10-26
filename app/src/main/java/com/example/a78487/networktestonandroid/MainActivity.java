package com.example.a78487.networktestonandroid;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
    //public static String serverIP = "13.115.245.244"; //"13.233.125.32";
    //public static String serverIP = "35.154.250.202"; //"13.233.125.32";35.154.250.202 印度第一台
    public static String serverIP = "13.232.64.25"; //印度乌班图
    //public static String serverIP = "192.168.202.20";
    public static String displayMessage = "WIFI";
    public static int serverPort = 9001;
    public static int packetLength = 1408;
    public static int latestSeq = 0;
    public static DatagramSocket clientSocket ;
    public static int localPort ;
    public static String localIP ;
    public static boolean connectionFlag = false; // 判断连接是否还在
    public static boolean onProgress = false; //进程开关
    public static boolean dupTimeout = true; //默认超时
    public static boolean reqSentFlag = false;
    public static boolean oneWayMode = true;
    public static String environment;
    public static TextView cliInfo;
    public static TextView senInfo;
    public static TextView recInfo;

    //---------------------------------------
    private static ProgressDialog progressDialog;
    private static AlertDialog dialog;
    //private AlertDialog dialog;

    @SuppressLint("HandlerLeak")
    private  Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (progressDialog != null) {
                progressDialog.setIndeterminate(false);
                progressDialog.setProgress(msg.what);
            }
        }
    };
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
        ipAddr.setText("Local IP: "+ this.getIP() + "(If showing 'null', re-check your network");
        //dialog = new AlertDialog.Builder(MainActivity.this);

                            // 创建数据
        final String[] items = new String[] { "WIFI", "Airtel 4G", "Jio 4G", "Other 4G","3G", "2G" };
                            // 创建对话框构建器
        final AlertDialog.Builder[] listBuilder = {new AlertDialog.Builder(this)};
                            // 设置参数
        listBuilder[0].setTitle("Choose a Environment, click out, and click on the sending button")
                .setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                            // TODO Auto-generated method stub
                            Toast.makeText(MainActivity.this, items[which],
                                    Toast.LENGTH_SHORT).show();
                            Log.i(TAG, "which = " + which);
                            switch (which)
                        {
                            case 0:{
                                environment = "WIFI";
                                listBuilder[0].create().dismiss();
                                //listBuilder[0] = null;
                            }break;
                            case 1:environment = "A4Gx";listBuilder[0].create().dismiss();break;
                            case 2:environment = "J4Gx";listBuilder[0].create().dismiss();break;
                            case 3:environment = "O4Gx";listBuilder[0].create().dismiss();break;
                            case 4:environment = "3Gxx";listBuilder[0].create().dismiss();break;
                            case 5:environment = "2Gxx";listBuilder[0].create().dismiss();break;

                        }
                        Log.i(TAG, "choose result:" + environment);
                        Log.i(TAG, "byte length:"+ environment.getBytes().length);
                    }
                });
        listBuilder[0].create().show();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                listBuilder[0].create().dismiss();
                listBuilder[0] = null;
                Log.i(TAG, "Client Started");
                cliInfo = (TextView)findViewById(R.id.textview);
                        cliInfo.setText("System Info: Button clicked, trying to connect to server");
                        try {
                            clientSocket = new DatagramSocket(); //
                            ExecutorService exec = Executors.newCachedThreadPool();
                            Thread thread1=new Thread(ClientReciever);
                            Thread thread2=new Thread(ClientSender);
                            Thread thread3=new Thread(AlertManager);
                            exec.execute(thread1);
                            exec.execute(thread2);
                            exec.execute(thread3);
                            exec.shutdown();
                        //Daemon daemon=new Daemon();
                    Thread daemoThread=new Thread(Daemon);
                    daemoThread.setDaemon(true);
                    daemoThread.start();
                    onProgress = true;
                    progressDialog = new ProgressDialog(MainActivity.this);
                            dialog = new AlertDialog.Builder(MainActivity.this).create();
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.setCancelable(false); // 能够返回
                    progressDialog.setTitle("Test is on progress"); // 不设置标题的话图标不会显示
                    progressDialog.setMessage("Please keep this APP on.");
                    progressDialog.setMax(50000*12);
                    //progressDialog.setCanceledOnTouchOutside(true); // 点击外部返回
                    progressDialog.setIndeterminate(true);//设置为不明确,则会再最大最小值之间滚动
                    progressDialog.show();
                } catch (SocketException e) {
                    e.printStackTrace();
                }
                    displayMessage = "Loading, please wait for 7s";
            }
        });
    }
    ///----------------------------------------------------------------------
    private Runnable ClientReciever = new Runnable(){
        @Override
        public void run(){
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
                            buf = to_sent.toByteArray(to_sent.getType());
                            System.out.println("dup notif sent to  " + serverIP +":" + serverPort);
                            DatagramPacket tosent = new DatagramPacket(buf, buf.length,
                                    InetAddress.getByName(serverIP), serverPort); //192.168.202.191  192.168.109.1
                            clientSocket.send(tosent);
                        }
                    }else if(arrival.getType() == 0) {//收到Data 自动进入dup模式 返回ACK
                        dupTimeout = false; //取消 超时 flag
                        oneWayMode = false;
                        connectionFlag = true;

                        //System.out.println("client enter duplex mode");
                        unicast_packet to_sent = arrival;
                        to_sent.seType(1);
                        to_sent.setArrival(System.currentTimeMillis());
                        to_sent.setFrom(environment);
                        buf = to_sent.toByteArray(to_sent.getType());
                        DatagramPacket tosent = new DatagramPacket(buf, buf.length,
                                InetAddress.getByName(serverIP), serverPort);
                        //Thread.sleep(2000);
                        clientSocket.send(tosent);
                        latestSeq = arrival.getSeq();
                        //handler.sendEmptyMessage(latestSeq*100/50000/9);
                        handler.sendEmptyMessage(latestSeq);
                        //System.out.println(arrival.getSeq() + " ACK sent back" + " size of: "+ buf.length);

                } else {
                    System.out.println("received a shit");
                }
                }
            }catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    };

    private Runnable ClientSender = new Runnable(){
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
                        // buf = new byte[packetLength];
                        unicast_packet to_sent = new unicast_packet(-1, -1);
                        //System.out.println("clent type = " + to_sent.getType());
                        byte[] buf = to_sent.toByteArray(to_sent.getType());
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
                        int i, interval,gapCounter,seq = 0;
                        for (interval = 0 ; interval < 12 ; interval++){
                            gapCounter = 0;
                            for(i = 0 ;i < 500000 ; i++) {
                                byte[] buf = new byte[packetLength];
                                unicast_packet to_sent = new unicast_packet(seq, 0);
                                to_sent.setDeparture(System.currentTimeMillis());
                                to_sent.setFrom(environment);
                                buf = to_sent.toByteArray(to_sent.getType());
                                //Log.i(TAG, i + " sent to " + serverIP + " at " + serverPort);
                                //System.out.println(i + " sent to " + serverIP + " at " + serverPort);
                                DatagramPacket tosent = new DatagramPacket(buf, buf.length,
                                        InetAddress.getByName(serverIP), serverPort);
                                clientSocket.send(tosent);
                                Thread.sleep((int)Math.floor(interval/3));
                                if (gapCounter >= (i+1)*30) { //实际上为cwdn
                                    Thread.sleep(20);
                                    gapCounter=0;
                                }
                                gapCounter++;
                                seq++;
                                latestSeq = seq;
                                //handler.sendEmptyMessage(seq*100/50000/9);
                                handler.sendEmptyMessage(seq);
                            }
                            Thread.sleep(1000);
                        }
                        Log.i(TAG, "Sender: one-way mode done, onProgress off");
                        System.out.println("Sender: one-way mode , onProgress off");
                        onProgress = false;
                    }
                }
            } catch (IOException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    };

    private Runnable AlertManager = new Runnable(){
        @Override
        public void run () {
            //dialog = new AlertDialog.Builder(MainActivity.this);
            while(true){ //Message uploader
                try{
                Thread.sleep(1000);

                if (onProgress && oneWayMode ){ //

                }else if (onProgress && !oneWayMode){
                    if (connectionFlag){
                        handler.sendEmptyMessage(-1);
                    }else{

                    }
                } else if (!onProgress) {

                }
                }catch (InterruptedException e) {
                    System.out.println("Alert Manager is dead");
                }
            }
        }
    };


    private Runnable Daemon = new Runnable(){
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
                            System.out.println("Deamon: Dup-way still running");
                            dupTimeout = true; // 1/0逻辑与服务端相反
                            Thread.sleep(5000); //最大收包间隔，若五秒内没有再收到则超时
                        }
                        System.out.println("Deamon Client Dup-way timeout!!");
                        onProgress = true;
                        connectionFlag = false;
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
    };

    //-----------------------------------------------------------------------
}
