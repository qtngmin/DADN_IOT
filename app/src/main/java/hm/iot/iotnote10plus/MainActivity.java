package hm.iot.iotnote10plus;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.angads25.toggle.widget.LabeledSwitch;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    MQTTHelper mqttHelper;
    TextView txtTemperature, txtHumidity, txtIntensity;
    LabeledSwitch btnLED, btnPUMP;
    List<Entry> entries = new ArrayList<>();
    LineDataSet dataSet = new LineDataSet(entries, "Label");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        txtTemperature = findViewById(R.id.txtTemperature);
        txtIntensity = findViewById(R.id.txtIntensity);
        txtHumidity = findViewById(R.id.txtHumidity);
        btnLED = findViewById(R.id.btnLED);
        btnPUMP = findViewById(R.id.btnBump);

        txtTemperature.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this,PlotActivity.class);
            intent.putExtra("cbTemperature", true);
            startActivity(intent);

        });
        txtIntensity.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this,PlotActivity.class);
            intent.putExtra("cbIntensity", true);
            startActivity(intent);
        });
        txtHumidity.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this,PlotActivity.class);
            intent.putExtra("cbHumidity", true);
            startActivity(intent);
        });
        btnLED.setOnToggledListener((toggleableView, isOn) -> {
            btnLED.setEnabled(false);
            if(isOn){
                sendDataMQTT("nathan0793/feeds/btnled", "1",3);
            } else{
                sendDataMQTT("nathan0793/feeds/btnled", "0",3);
            }
        });
        btnPUMP.setOnToggledListener((toggleableView, isOn) -> {
            if(isOn){
                sendDataMQTT("nathan0793/feeds/btnbump", "1",0);
            } else{
                sendDataMQTT("nathan0793/feeds/btnbump", "0",0);
            }
        });
        startMQTT();
    }
    public void sendDataMQTT(String topic, String value, int numberOfRetries){
        MqttMessage msg = new MqttMessage();
        msg.setId(1234);
        msg.setQos(0);
        msg.setRetained(false);
        byte[] b = value.getBytes(StandardCharsets.UTF_8);
        msg.setPayload(b);


        try {
            mqttHelper.mqttAndroidClient.publish(topic, msg);
        } catch (MqttException e) {
            e.printStackTrace();
        }

        if(topic.equals("nathan0793/feeds/btnled")){
            Timer timer = new Timer();
            int remainingRetries = numberOfRetries - 1;
            boolean remainingValue = Integer.parseInt(value) != 0;
            // Check if the button is still disabled (meaning the ACK payload was not received)
            if (!btnLED.isEnabled()) {
                // Wait for the ACK payload for up to 3 seconds
                timer.schedule(new TimerTask() {
                    // Decrement the retry count
                    public void run() {
                        // If there are remaining retries, resend the message
                        if (remainingRetries > 0) {
                            sendDataMQTT("nathan0793/feeds/btnled", remainingValue ? "1" : "0", remainingRetries);
                        }
                        else {
                            // If there are no more retries, revert the button state and re-enable it
                            btnLED.setOn(!btnLED.isOn());
                            btnLED.setEnabled(true);
                            //Testcase: When ACK have not been returned, revert the button on server (In case of virtual device)
                            sendDataMQTT("nathan0793/feeds/btnled", !remainingValue ? "1" : "0", remainingRetries);
                        }
                    }
                },3000);
            } else {
                timer.cancel();
            }
        }

    }

    public void startMQTT(){
        mqttHelper = new MQTTHelper(this);
        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

            }

            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                Date date = new Date();
                long day = date.getTime();
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                Log.d("TEST", topic + "***" + message.toString() +"***"+ inputFormat.format(date));

                if(topic.contains("adc-temperature")) {
                    String text = message.toString() + "°C";
                    txtTemperature.setText(text);
                }
                else if (topic.contains("adc-humidity")) {
                    String text = message.toString() + "%";
                    txtHumidity.setText(text);
                }
                else if (topic.contains("adc-intensity")) {
                    String text = message.toString() + "Lux";
                    txtIntensity.setText(text);
                }
                else if (topic.contains("ackled")){
                    if (!btnLED.isEnabled()) {
                        if (message.toString().equals("1")) {
                            btnLED.setEnabled(true);
                        } else {
                            btnLED.setOn(!btnLED.isOn());
                        }
                        btnLED.setEnabled(true);
                    }
                }
                else if (topic.equals("nathan0793/feeds/btnbump")){
                    btnPUMP.setOn(message.toString().equals("1"));
                }
                else if (topic.contains("nathan0793/feeds/btnled")){
                    btnLED.setOn(message.toString().equals("1"));
                }
            }
            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }
}