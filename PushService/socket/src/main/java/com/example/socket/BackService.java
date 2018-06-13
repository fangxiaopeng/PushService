package com.example.socket;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class BackService extends Service {
    private static final String TAG = "BackService";
    /**
     * 心跳检测时间
     */
    private static final long HEART_BEAT_RATE = 3 * 1000;
    /**
     * 主机IP地址
     */
    private static String HOST = "192.168.1.30";
    /**
     * 端口号
     */
    public static final int PORT = 10801;

    private String DEVICE_ID = "";
    /**
     * 消息广播
     */
    public static final String MESSAGE_ACTION = "org.feng.message_ACTION";
    /**
     * 心跳广播
     */
    public static final String HEART_BEAT_ACTION = "org.feng.heart_beat_ACTION";

    private long sendTime = 0L;

    private Socket socket;

    private ReadThread mReadThread;

    private InputStream is;//输入流
    private int count;//读取的字节长度

    private IBackService.Stub iBackService = new IBackService.Stub() {
        @Override
        public boolean sendMessage(String message) throws RemoteException {
            return sendMsg(message);
        }
    };

    @Override
    public IBinder onBind(Intent arg0) {
        return (IBinder) iBackService;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "ip-->" + HOST);
        Log.i(TAG, "port-->" + PORT);
        new InitSocketThread().start();
    }

    class InitSocketThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                initSocket();
            } catch (UnknownHostException e) {
                e.printStackTrace();
                Log.i(TAG, "socket-->" + e.getMessage());
                Log.i(TAG, "连接失败");
                Intent intent = new Intent(HEART_BEAT_ACTION);
                intent.putExtra("message", e.getMessage());
                sendBroadcast(intent);
            } catch (IOException e) {
                e.printStackTrace();
                Log.i(TAG, "socket-->" + e.getMessage());
                Intent intent = new Intent(HEART_BEAT_ACTION);
                intent.putExtra("message", e.getMessage());
                sendBroadcast(intent);
            }
        }
    }

    // 初始化socket
    private void initSocket() throws UnknownHostException, IOException {
        socket = new Socket(HOST, PORT);
        socket.setSoTimeout(10000);
        if (socket.isConnected()) {//连接成功
            if (sendMsg("mdc_" + DEVICE_ID)) {//发送成功
                //接收服务器返回的信息
                mReadThread = new ReadThread(socket);
                mReadThread.start();
            }
        }
    }

    // 发送心跳包
    private Handler mHandler = new Handler();
    private Runnable heartBeatRunnable = new Runnable() {
        @Override
        public void run() {
            ReadThread thread = new ReadThread(socket);
            thread.start();
        }
    };

    public class ReadThread extends Thread {
        private Socket rSocket;
        private boolean isStart = true;

        public ReadThread(Socket socket) {
            rSocket = socket;
        }

        public void release() {
            isStart = false;
            releaseLastSocket(rSocket);
        }

        @SuppressLint("NewApi")
        @Override
        public void run() {
            super.run();
            String line = "";
            if (null != rSocket) {
                while (isStart && !rSocket.isClosed() && !rSocket.isInputShutdown()) {
                    try {
                        Log.i(TAG, "开始读取消息");
                        is = rSocket.getInputStream();
                        count = is.available();
                        byte[] data = new byte[count];
                        is.read(data);
                        line = new String(data);
                        if (line != null) {
                            Log.i(TAG, "收到服务器发送来的消息：" + line);
                            if ("mdc_ok".equals(line) || "mdc_exist".equals(line) || "exist".equals(line)) {
                                sendMsg("connect");
                                mHandler.postDelayed(heartBeatRunnable, HEART_BEAT_RATE);// 初始化成功后，就准备发送心跳
                                return;
                            } else if ("mdc_connect".equals(line)) {//服务器发送继续接收的消息
                                boolean isSuccess = sendMsg("connect");
                                if (isSuccess) {//成功发送，接收回执信息
                                    mHandler.postDelayed(heartBeatRunnable, HEART_BEAT_RATE);// 初始化成功后，就准备发送心跳
                                } else {//发送失败，处理善后工作
                                    mHandler.removeCallbacks(heartBeatRunnable);
                                    release();
                                    releaseLastSocket(socket);
                                }
                                return;
                            } else if ("mdc_connectexit".equals(line) || "exit".equals(line)) {//断开连接消息
                                Intent intent = new Intent(HEART_BEAT_ACTION);
                                intent.putExtra("message", "exit");
                                sendBroadcast(intent);
                                return;
                            }
                        } else if (line == null) {
                            Log.i(TAG, "服务器发送过来的消息是空的");
                        }

                    } catch (IOException e) {
                        Log.i(TAG, "Read-->" + e.getClass().getName());
                        e.printStackTrace();
                        continue;
                    }
                }
            }
        }
    }

    public boolean sendMsg(String msg) {
        if (null == socket) {
            return false;
        }
        try {
            if (!socket.isClosed() && !socket.isOutputShutdown()) {
                OutputStream os = socket.getOutputStream();
                os.write(msg.getBytes());
                os.flush();
                sendTime = System.currentTimeMillis();// 每次发送成功数据，就改一下最后成功发送的时间，节省心跳间隔时间
                Log.i(TAG, "发送成功的时间：" + sendTime + "  内容-->" + msg);
            } else {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            Intent intent = new Intent(HEART_BEAT_ACTION);
            intent.putExtra("message", e.getMessage());
            sendBroadcast(intent);
            Log.i(TAG, "send-->" + e.getMessage());
            return false;
        }
        return true;
    }

    // 释放socket
    private void releaseLastSocket(Socket mSocket) {
        try {
            if (null != mSocket) {
                if (!mSocket.isClosed()) {
                    is.close();
                    mSocket.close();
                }
                mSocket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        mHandler.removeCallbacks(heartBeatRunnable);
        mReadThread.release();
        releaseLastSocket(socket);
    }

}