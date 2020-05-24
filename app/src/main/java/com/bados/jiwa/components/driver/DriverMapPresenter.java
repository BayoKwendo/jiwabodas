package com.bados.jiwa.components.driver;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Point;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.bados.jiwa.helpers.PreferenceHelper;
import com.bados.jiwa.models.Order;
import com.bados.jiwa.models.User;
import com.bados.jiwa.utils.TextFormatUtils;
import com.bodas.jiwa.R;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.bados.jiwa.components.MapPresenter;
import com.bados.jiwa.parsers.OrderParser;
import com.bados.jiwa.parsers.UserParser;
import com.bados.jiwa.tracking.TrackingPresenter;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dmax.dialog.SpotsDialog;


@SuppressWarnings("WeakerAccess")
public class DriverMapPresenter extends MapPresenter<DriverMapPresenter.DriverView, DriverMapPresenter.DriverState> {
    private static final String TAG = "DriverMapPresenter";

    private static final int ANIMATE_CAMERA_DURATION = 500;

    private final TrackingPresenter trackingPresenter;
    private final int mapCenterOffset;
    private PreferenceHelper preferenceHelper;
    MaterialEditText etName;
    MaterialEditText etPhone;



    private ListenerRegistration newOrderListenerRegistration , userListener;

    public DriverMapPresenter(Context context, DriverMapPresenter.DriverView view) {
        super(context, view, new DriverState(context));
        preferenceHelper = new PreferenceHelper(context);

        Resources r = context.getResources();
        float paddingTop = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, r.getDisplayMetrics());
        float paddingBottom = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 650, r.getDisplayMetrics());
        mapCenterOffset = (int) ((paddingBottom - paddingTop) / 2);

        trackingPresenter = new TrackingPresenter(context, view);
    }

    @Override
    public void map(@NonNull GoogleMap googleMap) {
        super.map(googleMap);



        Log.d(TAG, "Device id: " + mState.getUser().deviceId);
        hyperTrackViews.subscribeToDeviceUpdates(mState.getUser().deviceId, this);
        hyperTrackMap.bind(hyperTrackViews, mState.getUser().deviceId);
        hyperTrackMap.adapter().addTripFilter(this);

        preferenceHelper.putRole(mState.getUser().role);





        if (mState.getOrder() == null) {
            subscribeNewOrderUpdates();
            mView.showDriverInfo(mState.getUser());
           // Toast.makeText(mContext, "FONEEE", Toast.LENGTH_SHORT).show();
        }else {
            preferenceHelper.putDriverId(mState.getOrder().driver.id);
            preferenceHelper.putStatus(mState.getOrder().status);
        }


        hyperTrackMap.adapter().notifyDataSetChanged();

    }

    @Override
    protected boolean onMarkerClick(Marker marker) {
        if (marker.getTag() instanceof String) {
            String tag = (String) marker.getTag();
            if (mState.newOrders.containsKey(tag)) {
                selectOrder(tag);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onOrderChanged() {
        super.onOrderChanged();

        if (mState.getOrder() != null) {

            if (!TextUtils.isEmpty(mState.getOrder().tripId)) {
                hyperTrackMap.subscribeTrip(mState.getOrder().tripId);
            }
            mView.dismissDriverInfo();
            if (!Order.COMPLETED.equals(mState.getOrder().status)) {

                mView.showOrderInfo(mState.getOrder().rider, TextFormatUtils.getDestinationName(mContext, mState.getOrder()));
                mView.hideInfoUserStartTripButton();
                mView.hideInfoUserEndTripButton();
                if (Order.REACHED_PICKUP.equals(mState.getOrder().status)) {
                    mView.showInfoUserStartTripButton();
                }

                else if (Order.REACHED_DROPOFF.equals(mState.getOrder().status)) {
                    mView.showInfoUserEndTripButton();
                }
                mView.showUI();
            } else {
                mView.dismissOrderInfo();
            }
        } else {
            mView.dismissOrderInfo();
            mView.showDriverInfo(mState.getUser());
            subscribeNewOrderUpdates();
        }

        hyperTrackMap.adapter().notifyDataSetChanged();
    }

    public void selectOrder(String orderId) {
        mState.selectedOrder = mState.newOrders.get(orderId);
        if (mState.selectedOrder != null) {

            mView.showAcceptRide(mState.selectedOrder);
            moveToSelectedOrder(new GoogleMap.CancelableCallback() {
                @Override
                public void onFinish() {
                    mView.showAcceptRide(mState.selectedOrder);
                }

                @Override
                public void onCancel() {
                }
            });
        }
    }

    private void moveToSelectedOrder(GoogleMap.CancelableCallback callback) {
        if (googleMap != null && mState.selectedOrder != null) {
            Point locationInPx = googleMap.getProjection().toScreenLocation(mState.selectedOrder.pickup.getLatLng());
            locationInPx.y = locationInPx.y + mapCenterOffset;
            LatLng latLng = googleMap.getProjection().fromScreenLocation(locationInPx);
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(latLng);
            googleMap.animateCamera(cameraUpdate, ANIMATE_CAMERA_DURATION, callback);
        }
    }

    public void acceptRide() {


        if (mState.selectedOrder != null) {
            mView.showProgressBar();

            ObjectMapper mapper = new ObjectMapper();
            DocumentReference washingtonRef = db.collection("orders").document(mState.selectedOrder.id);
            washingtonRef
                    .update(
                            "status", Order.ACCEPTED,
                            "driver", mapper.convertValue(mState.getUser(), Map.class)
                    )
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mView.hideProgressBar();

                            if (newOrderListenerRegistration != null) {
                                newOrderListenerRegistration.remove();
                            }
                            mState.newOrders.remove(mState.selectedOrder.id);
                            mState.selectedOrder.marker.remove();

                            mState.updateOrder(mState.selectedOrder);
                            subscribeOrderUpdates();
                            Log.d(TAG, "DocumentSnapshot successfully updated!");
                            trackingPresenter.adjustTrackingState();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            mView.hideProgressBar();
                            Log.w(TAG, "Error updating document", e);
                        }
                    });
        }
    }

    public void startRide() {
        if (mState.getOrder() != null) {
            mView.showProgressBar();

            DocumentReference washingtonRef = db.collection("orders").document(mState.getOrder().id);
            washingtonRef
                    .update("status", Order.STARTED_RIDE)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mView.hideProgressBar();
                            Log.d(TAG, "DocumentSnapshot successfully updated!");
                            trackingPresenter.adjustTrackingState();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            mView.hideProgressBar();
                            Log.w(TAG, "Error updating document", e);
                        }
                    });
        }
    }

    public void endRide() {
        if (mState.getOrder() != null) {


            mView.showProgressBar();
            DocumentReference washingtonRef = db.collection("orders").document(mState.getOrder().id);
            washingtonRef
                    .update("status", Order.COMPLETED)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mView.hideProgressBar();


                            Log.d(TAG, "DocumentSnapshot successfully updated!");
                            trackingPresenter.adjustTrackingState();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            mView.hideProgressBar();
                            Log.w(TAG, "Error updating document", e);
                        }
                    });
        }
    }

    public void getDirections() {
        if (mState.getOrder() != null) {
            Uri gmmIntentUri;
            if (Order.PICKING_UP.equals(mState.getOrder().status)) {
                gmmIntentUri = Uri.parse("google.navigation:q="
                        + mState.getOrder().pickup.latitude + "," + mState.getOrder().pickup.longitude);
            } else {
                gmmIntentUri = Uri.parse("google.navigation:q="
                        + mState.getOrder().dropoff.latitude + "," + mState.getOrder().dropoff.longitude);
            }
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);// For android 9 and 10s
            mapIntent.setPackage("com.google.android.apps.maps");

            mContext.startActivity(mapIntent);
        }
    }

    private void subscribeNewOrderUpdates() {
        if (newOrderListenerRegistration != null) {
            newOrderListenerRegistration.remove();
        }
        newOrderListenerRegistration = db.collection("orders")
                .whereIn("status", Arrays.asList(Order.NEW, Order.CANCELLED))
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @SuppressWarnings("ConstantConditions")
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            return;
                        }

                        if (value != null) {
                            List<Order> orders = new ArrayList<>();
                            OrderParser parser = new OrderParser();
                            for (QueryDocumentSnapshot doc : value) {
                                if (Order.CANCELLED.equals(doc.getString("status"))) {
                                    if (mState.newOrders.containsKey(doc.getId())) {
                                        mState.newOrders.remove(doc.getId()).marker.remove();
                                    }
                                    continue;
                                }

                                Order order = parser.parse(doc);
                                orders.add(order);
                            }
                            updateNewOrders(orders);
                        }
                    }
                });
    }

    private void updateNewOrders(List<Order> orders) {
        if (orders.isEmpty()) {
            return;
        }

        if (mState.newOrders.isEmpty()) {
            for (Order order : orders) {
                order.marker = mView.addMarker(order);
                order.marker.setTag(order.id);
                mState.newOrders.put(order.id, order);
            }

            mView.updateNotifications(orders);


//            updateMapCamera();
        } else {
            List<Order> newOrders = new ArrayList<>();
            for (Order order : orders) {

                Order oldOrder = mState.newOrders.get(order.id);
                if (oldOrder == null) {
                    order.marker = mView.addMarker(order);
                    order.marker.setTag(order.id);
                    mState.newOrders.put(order.id, order);
                    newOrders.add(order);
                } else {
                    oldOrder.update(order);
                    oldOrder.marker.setPosition(
                            new LatLng(order.pickup.latitude, order.pickup.longitude)

                    );
                }
            }

            mView.updateNotifications(newOrders);
        }
    }

    private void updateMapCamera() {
        if (!mState.newOrders.isEmpty()) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            if (mState.currentLocation != null) {
                builder.include(
                        new LatLng(mState.currentLocation.getLatitude(), mState.currentLocation.getLongitude())
                );
            }
            for (Order order : mState.newOrders.values()) {
                builder.include(order.pickup.getLatLng());
            }
            animateCamera(builder.build());
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (newOrderListenerRegistration != null) {
            newOrderListenerRegistration.remove();
        }

        if (userListener != null) {
            userListener.remove();
        }
        trackingPresenter.destroy();
    }

    public interface DriverView extends MapPresenter.View, TrackingPresenter.View {

        Marker addMarker(Order order);

        void updateNotifications(Collection<Order> orders);

        void showAcceptRide(Order order);

        void showDriverInfo(User user);

        void dismissDriverInfo();

        void showInfoUserStartTripButton();

        void hideInfoUserStartTripButton();

        void showInfoUserEndTripButton();

        void hideInfoUserEndTripButton();

    }

    public static class DriverState extends MapPresenter.State {
        public Map<String, Order> newOrders = new HashMap<>();
        public Order selectedOrder;

        public DriverState(Context context) {
            super(context);
        }
    }

    public void pick() {
        if (mState.getOrder() != null) {
            mView.showProgressBar();
            DocumentReference washingtonRef = db.collection("orders").document(mState.getOrder().id);
            washingtonRef
                    .update("status", Order.REACHED_PICKUP)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mView.hideProgressBar();
                            Toast.makeText(mContext, "Status successfully updated!", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "DocumentSnapshot successfully updated!");
                            trackingPresenter.adjustTrackingState();
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

    public void drop() {
        if (mState.getOrder() != null) {
            mView.showProgressBar();
            DocumentReference washingtonRef = db.collection("orders").document(mState.getOrder().id);
            washingtonRef
                    .update("status", Order.REACHED_DROPOFF)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mView.hideProgressBar();
                            Toast.makeText(mContext, "Status successfully updated!", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "DocumentSnapshot successfully updated!");
                            trackingPresenter.adjustTrackingState();

                            preferenceHelper.putName(mState.getOrder().tripId);
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


    void idtrip(){

        if (mState.getOrder() != null) {
            String tripID = mState.getTrip().getTripId();
        }

    }

    public void complete() {
        if (mState.getOrder() != null) {
            mView.showProgressBar();
            DocumentReference washingtonRef = db.collection("orders").document(mState.getOrder().id);
            washingtonRef
                    .update("status", Order.COMPLETED)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mView.hideProgressBar();
                            preferenceHelper.putName(mState.getOrder().tripId);



                            Toast.makeText(mContext, "Status successfully updated!", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "DocumentSnapshot successfully updated!");
                            trackingPresenter.adjustTrackingState();
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


    public void updateUser() {
        if (userListener != null) {
            userListener.remove();
        }
        userListener = db.collection("users")
                .whereIn( "name", Collections.singletonList(mState.getUser().name))
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @SuppressWarnings("ConstantConditions")
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            return;
                        }

                        if (value != null) {
                            List<User> users = new ArrayList<>();
                            UserParser parser = new UserParser();
                            for (QueryDocumentSnapshot doc : value) {
//                                if (User.USER_ROLE_RIDER.CANCELLED.equals(doc.getString("status"))) {
//                                    if (mState.newOrders.containsKey(doc.getId())) {
//                                        mState.newOrders.remove(doc.getId()).marker.remove();
//                                    }
//                                    continue;
//                                }

                                User user = parser.parse(doc);
                                users.add(user);
                            }
                            updateNewUsers(users);
                        }
                    }
                });
    }


    private void updateNewUsers (List<User> users) {

        if (users.isEmpty()) {
            return;
        }

        AlertDialog.Builder alertDialog;
        alertDialog = new AlertDialog.Builder(mContext, R.style.AlertDialogTheme);
        alertDialog.setTitle("UPDATE INFORMATION");
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        android.view.View layout_pwd = inflater.inflate(R.layout.layout_update_information, null);
        alertDialog.setView(layout_pwd);
        AlertDialog alert = alertDialog.create();
        etName = (MaterialEditText) layout_pwd.findViewById(R.id.etName);
        etPhone = (MaterialEditText) layout_pwd.findViewById(R.id.etPhone);
        etName.setText(mState.getUser().name);
        etName.requestFocus();
        etPhone.setText(mState.getUser().phoneNumber);
        Button updateButton = (Button) layout_pwd.findViewById(R.id.update);
        updateButton.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                final android.app.AlertDialog waitingDialog = new SpotsDialog(mContext, R.style.AlertDialogTheme);
                waitingDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

                waitingDialog.show();


                String name = etName.getText().toString();
                String phoneNumber = etPhone.getText().toString();
                if (mState.getUser() != null) {
                    mView.showProgressBar();
                    DocumentReference washingtonRef = db.collection("users").document(mState.getUser().id);
                    washingtonRef
                            .update("name", name)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {

                                    washingtonRef
                                            .update("phone_number", phoneNumber)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    if (newOrderListenerRegistration != null) {
                                                        newOrderListenerRegistration.remove();
                                                    }
                                                    mView.showDriverInfo(mState.getUser());

                                                    mView.hideProgressBar();
                                                    Toast.makeText(mContext, "Information Updated!", Toast.LENGTH_SHORT).show();
                                                    waitingDialog.dismiss();
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    mView.hideProgressBar();
                                                    waitingDialog.dismiss();
                                                    alert.dismiss();
                                                    Toast.makeText(mContext, "Error updating name", Toast.LENGTH_SHORT).show();
                                                    Log.w(TAG, "Error updating document", e);
                                                }
                                            });
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    mView.hideProgressBar();
                                    waitingDialog.dismiss();
                                    alert.dismiss();
                                    Toast.makeText(mContext, "Error updating name", Toast.LENGTH_SHORT).show();
                                    Log.w(TAG, "Error updating document", e);
                                }
                            });

                }
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

        alert.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

        alert.show();

    }



}
