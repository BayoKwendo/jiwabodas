package com.bados.jiwa.components;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.bados.jiwa.FirebaseFirestoreApi;
import com.bados.jiwa.MySharedPreferences;
import com.bados.jiwa.PermissionsFragment;
import com.bados.jiwa.models.User;
import com.bados.jiwa.views.status;
import com.bodas.jiwa.R;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.bados.jiwa.components.driver.DriverMapFragment;
import com.bados.jiwa.components.rider.RiderMapFragment;
import com.bados.jiwa.parsers.UserParser;
import com.rengwuxian.materialedittext.MaterialEditText;

import de.hdodenhof.circleimageview.CircleImageView;
import dmax.dialog.SpotsDialog;

public class MainActivity extends FragmentActivity implements
        NavigationView.OnNavigationItemSelectedListener {

    public static final int REQUEST_ACCESS_FINE_LOCATION = 10;

    public static int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 5469;


    public static final String GOOGLE_API_KEY = "AIzaSyDJnDn_O0nybXXsBypZ_5wTu1lVXOgHIjU";

    private User user;
    private Fragment fragment;
    private TextView tvName;
    protected status listener;

    private Toolbar toolbar;
    private ListenerRegistration userListenerRegistration;



    MaterialEditText etName;
    MaterialEditText etPhone;

    boolean doubleBackToExitPressedOnce = false;


    FloatingActionButton fab;
    private DriverMapFragment driverMapFragment;


    public void setOnButtonListener(status actionButtonListener)
    {
        this.listener = actionButtonListener;


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        statusCheck();
        checkPermission();
        ObjectMapper mapper = new ObjectMapper();
        String json = MySharedPreferences.get(this).getString(MySharedPreferences.USER_KEY, "");
        try {
            user = mapper.readValue(json, User.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        if (user == null) return;

        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            startMap();


            // toolbar = (Toolbar) findViewById(R.id.toolbar);

        } else {

            startPermissionsRequest();
        }


    }

    private void startMap() {


        fragment = user.role.equals("driver") ?
                DriverMapFragment.newInstance() : RiderMapFragment.newInstance();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();



        transaction.replace(R.id.fragment_frame, fragment);

        transaction.commit();

        initDrawer();

    }

    private void startPermissionsRequest() {
        Fragment fragment = new PermissionsFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_frame, fragment);
        transaction.commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_ACCESS_FINE_LOCATION) {

            if ((permissions.length == 1 &&
                    permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION) &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    || User.USER_ROLE_RIDER.equals(user.role)) {
                startMap();
            } else {
                AlertDialog alertDialog = new AlertDialog.Builder(this)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.open, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.parse("package:" + getPackageName());
                                intent.setData(uri);
                                startActivityForResult(intent, REQUEST_ACCESS_FINE_LOCATION);
                            }
                        })
                        .setTitle(R.string.app_settings)
                        .setMessage(R.string.you_can_allow)
                        .create();
                alertDialog.show();
            }
        }
    }


    public void statusCheck() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();

        }
    }


    private void buildAlertMessageNoGps() {

        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("GPS");
        alertDialog.setCancelable(false);
        alertDialog.setMessage(getResources().getString(R.string.gps_network_not_enabled));
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(i);
                        Toast.makeText(MainActivity.this, "Restart app after enabling GPS", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //Nothing to do here
                        Toast.makeText(MainActivity.this, "Enable GPS to allow app to function", Toast.LENGTH_SHORT).show();
                    }
                });
        alertDialog.show();

        Button btnPositive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button btnNegative = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) btnPositive.getLayoutParams();

        layoutParams.weight = 10;

        btnPositive.setLayoutParams(layoutParams);

        btnPositive.setTextColor(getResources().getColor(R.color.colorPrimaryDark));

        btnNegative.setLayoutParams(layoutParams);


    }


    public void initDrawer() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();


        fab = findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("WrongConstant")
            @Override
            public void onClick(View view) {
                // If the navigation drawer is not open then open it, if its already open then close it.
                if (!drawer.isDrawerOpen(GravityCompat.START)) drawer.openDrawer(Gravity.START);
                else drawer.closeDrawer(Gravity.END);

            }
        });


        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(MainActivity.this);

        View navigationHeaderView = navigationView.getHeaderView(0);
        tvName = (TextView) navigationHeaderView.findViewById(R.id.tvRiderName);


        Menu menu = navigationView.getMenu();

        if (user.role.equals("driver")) {
            for (int menuItemIndex = 0; menuItemIndex < menu.size(); menuItemIndex++) {
                MenuItem menuItem = menu.getItem(menuItemIndex);

                if (menuItem.getItemId() == R.id.status) {
                    menuItem.setVisible(true);
                }
            }
        }

        tvName.setText(user.name);
        TextView tvStars = (TextView) findViewById(R.id.tvStars);
        CircleImageView imageAvatar = (CircleImageView) navigationHeaderView.findViewById(R.id.imgAvatar);

