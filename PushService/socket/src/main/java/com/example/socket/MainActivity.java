package com.example.socket;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

/**
 * Description: 基于socket实现推送
 *
 * Author: fxp
 * Create at: 2018.06.13
 *
 * 完整Demo  https://github.com/fangxiaopeng/PushService
 */
public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";

    private Context context;

    private IBackService iBackService;

    private Intent mServiceIntent;

    private MessageBackReciver mReciver;

    private IntentFilter mIntentFilter;

    private LocalBroadcastManager mLocalBroadcastManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();

        initPushService();

        startPushService();
    }

    private ServiceConnection conn = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            iBackService = null;

        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            iBackService = IBackService.Stub.asInterface(service);
        }
    };

    private void initPushService(){
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(context);

        mReciver = new MessageBackReciver();

        mServiceIntent = new Intent(context, BackService.class);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(BackService.HEART_BEAT_ACTION);
        mIntentFilter.addAction(BackService.MESSAGE_ACTION);
    }

    protected void startPushService() {
        mLocalBroadcastManager.registerReceiver(mReciver, mIntentFilter);
        context.bindService(mServiceIntent, conn, BIND_AUTO_CREATE);
    }

    protected void stopPushService() {
        context.unbindService(conn);
        mLocalBroadcastManager.unregisterReceiver(mReciver);
    }

    private void sendMsg(){
        try{
            boolean isSend = iBackService.sendMessage("123");//Send Content by socket
            Toast.makeText(context, isSend ? "success" : "fail", Toast.LENGTH_SHORT).show();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static class MessageBackReciver extends BroadcastReceiver {

        public MessageBackReciver() {

        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String message = intent.getStringExtra("message");

            if (action.equals(BackService.HEART_BEAT_ACTION)) {
                Log.e(TAG,"Get a heart heat - " + message);

                Toast.makeText(context,"Get a heart heat - " + message,Toast.LENGTH_LONG).show();
            } else {
                Log.e(TAG,"Get a message:" + message);
                Toast.makeText(context,"Get a message:" + message,Toast.LENGTH_LONG).show();
            }
        };
    }

}

