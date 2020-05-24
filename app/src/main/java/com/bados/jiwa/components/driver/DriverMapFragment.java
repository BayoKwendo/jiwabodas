package com.bados.jiwa.components.driver;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bados.jiwa.FirebaseFirestoreApi;
import com.bados.jiwa.adapters.pushNotification;
import com.bados.jiwa.components.MainActivity;
import com.bados.jiwa.models.Order;
import com.bados.jiwa.models.User;
import com.bados.jiwa.views.Dialog;
import com.bados.jiwa.views.Snackbar;
import com.bados.jiwa.views.status;
import com.bodas.jiwa.R;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.bados.jiwa.parsers.UserParser;
import com.hypertrack.sdk.TrackingError;
import com.bados.jiwa.adapters.OrdersAdapter;
import com.bados.jiwa.components.MapFragment;

import java.util.Collection;
import java.util.Objects;

public class DriverMapFragment extends MapFragment<DriverMapPresenter> implements DriverMapPresenter.DriverView, status {
    private static final String TAG = "DriverMapFragment";

    private Dialog acceptRideDialog;
    User usesr;

    String type = "tokens";


    private static final String AUTH_KEY = "AAAAako3prY:APA91bHAP1yMkExBOW-EYLbim7SUp-wSjOjwEbxzBboBXbEIccuUY2BxEjKCykBMUqmn7612tNyWTNP2vgyI1c7fbqCp5AMlqbdfKwMN94NjDfstJUNYgzy-C4-KLQiCU3inZaZl-jA6";
    private String token;


    private ListenerRegistration userListenerRegistration;

    private Snackbar infoDriverSnackbar;

   User user;
    private TextView t1, t2, t3, t4;

    public static SupportMapFragment newInstance() {
        SupportMapFragment fragment = new DriverMapFragment();


        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater layoutInflater, @Nullable ViewGroup viewGroup, @Nullable Bundle bundle) {
        presenter = new DriverMapPresenter(getActivity(), this);
//        listner = getActivity();

        ((MainActivity) Objects.requireNonNull(getActivity())).setOnButtonListener(this);

        //Toast.makeText(getActivity(), user.role,  Toast.LENGTH_SHORT).show();



        pushNotification job = new pushNotification();
     //   job.execute(token);


        return super.onCreateView(layoutInflater, viewGroup, bundle);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        infoOrderSnackbar.setAction(R.id.get_directions, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Intent i = new Intent().setClass(getContext(), MainActivity.class);
//                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
////                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
//                getActivity().startActivity(i);

                presenter.getDirections();
            }
        });


        ordersAdapter.setOnItemClickListener(new OrdersAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView.Adapter<?> adapter, View view, int position) {
                recyclerView.setVisibility(View.INVISIBLE);
                presenter.selectOrder(ordersAdapter.getOrder(position).id);
            }
        });
        acceptRideDialog = new Dialog(getActivity(), R.layout.dialog_accept_ride)
                .setAction(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        presenter.acceptRide();
                        acceptRideDialog.dismiss();
                        ordersAdapter.clear();
                        ordersAdapter.notifyDataSetChanged();
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                })
                .setAction(R.id.cancel, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        acceptRideDialog.dismiss();
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                });
        infoDriverSnackbar = Snackbar.make(view, R.layout.snackbar_driver_info, Snackbar.LENGTH_INDEFINITE);
    }

    @Override
    public Marker addMarker(Order order) {
        MarkerOptions options = new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.iconmap_marker_filled))
                .title("cmccjcj")
                .snippet("")
                .position(new LatLng(order.pickup.latitude, order.pickup.longitude));

        return mGoogleMap.addMarker(options);

    }





    @Override
    public void onResume() {
        super.onResume();
        // MainActivity.gettingContext(Context context);

    }

    @Override
    public void updateNotifications(Collection<Order> orders) {
        ordersAdapter.addAll(orders);
        ordersAdapter.notifyDataSetChanged();


    }

    @Override
    public void showAcceptRide(Order order) {
        if (order != null) {
            recyclerView.setVisibility(View.INVISIBLE);
            TextView pickupAddress = acceptRideDialog.findViewById(R.id.pickup_address);
            TextView riderName = acceptRideDialog.findViewById(R.id.rider_name);
            pickupAddress.setText(order.pickup.address);
            riderName.setText(order.rider.name);
            acceptRideDialog.show();
        }
    }


    public void updateUser() {


        presenter.updateUser();


    }



    @Override
    public void showDriverInfo(User user) {
        if (user != null) {
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
                                TextView name = infoDriverSnackbar.getView().findViewById(R.id.name);
                                name.setText(usesr.name);
                            }
                        }
                    });

            presenter.addSnackbar(infoDriverSnackbar);
        }
    }


    @Override
    public void dismissDriverInfo() {
        presenter.removeSnackbar(infoDriverSnackbar);
    }

    @Override
    public void showInfoUserStartTripButton() {
        infoOrderSnackbar.setAction(R.id.start_trip, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.startRide();
            }
        });
    }

    @Override
    public void hideInfoUserStartTripButton() {
        infoOrderSnackbar.setAction(R.id.start_trip, null);
    }

    @Override
    public void showInfoUserEndTripButton() {
        infoOrderSnackbar.setAction(R.id.end_trip, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.endRide();
            }
        });
    }

