package com.example.mqtt.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Title:       MQTTService
 * <p>
 * Package:     com.example.mqtt.service
 * <p>
 * Author:      fxp
 * <p>
 * Create at:   2018/6/26 下午8:34
 * <p>
 * Description: MQTT消息推送服务
 * 验证成功。使用方式：context.startService(new Intent(context, MQTTService.class));
 * <p>
 * <p>
 * Modification History:
 * <p>
 * Date       Author       Version      Description
 * -----------------------------------------------------------------
 * 2018/6/26    fxp       1.0         First Created
 * <p>
 * Github:  https://github.com/fangxiaopeng
 */
public class MQTTService extends Service {

    public static final String TAG = MQTTService.class.getSimpleName();

    private MqttAndroidClient client;

    private MqttConnectOptions conOpt;

    /**
     * 服务器地址（协议+地址+端口号）
     */
    private String HOST = "ws://172.29.3.76:9241";

    private String userId = "421a56d5-7df9-4b49-b1e9-aebf7282f28c";

    private String userCode = "xxx";

    /**
     * 设备IMEI
     */
    private String deviceIMEI = "867709035428448";

    /**
     * 订阅主题（前半部分）
     */
    private String TOPIC_BASE = "xxx_";

    /**
     * 订阅主题 - 根据userId
     */
    private String TOPIC_USERID = TOPIC_BASE + userId;

    /**
     * 订阅主题 - 根据userCode
     */
    private String TOPIC_USERCODE = TOPIC_BASE + userCode;

    /**
     * 订阅主题 - 接收全部
     */
    private String TOPIC_ALL = TOPIC_BASE + "all";

    /**
     * MQTT Client Id，为确保唯一性，由TOPIC_BASE、userId、设备imei三部分拼接而成
     */
    private String CLIENTID = TOPIC_BASE + userId + "_" + deviceIMEI;

    /**
     * MQTT连接状态标志
     */
    private boolean isConnect = false;

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isConnect){
            initMQTT();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnectFromMQTTServer();
    }

    /**
     * @Description: 初始化MQTT链接
     *
     * @Author:  fxp
     * @Date:    2018/6/26   下午10:39
     * @param
     * @return   void
     * @exception/throws
     */
    private void initMQTT() {
        // 新建MqttAndroidClient对象
        client = new MqttAndroidClient(this, HOST, CLIENTID);
        // 设置MQTT监听并且接受消息
        client.setCallback(mqttCallback);

        // 新建MqttConnectOptions对象-用于设置MQTT参数
        conOpt = new MqttConnectOptions();
        // 清除缓存
        conOpt.setCleanSession(true);
        // 设置超时时间，单位：秒
        conOpt.setConnectionTimeout(10);
        // 心跳包发送间隔，单位：秒
        conOpt.setKeepAliveInterval(50);
        // 用户名
        conOpt.setUserName("admin");
        // 密码
        conOpt.setPassword("password@123".toCharArray());

        String message = "{\"terminal_uid\":\"" + CLIENTID + "\"}";
        String topic = TOPIC_USERCODE;
        Integer qos = 0;
        Boolean retained = false;
        if ((!message.equals("")) || (!topic.equals(""))) {
            try {
                conOpt.setWill(TOPIC_ALL, message.getBytes(), qos.intValue(), retained.booleanValue());
            } catch (Exception e) {
                Log.e(TAG, "conOpt.setWill Exception Occured --> " + e.toString());
                isConnect = false;
                iMqttActionListener.onFailure(null, e);
            }
        }

        connectToMQTTServer();
    }

    /**
     * @Description:  连接到MQTT服务器
     *
     * @Author:  fxp
     * @Date:    2018/6/26   下午10:46
     * @param
     * @return   void
     * @exception/throws
     */
    private void connectToMQTTServer() {
        Log.e(TAG, "connectToMQTTServer");
        if (!client.isConnected() && isNetConnected()) {
            try {
                client.connect(conOpt, null, iMqttActionListener);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @Description: 从MQTT服务器断开链接
     *
     * @Author:  fxp
     * @Date:    2018/6/26   下午11:01
     * @param
     * @return   void
     * @exception/throws
     */
    private void disconnectFromMQTTServer(){
        try {
            client.unregisterResources();
            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * @Description: MQTT连接状态监听 - MQTT是否连接成功
     *
     * @Author:  fxp
     * @Date:    2018/6/26   下午10:43
     * @param
     * @return
     * @exception/throws
     */
    private IMqttActionListener iMqttActionListener = new IMqttActionListener() {

        @Override
        public void onSuccess(IMqttToken arg0) {
            Log.i(TAG, "连接成功");
            isConnect = true;
            try {
                client.subscribe(new String[]{TOPIC_USERCODE, TOPIC_USERID, TOPIC_ALL}, new int[]{2, 2, 2});
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFailure(IMqttToken arg0, Throwable arg1) {
            Log.e(TAG, "连接失败");
            arg1.printStackTrace();
            isConnect = false;
            client.unregisterResources();

            // 连接失败，重连
            connectToMQTTServer();
        }
    };

    /**
     * @Description: MQTT回调监听 - 监听并且接收MQTT消息
     *
     * @Author:  fxp
     * @Date:    2018/6/26   下午10:44
     * @param
     * @return
     * @exception/throws
     */
    private MqttCallback mqttCallback = new MqttCallback() {
        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            String content = new String(message.getPayload(), "utf-8");
            /*
            messageArrived: hiboard_421a56d5-7df9-4b49-b1e9-aebf7282f28c
            {"terminal_uid":"hiboard_421a56d5-7df9-4b49-b1e9-aebf7282f28c_861695037157976"}
            1
            */
            Log.e(TAG, "messageArrived: " + topic + "\n" + content + "\n" + Thread.currentThread().getId());

        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken arg0) {

        }

        @Override
        public void connectionLost(Throwable arg0) {
            // 失去连接，重连
            if (arg0 != null){
                Log.e(TAG, "connectionLost" + arg0.getMessage());

                isConnect = false;

                disconnectFromMQTTServer();

                initMQTT();
            }
        }
    };

    /**
     * @Description: 判断网络是否连接
     *
     * @Author:  fxp
     * @Date:    2018/6/26   下午10:42
     * @param
     * @return   boolean
     * @exception/throws
     */
    private boolean isNetConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
            String name = info.getTypeName();
            Log.i(TAG, "Current net type --> " + name);
            return true;
        } else {
            return false;
        }
    }

}
