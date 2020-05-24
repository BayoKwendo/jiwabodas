package com.bados.jiwa.components;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bados.jiwa.helpers.PreferenceHelper;
import com.bados.jiwa.models.Order;
import com.bados.jiwa.models.User;
import com.bodas.jiwa.R;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.bados.jiwa.models.History;
import com.google.firebase.firestore.QuerySnapshot;


import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

public class TripHistory extends AppCompatActivity {

    private ListenerRegistration userListenerRegistration;


    @BindView(R.id.progress_bar)
    ProgressBar progressBar;
    private List<Order> orders = new ArrayList<>();


    @BindView(R.id.texterror)
    TextView textError;
    @BindView(R.id.rvHistory)
    RecyclerView friendList;

    private FirebaseFirestore db;
    private FirestoreRecyclerAdapter adapter;
    LinearLayoutManager linearLayoutManager;


    private PreferenceHelper preferenceHelper;

    String rider, driver, role, status;
    Query query;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_history);
        ButterKnife.bind(this);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        progressBar.setVisibility(View.VISIBLE);
        textError.setVisibility(View.GONE);

        preferenceHelper = new PreferenceHelper(this);
        rider = preferenceHelper.getRider();
        driver = preferenceHelper.getDriverid();
        role = preferenceHelper.getRole();

        status = preferenceHelper.getStatuss();


      //  Toast.makeText(this, "" + role, Toast.LENGTH_SHORT).show();

        init();

        getFriendList();

    }

    private void init() {
        linearLayoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false);
        friendList.setLayoutManager(linearLayoutManager);
        db = FirebaseFirestore.getInstance();
    }

    private void getFriendList() {
        if (userListenerRegistration != null) {
            userListenerRegistration.remove();
        }
        if (role.equals("rider")) {
            query = db.collection("orders")
                    .whereIn("rider.id", Collections.singletonList(rider));
        }
        else if(role.equals("driver")){
            query = db.collection("orders")
                    .whereIn("driver.id", Collections.singletonList(driver));
        }


        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull com.google.android.gms.tasks.Task<QuerySnapshot> task) {

                if (task.isSuccessful()) {
                    boolean b = Objects.requireNonNull(task.getResult()).isEmpty();
                    if (b){
                        progressBar.setVisibility(View.GONE);
                        textError.setVisibility(View.VISIBLE);
                    } else { } }
            }

        });

        FirestoreRecyclerOptions<History> response = new FirestoreRecyclerOptions.Builder<  History>()
                .setQuery(query, History.class)
                .build();
                adapter = new FirestoreRecyclerAdapter<History, FriendsHolder>(response) {
                @Override
                public void onBindViewHolder(@NotNull FriendsHolder holder, int position, @NotNull History history) {
                    progressBar.setVisibility(View.GONE);


                    holder.textTitle.setText("Created On: " + history.getCreated_at());
                    holder.textName.setText("Trip ID: " + history.getTrip_id());

                    holder.itemView.setOnClickListener(v -> {
                        if (orders != null) {
                            if (history.getTrip_id() != null){
                                Intent topicsIntent = new Intent(TripHistory.this, TripDetail.class);
                                topicsIntent.putExtra("tripid", history.getTrip_id());
                                startActivity(topicsIntent);
                            }else
                            {        Toast.makeText(TripHistory.this, " More Details for this trip not found: Incomplete Journey", Toast.LENGTH_SHORT).show();

                            }

                        }
//                    Snackbar.make(, order.tripId + ", " + order.id + " at " + order.rider, Snackbar.LENGTH_LONG)
//                            .setAction("Action", null).show();
                    });
                }

                @Override
                public FriendsHolder onCreateViewHolder(ViewGroup group, int i) {
                    View view = LayoutInflater.from(group.getContext())
                            .inflate(R.layout.list_item, group, false);

                    return new FriendsHolder(view);
                }

                @Override
                public void onError(FirebaseFirestoreException e) {
                    Log.e("error", e.getMessage());
                }
            };

            adapter.notifyDataSetChanged();
            friendList.setAdapter(adapter);

    }

    public static class FriendsHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.name)
        TextView textName;
        @BindView(R.id.image)
        CircleImageView imageView;
        @BindView(R.id.title)
        TextView textTitle;

        public FriendsHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