//    @Override
//    public void onAttach(Activity activity) {
//        super.onAttach(activity);
//
//        try {
//            status listner = (status) activity;
//        }
//        catch
//        (Exception e){
//            Toast.makeText(activity, "Error", Toast.LENGTH_SHORT).show();
//            // you should really do something here with this exception
//        }
//    }

//    @Override
//    public void onAttach(@NotNull Context context) {
//        super.onAttach(context);
//        status = (status) context;

    //Your callback initialization here
    //  }

 public void  v (){
        presenter.idtrip();
 }
    public void changeStatus() {
        AlertDialog.Builder alertDialog;
        alertDialog = new AlertDialog.Builder(Objects.requireNonNull(getActivity()));
        alertDialog.setTitle("JOURNEY CHANGE STATUS");
        LayoutInflater inflater = this.getLayoutInflater();
        android.view.View layout_pwd = inflater.inflate(R.layout.layout_order_status, null);
        alertDialog.setView(layout_pwd);
        AlertDialog alert = alertDialog.create();
        t1 = layout_pwd.findViewById(R.id.pickup);
        t2 = layout_pwd.findViewById(R.id.drop_off);
        t3 = layout_pwd.findViewById(R.id.complete);
        t1.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                alert.dismiss();

                AlertDialog dialog = new AlertDialog.Builder(Objects.requireNonNull(getActivity()))
                           .setTitle("Sure??")
                           .setMessage("Do you want to confirm this action?")
                           .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                presenter.pick();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setCancelable(true)
                        .show();


                Button btnPositive = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
                Button btnNegative = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE);

                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) btnPositive.getLayoutParams();
                layoutParams.weight = 10;

                btnPositive.setLayoutParams(layoutParams);
                btnNegative.setLayoutParams(layoutParams);
                btnPositive.setTextColor(getResources().getColor(R.color.colorAccent));
                btnNegative.setTextColor(getResources().getColor(R.color.error));

            }

        });

        t2.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                alert.dismiss();

                AlertDialog dialog = new AlertDialog.Builder(Objects.requireNonNull(getActivity()))
                        .setTitle("Sure??")
                        .setMessage("Do you want to confirm this action?")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                presenter.drop();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setCancelable(true)
                        .show();


                Button btnPositive = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
                Button btnNegative = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE);

                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) btnPositive.getLayoutParams();
                layoutParams.weight = 10;

                btnPositive.setLayoutParams(layoutParams);
                btnNegative.setLayoutParams(layoutParams);
                btnPositive.setTextColor(getResources().getColor(R.color.colorAccent));
                btnNegative.setTextColor(getResources().getColor(R.color.error));
            }
        });
        t3.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                alert.dismiss();


                AlertDialog dialog = new AlertDialog.Builder(Objects.requireNonNull(getActivity()))
                        .setTitle("Sure??")
                        .setMessage("Do you want to confirm this action?")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                presenter.complete();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setCancelable(true)
                        .show();


                Button btnPositive = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
                Button btnNegative = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE);

                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) btnPositive.getLayoutParams();
                layoutParams.weight = 10;

                btnPositive.setLayoutParams(layoutParams);
                btnNegative.setLayoutParams(layoutParams);
                btnPositive.setTextColor(getResources().getColor(R.color.colorAccent));
                btnNegative.setTextColor(getResources().getColor(R.color.error));

            }

        });
        Button dismissButton = (Button) layout_pwd.findViewById(R.id.cancel);
        dismissButton.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                alert.dismiss();
            }
        });

        alertDialog.setView(layout_pwd);

        alert.show();
    }

    @Override
    public void hideInfoUserEndTripButton() {
        infoOrderSnackbar.setAction(R.id.end_trip, null);

    }

    @Override
    public void onError(TrackingError trackingError) {

    }

    @Override
    public void onTrackingStart() {

    }

    @Override
    public void onTrackingStop() {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (userListenerRegistration != null) {
            userListenerRegistration.remove();
        }

    }


}
