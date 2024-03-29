package com.bados.jiwa.components.rider;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bados.jiwa.FirebaseFirestoreApi;
import com.bados.jiwa.MyDeviceUpdatesHandler;
import com.bados.jiwa.adapters.OrdersAdapter;
import com.bados.jiwa.helpers.PreferenceHelper;
import com.bados.jiwa.models.Order;
import com.bados.jiwa.models.Place;
import com.bados.jiwa.models.User;
import com.bados.jiwa.utils.MapUtils;
import com.bados.jiwa.utils.TextFormatUtils;
import com.bados.jiwa.views.Snackbar;
import com.bodas.jiwa.R;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.hypertrack.maps.google.widget.GoogleMapAdapter;
import com.hypertrack.maps.google.widget.GoogleMapConfig;
import com.hypertrack.sdk.views.dao.Trip;
import com.hypertrack.sdk.views.maps.HyperTrackMap;
import com.bados.jiwa.adapters.MapInfoWindowAdapter;
import com.bados.jiwa.components.MapPresenter;
import com.bados.jiwa.parsers.UserParser;
import com.rengwuxian.materialedittext.MaterialEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.reactivex.functions.Consumer;
import timber.log.Timber;

@SuppressWarnings("WeakerAccess")
public class RiderMapPresenter extends MapPresenter<RiderMapPresenter.RiderView, RiderMapPresenter.RiderState> {
    private static final String TAG = "RiderMapPresenter";

    private static final int PICKUP_AUTOCOMPLETE_REQUEST_CODE = 201;
    private static final int DROPOFF_AUTOCOMPLETE_REQUEST_CODE = 202;
    protected Snackbar tripEndSnackbar;


    private PreferenceHelper preferenceHelper;

    private final String myLocationText;
    private final GoogleMapConfig driverMapConfig;
    private ListenerRegistration driversListenerRegistration;




    private ListenerRegistration newOrderListenerRegistration;

    TextView distance, riderTime, fare;
    String parsedDistance, parsedDistanc, parseTime;
    LatLng pickUp, dropOff;
    Order order;
    double price2;
    User user;

    String[] colors = {"red", "green", "blue", "black"};


    TextView riderName;
    URL url;
   // protected P presenter;

    User usesr;
    private ListenerRegistration userListenerRegistration;

    Button btn;
    protected GoogleMap mGoogleMap;
    private FloatingActionButton locationButton;


    protected Snackbar stateSnackbar;
    protected Snackbar infoOrderSnackbar;
  //  protected Snackbar tripEndSnackbar;
    protected RecyclerView recyclerView;
    protected android.view.View blockingView;

    protected OrdersAdapter ordersAdapter;

    AlertDialog   al;
    //private PreferenceHelper preferenceHelper;

    String totalfare;
    String perishablefare;


    View view;

    private Boolean dialogShownOnce = false;

    private ListenerRegistration  userListener;


