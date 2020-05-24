package com.bados.jiwa.components.mpesa;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;

import android.os.Handler;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.androidstudy.daraja.Daraja;
import com.androidstudy.daraja.DarajaListener;
import com.androidstudy.daraja.model.AccessToken;
import com.androidstudy.daraja.model.LNMExpress;
import com.androidstudy.daraja.model.LNMResult;
import com.androidstudy.daraja.util.TransactionType;
import com.bados.jiwa.FirebaseFirestoreApi;
import com.bados.jiwa.RegistrationActivity;
import com.bados.jiwa.helpers.PreferenceHelper;
import com.bados.jiwa.helpers.PromptPopUpView;
import com.bados.jiwa.models.Order;
import com.bados.jiwa.parsers.UserParser;
import com.bodas.jiwa.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

import static com.bados.jiwa.FirebaseFirestoreApi.db;

public class MPESAExpressActivity extends AppCompatActivity {

    @BindView(R.id.editTextPhoneNumber)
    EditText editTextPhoneNumber;
    @BindView(R.id.sendButton)
    Button sendButton;
    String p;
    AlertDialog dialog;

    Button send, btn_back, confirmPay;
    EditText phone, mpesa_code;
    ConstraintLayout constraintLayout, constraintLayout2;

    //ProgressDialog dialog;

    private PromptPopUpView promptPopUpView;


    private static final String OPTIONAL_ZERO = "(0";
    private static final String OPTIONAL_ZERO_REGEX = Pattern.quote(OPTIONAL_ZERO);

    Order order;

    private ProgressDialog mProgressDialog, mProgress;

    //Declare Daraja :: Global Variable
    Daraja daraja;
    private PreferenceHelper preferenceHelper;


    String phoneNumber;
    private boolean Execute;

