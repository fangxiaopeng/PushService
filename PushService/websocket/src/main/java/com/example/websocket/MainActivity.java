package com.example.websocket;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * Description: 基于OkHttp（https://github.com/square/okhttp）提供的WebSocket接口实现推送
 * 验证成功
 *
 * Author: fxp
 * Create at: 2018.06.13
 *
 * 完整Demo  https://github.com/fangxiaopeng/PushService
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private String TAG = "MainActivity";

    private Context context;

    private String protocol = "ws:";

    private String ip = "172.29.3.76";

    private String port = "9236";

    private String service = "/websocket/";

    private String userCode = "xxx";

    private String url;

    private OkHttpClient client;

    private Request request;

    private EchoWebSocketListener listener;

    private WebSocket mWebSocket;

    private TextView tvContent;

    private Button btnConnect, btnDisconnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findView();

        initData();

        initViews();

        initListener();
    }

    private void findView(){
        tvContent = findViewById(R.id.content);
        btnConnect = findViewById(R.id.connect);
        btnDisconnect = findViewById(R.id.disconnect);
    }

    private void initData(){

        context = getApplicationContext();

        url = protocol + ip + ":" + port + service + userCode;
    }

    private void initViews(){

    }

    private void initListener(){
        btnConnect.setOnClickListener(this);
        btnDisconnect.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.connect){
            new PushThread().run();
        }else if (id == R.id.disconnect){
            disconnectWebCocket();
        }else {}
    }

    class PushThread extends Thread {

        public PushThread() {

        }

        @Override
        public void run() {
            connectWebCocket();
        }

    }

    private void connectWebCocket() {

        initWebSocket();

        client.newWebSocket(request, listener);

        client.dispatcher().executorService().shutdown();
    }

    private void initWebSocket() {
        client = new OkHttpClient.Builder()
                .readTimeout(3, TimeUnit.SECONDS)//设置读取超时时间
                .writeTimeout(3, TimeUnit.SECONDS)//设置写的超时时间
                .connectTimeout(3, TimeUnit.SECONDS)//设置连接超时时间
                .pingInterval(3,TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();

        request = new Request.Builder()
                .url(url)
                .build();

        listener = new EchoWebSocketListener();
    }

    private final class EchoWebSocketListener extends WebSocketListener {

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            super.onOpen(webSocket, response);

            mWebSocket = webSocket;
        }

        @Override
        public void onMessage(WebSocket webSocket, final String text) {
            super.onMessage(webSocket, text);

            Log.i(TAG,"onMessage: " + text);

            // 当前为子线程，不能直接更新UI
            setReceivedMsg(text);
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            super.onMessage(webSocket, bytes);

            Log.i(TAG,"onMessage byteString: " + bytes);

        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            super.onClosing(webSocket, code, reason);

            Log.e(TAG,"onClosing: " + code + "/" + reason);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            super.onClosed(webSocket, code, reason);

            Log.e(TAG,"onClosed: " + code + "/" + reason);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            super.onFailure(webSocket, t, response);

            Log.e(TAG,"onFailure: " + t.getMessage());
            // 延时 30秒  在启动
            tvContent.postDelayed(new Runnable() {
                @Override
                public void run() {
                    new PushThread().run();
                }
            }, 1000 * 30);
        }
    }

    private void disconnectWebCocket() {
        if (mWebSocket != null) {
            mWebSocket.close(1000, null);
            mWebSocket = null;
        } else {
            Toast.makeText(context, "未建立链接", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 切换到主线程更新UI
     * @param data
     */
    private void setReceivedMsg(final String data) {

        Observable<String> observable = Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                emitter.onNext(data);
            }
        });

        Observer<String> observer = new Observer<String>() {
            @Override
            public void onSubscribe(Disposable d) {
                Log.i(TAG,"onSubscribe");
            }

            @Override
            public void onNext(String value) {
                Log.i(TAG,"onNext: " + value);
                updateTextView(value);
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG,"onError");
            }

            @Override
            public void onComplete() {
                Log.i(TAG,"onComplete");
            }
        };

        observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);
    }

    public void updateTextView(String str) {
        tvContent.setText(str);
    }

}
