package com.bados.jiwa;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.bados.jiwa.models.User;
import com.bados.jiwa.utils.HyperTrackUtils;
import com.bodas.jiwa.R;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.gson.Gson;
import com.bados.jiwa.helpers.PromptPopUpView;
import com.hypertrack.sdk.HyperTrack;
import com.bados.jiwa.components.MainActivity;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RegistrationActivity extends AppCompatActivity {
    private static final String TAG = "RegistrationActivity";
    private ProgressDialog mProgress;
    private PromptPopUpView promptPopUpView;
    private Gson gson = new Gson();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Saving...");
        mProgress.setCancelable(false);




        String json = MySharedPreferences.get(this).getString(MySharedPreferences.USER_KEY, "{}");
        User user = gson.fromJson(json, User.class);
        next(user);
    }

    public void next(final User user) {

  if (TextUtils.isEmpty(user.role)) {
            if (getPackageName().contains(User.USER_ROLE_DRIVER)) {
                user.role = User.USER_ROLE_DRIVER;
                next(user);
            } else if (getPackageName().contains(User.USER_ROLE_RIDER)) {
                user.role = User.USER_ROLE_RIDER;
                next(user);
            } else {
                addFragment(RoleRegistrationFragment.newInstance(user));
            }
        } else if (TextUtils.isEmpty(user.name)) {
            addFragment(NameRegistrationFragment.newInstance(user));
        } else if (TextUtils.isEmpty(user.id)) {
            mProgress.show();

            if (User.USER_ROLE_DRIVER.equals(user.role)) {
                HyperTrack hyperTrack = HyperTrack.getInstance(this, HyperTrackUtils.getPubKey(this));
                hyperTrack.setDeviceName(user.name);
                Map<String, Object> metadata = new HashMap<>();

                metadata.put("userID", user.id);
                metadata.put("name", user.name);
                metadata.put("phone_number", user.phoneNumber);
                Map<String, Object> car = new HashMap<>();
                car.put("model", user.car.model);
                car.put("license_plate", user.car.licensePlate);
                metadata.put("car", car);
                hyperTrack.setDeviceMetadata(metadata);

                user.deviceId = hyperTrack.getDeviceID();
            }
            FirebaseFirestoreApi.createUser(user)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            mProgress.dismiss();

                            PromptPopUpView promptPopUpView = new PromptPopUpView(RegistrationActivity.this);
                            promptPopUpView.changeStatus(2, "Successful Registered");
                            AlertDialog dialog = new AlertDialog.Builder(RegistrationActivity.this)
                                    .setPositiveButton("PROCESSING..", (dialogInterface, i) -> finish())
                                    .setCancelable(false)
                                    .setView(promptPopUpView)
                                    .show();
                            new Handler().postDelayed(new Runnable() {
                                public void run() {
                                    Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                                    user.id = documentReference.getId();
                                    next(user);
                                    dialog.dismiss();
                                }
                            }, 4000);
                            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                            button.setEnabled(false);
                            button.setTextColor(getResources().getColor(R.color.black));

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            network();
                            mProgress.dismiss();
                            Log.w(TAG, "Error adding document", e);
                        }
                    });
        } else {
            try {
                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writeValueAsString(user);
                MySharedPreferences.get(this).edit()
                        .putString(MySharedPreferences.USER_KEY, json)
                        .apply();
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void addFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_frame, fragment);
        transaction.commitAllowingStateLoss();
    }


    private void network(){
        promptPopUpView = new PromptPopUpView(this);

        promptPopUpView.changeStatus(1, "Error");


        AlertDialog dialog = new AlertDialog.Builder(Objects.requireNonNull(this))
                .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setCancelable(false)
                .setView(promptPopUpView)
                .show();

        Button btnPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) btnPositive.getLayoutParams();
        layoutParams.weight = 10;

        btnPositive.setLayoutParams(layoutParams);
        btnPositive.setTextColor(getResources().getColor(R.color.error));
    }
}
