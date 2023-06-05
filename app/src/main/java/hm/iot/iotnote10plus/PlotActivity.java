package hm.iot.iotnote10plus;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;


public class PlotActivity extends AppCompatActivity {

    MQTTHelper mqttHelper;
    Button btnBack;
    ImageView startDateBtn, endDateBtn, startTimeBtn, endTimeBtn;
    EditText startDate, endDate, startTime, endTime;
    CheckBox cbIntensity, cbTemperature, cbHumidity;
    // Get the chart view from the layout
    LineChart lineChartTemperature, lineChartHumidity, lineChartIntensity;
    Button generate, btnReset;
    String username = "nathan0793";
    ArrayList<String> feedKeys = new ArrayList<>();
    private List<Entry> entries;
    private LineDataSet dataSet;
    private LineData lineData;
    // Declare a boolean flag variable
    private boolean isFunctionEnabled = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plot);

        btnBack = findViewById(R.id.btnBack);

        startDate = findViewById(R.id.startDate);
        startDateBtn = findViewById(R.id.startDateBtn);
        endDate = findViewById(R.id.endDate);
        endDateBtn = findViewById(R.id.endDateBtn);

        startTime = findViewById(R.id.startTime);
        startTimeBtn = findViewById(R.id.startTimeBtn);
        endTime = findViewById(R.id.endTime);
        endTimeBtn = findViewById(R.id.endTimeBtn);

        cbIntensity = findViewById(R.id.intensityADC);
        cbTemperature = findViewById(R.id.temperatureADC);
        cbHumidity = findViewById(R.id.humidityADC);

        generate = findViewById(R.id.generate);
        generate.setOnClickListener(v -> {
            isFunctionEnabled = false;
            getChart();
        });

        btnReset = findViewById(R.id.reset);
        btnReset.setOnClickListener(v -> {
            // Code to enable the function
            resetData();
            isFunctionEnabled = true;
        });

        lineChartTemperature = findViewById(R.id.lineChartTemperature);
        lineChartHumidity = findViewById(R.id.lineChartHumidity);
        lineChartIntensity = findViewById(R.id.lineChartIntensity);

        lineChartTemperature.setVisibility(View.INVISIBLE);
        lineChartHumidity.setVisibility(View.INVISIBLE);
        lineChartIntensity.setVisibility(View.INVISIBLE);

                setConfigChart(lineChartTemperature, "Temperature", "Temperature Data °C", 15,50);
                setConfigChart(lineChartHumidity, "Humidity", "Humidity", 20,80);
                setConfigChart(lineChartIntensity, "Intensity", "Intensity", 50,400);
        DateUpdate(startDate);
        DateUpdate(endDate);
        startTime.setText("00:00:00");
        TimeUpdate(endTime);

        cbIntensity.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b){
                feedKeys.add("adc-intensity");
                lineChartIntensity.setVisibility(View.VISIBLE);
            } else {
                feedKeys.remove("adc-intensity");
                lineChartIntensity.setVisibility(View.GONE);
            }
        });
        cbTemperature.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b){
                feedKeys.add("adc-temperature");
                lineChartTemperature.setVisibility(View.VISIBLE);

            } else {
                feedKeys.remove("adc-temperature");
                lineChartTemperature.setVisibility(View.GONE);
            }
        });
        cbHumidity.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b){
                feedKeys.add("adc-humidity");
                lineChartHumidity.setVisibility(View.VISIBLE);

            } else {
                feedKeys.remove("adc-humidity");
                lineChartHumidity.setVisibility(View.GONE);
            }
        });


        cbTemperature.setChecked(getIntent().getBooleanExtra("cbTemperature",false));
        cbIntensity.setChecked(getIntent().getBooleanExtra("cbIntensity",false));
        cbHumidity.setChecked(getIntent().getBooleanExtra("cbHumidity",false));

        btnBack.setOnClickListener(view -> {
            Intent intent = new Intent(PlotActivity.this,MainActivity.class);
            startActivity(intent);
        });
        startDateBtn.setOnClickListener(view -> DatePicker(startDate));
        endDateBtn.setOnClickListener(view -> DatePicker(endDate));
        startTimeBtn.setOnClickListener(view -> TimePicker(startTime));
        endTimeBtn.setOnClickListener(view -> TimePicker(endTime));
    //========================================


        startMQTT();
    }

    //=============================================
    private void setConfigChart( LineChart chart, String description, String label, float lowDanger, float highDanger){
        chart.setNoDataText("No data available");
        chart.setNoDataTextColor(Color.BLUE);

        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);

        chart.setDrawGridBackground(true);
        chart.setDrawBorders(true);
        chart.setBorderColor(Color.BLUE);
        chart.setBorderWidth(3);

        Description des = new Description();
        des.setText(description);
        des.setTextColor(Color.RED);
        des.setTextSize(10);
        chart.setDescription(des);

        Legend legend = chart.getLegend();
        legend.setEnabled(true);
        legend.setTextColor(Color.RED);
        legend.setTextSize(15);
        legend.setForm(Legend.LegendForm.LINE);
        legend.setFormSize(10);
        legend.setXEntrySpace(5);

        entries = new ArrayList<>();
        // Initialize the line dataset with empty entries
        dataSet = new LineDataSet(entries, label);
        dataSet.setColor(Color.RED);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setLineWidth(2f);
        // Create a LineData object and set the dataset
        lineData = new LineData(dataSet);
        // Set the data to the line chart
        chart.setData(lineData);
        chart.invalidate(); // Refresh the chart

        LimitLine upper_limit = new LimitLine(highDanger,"High Danger");
        upper_limit.setLineWidth(4f);
        upper_limit.enableDashedLine(10f, 10f, 10f);
        upper_limit.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        upper_limit.setTextSize(15f);

        LimitLine lower_limit = new LimitLine(lowDanger,"Low Danger");
        lower_limit.setLineWidth(4f);
        lower_limit.enableDashedLine(10f, 10f, 10f);
        lower_limit.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
        lower_limit.setTextSize(15f);

        chart.getAxisRight().setEnabled(false);
        YAxis yl = chart.getAxisLeft();
        yl.removeAllLimitLines();
        yl.addLimitLine(upper_limit);
        yl.addLimitLine(lower_limit);
        yl.setTextColor(Color.BLACK);
        yl.resetAxisMaximum();
        yl.setDrawGridLines(true);
    }
    private void addEntry(long timestamp, float value, LineChart chart){
        LineData data = chart.getData();
        if (data != null) {
            LineDataSet set = (LineDataSet) data.getDataSetByIndex(0);
            if (set==null){
                //Creation if null
                set = createSet();
                data.addDataSet(set);
            }

            // Add a new value
            data.addEntry(new Entry(set.getEntryCount(), value),0);

            //Enable the way Chart know when data has changed
            chart.notifyDataSetChanged();

            // Set the X-axis value formatter to display the timestamp labels
            XAxis xAxis = chart.getXAxis();
            xAxis.setValueFormatter(new MyXAxisValueFormatter(timestamp));

            // Limit the number of visible entries
            chart.setVisibleXRangeMaximum(10);

            // Move the chart to the latest entry
            chart.moveViewToX(data.getEntryCount());

            // Refresh the chart
            chart.invalidate();
        }
    }
    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "Temp");

        set.setDrawCircles(true);
        set.setCircleRadius(0.2f);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(ColorTemplate.getHoloBlue());
        set.setLineWidth(2f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244,177,177));
        set.setValueTextColor(Color.BLACK);
        set.setValueTextSize(10f);
        return set;
    }
    private class MyXAxisValueFormatter extends ValueFormatter {
        private long baseTimestamp;

        public MyXAxisValueFormatter(long baseTimestamp) {
            this.baseTimestamp = baseTimestamp;
        }

        @Override
        public String getAxisLabel(float value, AxisBase axis) {
            long timestamp = baseTimestamp + (long) value;
            // Format the timestamp as desired (e.g., using SimpleDateFormat)
            // Example format: "HH:mm:ss"
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }
    private void DatePicker(final EditText textview){
        Calendar calendar = Calendar.getInstance();
        int dayOfMonth = calendar.get(Calendar.DATE);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        DatePickerDialog datePickerDialog = new DatePickerDialog(PlotActivity.this, (datePicker, i, i1, i2) -> {
            //i: year - i1: month - i2: dayOfMonth
            calendar.set(i, i1, i2);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
            textview.setText(simpleDateFormat.format(calendar.getTime()));
        },year,month,dayOfMonth);
        datePickerDialog.setTitle("SELECT DATE");
        datePickerDialog.show();
    }
    private void DateUpdate(final EditText textview){
        Calendar calendar = Calendar.getInstance();
        int dayOfMonth = calendar.get(Calendar.DATE);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);

        calendar.set(year, month, dayOfMonth);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
        textview.setText(simpleDateFormat.format(calendar.getTime()));
    }
    private void TimePicker(final EditText textview){
        int style = AlertDialog.THEME_HOLO_DARK;
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        TimePickerDialog timePickerDialog = new TimePickerDialog(PlotActivity.this, style, (timePicker, i, i1) -> {
            calendar.set(0,0,0, i, i1,0);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
            textview.setText(simpleDateFormat.format(calendar.getTime()));
        }, hour, minute, true);
        timePickerDialog.setTitle("SELECT TIME");
        timePickerDialog.show();
    }
    private void TimeUpdate(final EditText textview){
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        calendar.set(0,0,0, hour, minute,0);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        textview.setText(simpleDateFormat.format(calendar.getTime()));
    }
    public void resetData() {
        lineChartTemperature.clear();
        lineChartHumidity.clear();
        lineChartIntensity.clear();
        setConfigChart(lineChartTemperature, "Temperature", "Temperature Data °C", 15,50);
        setConfigChart(lineChartHumidity, "Humidity", "Humidity", 20,80);
        setConfigChart(lineChartIntensity, "Intensity", "Intensity", 50,400);
    }
    public void getChart() {
        resetData();
        RequestQueue queue = Volley.newRequestQueue(PlotActivity.this);

        for (String feedKey : feedKeys) {
            String baseUrl = "https://io.adafruit.com/api/v2/{username}/feeds/{feedKey}/data/chart";
            String url = baseUrl.replace("{username}", username).replace("{feedKey}", feedKey);
            url += "?start_time=" + modifyAdafruitDate(startDate) + "T" + startTime.getText().toString() + "Z";
            url += "&" + "end_time=" + modifyAdafruitDate(endDate) + "T" + endTime.getText().toString() + "Z";
//            String finalUrl = url;
            String finalUrl ="https://io.adafruit.com/api/v2/nathan0793/feeds/adc-temperature/data/chart?start_time=2023-04-09T00:00:00Z&end_time=2023-04-09T24:00:00Z";
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.GET, finalUrl, null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    JSONArray data;
                    List<String> dates = new ArrayList<>();
                    List<String> values = new ArrayList<>();
                    try {
                        data = response.getJSONArray("data");
                        int n = data.length();
                        String date, value;
                        for (int i = 0; i < n; i++) {
                            JSONArray innerData = data.getJSONArray(i);
                            date = innerData.getString(0);
                            value = innerData.getString(1);
                            dates.add(date);
                            values.add(value);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    switch (feedKey) {
                        case "adc-temperature":
                            drawLineChart(dates, values, lineChartTemperature);
                            break;
                        case "adc-humidity":
                            drawLineChart(dates, values, lineChartHumidity);
                            break;
                        case "adc-intensity":
                            drawLineChart(dates, values, lineChartIntensity);
                            break;
                    }
                }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle error
                        Toast.makeText(PlotActivity.this, "Error", Toast.LENGTH_SHORT).show();
                    }
                });
            queue.add(request);
        }
    }
    private void drawLineChart(List<String> dates, List<String> values, LineChart chart){
        // Create a list of data entries
        List<Entry> entries = new ArrayList<>();
        try {
            for (int i = 0; i < dates.size(); i++) {
                String dateString = dates.get(i);
                float value = Float.parseFloat(values.get(i));
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                Date date = inputFormat.parse(dateString);
                long timestamp = date.getTime();
                entries.add(new Entry(timestamp, value));
            }

            // Create a data set from the data entries
            LineDataSet dataSet = new LineDataSet(entries, "Label");
            // Format the x-axis to show dates
            chart.getXAxis().setValueFormatter(new AutoScaleXAxisValueFormatter());
            // Add the data set to the chart and customize as desired
            LineData lineData = new LineData(dataSet);
            chart.setData(lineData);
            chart.invalidate();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private String modifyAdafruitDate(EditText texView) {
        String adafruitDate = null;
        // perform some modifications on the input string
        SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date date = inputFormat.parse(texView.getText().toString());
            adafruitDate = outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return adafruitDate;
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

                if (isFunctionEnabled && topic.contains("adc-temperature")) {
                    String s = message.toString();
                    float value = Float.parseFloat(s);
                    addEntry(day, value, lineChartTemperature);
                }
                if (isFunctionEnabled && topic.contains("adc-intensity")) {
                    String s = message.toString();
                    float value = Float.parseFloat(s);
                    addEntry(day, value, lineChartIntensity);
                }
                if (isFunctionEnabled && topic.contains("adc-humidity")) {
                    String s = message.toString();
                    float value = Float.parseFloat(s);
                    addEntry(day, value, lineChartHumidity);
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }
    // Define a custom value formatter for the x-axis to show dates
    public static class AutoScaleXAxisValueFormatter extends ValueFormatter {
        private final SimpleDateFormat secondFormat = new SimpleDateFormat("mm:ss");
        private final SimpleDateFormat minuteFormat = new SimpleDateFormat("HH:mm:ss");
        private final SimpleDateFormat hourFormat = new SimpleDateFormat("dd/MM HH:mm");
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

        private float previousLowestVisibleX = Float.MIN_VALUE;
        private float previousHighestVisibleX = Float.MAX_VALUE;
        private float previousVisibleXRange = 0;

        @Override
        public String getAxisLabel(float value, AxisBase axis) {
            if (axis instanceof XAxis) {
                XAxis xAxis = (XAxis) axis;
                float visibleXRange = xAxis.mAxisRange / (1000 * 60);

                if (visibleXRange != previousVisibleXRange ||
                        xAxis.getAxisMinimum() < previousLowestVisibleX ||
                        xAxis.getAxisMaximum() > previousHighestVisibleX) {

                    previousVisibleXRange = visibleXRange;
                    previousLowestVisibleX = (long) xAxis.getAxisMaximum();
                    previousHighestVisibleX = (long) xAxis.getAxisMinimum();

                    if (visibleXRange < 1) {
                        // Show seconds when zoomed in to 1 minute or less
                        return secondFormat.format(new Date((long) value));
                    } else if (visibleXRange >= 1 && visibleXRange < 60) {
                        // Show hours when zoomed out to 1 hour or less
                        return minuteFormat.format(new Date((long) value));
                    } else if (visibleXRange >= 60 && visibleXRange < 1440) {
                        // Show date when zoomed out to more than 1 day
                        return hourFormat.format(new Date((long) value));
                    } else if (visibleXRange >= 1440 && visibleXRange < 40320) {
                        // Show date when zoomed out to more than 1 day
                        return dateFormat.format(new Date((long) value));
                    }
                }
            }
            return super.getAxisLabel(value, axis);
        }
    }

}