    public RiderMapPresenter(Context context, RiderMapPresenter.RiderView view) {
        super(context, view, new RiderState(context));
        preferenceHelper = new PreferenceHelper(context);
        myLocationText = context.getString(R.string.my_location);
        driverMapConfig = MapUtils.getBuilder(context)
                .locationMarker(new MarkerOptions()
                        .anchor(0.5F, 0.5F)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_drive_base_transparent)))
                .accuracyCircle(null)
                .bearingMarker(new MarkerOptions()
                        .anchor(0.5f, 0.5f)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.car_marker_dark)))
                .build();
    }

    @Override
    public void map(@NonNull GoogleMap googleMap) {
        super.map(googleMap);
        googleMap.setOnMapLongClickListener(latLng ->
                disposables.add(MapUtils.getAddress(mContext, latLng).subscribe(s -> {
                    Place place = new Place(latLng);
                    place.preview = null;
                    place.address = s;
                    updateDropOffPlace(place);
                })));

        preferenceHelper.putRole(mState.getUser().role);




        googleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) { }
            @Override
            public void onMarkerDrag(Marker marker) { }
            @Override
            public void onMarkerDragEnd(final Marker marker) {
                if (mState.pickupPlace.marker != null &&
                        mState.pickupPlace.marker.getId().equals(marker.getId())) {
                    mState.isMyLocationBind = false;
                    disposables.add(MapUtils.getAddress(mContext, marker.getPosition()).subscribe(new Consumer<String>() {
                        @Override
                        public void accept(String s) {
                            Place place = new Place(marker.getPosition());
                            place.preview = null;
                            place.address = s;
                            updatePickupPlace(place);
                        }
                    }));
                } else if (mState.dropOffPlace.marker != null &&
                        mState.dropOffPlace.marker.getId().equals(marker.getId())) {
                    disposables.add(MapUtils.getAddress(mContext, marker.getPosition()).subscribe(new Consumer<String>() {
                        @Override
                        public void accept(String s) {
                            Place place = new Place(marker.getPosition());
                            place.preview = null;
                            place.address = s;
                            updateDropOffPlace(place);
                        }
                    }));
                }
            }
        });
    }

    @Override
    public void onViewReady() {
        super.onViewReady();

        if (mState.getOrder() == null) {
            if (mState.dropOffPlace == null) {
                mView.showChooseDest();
            } else {
                mView.showBookRide();
            }
        }else{
            preferenceHelper.putPay(mState.getOrder().id);
            preferenceHelper.putRiderId(mState.getOrder().rider.id);
            preferenceHelper.putStatus(mState.getOrder().status);

        }
    }

    @Override
    public void onLocationChanged(Location location) {
        super.onLocationChanged(location);
        if (mState.isMyLocationBind) {
            setMyLocationAsPickup();
        }
    }

    @Override
    protected boolean onMarkerClick(Marker marker) {
        return false;
    }

    @Override
    protected void onOrderChanged() {
        super.onOrderChanged();

        if (mState.getOrder() != null) {

            if (hyperTrackMap != null) {
                hyperTrackMap.setMyLocationEnabled(false);
            }
            if (Order.COMPLETED.equals(mState.getOrder().status)) {
                updatePickupPlace(null);
                updateDropOffPlace(null);
                preferenceHelper.putName(mState.getOrder().tripId);

            } else {
                updatePickupPlace(mState.getOrder().pickup);
                updateDropOffPlace(mState.getOrder().dropoff);
            }
            if (mState.getOrder().driver == null) {
                final String[] Options = {"Person", "Non-Perishable Parcel","Perishable Parcel"};
                AlertDialog.Builder window = new AlertDialog.Builder(mContext,  R.style.AlertDialogTheme);
                window.setCancelable(false);
                window.setTitle("Category");
                window.setItems( Options, (dialog, which) -> {
                    if(which == 0){
                        mView.hideProgressBar();
                        dialog.dismiss();
                        fares();

                    }else if(which == 1){


                        mView.hideProgressBar();
                        dialog.dismiss();
                        fares();
                    }
                    else if(which == 2){


                        mView.hideProgressBar();
                        dialog.dismiss();
                        al.dismiss();

                        bayos();

                    }
                });
                    window.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                      public void onClick(DialogInterface dialog, int which) {
                          dialog.dismiss();
                          cancelled();
                      }
                });
                al = window.create();
                if  (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Objects.requireNonNull(al.getWindow()).setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY); } else {
                    Objects.requireNonNull(al.getWindow()).setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT); }
                if  (!al.isShowing() && !dialogShownOnce) {
                    al.show();
                    dialogShownOnce = true;
                }
                al.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        dialogShownOnce = false;
                    }
                });
            } else {
             subscribeDriver(mState.getOrder().driver.deviceId);
                removePickupMarker();
                removeDropOffMarker();
                mView.dismissState();
                if (!Order.COMPLETED.equals(mState.getOrder().status)) {
                    mView.showOrderInfo(mState.getOrder().driver,
                            TextFormatUtils.getDestinationName(mContext, mState.getOrder()));} else {
                    mView.dismissOrderInfo();
                }
            }
            mView.dismissBookRide();
        } else {

            if (hyperTrackMap != null) {
                hyperTrackMap.setMyLocationEnabled(true);
            }
            unsubscribeDriver();
//            subscribeDriverUpdates();

            setMyLocationAsPickup();
            if (mState.dropOffPlace != null) {
                updateDropOffPlace(mState.dropOffPlace);
            }

            mView.dismissState();
            mView.dismissOrderInfo();
            mView.showBookRide();
        }
        hyperTrackMap.adapter().notifyDataSetChanged();
    }



  void bayos(){

      DocumentReference washingtonRef = db.collection("orders").document(mState.getOrder().id);
             washingtonRef.update("type", "Perishable")
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    al.dismiss();

                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(mContext, "Error", Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "Error updating document", e);
                }
            });

      fares();

  }


