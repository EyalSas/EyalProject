package com.example.eyalproject.ui.about;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eyalproject.Models.Place;
import com.example.eyalproject.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragment responsible for displaying information about the company's physical stores.
 * It provides two views: a list (RecyclerView) and an interactive Google Map (MapView),
 * allowing the user to toggle between them.
 */
public class AboutFragment extends Fragment implements OnMapReadyCallback {

    private MapView mapView;           // View container for the Google Map.
    private GoogleMap googleMap;       // The actual Google Map object.
    private RecyclerView storesRecyclerView; // List view to display store details.
    private FloatingActionButton toggleFab; // Button to switch between map and list views.

    private List<Place> gameStores;    // List of Place models (stores).
    private StoreAdapter storeAdapter; // Adapter for the RecyclerView.
    private boolean showingMap = false; // State flag: true if map is visible, false if list is visible.

    // Map to link a Place's ID to its corresponding Google Maps Marker object.
    private Map<String, Marker> markerMap = new HashMap<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_about, container, false);

        initializeViews(rootView);
        initializeGameStores();
        setupRecyclerView();

        toggleFab.setOnClickListener(v -> toggleView());

        // Important for MapView lifecycle: MUST be called in onCreateView.
        if (mapView != null) {
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(this);
        }

        return rootView;
    }

    private void initializeViews(View rootView) {
        mapView = rootView.findViewById(R.id.mapView);
        storesRecyclerView = rootView.findViewById(R.id.storesRecyclerView);
        toggleFab = rootView.findViewById(R.id.toggleFab);
    }

    private void initializeGameStores() {
        gameStores = new ArrayList<>();
        gameStores.add(new Place(1L, "GameZone Jerusalem", 31.7683, 35.2137, "+972-2-500-1001", "Jaffa St 123, Jerusalem"));
        gameStores.add(new Place(2L, "GameZone Tel Aviv", 32.0853, 34.7818, "+972-3-500-1002", "Dizengoff St 45, Tel Aviv"));
        gameStores.add(new Place(3L, "GameZone Haifa", 32.7940, 34.9896, "+972-4-500-1003", "HaAtzmaut St 67, Haifa"));
        gameStores.add(new Place(4L, "GameZone Be'er Sheva", 31.2529, 34.7915, "+972-8-500-1004", "Rager Blvd 89, Be'er Sheva"));
        gameStores.add(new Place(5L, "GameZone Netanya", 32.3320, 34.8590, "+972-9-500-1005", "Herzl St 34, Netanya"));
        gameStores.add(new Place(6L, "GameZone Ashdod", 31.8044, 34.6553, "+972-8-500-1006", "HaShalom St 56, Ashdod"));
        gameStores.add(new Place(7L, "GameZone Rishon LeZion", 31.9730, 34.7925, "+972-3-500-1007", "Moshe Levi St 78, Rishon LeZion"));
        gameStores.add(new Place(8L, "GameZone Petah Tikva", 32.0871, 34.8875, "+972-3-500-1008", "Jabotinsky St 90, Petah Tikva"));
        gameStores.add(new Place(9L, "GameZone Holon", 32.0158, 34.7874, "+972-3-500-1009", "Golda Meir St 12, Holon"));
        gameStores.add(new Place(10L, "GameZone Eilat", 29.5577, 34.9519, "+972-8-500-1010", "HaTmarim Blvd 34, Eilat"));
    }

    private void setupRecyclerView() {
        storesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        storeAdapter = new StoreAdapter(gameStores, store -> focusOnStore(store));
        storesRecyclerView.setAdapter(storeAdapter);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        addStoreMarkers();

        if (!gameStores.isEmpty()) {
            LatLng israelCenter = new LatLng(32.0853, 34.7818);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(israelCenter, 8));
        }
    }

    private void addStoreMarkers() {
        if (googleMap == null) return;
        markerMap.clear();
        for (Place store : gameStores) {
            LatLng storeLocation = new LatLng(store.getLatitude(), store.getLongitude());
            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(storeLocation)
                    .title(store.getName())
                    .snippet(store.getAddress()));

            markerMap.put(String.valueOf(store.getId()), marker);
        }
    }

    private void toggleView() {
        showingMap = !showingMap;
        if (showingMap) {
            // Show map, hide list
            mapView.setVisibility(View.VISIBLE);
            storesRecyclerView.setVisibility(View.GONE);
            // 💡 FIX: Set icon to 'list' so user knows tapping it goes back to the list
            toggleFab.setImageResource(R.drawable.ic_store);
        } else {
            // Show list, hide map
            mapView.setVisibility(View.GONE);
            storesRecyclerView.setVisibility(View.VISIBLE);
            // 💡 FIX: Assuming you have a map icon or default dashboard icon.
            toggleFab.setImageResource(android.R.drawable.ic_dialog_map);
        }
    }

    private void focusOnStore(Place store) {
        if (!showingMap) {
            toggleView();
        }
        LatLng storeLocation = new LatLng(store.getLatitude(), store.getLongitude());
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(storeLocation, 15f), 1000, null);

        Marker marker = markerMap.get(String.valueOf(store.getId()));
        if (marker != null) {
            marker.showInfoWindow();
        }
    }

    // --- 💡 FIX: Complete MapView Lifecycle Methods to prevent memory leaks ---
    @Override
    public void onStart() {
        super.onStart();
        if (mapView != null) mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mapView != null) mapView.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 💡 FIX: Destroy map here when Fragment's view is destroyed to free memory
        if (mapView != null) mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }
}

// --- RecyclerView Adapter and ViewHolder ---

class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.StoreViewHolder> {

    private final List<Place> stores;
    private final OnStoreClickListener listener;

    public interface OnStoreClickListener {
        void onStoreClick(Place store);
    }

    public StoreAdapter(List<Place> stores, OnStoreClickListener listener) {
        this.stores = stores;
        this.listener = listener;
    }

    @NonNull
    @Override
    public StoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_store, parent, false);
        return new StoreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoreViewHolder holder, int position) {
        Place store = stores.get(position);
        holder.bind(store, listener);
    }

    @Override
    public int getItemCount() {
        return stores.size();
    }

    static class StoreViewHolder extends RecyclerView.ViewHolder {
        TextView storeName, storeAddress, storePhone;
        MaterialButton callButton, directionsButton;

        public StoreViewHolder(@NonNull View itemView) {
            super(itemView);
            storeName = itemView.findViewById(R.id.storeName);
            storeAddress = itemView.findViewById(R.id.storeAddress);
            storePhone = itemView.findViewById(R.id.storePhone);
            callButton = itemView.findViewById(R.id.callButton);
            directionsButton = itemView.findViewById(R.id.directionsButton);
        }

        public void bind(final Place store, final OnStoreClickListener listener) {
            storeName.setText(store.getName());
            storeAddress.setText(store.getAddress());
            storePhone.setText(store.getPhoneNumber());

            itemView.setOnClickListener(v -> listener.onStoreClick(store));

            callButton.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + store.getPhoneNumber()));
                itemView.getContext().startActivity(intent);
            });

            directionsButton.setOnClickListener(v -> {
                Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + store.getLatitude() + "," + store.getLongitude() + "(" + Uri.encode(store.getName()) + ")");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");

                if (mapIntent.resolveActivity(itemView.getContext().getPackageManager()) != null) {
                    itemView.getContext().startActivity(mapIntent);
                }
            });
        }
    }
}