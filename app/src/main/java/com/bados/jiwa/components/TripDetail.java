package com.bados.jiwa.components;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;

import com.bados.jiwa.FirebaseFirestoreApi;
import com.bados.jiwa.helpers.SendPushNotification;
import com.bados.jiwa.models.Order;
import com.bodas.jiwa.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.bados.jiwa.parsers.OrderParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;


import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import timber.log.Timber;

import static com.bados.jiwa.FirebaseFirestoreApi.db;

public class TripDetail extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ListenerRegistration newOrderListenerRegistration;
    String token;
    String AUTH = "";
    String TRIP;
    String parsedDistance, parsedDistanc, parseTime;
    LatLng pickUp;

    String types;

    URL url;
   // String resp;
    ArrayList<LatLng> markerPoints;

    LatLng dropOff;
    private TextView txtDate, txtFee, txtBaseFare, txtTime, tripTravel, txtDistance, txtEstimatedPayout, txtFrom, txtTo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_detail);
    // getSupportActionBar().setDisplayHomeAsUpEnabl‌​‌​ed(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        txtDate = (TextView) findViewById(R.id.txtDate);
        txtFee = (TextView) findViewById(R.id.txtFee);
        markerPoints = new ArrayList<LatLng>();

        txtBaseFare = (TextView) findViewById(R.id.txtBaseFare);
        txtTime = (TextView) findViewById(R.id.txtTime);

        tripTravel = (TextView) findViewById(R.id.timetravel);
        txtDistance = (TextView) findViewById(R.id.txtDistance);
        txtEstimatedPayout = (TextView) findViewById(R.id.txtEstimatedPayout);
        txtFrom = (TextView) findViewById(R.id.txtFrom);
        txtTo = (TextView) findViewById(R.id.txtTo);

        TRIP = getIntent().getStringExtra("tripid");


        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                if (!task.isSuccessful()) {
                   // String toke = task.getException().getMessage();
                    Log.w("FCM TOKEN Failed", task.getException());
                } else {
                    token = task.getResult().getToken();
                    Log.i("FCM TOKEN", token);
                }
            }
        });





    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