    String price, docid;
    private ListenerRegistration userListenerRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mpesa);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowHomeEnabled(true);


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ButterKnife.bind(this);

        Execute = false;

        preferenceHelper = new PreferenceHelper(this);
        price = preferenceHelper.getPrice();
        docid = preferenceHelper.getPay();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        send = findViewById(R.id.send);
        mpesa_code = findViewById(R.id.mpesa_code);
        btn_back = findViewById(R.id.btn_back);
        confirmPay = findViewById(R.id.btn_confirm);

        constraintLayout = findViewById(R.id.view1);

        constraintLayout2 = findViewById(R.id.view2);
        mProgress = new ProgressDialog(MPESAExpressActivity.this);
        mProgress.setMessage("Please wait...");
        mProgress.setCancelable(true);


        btn_back.setOnClickListener(view -> {
            constraintLayout.setVisibility(View.VISIBLE);
            constraintLayout2.setVisibility(View.GONE);

        });

        confirmPay.setOnClickListener(view -> {
            if (!isNetworkAvailable()) {
                network();
            }
            phoneNumber = editTextPhoneNumber.getText().toString().trim();
            if (TextUtils.isEmpty(phoneNumber)) {
                editTextPhoneNumber.requestFocus();
                editTextPhoneNumber.setError("Re-Enter Phone No. to confirm");
                return;
            }
            mProgress.show();
            if (!isNetworkAvailable()) {
                network();
            }
            p = phoneNumber.replaceFirst("^0+(?!$)", "");
            getJSON("https://youthsofhope.co.ke/mpesa/read.php?phone=" + 254 + p);
        });

        if (!isNetworkAvailable()) {
            network();
        }

        send.setOnClickListener(view -> {
            String transaction = mpesa_code.getText().toString();
            int length = mpesa_code.getText().length();
            if (transaction.isEmpty()) {
                mpesa_code.setError("Transaction code Require");
                mProgress.dismiss();

            } else if (length < 10 || length > 10) {
                mpesa_code.setError("Code not Valid");
                mProgress.dismiss();

            } else {
                enterTransaction();
            }
        });

        //Init Daraja
        //TODO :: REPLACE WITH YOUR OWN CREDENTIALS  :: THIS IS SANDBOX DEMO
        daraja = Daraja.with("A6TfG95zr2R8unedQreSwmOAeQogAm3O", "YgApp0EDr3YhA1dh", new DarajaListener<AccessToken>() {
            @Override
            public void onResult(@NonNull AccessToken accessToken) {
                Log.i(MPESAExpressActivity.this.getClass().getSimpleName(), accessToken.getAccess_token());
                //   Toast.makeText(MPESAExpressActivity.this, "TOKEN : " + accessToken.getAccess_token(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                Log.e(MPESAExpressActivity.this.getClass().getSimpleName(), error);
            }
        });

        //TODO :: THIS IS A SIMPLE WAY TO DO ALL THINGS AT ONCE!!! DON'T DO THIS :)
        sendButton.setOnClickListener(v -> {

            //Get Phone Number from User Input
            phoneNumber = editTextPhoneNumber.getText().toString().trim();

            if (TextUtils.isEmpty(phoneNumber)) {

                editTextPhoneNumber.requestFocus();
                editTextPhoneNumber.setError("Please Provide a Your Number");
                return;
            }


            mProgress.show();


            if (!isNetworkAvailable()) {
                network();
            }

            p = phoneNumber.replaceFirst("^0+(?!$)", "");

            getJSON("https://youthsofhope.co.ke/mpesa/read.php?phone=" + 254 + p);

            //Toast.makeText(this,  254+p, Toast.LENGTH_SHORT).show();

        });
    }

    void enterTransaction() {
        if (!isNetworkAvailable()) {
            network();
        }
        mProgress.show();

        String p = mpesa_code.getText().toString().trim();
        getJSO("https://youthsofhope.co.ke/mpesa/update.php?phone=" + p);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void getJSO(final String urlWebService) {

        class GetJSON extends AsyncTask<Void, Void, String> {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                // Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
                //
                //
                try {
                    loadIntoListVie(s);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            protected String doInBackground(Void... voids) {
                try {
                    URL url = new URL(urlWebService);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    StringBuilder sb = new StringBuilder();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String json;
                    while ((json = bufferedReader.readLine()) != null) {
                        sb.append(json + "\n");
                    }
                    return sb.toString().trim();
                } catch (Exception e) {
                    return null;
                }
            }
        }
        GetJSON getJSON = new GetJSON();
        getJSON.execute();
    }


    private void getJSON(final String urlWebService) {

        @SuppressLint("StaticFieldLeak")
        class GetJSON extends AsyncTask<Void, Void, String> {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
                try {
                    loadIntoListView(s);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            protected String doInBackground(Void... voids) {
                try {
                    URL url = new URL(urlWebService);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    StringBuilder sb = new StringBuilder();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String json;
                    while ((json = bufferedReader.readLine()) != null) {
                        sb.append(json + "\n");
                    }
                    return sb.toString().trim();
                } catch (Exception e) {
                    return null;
                }
            }
        }
        GetJSON getJSON = new GetJSON();
        getJSON.execute();
    }


    private void loadIntoListView(String json) throws JSONException {

        try {
            JSONObject jsonObject = new JSONObject(json);
            if (jsonObject.getString("status").equals("true")) {
                JSONArray dataArray = jsonObject.getJSONArray("postData");
                for (int i = 0; i < dataArray.length(); i++) {

                    if (dataArray.length() == 1) {

                        editTextPhoneNumber.setEnabled(false);
                        mProgress.dismiss();
                        JSONObject dataobj = dataArray.getJSONObject(i);


                        constraintLayout.setVisibility(View.GONE);
                        constraintLayout2.setVisibility(View.VISIBLE);
                        mpesa_code.setText(dataobj.getString("TransID"));
                        Toast.makeText(this, "Confirm your transaction", Toast.LENGTH_SHORT).show();

                    } else {
                        mProgress.dismiss();

                        constraintLayout.setVisibility(View.VISIBLE);
                        constraintLayout2.setVisibility(View.GONE);
                        phoneNumber = editTextPhoneNumber.getText().toString().trim();

                        editTextPhoneNumber.setEnabled(true);

                    }

                }
            } else {
                // Toast.makeText(this, "" + price, Toast.LENGTH_SHORT).show();

                //TODO :: REPLACE WITH YOUR OWN CREDENTIALS  :: THIS IS SANDBOX DEMO
                LNMExpress lnmExpress = new LNMExpress(
                        "174379",
                        "bfb279f9aa9bdbcf158e97dd71a467cd2e0c893059b10f78e6b72ada1ed2c919",  //https://developer.safaricom.co.ke/test_credentials
                        TransactionType.CustomerPayBillOnline,
                        "1",
                        "254717629732",
                        "174379",
                        phoneNumber,
                        "https://youthsofhope.co.ke/mpesa/mpesa.php",
                        "001ABC",
                        "Goods Payment"
                );

                //This is the
                daraja.requestMPESAExpress(lnmExpress,
                        new DarajaListener<LNMResult>() {
                            @Override
                            public void onResult(@NonNull LNMResult lnmResult) {
                                Timber.i(lnmResult.ResponseDescription);
                                mProgress.dismiss();
                                Toast.makeText(MPESAExpressActivity.this, lnmResult.ResponseDescription, Toast.LENGTH_SHORT).show();


                            }

                            @Override
                            public void onError(String error) {
                                Timber.i(error);
                                mProgress.dismiss();

                                Toast.makeText(MPESAExpressActivity.this, error, Toast.LENGTH_SHORT).show();
                            }
                        }
                );
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    private void loadIntoListVie(String json) throws JSONException {

        try {
            JSONObject jsonObject = new JSONObject(json);

            if (jsonObject.getString("status").equals("true")) {

                JSONArray dataArray = jsonObject.getJSONArray("postData");

                for (int i = 0; i < dataArray.length(); i++) {

                    JSONObject dataobj = dataArray.getJSONObject(i);

                    Toast.makeText(this, "" + dataobj.getString("TransID"), Toast.LENGTH_SHORT).show();

                    String p = mpesa_code.getText().toString().trim();

                    if (p.equals(dataobj.getString("TransID"))) {


                        DocumentReference washingtonRef = db.collection("orders").document(docid);
                        washingtonRef
                                .update("payment", "PAID").addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                mProgress.dismiss();
                                PromptPopUpView promptPopUpView = new PromptPopUpView(MPESAExpressActivity.this);
                                promptPopUpView.changeStatus(2, "Payment Successful");
                                androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(MPESAExpressActivity.this)
                                        .setPositiveButton("Great!", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                //MPESAExpressActivity.this.finish();
                                            }
                                        })
                                        .setCancelable(false)
                                        .setView(promptPopUpView)
                                        .show();
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        finish();
                                        dialog.dismiss();
                                    }
                                }, 3000);
                                Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                                button.setEnabled(false);
                                button.setTextColor(getResources().getColor(R.color.black));
                               }

                               })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(MPESAExpressActivity.this, e.toString() + "Error ",
                                                Toast.LENGTH_SHORT).show();

                                    }
                                });


                    }

                }
            } else {

                mProgress.dismiss();
                promptPopUpView = new PromptPopUpView(this);
                promptPopUpView.changeStatus(1, "WRONG CODE");


                AlertDialog dialog = new AlertDialog.Builder(Objects.requireNonNull(this))
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setCancelable(true)
                        .setView(promptPopUpView)
                        .show();


                Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                // button.setEnabled(false);
                button.setTextColor(getResources().getColor(R.color.black));

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private void network() {
        promptPopUpView = new PromptPopUpView(this);

        promptPopUpView.changeStatus(1, "Network Error");


        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(Objects.requireNonNull(this))
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

        Button btnPositive = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
        Button btnNegative = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE);

        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) btnPositive.getLayoutParams();
        layoutParams.weight = 10;

        btnPositive.setLayoutParams(layoutParams);
        btnNegative.setLayoutParams(layoutParams);
        btnPositive.setTextColor(getResources().getColor(R.color.success));
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


    @Override
    protected void onResume() {
        super.onResume();
        if (Execute) {
            recreate();
        } else {
            Execute = true;
        }
        // recreate();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }
}


