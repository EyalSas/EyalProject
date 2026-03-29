package com.example.eyalproject.ui.about;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eyalproject.Models.Place;
import com.example.eyalproject.R;
import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * Adapter for displaying a list of stores in a RecyclerView.
 * Handles binding store data and user interactions (click, call, directions).
 */
public class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.StoreViewHolder> {

    // List of store objects displayed in the RecyclerView
    private final List<Place> stores;

    // Listener used to handle item click events
    private final OnStoreClickListener listener;

    /**
     * Interface for handling clicks on a store item.
     */
    public interface OnStoreClickListener {
        void onStoreClick(Place store);
    }

    /**
     * Constructor that initializes the adapter with store data and a click listener.
     */
    public StoreAdapter(List<Place> stores, OnStoreClickListener listener) {
        this.stores = stores;
        this.listener = listener;
    }

    /**
     * Inflates the layout for a single store item and creates a ViewHolder.
     */
    @NonNull
    @Override
    public StoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_store, parent, false);
        return new StoreViewHolder(view);
    }

    /**
     * Binds store data to the ViewHolder at the given position.
     */
    @Override
    public void onBindViewHolder(@NonNull StoreViewHolder holder, int position) {
        Place store = stores.get(position);
        holder.bind(store, listener);
    }

    /**
     * Returns the total number of store items.
     */
    @Override
    public int getItemCount() {
        return stores.size();
    }

    /**
     * ViewHolder representing a single store item in the list.
     */
    static class StoreViewHolder extends RecyclerView.ViewHolder {

        // UI components for displaying store details
        TextView storeName, storeAddress, storePhone;
        MaterialButton callButton, directionsButton;

        /**
         * Initializes the UI elements from the item layout.
         */
        public StoreViewHolder(@NonNull View itemView) {
            super(itemView);
            storeName = itemView.findViewById(R.id.storeName);
            storeAddress = itemView.findViewById(R.id.storeAddress);
            storePhone = itemView.findViewById(R.id.storePhone);
            callButton = itemView.findViewById(R.id.callButton);
            directionsButton = itemView.findViewById(R.id.directionsButton);
        }

        /**
         * Binds a store object to the UI and sets all click listeners.
         */
        public void bind(final Place store, final OnStoreClickListener listener) {

            // Display store information
            storeName.setText(store.getName());
            storeAddress.setText(store.getAddress());
            storePhone.setText(store.getPhoneNumber());

            // Handle click on the entire item
            itemView.setOnClickListener(v -> listener.onStoreClick(store));

            // Open phone dialer with the store's number
            callButton.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL,
                        Uri.parse("tel:" + store.getPhoneNumber()));
                itemView.getContext().startActivity(intent);
            });

            // Open Google Maps with directions to the store
            directionsButton.setOnClickListener(v -> {
                Uri gmmIntentUri = Uri.parse(
                        "geo:0,0?q=" + store.getLatitude() + "," +
                                store.getLongitude() + "(" +
                                Uri.encode(store.getName()) + ")"
                );

                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");

                // Launch only if Google Maps is available
                if (mapIntent.resolveActivity(itemView.getContext().getPackageManager()) != null) {
                    itemView.getContext().startActivity(mapIntent);
                }
            });
        }
    }
}