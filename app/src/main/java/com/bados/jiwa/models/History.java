package com.bados.jiwa.models;


import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class History {
    private String trip_id;
    private String updated_at;


    public String getTrip_id() {
        return trip_id;
    }

    public void setTrip_id(String trip_id) {
        this.trip_id = trip_id;
    }

    public String getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(String updated_at) {
        this.updated_at = updated_at;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    private String created_at;
   // private String image;

    public History() {
    }

    public History(String trip_id, String updated_at, String created_at) {
        this.trip_id = trip_id;
        this.updated_at = updated_at;
        this.created_at = created_at;
    }


}