//        tvName.setText(Common.currentUser.getName());
//        if(Common.currentUser.getRates()!=null &&
//                !TextUtils.isEmpty(Common.currentUser.getRates()))
//            tvStars.setText(Common.currentUser.getRates());
//
//        if(isLoggedInFacebook)
//            Picasso.get().load("https://graph.facebook.com/" + Common.userID + "/picture?width=500&height=500").into(imageAvatar);
//        else if(account!=null)
//            Picasso.get().load(account.getPhotoUrl()).into(imageAvatar);
//        if(Common.currentUser.getAvatarUrl()!=null &&
//                !TextUtils.isEmpty(Common.currentUser.getAvatarUrl()))
//            Picasso.get().load(Common.currentUser.getAvatarUrl()).into(imageAvatar);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {

            if (doubleBackToExitPressedOnce) {
                super.onBackPressed();
            } else {
                this.doubleBackToExitPressedOnce = true;
                Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();
                new Handler().postDelayed(() -> {
                    doubleBackToExitPressedOnce = false;
                }, 2000);

            }
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_trip_history:
                showTripHistory();
                break;
            case R.id.status:
                listener.changeStatus();
                break;
            case R.id.nav_updateInformation:
                showDialogUpdateInfo(user);;
            break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showTripHistory() {
//        Toast.makeText(this, "DONE", Toast.LENGTH_SHORT).show();
        Intent intent=new Intent(MainActivity.this, TripHistory.class);
        startActivity(intent);
    }






    private void subscribeDriverUpdates() {

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
                            //Log.w(TAG, "Listen failed.", e);
                            return;
                        }

                        if (value != null) {
                            UserParser parser = new UserParser();
                            for (QueryDocumentSnapshot doc : value) {
                                    user = parser.parse(doc);

                                assert user != null;
                                }


                        }

                    }

                });
    }

    private void showDialogUpdateInfo(User user) {
        subscribeDriverUpdates();
        AlertDialog.Builder alertDialog;
        alertDialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog.setTitle("UPDATE INFORMATION");
        LayoutInflater inflater = this.getLayoutInflater();
        View layout_pwd = inflater.inflate(R.layout.layout_update_information, null);
        alertDialog.setView(layout_pwd);
        AlertDialog alert = alertDialog.create();
        etName = (MaterialEditText) layout_pwd.findViewById(R.id.etName);
        etPhone = (MaterialEditText) layout_pwd.findViewById(R.id.etPhone);
        final ImageView image_upload = (ImageView) layout_pwd.findViewById(R.id.imageUpload);


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
                                User user = parser.parse(doc);
                                etName.setText(user.name);
                                etPhone.setText(user.phoneNumber);
                            }
                        }
                    }
                });
        etName.requestFocus();
        Button updateButton = (Button) layout_pwd.findViewById(R.id.update);
        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final android.app.AlertDialog waitingDialog = new SpotsDialog(MainActivity.this);
                waitingDialog.show();
                user.name = etName.getText().toString();
                user.phoneNumber = etPhone.getText().toString();

                DocumentReference washingtonRef = FirebaseFirestoreApi.db.collection("users").document(user.id);
                washingtonRef
                        .update("name", user.name)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {


                                washingtonRef
                                        .update("phone_number", user.phoneNumber)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Toast.makeText(MainActivity.this, "Information Updated!", Toast.LENGTH_SHORT).show();
                                                waitingDialog.dismiss();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                waitingDialog.dismiss();
                                                alert.dismiss();
                                                Toast.makeText(MainActivity.this, "Error updating name", Toast.LENGTH_SHORT).show();
                                               // Log.w(TAG, "Error updating document", e);
                                            }
                                        });

                                alert.dismiss();

                                Toast.makeText(MainActivity.this, "Information Updated!", Toast.LENGTH_SHORT).show();
                                waitingDialog.dismiss();

                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                waitingDialog.dismiss();
                                Toast.makeText(MainActivity.this, "Error updating name", Toast.LENGTH_SHORT).show();
                            }
                        });
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

    public void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
            }
        }
    }
    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE) {
            if (!Settings.canDrawOverlays(this)) {
                // You don't have permission
                checkPermission();
            } else {
                // Do as per your logic
            }

        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
            if (userListenerRegistration != null) {
                userListenerRegistration.remove();

            }

         }

}


