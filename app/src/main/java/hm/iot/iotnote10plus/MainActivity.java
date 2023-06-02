package hm.iot.iotnote10plus;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.github.angads25.toggle.interfaces.OnToggledListener;
import com.github.angads25.toggle.model.ToggleableView;
import com.github.angads25.toggle.widget.LabeledSwitch;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    MQTTHelper mqttHelper;
    TextView txtTemperature, txtHumidity, txtIntensity;
    LabeledSwitch btnLED, btnPUMP;
    String btnLED_ack = "";
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
        txtIntensity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,PlotActivity.class);
                intent.putExtra("cbIntensity", true);
                startActivity(intent);
            }
        });
        txtHumidity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,PlotActivity.class);
                intent.putExtra("cbHumidity", true);
                startActivity(intent);
            }
        });
//        btnLED.setTag(3);
        btnLED.setOnToggledListener(new OnToggledListener() {
            @Override
            public void onSwitched(ToggleableView toggleableView, boolean isOn) {
                if(isOn){
                    sendDataMQTT("nathan0793/feeds/btnLED", "1");
                } else{
                    sendDataMQTT("nathan0793/feeds/btnLED", "0");
                }
            }
        });

        btnPUMP.setOnToggledListener(new OnToggledListener() {
            @Override
            public void onSwitched(ToggleableView toggleableView, boolean isOn) {
                if(isOn){
                    sendDataMQTT("nathan0793/feeds/btnBUMP", "1");
                } else{
                    sendDataMQTT("nathan0793/feeds/btnBUMP", "0");
                }
            }
        });
        startMQTT();
    }
    public void sendDataMQTT(String topic, String value){
        MqttMessage msg = new MqttMessage();
        msg.setId(1234);
        msg.setQos(0);
        msg.setRetained(false);

        byte[] b = value.getBytes(StandardCharsets.UTF_8);
        msg.setPayload(b);

        btnLED.setEnabled(false);

        try {
            mqttHelper.mqttAndroidClient.publish(topic, msg);
        } catch (MqttException e) {
            e.printStackTrace();
        }

        // Wait for the ACK payload for up to 3 seconds
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Check if the button is still disabled (meaning the ACK payload was not received)
                if (!btnLED.isEnabled()) {
                    // Decrement the retry count
                    int maxTryCount = Integer.parseInt(btnLED.getTag().toString());
                    maxTryCount--;
                    btnLED.setTag(maxTryCount);

                    // If there are remaining retries, resend the message
                    if (maxTryCount > 0) {
                        sendDataMQTT(topic, value);
                    } else {
                        // If there are no more retries, revert the button state and re-enable it
                        btnLED.setOn(!btnLED.isOn());
                        btnLED.setEnabled(true);
                    }
                }
            }
        }, 3000);
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
                Log.d("TEST", topic + "***" + message.toString());
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
                else if (topic.contains("btnLED-ack")){
                    if (!btnLED.isEnabled()) {
                        if (message.toString().equals("L1")) {
                            btnLED_ack = "L1";
                        } else {
                            btnLED.setOn(!btnLED.isOn());
                            btnLED.setEnabled(true);
                            btnLED_ack = "";
                        }
                    }
                }
                else if (topic.contains("btnLED") && btnLED_ack.equals("L1")){
                    btnLED.setOn(message.toString().equals("1"));
                    btnLED.setEnabled(true);
                    btnLED_ack = "";
                }
                else if (topic.contains("btnBUMP")){
                    btnPUMP.setOn(message.toString().equals("1"));
                }
            }
// In Case: Push Button from MCU
//                else if (topic.contains("BBC-LED")){
//                    if(message.toString().equals("1")){
//                        btnLED.setOn(true);
//                    }else{
//                        btnLED.setOn(false);
//                    }
//                }
            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }
}