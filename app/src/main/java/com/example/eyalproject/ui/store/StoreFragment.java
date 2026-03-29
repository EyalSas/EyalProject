package com.example.eyalproject.ui.store;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.example.eyalproject.FirebaseHelper;
import com.example.eyalproject.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StoreFragment extends Fragment {

    // ── Views ──────────────────────────────────────────────────────────────────
    private FirebaseHelper fbHelper;
    private LinearLayout tableLayout;
    private Spinner spinnerFilter;
    private TextInputEditText editTextSearch;
    private ProgressBar progressBar;
    private NestedScrollView nestedScrollView;

    // ✅ FIX: Track active popup to prevent WindowLeaked exceptions
    private PopupWindow activePopupWindow;

    // ── State ──────────────────────────────────────────────────────────────────
    private String selectedFilter = "All";
    private String currentSearchQuery = "";

    /** Raw product list from the last Firebase fetch — never re-fetched unless filter changes. */
    private List<FirebaseHelper.Product> cachedProducts = null;
    /** Which filter value the cache belongs to. */
    private String cachedFilterKey = null;

    // ── Threading ──────────────────────────────────────────────────────────────
    /** Posts UI updates — always the main thread. */
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Dedicated background thread for filtering + grouping work.
     * Keeps the main thread completely free during search.
     */
    private HandlerThread filterThread;
    private Handler filterHandler;

    /** Debounce runnable: re-scheduled on every keystroke, fires 300 ms after the last one. */
    private final Runnable searchRunnable = () -> filterAndRender(cachedProducts);

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_store, container, false);

        fbHelper = new FirebaseHelper();
        startFilterThread();
        initViews(root);
        setupSpinner();
        setupSearch();
        fetchProducts();

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cancel every pending callback so nothing fires on a detached fragment.
        mainHandler.removeCallbacksAndMessages(null);
        filterHandler.removeCallbacksAndMessages(null);
        filterThread.quitSafely();

        // ✅ FIX: Safely dismiss popup to prevent WindowLeaked exceptions on rotation/navigation
        if (activePopupWindow != null && activePopupWindow.isShowing()) {
            activePopupWindow.dismiss();
            activePopupWindow = null;
        }
    }

    // ── Init helpers ───────────────────────────────────────────────────────────

    private void startFilterThread() {
        filterThread = new HandlerThread("StoreFilterThread");
        filterThread.start();
        filterHandler = new Handler(filterThread.getLooper());
    }

    private void initViews(View root) {
        tableLayout    = root.findViewById(R.id.tableLayout);
        spinnerFilter  = root.findViewById(R.id.spinnerFilter);
        editTextSearch = root.findViewById(R.id.editTextSearch);
        progressBar    = root.findViewById(R.id.progressBar);
        nestedScrollView = root.findViewById(R.id.nestedScrollView);

        // Hardware layer lets the GPU composite the scroll surface — smoother flings.
        nestedScrollView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    private void setupSpinner() {
        try {
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    requireContext(), R.array.filter_options, R.layout.spinner_item);
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            spinnerFilter.setAdapter(adapter);
        } catch (Exception e) {
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    requireContext(), R.array.filter_options,
                    android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerFilter.setAdapter(adapter);
        }

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String newFilter = parent.getItemAtPosition(pos).toString();
                if (newFilter.equals(selectedFilter)) return; // no-op if nothing changed
                selectedFilter = newFilter;
                // Invalidate cache — next fetchProducts() will hit Firebase.
                cachedProducts  = null;
                cachedFilterKey = null;
                fetchProducts();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupSearch() {
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim().toLowerCase(Locale.ROOT);

                // Debounce: cancel previous pending search, schedule a new one.
                filterHandler.removeCallbacks(searchRunnable);

                if (cachedProducts != null) {
                    // Products already loaded — filter locally, no network call.
                    filterHandler.postDelayed(searchRunnable, 300);
                }
                // If cache is empty a fetch is already in-flight; the callback will call
                // filterAndRender() with the correct query when it arrives.
            }
        });
    }

    // ── Data loading ───────────────────────────────────────────────────────────

    /**
     * Hits Firebase only when the cache is stale (filter changed or first load).
     * Otherwise jumps straight to filterAndRender() on the background thread.
     */
    private void fetchProducts() {
        if (cachedProducts != null && selectedFilter.equals(cachedFilterKey)) {
            // Cache is warm — skip the network entirely.
            filterHandler.post(() -> filterAndRender(cachedProducts));
            return;
        }

        showProgress(true);

        FirebaseHelper.ProductsCallback callback = new FirebaseHelper.ProductsCallback() {
            @Override
            public void onProductsLoaded(List<FirebaseHelper.Product> products) {
                if (!isAdded()) return;
                cachedProducts  = products;
                cachedFilterKey = selectedFilter;
                // Hand off to background thread immediately.
                filterHandler.post(() -> filterAndRender(products));
            }
            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                mainHandler.post(() -> {
                    showProgress(false);
                    showSnackbar("Error loading products: " + error);
                });
            }
        };

        if ("All".equals(selectedFilter)) {
            fbHelper.getAllProducts(callback);
        } else {
            String key = selectedFilter.toUpperCase(Locale.ROOT).replace(" ", "_");
            fbHelper.getProductsByType(key, callback);
        }
    }

    // ── Background: filter + group ─────────────────────────────────────────────

    /**
     * Runs on {@code filterThread}.
     * Filters by search query and groups by type — all off the main thread.
     * Posts one single Runnable to the main thread when done.
     */
    private void filterAndRender(List<FirebaseHelper.Product> source) {
        if (source == null) return;

        // LinkedHashMap preserves insertion order for stable category ordering.
        Map<String, List<FirebaseHelper.Product>> grouped = new LinkedHashMap<>();
        String query = currentSearchQuery; // snapshot to avoid race with UI thread

        for (FirebaseHelper.Product p : source) {
            if (query.isEmpty() || p.name.toLowerCase(Locale.ROOT).contains(query)) {
                String type = (p.type != null) ? p.type : "OTHER";
                grouped.computeIfAbsent(type, k -> new ArrayList<>()).add(p);
            }
        }

        // Snapshot the result — hand immutable data to the main thread.
        final Map<String, List<FirebaseHelper.Product>> result = grouped;

        mainHandler.post(() -> {
            if (!isAdded() || getContext() == null) return;
            showProgress(false);
            renderSections(result);
        });
    }

    // ── Main thread: render ────────────────────────────────────────────────────

    /**
     * Called on the main thread with the fully prepared, filtered, grouped data.
     * Clears the table and adds every category in one synchronous pass —
     * no recursive handler loops, no artificial delays.
     */
    private void renderSections(Map<String, List<FirebaseHelper.Product>> grouped) {
        tableLayout.removeAllViews();

        if (grouped.isEmpty()) {
            showSnackbar("No products found.");
            return;
        }

        LayoutInflater inflater = getLayoutInflater();
        for (Map.Entry<String, List<FirebaseHelper.Product>> entry : grouped.entrySet()) {
            addHorizontalProductSection(inflater, entry.getKey(), entry.getValue());
        }
    }

    // ── Section & card builders ────────────────────────────────────────────────

    private void addHorizontalProductSection(LayoutInflater inflater,
                                             String title,
                                             List<FirebaseHelper.Product> products) {
        // Category title
        TextView header = new TextView(getContext());
        String display = title.replace('_', ' ').toLowerCase(Locale.ROOT);
        display = Character.toUpperCase(display.charAt(0)) + display.substring(1);
        header.setText(display);
        header.setTextSize(20);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setTextColor(Color.parseColor("#1976D2"));
        header.setPadding(16, 24, 16, 8);
        tableLayout.addView(header);

        // Horizontal scroll row
        HorizontalScrollView hScroll = new HorizontalScrollView(getContext());
        hScroll.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        hScroll.setHorizontalScrollBarEnabled(false);
        // Hardware layer on each row → GPU composited horizontal scroll.
        hScroll.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        LinearLayout row = new LinearLayout(getContext());
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(8, 0, 8, 0);

        for (FirebaseHelper.Product p : products) {
            addProductCard(inflater, row, p.name, p.price, p.imageUrl);
        }

        hScroll.addView(row);
        tableLayout.addView(hScroll);
    }

    private void addProductCard(LayoutInflater inflater, ViewGroup container,
                                String productName, double productPrice, String imageUrl) {
        View card = inflater.inflate(R.layout.table_row_products, container, false);

        final ImageView imageView = card.findViewById(R.id.imageViewProduct);
        final int thumbSize = (int) getResources().getDimension(R.dimen.product_thumbnail_size);

        try {
            Picasso.get()
                    .load(imageUrl)
                    .resize(thumbSize, thumbSize)
                    .centerCrop()
                    .noFade()                         // skip cross-fade → saves GPU work
                    .placeholder(placeholderDrawable())
                    .error(errorDrawable())
                    .into(imageView);
        } catch (Exception e) {
            imageView.setBackgroundColor(Color.LTGRAY);
        }

        ((TextView) card.findViewById(R.id.textViewName)).setText(productName);
        ((TextView) card.findViewById(R.id.textViewPrice))
                .setText(String.format(Locale.ROOT, "$%.2f", productPrice));

        imageView.setOnClickListener(v -> showProductPopup(productName, imageUrl, productPrice));
        setupQuantityControls(card, productName, productPrice);

        container.addView(card);
    }

    private void setupQuantityControls(View card, String productName, double productPrice) {
        TextView counter  = card.findViewById(R.id.textViewCount);
        Button minus      = card.findViewById(R.id.minusButton);
        Button plus       = card.findViewById(R.id.plusButton);
        Button buy        = card.findViewById(R.id.buyButton);

        minus.setOnClickListener(v -> {
            int n = getSafeQuantity(counter);
            if (n > 0) counter.setText(String.valueOf(n - 1));
        });

        plus.setOnClickListener(v -> {
            int n = getSafeQuantity(counter);
            counter.setText(String.valueOf(n + 1));
        });

        buy.setOnClickListener(v -> {
            int n = getSafeQuantity(counter);
            if (n > 0) {
                purchaseProduct(productName, productPrice, n);
                counter.setText("0");
            } else {
                showSnackbar("Please specify a quantity greater than zero");
            }
        });
    }

    // ✅ FIX: Safely parse integer to prevent NumberFormatException crashes
    private int getSafeQuantity(TextView counter) {
        try {
            return Integer.parseInt(counter.getText().toString().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ── Popup ──────────────────────────────────────────────────────────────────

    private void showProductPopup(String name, String imageUrl, double price) {
        if (getContext() == null || getView() == null) return;

        // Dismiss existing popup if still lingering
        if (activePopupWindow != null && activePopupWindow.isShowing()) {
            activePopupWindow.dismiss();
        }

        View popupView = getLayoutInflater().inflate(R.layout.product_details_popup, null);

        ((TextView) popupView.findViewById(R.id.textViewPopupName)).setText(name);
        ((TextView) popupView.findViewById(R.id.textViewPopupPrice))
                .setText(String.format(Locale.ROOT, "$%.2f", price));

        ImageView img = popupView.findViewById(R.id.imageViewPopupProduct);
        try {
            Picasso.get()
                    .load(imageUrl)
                    .noFade()
                    .placeholder(placeholderDrawable())
                    .error(errorDrawable())
                    .fit()
                    .centerCrop()
                    .into(img);
        } catch (Exception e) {
            img.setBackgroundColor(Color.LTGRAY);
        }

        activePopupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);
        activePopupWindow.setElevation(100);
        activePopupWindow.setOutsideTouchable(true);
        activePopupWindow.setBackgroundDrawable(placeholderDrawable());
        activePopupWindow.showAtLocation(requireView(), Gravity.CENTER, 0, 0);

        popupView.findViewById(R.id.btnClosePopup).setOnClickListener(v -> activePopupWindow.dismiss());
    }

    // ── Purchase ───────────────────────────────────────────────────────────────

    private void purchaseProduct(String name, double price, int qty) {
        if (fbHelper.getCurrentUserId() == null) {
            showSnackbar("Error: User session not found. Please log in.");
            return;
        }
        fbHelper.addToCart(name, price, qty);
        Toast.makeText(getContext(), qty + " × " + name + " added to cart!",
                Toast.LENGTH_SHORT).show();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void showProgress(boolean visible) {
        progressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void showSnackbar(String message) {
        if (getView() != null) {
            Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG)
                    .setAction("OK", v -> {})
                    .show();
        }
    }

    private Drawable placeholderDrawable() {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setColor(Color.parseColor("#F5F5F5"));
        d.setCornerRadius(16f);
        return d;
    }

    private Drawable errorDrawable() {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setColor(Color.parseColor("#FFCDD2"));
        d.setCornerRadius(16f);
        return d;
    }
}