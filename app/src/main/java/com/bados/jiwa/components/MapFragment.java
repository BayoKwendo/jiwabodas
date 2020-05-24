package com.bados.jiwa.components;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bados.jiwa.FirebaseFirestoreApi;
import com.bados.jiwa.components.mpesa.MPESAExpressActivity;
import com.bados.jiwa.helpers.PreferenceHelper;
import com.bados.jiwa.models.Order;
import com.bados.jiwa.models.User;
import com.bados.jiwa.parsers.OrderParser;
import com.bados.jiwa.views.Snackbar;
import com.bados.jiwa.views.status;
import com.bodas.jiwa.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.bados.jiwa.adapters.OrdersAdapter;
import com.bados.jiwa.parsers.UserParser;
import com.hypertrack.sdk.views.dao.Trip;

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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public abstract class MapFragment<P extends MapPresenter> extends SupportMapFragment
        implements OnMapReadyCallback, MapPresenter.View {
    private static final String TAG = "MapFragment";
    String token;
    String AUTH = "";
    protected status listener;
    public void setOnButtonListener(status actionButtonListener)
    {
        this.listener = actionButtonListener;


    }
    TextView distance, riderTime, fare;
    String parsedDistance, parsedDistanc, parseTime;
    LatLng pickUp, dropOff;
    Order order;
double price2;
User user;

TextView riderName;
    URL url;
    protected P presenter;

    User usesr;
    private ListenerRegistration userListenerRegistration;

   Button btn;
    protected GoogleMap mGoogleMap;
    private FloatingActionButton locationButton;
    private PreferenceHelper preferenceHelper;


    protected Snackbar stateSnackbar;
    protected Snackbar infoOrderSnackbar;
    protected Snackbar tripEndSnackbar;
    protected RecyclerView recyclerView;
    protected View blockingView;

    protected OrdersAdapter ordersAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater layoutInflater, @Nullable ViewGroup viewGroup, @Nullable Bundle bundle) {
        View view = layoutInflater.inflate(R.layout.fragment_map, null);
        View mapView = super.onCreateView(layoutInflater, viewGroup, bundle);
        ((FrameLayout) view.findViewById(R.id.map_frame))
                .addView(mapView, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        preferenceHelper = new PreferenceHelper(Objects.requireNonNull(getActivity()));

        return view;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                layoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.setLayoutManager(layoutManager);
        ordersAdapter = new OrdersAdapter();
        recyclerView.setAdapter(ordersAdapter);
        blockingView = view.findViewById(R.id.blocking_view);

        stateSnackbar = Snackbar.make(view, R.layout.snackbar_state, Snackbar.LENGTH_INDEFINITE);
        infoOrderSnackbar = Snackbar.make(view, R.layout.snackbar_order_info, Snackbar.LENGTH_INDEFINITE);
        tripEndSnackbar = Snackbar.make(view, R.layout.snackbar_trip_end_info, Snackbar.LENGTH_INDEFINITE);
        locationButton = view.findViewById(R.id.locationButton);
        locationButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                presenter.setCameraFixedEnabled(true);
                blockingView.setOnTouchListener(new View.OnTouchListener() {

                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        presenter.setCameraFixedEnabled(false);
                        blockingView.setOnTouchListener(null);
                        return false;
                    }
                });
            }
        });

        getMapAsync(this);
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                presenter.onViewReady();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        presenter.map(googleMap);
    }

    @Override
    public void showState() {
        presenter.addSnackbar(stateSnackbar);
    }

    @Override
    public void dismissState() {
        presenter.removeSnackbar(stateSnackbar);
    }

    public void showOrderCancelDialog() {
        if (getActivity() != null) {
            AlertDialog cancelledOrderAlert = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.cancel_ride)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            presenter.cancelOrder();
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .create();
            cancelledOrderAlert.show();
        }
    }

    @Override
    public void showOrderInfo(final User user, CharSequence address) {
        if (user != null) {
            TextView destination = infoOrderSnackbar.getView().findViewById(R.id.destination);
            destination.setText(address);
            TextView name = infoOrderSnackbar.getView().findViewById(R.id.name);
            TextView rating = infoOrderSnackbar.getView().findViewById(R.id.rating);
            RatingBar ratingBar = infoOrderSnackbar.getView().findViewById(R.id.rating_bar);
            float ratingIndex = 4.8f;
            rating.setText(String.valueOf(ratingIndex));
            ratingBar.setRating(ratingIndex);

            infoOrderSnackbar.setAction(R.id.cancel, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showOrderCancelDialog();
                }
            });
            infoOrderSnackbar.setAction(R.id.call, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (userListenerRegistration != null) {
                        userListenerRegistration.remove();
                    }
                    userListenerRegistration = FirebaseFirestoreApi.db.collection("users")
                            .whereEqualTo("role", user.role)
                            .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                @Override
                                public void onEvent(@Nullable QuerySnapshot value,
                                                    @Nullable FirebaseFirestoreException e) {
                                    if (e != null) {
                                        return;
                                    }
                                    if (value != null) {
                                        UserParser parser = new UserParser();
                                        for (QueryDocumentSnapshot doc : value) {
                                            usesr = parser.parse(doc);
                                        }
                                        Intent intent = new Intent(Intent.ACTION_VIEW);

                                        intent.setData(Uri.parse("tel:" + usesr.phoneNumber));
                                        startActivity(intent);
                                    }
                                }
                            });

                }
            });

            if (userListenerRegistration != null) {
                userListenerRegistration.remove();
            }
            userListenerRegistration = FirebaseFirestoreApi.db.collection("users")
                    .whereEqualTo("role", user.role)
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot value,
                                            @Nullable FirebaseFirestoreException e) {
                            if (e != null) {
                                return;
                            }
                            if (value != null) {
                                UserParser parser = new UserParser();
                                for (QueryDocumentSnapshot doc : value) {
                                    usesr = parser.parse(doc);
                                }
                                name.setText(usesr.name);
                            }
                        }
                    });
            presenter.addSnackbar(infoOrderSnackbar);
        }
    }

    @Override
    public void dismissOrderInfo() {
        presenter.removeSnackbar(infoOrderSnackbar);
    }



    @Override
    public void showTripEndInfo(Trip trip, User user) {

        TextView header = tripEndSnackbar.getView().findViewById(R.id.header);
        btn = tripEndSnackbar.getView().findViewById(R.id.btnpay);

        distance = tripEndSnackbar.getView().findViewById(R.id.distance);
        riderTime = tripEndSnackbar.getView().findViewById(R.id.ride_time);
        fare = tripEndSnackbar.getView().findViewById(R.id.fare);

        String headerText = User.USER_ROLE_DRIVER.equals(user.role) ?
                getString(R.string.book_another_ride) : getString(R.string.find_new_rides);
        header.setText(headerText);


        details();


        if (trip != null) {
            if (trip.getSummary() != null) {
                double miles = trip.getSummary().getDistance() * 0.000621371;
                distance.setText(String.format(getString(R.string.miles), miles));
                long mins = TimeUnit.SECONDS.toMinutes(trip.getSummary().getDuration());
                riderTime.setText(String.format(getString(R.string.mins), mins));
                fare.setText(String.format(getString(R.string.fare), (int) (miles * 2)));
            }
            tripEndSnackbar.setAction(R.id.cancel, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    presenter.closeCompletedOrder();
                }
            });
            riderName = tripEndSnackbar.getView().findViewById(R.id.name);
            riderName.setText(user.name);


            if(User.USER_ROLE_DRIVER.equals(user.role)){
                details();
                riderName.setVisibility(View.GONE);
                btn.setOnClickListener(v -> {
                    Intent topicsIntent = new Intent(getActivity(), MPESAExpressActivity.class);
                    startActivity(topicsIntent);
                }) ;


            }else{
                btn.setOnClickListener(v -> {
                    Toast.makeText(getActivity(), "Your not the Customer! Make sure the rider has pay", Toast.LENGTH_SHORT).show();

                }) ;


            }

            presenter.addSnackbar(tripEndSnackbar);
        }
    }




    private void details() {


        String tripID =  preferenceHelper.getName();


        userListenerRegistration = FirebaseFirestoreApi.db.collection("orders")
                .whereIn("trip_id", Collections.singletonList(tripID))
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
                            assert order != null;
                            btn.setText(order.payment);
                            if(order.payment.equals("UNPAID!")){
                                btn.setBackgroundColor(getResources().getColor(R.color.error));
                            } else if (order.payment.equals("PAID")){
                                btn.setBackgroundColor(getResources().getColor(R.color.success));
                            }
                            pickUp = new LatLng(Double.parseDouble(String.valueOf(order.pickup.latitude)), Double.parseDouble(String.valueOf(order.pickup.longitude)));
                            dropOff = new LatLng(Double.parseDouble(String.valueOf(order.dropoff.latitude)), Double.parseDouble(String.valueOf(order.dropoff.longitude)));

                            getDirectionsUrl();

                        }
                    }

                });
    }
    @Override
    public void dismissTripEndInfo() {
        presenter.removeSnackbar(tripEndSnackbar);
    }

    @Override
    public void showAlertDialog(final String key, int textResId) {
        if (getActivity() != null) {
            AlertDialog cancelledOrderAlert = new AlertDialog.Builder(getActivity())
                    .setTitle(textResId)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            presenter.onAlertDialogDismissed(key);
                        }
                    })
                    .create();
            cancelledOrderAlert.show();
        }
    }

    @Override
    public void showUI() {
        locationButton.show();
    }

    @Override
    public void hideUI() {
        locationButton.hide();
    }

    @Override
    public void showProgressBar() {
    }

    @Override
    public void hideProgressBar() {
    }

    @Override
    public void showNotification(int textResId) {
        if (getActivity() != null) {
            Toast.makeText(getActivity(), textResId, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        presenter.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter.destroy();

        if (userListenerRegistration != null) {
            userListenerRegistration.remove();
        }
    }


    private void getDirectionsUrl() {

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

        distance.setText(parseTime);
        riderTime.setText(parsedDistanc);
        double a = Double.parseDouble(parsedDistance);

        double b = a / 1000;

        double c = b * 10;
        DecimalFormat decim = new DecimalFormat("#");
        price2 = Double.parseDouble(decim.format(c));


        DecimalFormat df = new DecimalFormat("#");


        //20% more
        double d = c * 1.2;
        double  pri = Double.parseDouble(decim.format(d));


        //  txtFee.setText("Ksh. " + price2);

        String tripID =  preferenceHelper.getName();


        userListenerRegistration = FirebaseFirestoreApi.db.collection("orders")
                .whereIn("trip_id", Collections.singletonList(tripID))
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
                            assert order != null;

                            if(order.type.equals("Perishable")){
                                String bayo = df.format(pri);
                                fare.setText("Ksh. " + pri);
                                preferenceHelper.putPrice(bayo);
                            }else{
                                String bay = df.format(price2);
                                fare.setText("Ksh. " + price2);
                                preferenceHelper.putPrice(bay);

                            }

                        }
                    }

                });


      //  String totalfare = String.valueOf(bayo);

   //     fare.setText("Ksh. " + bayo);

    }

    private String iStreamToString(InputStream is1) {
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





}
