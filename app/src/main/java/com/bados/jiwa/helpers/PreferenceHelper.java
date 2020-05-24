package com.bados.jiwa.helpers;

/**

 */
import android.content.Context;
import android.content.SharedPreferences;

import io.reactivex.internal.operators.single.SingleTakeUntil;

public class PreferenceHelper {

    private final String INTRO = "intro";
    private final String TRIP = "trip";

    private final String PAYMENT = "payment";

    private final String PRICE = "price2";


    private final String RIDER = "id";

    private final String STATUS = "status";


    private final String USER = "role";

    private final String DRIVER = "id";

    private SharedPreferences app_prefs;
    private Context context;

    public PreferenceHelper(Context context) {
        app_prefs = context.getSharedPreferences("shared",
                Context.MODE_PRIVATE);
        this.context = context;
    }

    public void putIsLogin(boolean loginorout) {
        SharedPreferences.Editor edit = app_prefs.edit();
        edit.putBoolean(INTRO, loginorout);
        edit.commit();
    }
    public boolean getIsLogin() {
        return app_prefs.getBoolean(INTRO, false);
    }

    public void putName(String loginorout) {
        SharedPreferences.Editor edit = app_prefs.edit();
        edit.putString(TRIP, loginorout);
        edit.commit();
    }
    public String getName() {
        return app_prefs.getString(TRIP, "");
    }


    public void putPay(String loginorout) {
        SharedPreferences.Editor edit = app_prefs.edit();
        edit.putString(PAYMENT, loginorout);
        edit.commit();
    }
    public String getPay() {
        return app_prefs.getString(PAYMENT, "");
    }

    public void putPrice( String loginorout) {
        SharedPreferences.Editor edit = app_prefs.edit();
        edit.putString(PRICE, loginorout);
        edit.apply();
    }
    public String getPrice() {
        return app_prefs.getString(PRICE, "");
    }



    public void putRiderId( String loginorout) {
        SharedPreferences.Editor edit = app_prefs.edit();
        edit.putString(RIDER, loginorout);
        edit.apply();
    }
    public String getRider() {
        return app_prefs.getString(RIDER, "");
    }


    public void putDriverId( String loginorout) {
        SharedPreferences.Editor edit = app_prefs.edit();
        edit.putString(DRIVER, loginorout);
        edit.apply();
    }
    public String getDriverid() {
        return app_prefs.getString(USER, "");
    }

    public void putRole( String loginorout) {
        SharedPreferences.Editor edit = app_prefs.edit();
        edit.putString(USER, loginorout);
        edit.apply();
    }
    public String getRole() {
        return app_prefs.getString(USER, "");
    }

    public void putStatus( String loginorout) {
        SharedPreferences.Editor edit = app_prefs.edit();
        edit.putString(STATUS, loginorout);
        edit.apply();
    }
    public String getStatuss() {
        return app_prefs.getString(STATUS, "");
    }
}
