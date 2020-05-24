package com.bados.jiwa;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.bodas.jiwa.R;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;

import com.bados.jiwa.components.MainActivity;
import com.bados.jiwa.helpers.PromptPopUpView;

import java.util.Arrays;
import java.util.Objects;

import static android.content.ContentValues.TAG;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class LauncherActivity extends Activity{

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 1000;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private View mControlsView;
    private LinearLayout mProgressLayout;
    boolean doubleBackToExitPressedOnce = false;
    private static final int RC_SIGN_IN = 123;
    private PromptPopUpView promptPopUpView;

    private Context ctx = null;


    private View text;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Show the system bar
//            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

            // Delayed display of UI elements
//            mControlsView.setVisibility(View.VISIBLE);
//            YoYo.with(Techniques.ZoomInRight)
//                    .duration(1200)
//                    .pivot(YoYo.CENTER_PIVOT, YoYo.CENTER_PIVOT)
//                    .interpolate(new AccelerateDecelerateInterpolator())
//                    .playOn(text);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);



        mProgressLayout = findViewById(R.id.layout_progress);


        if (!isNetworkAvailable()){
            network();
        }else {
            if (MySharedPreferences.get(this).contains(MySharedPreferences.USER_KEY)) {
                start(MainActivity.class);
            } else {

                startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setTheme(R.style.LoginTheme)
                                .setTosAndPrivacyPolicyUrls("https://urls", "https://urls")
                                .setAvailableProviders(
                                        Arrays.asList(
                                                new AuthUI.IdpConfig.PhoneBuilder().build()
                                        ))
                                .build(),
                        RC_SIGN_IN);
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // created, to briefly hint to the user that UI controls
        // are available.
        //hide();
    }

    private void start(Class<?> cls) {

        startActivity(new Intent(this, cls));
        finish();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // RC_SIGN_IN is the request code you passed into startActivityForResult(...) when starting the sign in flow.
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            // Successfully signed in
            if (resultCode == RESULT_OK) {
                startActivity(new Intent(LauncherActivity.this, RegistrationActivity.class));
                Toast.makeText(this, "OTP verification success", Toast.LENGTH_SHORT).show();
            } else {
                // Sign in failed
                if (response == null) {
                    finishAffinity();
//
//                    if (doubleBackToExitPressedOnce) {
//                        super.onBackPressed();
//                    } else {
//                        this.doubleBackToExitPressedOnce = true;
//                        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();
//
//
//                        new Handler().postDelayed(() -> {
//                            doubleBackToExitPressedOnce=false;
//                        }, 2000);
//
//
//                    }

                    return;
                }

                if (response.getError().getErrorCode() == ErrorCodes.NO_NETWORK) {
                    Toast.makeText(this, "No Internet Connection", Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.e(TAG, "Sign-in error: ", response.getError());
                Toast.makeText(this, "Unknown sign in response", Toast.LENGTH_SHORT).show();

            }
         }



        }


    private void network(){
        promptPopUpView = new PromptPopUpView(this);

        promptPopUpView.changeStatus(1, "Network Error");


        AlertDialog dialog = new AlertDialog.Builder(Objects.requireNonNull(this))
                .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        recreate();
                    }
                })
                .setNegativeButton("Exit", (dialog12, which) -> finish())
                .setCancelable(false)
                .setView(promptPopUpView)
                .show();

        Button btnPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button btnNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) btnPositive.getLayoutParams();
        layoutParams.weight = 10;

        btnPositive.setLayoutParams(layoutParams);
        btnNegative.setLayoutParams(layoutParams);
        btnPositive.setTextColor(getResources().getColor(R.color.colorAccent));
        btnNegative.setTextColor(getResources().getColor(R.color.error));
    }


    private boolean isNetworkAvailable() {
        // Using ConnectivityManager to check for Network Connection
        ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        assert connectivityManager != null;
        NetworkInfo activeNetworkInfo = connectivityManager
                .getActiveNetworkInfo();
        return activeNetworkInfo != null;
    }


}
