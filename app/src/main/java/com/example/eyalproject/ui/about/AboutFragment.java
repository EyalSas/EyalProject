package com.example.eyalproject.ui.about;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragment that displays company store locations using two modes:
 * a list view (RecyclerView) and a map view (Google Map).
 * The user can switch between these views using a floating action button.
 */
public class AboutFragment extends Fragment implements OnMapReadyCallback {

    // Google Map container and instance
    private MapView mapView;
    private GoogleMap googleMap;

    // UI components
    private RecyclerView storesRecyclerView;
    private FloatingActionButton toggleFab;

    // Data and adapter
    private List<Place> gameStores;
    private StoreAdapter storeAdapter;

    // State flag to determine which view is currently displayed
    private boolean showingMap = false;

    // Maps store IDs to their corresponding markers on the map
    private Map<String, Marker> markerMap = new HashMap<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the fragment layout
        View rootView = inflater.inflate(R.layout.fragment_about, container, false);

        // Initialize UI components and data
        initializeViews(rootView);
        initializeGameStores();
        setupRecyclerView();

        // Set click listener to toggle between list and map views
        toggleFab.setOnClickListener(v -> toggleView());

        // Initialize MapView lifecycle and request map asynchronously
        if (mapView != null) {
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(this);
        }

        return rootView;
    }

    /**
     * Binds UI elements from the layout to class variables.
     */
    private void initializeViews(View rootView) {
        mapView = rootView.findViewById(R.id.mapView);
        storesRecyclerView = rootView.findViewById(R.id.storesRecyclerView);
        toggleFab = rootView.findViewById(R.id.toggleFab);
    }

    /**
     * Initializes a static list of store locations.
     */
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

    /**
     * Configures the RecyclerView with a layout manager and adapter.
     */
    private void setupRecyclerView() {
        storesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        storeAdapter = new StoreAdapter(gameStores, store -> focusOnStore(store));
        storesRecyclerView.setAdapter(storeAdapter);
    }

    /**
     * Callback triggered when the Google Map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        // Configure map settings
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Add markers for all stores
        addStoreMarkers();

        // Move camera to a default location (center of Israel)
        if (!gameStores.isEmpty()) {
            LatLng israelCenter = new LatLng(32.0853, 34.7818);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(israelCenter, 8));
        }
    }

    /**
     * Adds markers to the map for each store location.
     */
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

    /**
     * Toggles between map view and list view.
     */
    private void toggleView() {
        showingMap = !showingMap;

        if (showingMap) {
            mapView.setVisibility(View.VISIBLE);
            storesRecyclerView.setVisibility(View.GONE);
            toggleFab.setImageResource(R.drawable.ic_store);
        } else {
            mapView.setVisibility(View.GONE);
            storesRecyclerView.setVisibility(View.VISIBLE);
            toggleFab.setImageResource(android.R.drawable.ic_dialog_map);
        }
    }

    /**
     * Focuses the map camera on a selected store and shows its marker info.
     */
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

    /**
     * MapView lifecycle handling to prevent memory leaks.
     */
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
        if (mapView != null) mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }
}