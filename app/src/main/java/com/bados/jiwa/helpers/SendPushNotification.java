package com.bados.jiwa.helpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class SendPushNotification extends AsyncTask<Void, Void, Void> {

    private final String FIREBASE_URL = "https://fcm.googleapis.com/fcm/send";
    private final String SERVER_KEY = "AAAAako3prY:APA91bHAP1yMkExBOW-EYLbim7SUp-wSjOjwEbxzBboBXbEIccuUY2BxEjKCykBMUqmn7612tNyWTNP2vgyI1c7fbqCp5AMlqbdfKwMN94NjDfstJUNYgzy-C4-KLQiCU3inZaZl-jA6";
    @SuppressLint("StaticFieldLeak")
    private Context context;
    private String token;

    public SendPushNotification(Context context, String token) {
        this.context = context;
        this.token = token;


        Toast.makeText(context, "" +token, Toast.LENGTH_SHORT).show();

    }


    @Override
    protected Void doInBackground(Void... voids) {

        /*{
            "to": "DEVICE_TOKEN",
            "data": {
            "type": "type",
                "title": "Android",
                "message": "Push Notification",
                "data": {
                    "key": "Extra data"
                }
            }
        }*/

        try {



            URL url = new URL(FIREBASE_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "key=" + SERVER_KEY);

            JSONObject message = new JSONObject();


            message.put("priority", "high");
            message.put("content_available", true);
            JSONObject notification = new JSONObject();

           notification.put("title", "Java");
           notification.put("sound", "default");
            notification.put("body", "Notification do Java");
            message.put("notification", notification);

            message.put("to", token);


            JSONObject innerData = new JSONObject();
            innerData.put("key", "Extra data");


              String pushMessage = message.toString();
                Log.e("PushNotification", "Data Format: " + pushMessage);

            try {
                OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                writer.write(message.toString());
                writer.flush();
                writer.close();

                int responseCode = connection.getResponseCode();

                Log.e("PushNotification", "Request Code: " + responseCode);

                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader((connection.getInputStream())));

                String output;
                StringBuilder builder = new StringBuilder();
                while ((output = bufferedReader.readLine()) != null) {
                    builder.append(output);
                }
                bufferedReader.close();
                String result = builder.toString();
                Log.e("PushNotification", "Result JSON: " + result);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("PushNotification", "Error: " + e.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("PushNotification", "Error: " + e.getMessage());
        }

        return null;
    }
}
