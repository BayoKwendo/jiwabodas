package com.bados.jiwa;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.bados.jiwa.models.Car;
import com.bados.jiwa.models.User;
import com.bados.jiwa.helpers.PromptPopUpView;
import com.bodas.jiwa.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class NameRegistrationFragment extends Fragment {

    private EditText usernameEditText;
    private EditText phoneEditText;
    private EditText carModelEditText;
    private String phoneNumbe, userPhone;

    Button btn;

    private EditText driverLicenceEditText;
    private PromptPopUpView promptPopUpView;

    private Context ctx = null;


    private User user;


    public static Fragment newInstance(@NonNull User user) {
        Fragment fragment = new NameRegistrationFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(MySharedPreferences.USER_KEY, user);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx  = this.getActivity();

        if (getArguments() != null) {
            user = getArguments().getParcelable(MySharedPreferences.USER_KEY);
        }
        if (user == null) {
            user = new User();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_name_registration, null);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View driver = view.findViewById(R.id.driver);
        if (User.USER_ROLE_DRIVER.equals(user.role)) {
            driver.setVisibility(View.VISIBLE);
        }


        FirebaseUser phone = FirebaseAuth.getInstance().getCurrentUser();

        assert phone != null;
        phoneNumbe = phone.getPhoneNumber();


        usernameEditText = view.findViewById(R.id.username);
        phoneEditText = view.findViewById(R.id.phone);
        carModelEditText = view.findViewById(R.id.car_model);
        driverLicenceEditText = view.findViewById(R.id.driver_licence);
        usernameEditText.requestFocus();




        phoneEditText.setText(phoneNumbe);

        phoneEditText.setEnabled(false);

        btn =view.findViewById(R.id.save);

        btn.setOnClickListener(new View.OnClickListener() {

            @RequiresApi(api = Build.VERSION_CODES.O)
            @SuppressWarnings("ConstantConditions")
            @Override
            public void onClick(View view) {
                user.name = usernameEditText.getText().toString();
                user.phoneNumber = phoneNumbe;
                String name = "^[A-Z][a-zA-Z]{3,}(?: [A-Z][a-zA-Z]*){0,2}$";
                String phone = "/^(?:[5-9]|(?:[1-9][0-9])|(?:[1-4][0-9][0-9])|(?:500))$/\n";


                if (TextUtils.isEmpty(user.name)) {
                    username();
                    usernameEditText.setError("Enter your full name ");
                    return;
                }

                if (user.name.length() < 6){
                    username();
                    usernameEditText.setError("Enter your full names; six characters atleast");
                    return;
                }


                if (TextUtils.isEmpty(user.phoneNumber)) {
                    phoneEditText.requestFocus();
                    phoneEditText.setFocusable(true);

                    phoneEditText.setError("Enter your phone number ");
                    return;
                }


                if (User.USER_ROLE_DRIVER.equals(user.role)) {
                    user.car = new Car();
                    user.car.model = carModelEditText.getText().toString();
                    user.car.licensePlate = driverLicenceEditText.getText().toString();
                }
                if (!TextUtils.isEmpty(user.name) && !TextUtils.isEmpty(user.phoneNumber)) {

                    btn.setText(R.string.saving);


                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            btn.setText("Save");
                        }
                    }, 4000);

                    ((RegistrationActivity) getActivity()).next(user);
                }
            }
        });
    }

    private  void username(){
        usernameEditText.setFocusable(true);
        usernameEditText.isFocusable();
        usernameEditText.isFocusableInTouchMode();
        usernameEditText.isFocused();
    }



}
