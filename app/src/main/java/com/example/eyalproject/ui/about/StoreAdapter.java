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
 * An adapter responsible for managing and displaying a collection of physical store locations
 * within a RecyclerView. It handles data binding for individual store details and sets up
 * intents for interactive elements like phone dialing and map navigation.
 */
public class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.StoreViewHolder> {

    private final List<Place> stores;
    private final OnStoreClickListener listener;

    /**
     * An interface defining the callback to be invoked when a store item is clicked.
     */
    public interface OnStoreClickListener {
        /**
         * Called when a specific store item in the RecyclerView is selected.
         *
         * @param store The Place object corresponding to the clicked item.
         */
        void onStoreClick(Place store);
    }

    /**
     * Constructs a new StoreAdapter.
     *
     * @param stores   The list of Place objects containing store data to be displayed.
     * @param listener The callback listener for item click events.
     */
    public StoreAdapter(List<Place> stores, OnStoreClickListener listener) {
        this.stores = stores;
        this.listener = listener;
    }

    /**
     * Called when RecyclerView needs a new ViewHolder of the given type to represent an item.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return A new StoreViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public StoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_store, parent, false);
        return new StoreViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the item at the given position.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull StoreViewHolder holder, int position) {
        Place store = stores.get(position);
        holder.bind(store, listener);
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of stores.
     */
    @Override
    public int getItemCount() {
        return stores.size();
    }

    /**
     * A ViewHolder class that encapsulates the view hierarchy for a single store item.
     */
    static class StoreViewHolder extends RecyclerView.ViewHolder {

        TextView storeName, storeAddress, storePhone;
        MaterialButton callButton, directionsButton;

        /**
         * Constructs a new StoreViewHolder and resolves its UI component references.
         *
         * @param itemView The root view of the store list item layout.
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
         * Binds the specific store data to the UI components and configures the explicit
         * intents for making phone calls and opening map directions.
         *
         * @param store    The Place object containing the data to display.
         * @param listener The listener to invoke for general item clicks.
         */
        public void bind(final Place store, final OnStoreClickListener listener) {

            storeName.setText(store.getName());
            storeAddress.setText(store.getAddress());
            storePhone.setText(store.getPhoneNumber());

            itemView.setOnClickListener(v -> listener.onStoreClick(store));

            callButton.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL,
                        Uri.parse("tel:" + store.getPhoneNumber()));
                itemView.getContext().startActivity(intent);
            });

            directionsButton.setOnClickListener(v -> {
                Uri gmmIntentUri = Uri.parse(
                        "geo:0,0?q=" + store.getLatitude() + "," +
                                store.getLongitude() + "(" +
                                Uri.encode(store.getName()) + ")"
                );

                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");

                if (mapIntent.resolveActivity(itemView.getContext().getPackageManager()) != null) {
                    itemView.getContext().startActivity(mapIntent);
                }
            });
        }
    }
}