//        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.uber_style_map));
        settingInformation();
    }

    private void settingInformation() {
        if (newOrderListenerRegistration != null) {
            newOrderListenerRegistration.remove();
        }
        newOrderListenerRegistration = db.collection("orders")
                .whereIn("trip_id", Collections.singletonList(TRIP))
                .addSnapshotListener((value, e) -> {
                    if (e != null) {
                        return;
                    }

                    if (value != null) {
                        List<Order> orders = new ArrayList<>();
                        OrderParser parser = new OrderParser();
                        for (QueryDocumentSnapshot doc : value) {

                            Order order = parser.parse(doc);
                            orders.add(order);


                            Calendar calendar = Calendar.getInstance();
                            String date = String.format("%s, %d/%d", convertToDayOfWeek(calendar.get(Calendar.DAY_OF_WEEK)), calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH));



//                                String dist = parsedDistance ;
//                                txt  txtDate.setText(date);
//BaseFare.setText("Ksh. " );


                            txtFrom.setText(getIntent().getStringExtra("start_address"));
                            assert order != null;
                            txtBaseFare.setText(order.rider.name);
                            tripTravel.setText(order.rider.phoneNumber);
                            txtEstimatedPayout.setText(order.payment);


                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd HH:MM");
                            Date testDate = order.updatedAt;
                            try {
                                testDate = sdf.parse(date);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                            SimpleDateFormat formatter = new SimpleDateFormat("MMM dd,yyyy");
                            String newFormat = formatter.format(testDate);

                            txtDate.setText(newFormat);
                            txtTo.setText(getIntent().getStringExtra("end_address"));

                            //add icon_marker
                            // Order order = ;


                            types = order.type;
                            pickUp = new LatLng(Double.parseDouble(String.valueOf(order.pickup.latitude)), Double.parseDouble(String.valueOf(order.pickup.longitude)));
                            dropOff = new LatLng(Double.parseDouble(String.valueOf(order.dropoff.latitude)), Double.parseDouble(String.valueOf(order.dropoff.longitude)));


                            Marker marker = mMap.addMarker(new MarkerOptions().position(dropOff)
                                    .title("Dropped Off Here")
                                    .snippet(order.dropoff.address)
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker)));

                            marker.showInfoWindow();


                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(dropOff, 12.0f));

                            getDirectionsUrl();


//                                // Getting URL to the Google Directions API
//                                String url = ;
//
//                                DownloadTask downloadTask = new DownloadTask();
//
//                                // Start downloading json data from Google Directions API
//                                downloadTask.execute(url);
                        }
                    }

                });
    }

    private String convertToDayOfWeek(int day) {
        switch (day) {
            case Calendar.SUNDAY:
                return "SUNDAY";
            case Calendar.MONDAY:
                return "MONDAY";
            case Calendar.TUESDAY:
                return "TUESDAY";
            case Calendar.WEDNESDAY:
                return "WEDNESDAY";
            case Calendar.THURSDAY:
                return "THURSDAY";
            case Calendar.FRIDAY:
                return "FRIDAY";
            case Calendar.SATURDAY:
                return "SATURDAY";
            default:
                return "UNK";
        }
    }

    public void getDirectionsUrl() {

        //Toast.makeText(this, "" + token, Toast.LENGTH_SHORT).show();
//        SendPushNotification sendPushNotification = new SendPushNotification(this, token);
//        sendPushNotification.execute();



        Thread thread = new Thread(new Runnable() {

            @Override

            public void run() {

                try {

                    url = new URL("https://maps.googleapis.com/maps/api/directions/json?origin=" + pickUp.latitude + ","

                            + pickUp.longitude + "&destination=" + dropOff.latitude + "," +

                            dropOff.longitude + "&sensor=false&units=metric&mode=driving&key=AIzaSyDJnDn_O0nybXXsBypZ_5wTu1lVXOgHIjU");

                    final HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                    conn.setRequestMethod("POST");

                    InputStream in = new BufferedInputStream(conn.getInputStream());

                    String response = iStreamToString(in);

                    JSONObject jsonObject = new JSONObject(response);

                    JSONArray array = jsonObject.getJSONArray("routes");

                    JSONObject routes = array.getJSONObject(0);

                    JSONArray legs = routes.getJSONArray("legs");

                    JSONObject steps = legs.getJSONObject(0);

                    JSONObject distance = steps.getJSONObject("distance");

                    JSONObject time = steps.getJSONObject("duration");

                    parseTime = time.getString("text");

                    parsedDistance = distance.getString("value");

                    parsedDistanc = distance.getString("text");


                    Timber.tag("URL").v("%s", url);


                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    //routes.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        txtTime.setText(parseTime);
        txtDistance.setText(parsedDistanc);

        double a = Double.parseDouble(parsedDistance);

        double b = a / 1000;

        double c = b * 10;
        DecimalFormat decim = new DecimalFormat("#" );
        double price2 = Double.parseDouble(decim.format(c));

        //20% more
        double d = c * 1.2;
        double  pri = Double.parseDouble(decim.format(d));

      //  txtFee.setText("Ksh. " + price2);

        if(types.equals("Perishable")){
            txtFee.setText("Ksh. " + pri);

        }else{
            txtFee.setText("Ksh. " + price2);


        }



    }


    public String iStreamToString(InputStream is1) {
        BufferedReader rd = new BufferedReader(new InputStreamReader(is1), 4096);
        String line;
        StringBuilder sb = new StringBuilder();
        try {
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            rd.close();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String contentOfMyInputStream = sb.toString();
        return contentOfMyInputStream;
    }


//    private void pushNotification() {
//        Thread thread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                JSONObject jPayload = new JSONObject();
//                JSONObject jNotification = new JSONObject();
//                JSONObject jData = new JSONObject();
//                try {
//                    jNotification.put("title", "Google I/O 2016");
//                    jNotification.put("body", "Firebase Cloud Messaging (App)");
//                    jNotification.put("sound", "default");
//                    jNotification.put("badge", "1");
//                    jNotification.put("click_action", "OPEN_ACTIVITY_1");
//                    jNotification.put("icon", "ic_notification");
//
//
//                    JSONArray ja = new JSONArray();
//                    ja.put(token);
//                    jPayload.put("registration_ids", ja);
//
//                    jPayload.put("priority", "high");
//                    jPayload.put("notification", jNotification);
//                   // jPayload.put("data", jData);
//
//                    URL url = new URL("https://fcm.googleapis.com/fcm/send");
//                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//                    conn.setRequestMethod("POST");
//                    conn.setRequestProperty("Authorization", AUTH);
//                    conn.setRequestProperty("Content-Type", "application/json");
//                    conn.setDoOutput(true);
//
//                    // Send FCM message content.
//                    OutputStream outputStream = conn.getOutputStream();
//                    outputStream.write(jPayload.toString().getBytes());
//
//                    // Read FCM response.
//                    InputStream inputStream = conn.getInputStream();
//                     resp = convertStreamToString(inputStream);
//
//
//                } catch (JSONException | IOException e) {
//                    e.printStackTrace();
//                }
//                Toast.makeText(TripDetail.this, "" + resp, Toast.LENGTH_SHORT).show();
//
//
//            }
//        });
//        thread.start();
//        try {
//            thread.join();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//    }
//
//    private String convertStreamToString(InputStream is) {
//        Scanner s = new Scanner(is).useDelimiter("\\A");
//        return s.hasNext() ? s.next().replace(",", ",\n") : "";
//    }

}
