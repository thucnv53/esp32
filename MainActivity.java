package com.aitogy.iotapp;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    TextView tvTemperature;
    MaterialButton btnOn;
    MaterialButton btnOff;

    MqttHelper mqttHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvTemperature = findViewById(R.id.tvTemp);
        btnOff = findViewById(R.id.btOff);
        btnOn = findViewById(R.id.btOn);

        if (isNetworkAvailable()) {
            runMqttService();
        } else {
            Toast.makeText(this, "Không có kết nối internet!", Toast.LENGTH_SHORT).show();
        }

        btnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mqttHelper.publishMessageToTopic("ledControl", "on");
            }
        });

        btnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mqttHelper.publishMessageToTopic("ledControl", "off");
            }
        });

    }

    private void runMqttService() {
        mqttHelper = new MqttHelper(getApplicationContext());

        mqttHelper.mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                Log.w("Debug","Connected");
                Toast.makeText(MainActivity.this, "Đã kết nối!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void connectionLost(Throwable throwable) {
                Toast.makeText(MainActivity.this, "Mất kết nối!", Toast.LENGTH_SHORT).show();
            }

            @Override
            @TargetApi(24)
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                Log.w("Debug",mqttMessage.toString());

                if (topic.equalsIgnoreCase("UNETITemperature")) {
                    tvTemperature.setText(mqttMessage.toString());
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                Toast.makeText(MainActivity.this, "Thành công!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static String random() {
        Random generator = new Random();
        StringBuilder randomStringBuilder = new StringBuilder();
        int randomLength = generator.nextInt(10);
        char tempChar;
        for (int i = 0; i < randomLength; i++){
            tempChar = (char) (generator.nextInt(96) + 32);
            randomStringBuilder.append(tempChar);
        }
        return randomStringBuilder.toString();
    }

    public class MqttHelper {
        public MqttAndroidClient mqttAndroidClient;

        String serverUri;

        String clientId;
        String subscriptionTopic;

        String username;
        String password;

        public MqttHelper(Context context){

            serverUri = "tcp://broker.hivemq.com:1883";

            clientId = random();
            subscriptionTopic = "UNETITemperature";

//            username = MainActivity.userName;
//            password = MainActivity.password;

            mqttAndroidClient = new MqttAndroidClient(context, serverUri, clientId);
            mqttAndroidClient.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean b, String s) {
                    Log.w("mqtt", s);
                }

                @Override
                public void connectionLost(Throwable throwable) {

                }

                @Override
                public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                    Log.w("Mqtt", mqttMessage.toString());
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

                }
            });
            connect();
        }

        public void setCallback(MqttCallbackExtended callback) {
            mqttAndroidClient.setCallback(callback);
        }

        private void connect(){
            MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
            mqttConnectOptions.setAutomaticReconnect(true);
            mqttConnectOptions.setCleanSession(false);
//            mqttConnectOptions.setUserName(username);
//            mqttConnectOptions.setPassword(password.toCharArray());

            try {

                mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {

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
                        Log.w("Mqtt", "Failed to connect to: " + serverUri + exception.toString());
                    }
                });


            } catch (MqttException ex){
                ex.printStackTrace();
            }
        }


        private void subscribeToTopic() {
            try {
                mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.w("Mqtt","Subscribed!");
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.w("Mqtt", "Subscribed fail!");
                    }
                });

            } catch (MqttException ex) {
                System.err.println("Exception whilst subscribing");
                ex.printStackTrace();
            }
        }

        public void publishMessageToTopic(String topic, String messagedata){
            try{
                MqttMessage message = new MqttMessage();
                message.setPayload(messagedata.getBytes());
                mqttAndroidClient.publish(topic, message);
            } catch (MqttException ex){
                System.err.println("Exception whilst publishing");
                ex.printStackTrace();
            }
        }

        public void disconnect(){
            try {
                mqttAndroidClient.disconnect();
            } catch (MqttException e){

            }
        }
    }
}
