package com.example.mqtt;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Description: 基于mqtt（https://github.com/eclipse/paho.mqtt.android）实现推送
 *
 * Author: fxp
 * Create at: 2018.06.13
 *
 * 完整Demo  https://github.com/fangxiaopeng/PushService
 */
public class MainActivity extends AppCompatActivity {

    private String TAG = "MainActivity";

    private Context context;

    private MqttAndroidClient mqttAndroidClient;

    private MqttConnectOptions mqttConnectOptions;

    final String serverUri = "tcp://iot.eclipse.org:1883";

    String clientId = "ExampleAndroidClient";

    final String subscriptionTopic = "exampleAndroidTopic";

    final String publishTopic = "exampleAndroidPublishTopic";

    final String publishMessage = "Hello World!";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();

        initMQTT();

        connectToMQTT();
    }

    private void initMQTT() {

        clientId = clientId + System.currentTimeMillis();
        mqttAndroidClient = new MqttAndroidClient(context, serverUri, clientId);
        mqttAndroidClient.setCallback(new MyMqttCallbackExtended());

        mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);
    }

    private void connectToMQTT(){
        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, new MyIMqttActionListener());
        } catch (MqttException ex){
            ex.printStackTrace();
        }
    }

    private class MyMqttCallbackExtended implements MqttCallbackExtended{

        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            Log.e(TAG,"MyMqttCallbackExtended connectComplete");

            if (reconnect) {
                addToHistory("Reconnected to : " + serverURI);
                // Because Clean Session is true, we need to re-subscribe
                subscribeToTopic();
            } else {
                addToHistory("Connected to: " + serverURI);
            }
        }

        @Override
        public void connectionLost(Throwable cause) {
            Log.e(TAG,"MyMqttCallbackExtended connectionLost");

            addToHistory("The Connection was lost.");
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            Log.e(TAG,"MyMqttCallbackExtended messageArrived");

            addToHistory("Incoming message: " + new String(message.getPayload()));
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            Log.e(TAG,"MyMqttCallbackExtended deliveryComplete");

        }
    }

    private class MyIMqttActionListener implements IMqttActionListener{

        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            Log.e(TAG,"MyIMqttActionListener onSuccess");

            DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
            disconnectedBufferOptions.setBufferEnabled(true);
            disconnectedBufferOptions.setBufferSize(100);
            disconnectedBufferOptions.setPersistBuffer(false);
            disconnectedBufferOptions.setDeleteOldestMessages(false);
            mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
            subscribeToTopic();
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            Log.e(TAG,"MyIMqttActionListener onFailure");

            addToHistory("Failed to connect to: " + serverUri);
        }
    }

    public void subscribeToTopic(){
        try {
            mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.e(TAG,"subscribeToTopic onSuccess");

                    addToHistory("Subscribed!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG,"subscribeToTopic onFailure");

                    addToHistory("Failed to subscribe");
                }
            });

            // THIS DOES NOT WORK!
            mqttAndroidClient.subscribe(subscriptionTopic, 0, new MyIMqttMessageListener());

        } catch (MqttException ex){
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }

    private void addToHistory(String mainText){
        System.out.println("LOG: " + mainText);

    }

    private class MyIMqttMessageListener implements IMqttMessageListener{

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            Log.e(TAG,"MyIMqttMessageListener messageArrived");

            // message Arrived!
            System.out.println("Message: " + topic + " : " + new String(message.getPayload()));
        }
    }

    public void publishMessage(){

        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(publishMessage.getBytes());
            mqttAndroidClient.publish(publishTopic, message);
            addToHistory("Message Published");
            if(!mqttAndroidClient.isConnected()){
                addToHistory(mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.");
            }
        } catch (Exception e) {
            System.err.println("Error Publishing: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
