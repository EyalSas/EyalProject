package com.example.eyalproject.ui.about;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    /**
     * Called to have the fragment instantiate its user interface view.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_about, container, false);

        initializeViews(rootView);
        initializeGameStores();
        setupRecyclerView();

        // Set up the FAB to switch views when clicked.
        toggleFab.setOnClickListener(v -> toggleView());

        // Important for MapView lifecycle: MUST be called in onCreateView.
        mapView.onCreate(savedInstanceState);
        // Asynchronously loads the map and calls onMapReady when ready.
        mapView.getMapAsync(this);

        return rootView;
    }

    /**
     * Initializes UI elements from the layout.
     * @param rootView The root view inflated in onCreateView.
     */
    private void initializeViews(View rootView) {
        mapView = rootView.findViewById(R.id.mapView);
        storesRecyclerView = rootView.findViewById(R.id.storesRecyclerView);
        toggleFab = rootView.findViewById(R.id.toggleFab);
    }

    /**
     * Populates the list of game stores with dummy data.
     */
    private void initializeGameStores() {
        gameStores = new ArrayList<>();
        // Add 10 GameZone stores with their coordinates and details.
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

    /**
     * Sets up the RecyclerView with a layout manager and the store adapter.
     * The click listener on the adapter calls focusOnStore, linking the list to the map.
     */
    private void setupRecyclerView() {
        storesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        storeAdapter = new StoreAdapter(gameStores, store -> {
            // Click action: focus the map on the selected store.
            focusOnStore(store);
        });
        storesRecyclerView.setAdapter(storeAdapter);
    }

    /**
     * Callback method called when the map is ready to be used.
     * @param googleMap The ready GoogleMap instance.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        addStoreMarkers();

        if (!gameStores.isEmpty()) {
            // Center map view over a central point (Tel Aviv coordinates used here) and set zoom level.
            LatLng israelCenter = new LatLng(32.0853, 34.7818);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(israelCenter, 8));
        }
    }

    /**
     * Clears existing markers and adds a new marker for every store in the list.
     * It also populates the markerMap for easy lookup later.
     */
    private void addStoreMarkers() {
        if (googleMap == null) return;
        markerMap.clear();
        for (Place store : gameStores) {
            LatLng storeLocation = new LatLng(store.getLatitude(), store.getLongitude());
            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(storeLocation)
                    .title(store.getName())
                    .snippet(store.getAddress())); // Snippet provides extra detail in the info window

            // Map the Place ID to the Marker object.
            markerMap.put(String.valueOf(store.getId()), marker);
        }
    }

    /**
     * Toggles the visibility between the MapView and the RecyclerView.
     */
    private void toggleView() {
        showingMap = !showingMap;
        if (showingMap) {
            // Show map, hide list
            mapView.setVisibility(View.VISIBLE);
            storesRecyclerView.setVisibility(View.GONE);
            // Change FAB icon to represent the 'list' view (assuming ic_store represents the list icon)
            toggleFab.setImageResource(R.drawable.ic_store);
        } else {
            // Show list, hide map
            mapView.setVisibility(View.GONE);
            storesRecyclerView.setVisibility(View.VISIBLE);
            // Change FAB icon to represent the 'map' view
            toggleFab.setImageResource(R.drawable.ic_store); // Note: Should probably be an ic_map or similar
        }
    }

    /**
     * Centers the map on the selected store and shows its marker info window.
     * It also switches to map view if the list is currently showing.
     * @param store The Place object to focus on.
     */
    private void focusOnStore(Place store) {
        if (!showingMap) {
            toggleView(); // Switch to map view if not already showing
        }
        LatLng storeLocation = new LatLng(store.getLatitude(), store.getLongitude());
        // Animate camera movement to the store location with a closer zoom level (15f)
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(storeLocation, 15f), 1000, null);

        // Retrieve the corresponding marker and show its info window
        Marker marker = markerMap.get(String.valueOf(store.getId()));
        if (marker != null) {
            marker.showInfoWindow();
        }
    }


    // --- MapView Lifecycle Methods ---
    // These methods ensure the MapView is properly managed alongside the Fragment lifecycle.
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}

// --- RecyclerView Adapter and ViewHolder ---

/**
 * Adapter for the RecyclerView to display individual store details (Place objects).
 */
class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.StoreViewHolder> {

    private final List<Place> stores;
    private final OnStoreClickListener listener; // Interface for handling clicks.

    /**
     * Interface to define the action taken when a store item is clicked.
     */
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
        // Inflate the layout for a single list item.
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

    /**
     * ViewHolder class to hold and manage views for a single store item.
     */
    static class StoreViewHolder extends RecyclerView.ViewHolder {
        TextView storeName, storeAddress, storePhone;
        MaterialButton callButton, directionsButton;

        public StoreViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize views found in list_item_store.xml
            storeName = itemView.findViewById(R.id.storeName);
            storeAddress = itemView.findViewById(R.id.storeAddress);
            storePhone = itemView.findViewById(R.id.storePhone);
            callButton = itemView.findViewById(R.id.callButton);
            directionsButton = itemView.findViewById(R.id.directionsButton);
        }

        /**
         * Binds the Place data to the ViewHolder views and sets click listeners.
         * @param store The Place object containing store details.
         * @param listener The click listener to handle map focusing.
         */
        public void bind(final Place store, final OnStoreClickListener listener) {
            storeName.setText(store.getName());
            storeAddress.setText(store.getAddress());
            storePhone.setText(store.getPhoneNumber());

            // Set the main item click listener (used to focus map)
            itemView.setOnClickListener(v -> listener.onStoreClick(store));

            // Set button click listeners

            // 1. Call Button: Initiates a phone call Intent.
            callButton.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + store.getPhoneNumber()));
                itemView.getContext().startActivity(intent);
            });

            // 2. Directions Button: Launches Google Maps app with directions/location.
            directionsButton.setOnClickListener(v -> {
                // URI uses geo:0,0?q=latitude,longitude(StoreName) format for location lookup
                Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + store.getLatitude() + "," + store.getLongitude() + "(" + Uri.encode(store.getName()) + ")");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                // Explicitly set package to Google Maps app for reliability
                mapIntent.setPackage("com.google.android.apps.maps");

                // Check if Google Maps is installed before starting the activity
                if (mapIntent.resolveActivity(itemView.getContext().getPackageManager()) != null) {
                    itemView.getContext().startActivity(mapIntent);
                }
            });
        }
    }
}