//    void parcel(){
//
//        final String[] Options = {"Perishable", "Non-Perishable"};
//
//        AlertDialog.Builder  open= new AlertDialog.Builder(mContext,  R.style.AlertDialogTheme);
//        open.setCancelable(false);
//        open.setTitle("Parcel Weight");
//        open.setItems( Options, (dialog, which) -> {
//            if(which == 0){
//                fares();
//
//            }else if(which == 1){
//                //second option clicked, do this...
//                mView.showProgressBar();
//                DocumentReference washingtonRef = db.collection("orders").document(mState.getOrder().id);
//                washingtonRef.update("type", "Non-Perishable")
//                        .addOnSuccessListener(new OnSuccessListener<Void>() {
//                            @Override
//                            public void onSuccess(Void aVoid) {
//                                mView.hideProgressBar();
//                                dialog.dismiss();
//                                fares();
//                             }
//                        })
//                        .addOnFailureListener(new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                mView.hideProgressBar();
//                                Toast.makeText(mContext, "Error", Toast.LENGTH_SHORT).show();
//                                Log.w(TAG, "Error updating document", e);
//                            }
//                        });
//            }
//
//        });
//        open.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.dismiss();
//                cancelled();
//                // Write your code here to execute after dialog closed
//            }
//        });
//
//        AlertDialog alert = open.create();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            Objects.requireNonNull(alert.getWindow()).setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY); } else {
//            Objects.requireNonNull(alert.getWindow()).setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT); }
//
//
//        if (!alert.isShowing() && !dialogShownOnce) {
//            alert.show();
//            dialogShownOnce = true;
//        }
//
//        alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
//            @Override
//            public void onDismiss(DialogInterface dialog) {
//                dialogShownOnce = false;
//            }
//        });
//
//    }




    void fares(){

                AlertDialog.Builder alertDialog;
                alertDialog = new AlertDialog.Builder(mContext, R.style.AlertDialogTheme);
                alertDialog.setTitle("Confirm your booking");
                alertDialog.setCancelable(false);
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
                android.view.View layout_pwd = inflater.inflate(R.layout.confirm_rider,
                        null);
                alertDialog.setView(layout_pwd);
                AlertDialog alert = alertDialog.create();


                distance = layout_pwd.findViewById(R.id.distance);
                riderTime = layout_pwd.findViewById(R.id.ride_time);
                fare = layout_pwd.findViewById(R.id.fare);

                Button updateButton = (Button) layout_pwd.findViewById(R.id.cont);
                updateButton.setOnClickListener(v -> {
                    unsubscribeDriver();
                    mView.showState();
                    alert.dismiss();

                });
                Button dismissButton = (Button) layout_pwd.findViewById(R.id.cancel);
                dismissButton.setOnClickListener(v -> {
                    alert.dismiss();
                    cancelled();
                });
                alertDialog.setView(layout_pwd);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                  Objects.requireNonNull(alert.getWindow()).setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY); } else {
                  Objects.requireNonNull(alert.getWindow()).setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT); }



        if (!alert.isShowing() && !dialogShownOnce) {
            alert.show();
            dialogShownOnce = true;
        }

        alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                dialogShownOnce = false;
            }
        });


        alert.show();

            getDirectionsUrl();


    }



    public void cancelled() {
        if (mState.getOrder() != null) {
            mView.showProgressBar();
            DocumentReference washingtonRef = db.collection("orders").document(mState.getOrder().id);
            washingtonRef
                    .update("status", Order.CANCELLED)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mView.hideProgressBar();
                            Toast.makeText(mContext, "Order cancelled Successfully!", Toast.LENGTH_SHORT).show();
                            //Log.d(TAG, "DocumentSnapshot successfully updated!");
                            //  trackingPresenter.adjustTrackingState();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            mView.hideProgressBar();
                            Toast.makeText(mContext, "Error", Toast.LENGTH_SHORT).show();
                            Log.w(TAG, "Error updating document", e);
                        }
                    });
        }
    }



    public void getDirectionsUrl() {
        
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                  url = new URL("https://maps.googleapis.com/maps/api/directions/json?origin=" + mState.getOrder().pickup.latitude + ","
                            + mState.getOrder().pickup.longitude + "&destination=" + mState.getOrder().dropoff.latitude + "," +
                          mState.getOrder().dropoff.longitude + "&sensor=false&units=metric&mode=driving&key=AIzaSyDJnDn_O0nybXXsBypZ_5wTu1lVXOgHIjU");

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
        distance.setText(parseTime);
        riderTime.setText(parsedDistanc);
        double a = Double.parseDouble(parsedDistance);
        double b = a / 1000;
        double c = b * 10;
        DecimalFormat decim = new DecimalFormat("0");
        double price2 = Double.parseDouble(decim.format(c));

        //20% more
        double d = c * 1.2;
        double  pri = Double.parseDouble(decim.format(d));

       // Toast.makeText(mContext, "Fare" + mState.getOrder().type, Toast.LENGTH_SHORT).show();

        if (newOrderListenerRegistration != null) {
            newOrderListenerRegistration.remove();
        }
        DocumentReference washingtonRef = db.collection("orders").document(mState.getOrder().id);
        newOrderListenerRegistration = washingtonRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot snapshot,
                                    @Nullable FirebaseFirestoreException e) {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {

                        newOrderListenerRegistration.remove();

                        if (mState.getOrder().type != null) {
                            if (mState.getOrder().type.equals("Perishable")) {
                                totalfare = String.valueOf(pri);
                                fare.setText("Fare:\t" + totalfare);
                            } else {
                                perishablefare = String.valueOf(price2);
                                fare.setText("Fare:\t" +perishablefare);
                            }
                        }
                        else{

                        }
                    } else {
                        Log.d(TAG, "Current data: null");
                    }
                }
            });

        fare.setText("Fare: Ksh. " + totalfare);

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


    public void setMyLocationAsPickup() {
        if (mState.getOrder() == null
                && mState.currentLocation != null) {

            removePickupMarker();
            if (mState.pickupPlace == null) {
                mState.pickupPlace = new Place();
            }

            mState.isMyLocationBind = true;
            mState.pickupPlace.latitude = mState.currentLocation.getLatitude();
            mState.pickupPlace.longitude = mState.currentLocation.getLongitude();
            mState.pickupPlace.preview = myLocationText;

            disposables.add(MapUtils.getAddress(mContext, mState.pickupPlace.getLatLng()).subscribe(new Consumer<String>() {
                @Override
                public void accept(String s) {
                    mState.pickupPlace.address = s;
                    mView.updatePickup(mState.pickupPlace);
                    updateMapCamera();
                    //TODO debug
                    updatePickupPlace(mState.pickupPlace);
                }
            }));
        }
    }

    private void updatePickupPlace(Place place) {
        if (place == null) {
            removePickupMarker();
            mState.pickupPlace = null;
        } else {
            if (mState.pickupPlace == null) {
                mState.pickupPlace = new Place();
            }
            mState.pickupPlace.latitude = place.latitude;
            mState.pickupPlace.longitude = place.longitude;
            mState.pickupPlace.address = place.address;
            mState.pickupPlace.preview = place.preview;
            if (mState.pickupPlace.marker == null) {
                mState.pickupPlace.marker = mView.addMarker(mState.pickupPlace, R.drawable.ic_source_marker);
            } else {
                mState.pickupPlace.marker.setPosition(place.getLatLng());


            }
        }
        mView.updatePickup(place);
    }

    private void removePickupMarker() {
        if (mState.pickupPlace != null && mState.pickupPlace.marker != null) {
            mState.pickupPlace.marker.remove();
            mState.pickupPlace.marker = null;
        }
    }

    private void updateDropOffPlace(Place place) {
        if (place == null) {
            removeDropOffMarker();
            mState.dropOffPlace = null;
        } else {
            if (mState.dropOffPlace == null) {
                mView.dismissChooseDest();
                mView.showBookRide();
                mState.dropOffPlace = new Place();
            }

            mState.dropOffPlace.latitude = place.latitude;
            mState.dropOffPlace.longitude = place.longitude;
            mState.dropOffPlace.address = place.address;
            mState.dropOffPlace.preview = place.preview;
            if (mState.dropOffPlace.marker == null) {
                mState.dropOffPlace.marker = mView.addMarker(mState.dropOffPlace, R.drawable.ic_destination_marker);
            } else {
                mState.dropOffPlace.marker.setPosition(place.getLatLng());
            }
        }
        mView.updateDropoff(place);
    }

    private void removeDropOffMarker() {
        if (mState.dropOffPlace != null && mState.dropOffPlace.marker != null) {
            mState.dropOffPlace.marker.remove();
            mState.dropOffPlace.marker = null;
        }
    }

    private void updateMapCamera() {
        if (mState.pickupPlace != null && mState.dropOffPlace != null) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            if (mState.currentLocation != null) {
                builder.include(new LatLng(mState.currentLocation.getLatitude(), mState.currentLocation.getLongitude()));
            }
            builder.include(mState.pickupPlace.getLatLng());
            builder.include(mState.dropOffPlace.getLatLng());
            animateCamera(builder.build());
        } else {
            if (mState.pickupPlace != null) {
                animateCamera(mState.pickupPlace.getLatLng());
            } else if (mState.dropOffPlace != null) {
                animateCamera(mState.dropOffPlace.getLatLng());
            }
        }
    }

    public void choosePickup() {

        openPlaceSearch(mContext.getString(R.string.enter_pickup), PICKUP_AUTOCOMPLETE_REQUEST_CODE);
    }

    public void chooseDropoff() {


        openPlaceSearch(mContext.getString(R.string.enter_dropoff), DROPOFF_AUTOCOMPLETE_REQUEST_CODE);
    }

    public void orderTaxi() {
        if (mState.pickupPlace == null || mState.dropOffPlace == null) {
            mView.showNotification(R.string.choose_origin_and_destination);
            return;
        }

        final Order order = new Order();
        order.pickup = mState.pickupPlace;
        order.dropoff = mState.dropOffPlace;
        order.rider = mState.getUser();

        mView.showProgressBar();
        FirebaseFirestoreApi.createOrder(order)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        mView.hideProgressBar();

                        order.id = documentReference.getId();
                        mState.updateOrder(order);

                        subscribeOrderUpdates();
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        mView.hideProgressBar();
                        Log.w(TAG, "Error adding document", e);
                    }
                });
    }

    private void subscribeDriverUpdates() {
        if (driversListenerRegistration != null) {
            driversListenerRegistration.remove();
        }
        driversListenerRegistration = db.collection("users")
                .whereEqualTo("role", User.USER_ROLE_DRIVER)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            return;
                        }

                        if (value != null) {
                            UserParser parser = new UserParser();
                            for (QueryDocumentSnapshot doc : value) {
                                User driver = mState.drivers.get(doc.getId());
                                if (driver == null) {
                                    final User newDriver = parser.parse(doc);
                                    if (newDriver != null && !TextUtils.isEmpty(newDriver.deviceId)) {
                                        newDriver.deviceUpdatesHandler = new MyDeviceUpdatesHandler() {
                                            @Override
                                            public void onLocationUpdateReceived(@NonNull Location location) {
                                                newDriver.location = location;
                                                if (newDriver.marker == null) {
                                                    newDriver.marker = mView.addMarker(newDriver);
                                                } else {
                                                    newDriver.marker.setRotation(newDriver.location.getBearing());
                                                    newDriver.marker.setPosition(
                                                            new LatLng(newDriver.location.getLatitude(), newDriver.location.getLongitude())
                                                    );
                                                }
                                            }
                                        };
                                        hyperTrackViews.subscribeToDeviceUpdates(newDriver.deviceId, newDriver.deviceUpdatesHandler);
                                        mState.drivers.put(newDriver.id, newDriver);
                                    }
                                }
                            }
                        }
                    }
                });
    }

    private void unsubscribeDriverUpdates() {
        if (driversListenerRegistration != null) {
            driversListenerRegistration.remove();
        }
        List<User> drivers = new ArrayList<>(mState.drivers.values());
        for (User driver : drivers) {
            if (driver.deviceUpdatesHandler != null) {
                hyperTrackViews.unsubscribeFromDeviceUpdates(driver.deviceUpdatesHandler);
                driver.deviceUpdatesHandler = null;
            }
            if (driver.marker != null) {
                driver.marker.remove();
            }
        }
        mState.drivers.clear();
    }

    private void subscribeDriver(String deviceId) {
        Log.d("subscribeDriver", deviceId + " | " + mState.driver + " | " + mState.getOrder().tripId);
        if (mState.driver == null) {
            hyperTrackViews.subscribeToDeviceUpdates(deviceId, this);
            GoogleMapAdapter mapAdapter = new GoogleMapAdapter(googleMap, driverMapConfig);
            mapAdapter.addTripFilter(this);
            mState.driver = HyperTrackMap.getInstance(mContext, mapAdapter);
            mState.driver.bind(hyperTrackViews, deviceId);
            googleMap.setInfoWindowAdapter(new MapInfoWindowAdapter(mContext, mapAdapter));
        }
        if (!TextUtils.isEmpty(mState.getOrder().tripId)) {
            mState.driver.subscribeTrip(mState.getOrder().tripId);
        }
        if (Order.COMPLETED.equals(mState.getOrder().status)) {
            mState.driver.setMyLocationEnabled(false);
        }
        mState.driver.adapter().notifyDataSetChanged();
    }

    private void unsubscribeDriver() {
        Log.d("unsubscribeDriver", mState.driver + "");
        hyperTrackViews.unsubscribeFromDeviceUpdates(this);
        if (mState.driver != null) {
            mState.driver.destroy();
            mState.driver = null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICKUP_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                mState.isMyLocationBind = false;
                updatePickupPlace(new Place(Autocomplete.getPlaceFromIntent(data)));
                updateMapCamera();
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                Status status = Autocomplete.getStatusFromIntent(data);
                if (status.getStatusMessage() != null) Log.i(TAG, status.getStatusMessage());
            }
        } else if (requestCode == DROPOFF_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                updateDropOffPlace(new Place(Autocomplete.getPlaceFromIntent(data)));
                updateMapCamera();
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                Status status = Autocomplete.getStatusFromIntent(data);
                if (status.getStatusMessage() != null) Log.i(TAG, status.getStatusMessage());
            }
        }
    }

    @Override
    public void destroy() {
        super.destroy();

        unsubscribeDriverUpdates();
        unsubscribeDriver();

        if (newOrderListenerRegistration != null) {
            newOrderListenerRegistration.remove();
        }
    }

    public interface RiderView extends MapPresenter.View {

        void showChooseDest();

        void dismissChooseDest();

        void showBookRide();

        void dismissBookRide();

        Marker addMarker(User driver);

        Marker addMarker(Place place, int iconResId);

        void updatePickup(Place place);

        void updateDropoff(Place place);

    }

    public static class RiderState extends MapPresenter.State {
        public Map<String, User> drivers = new HashMap<>();
        private Place pickupPlace = new Place();
        private Place dropOffPlace;
        private HyperTrackMap driver;
        private boolean isMyLocationBind = true;

        public RiderState(Context context) {
            super(context);
        }
    }
}
