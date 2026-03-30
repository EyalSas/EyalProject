package com.example.eyalproject.ui.store;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
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
    private LinearLayout tableLayout;
    private Spinner spinnerFilter;
    private TextInputEditText editTextSearch;
    private ProgressBar progressBar;
    private PopupWindow activePopupWindow;

    // ── State ──────────────────────────────────────────────────────────────────
    private String selectedFilter = "All";
    private String currentSearchQuery = "";

    // ── App-level cache: survives fragment destroy/recreate ────────────────────
    // Keyed by filter string → list of products. Never wiped unless data changes.
    private static final Map<String, List<FirebaseHelper.Product>> productCache = new LinkedHashMap<>();

    // Shared FirebaseHelper — one Firestore connection for the whole app session.
    private static FirebaseHelper fbHelper;

    // ── Threading ──────────────────────────────────────────────────────────────
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Debounce runnable for search — runs on main thread since filtering is fast
    // (just iterating an in-memory list; no I/O involved).
    private final Runnable searchRunnable = () -> {
        List<FirebaseHelper.Product> cached = productCache.get(selectedFilter);
        if (cached != null) filterAndRender(cached);
    };

    // ── Drawables cached once per fragment instance ────────────────────────────
    private Drawable placeholder;
    private Drawable error;

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_store, container, false);

        // Create FirebaseHelper only once for the whole app session
        if (fbHelper == null) fbHelper = new FirebaseHelper();

        // Pre-build drawables once; reuse everywhere in this fragment
        placeholder = buildDrawable("#F5F5F5");
        error       = buildDrawable("#FFCDD2");

        initViews(root);
        setupSpinner();
        setupSearch();
        loadProducts();

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cancel any pending debounce or delayed renders
        mainHandler.removeCallbacksAndMessages(null);

        if (activePopupWindow != null && activePopupWindow.isShowing()) {
            activePopupWindow.dismiss();
            activePopupWindow = null;
        }
    }

    // ── Init ───────────────────────────────────────────────────────────────────

    private void initViews(View root) {
        tableLayout    = root.findViewById(R.id.tableLayout);
        spinnerFilter  = root.findViewById(R.id.spinnerFilter);
        editTextSearch = root.findViewById(R.id.editTextSearch);
        progressBar    = root.findViewById(R.id.progressBar);
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
                if (newFilter.equals(selectedFilter)) return;
                selectedFilter = newFilter;
                loadProducts();
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
                mainHandler.removeCallbacks(searchRunnable);
                // Only debounce if data is already loaded; fires after 200 ms of typing pause
                if (productCache.containsKey(selectedFilter)) {
                    mainHandler.postDelayed(searchRunnable, 200);
                }
            }
        });
    }

    // ── Data ───────────────────────────────────────────────────────────────────

    private void loadProducts() {
        // ✅ INSTANT: If we already have data for this filter, render immediately with no spinner
        List<FirebaseHelper.Product> cached = productCache.get(selectedFilter);
        if (cached != null) {
            filterAndRender(cached);
            return;
        }

        // ── First time for this filter: fetch from Firebase ──────────────────
        showProgress(true);

        FirebaseHelper.ProductsCallback callback = new FirebaseHelper.ProductsCallback() {
            @Override
            public void onProductsLoaded(List<FirebaseHelper.Product> products) {
                if (!isAdded()) return;
                // Store permanently; will never be re-fetched unless the app process dies
                productCache.put(selectedFilter, products);
                mainHandler.post(() -> {
                    showProgress(false);
                    filterAndRender(products);
                });
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

    // ── Filter + Render (all on main thread — fast, no I/O) ───────────────────

    /**
     * Filters the in-memory list and rebuilds the UI.
     * This runs entirely on the main thread because:
     *  • No I/O — it's just iterating an ArrayList in RAM.
     *  • Moving it to a background thread adds thread-hop latency (~4–8 ms)
     *    and requires synchronisation, which is slower than just doing it here.
     */
    private void filterAndRender(List<FirebaseHelper.Product> source) {
        if (source == null || !isAdded()) return;

        String query = currentSearchQuery;
        Map<String, List<FirebaseHelper.Product>> grouped = new LinkedHashMap<>();

        for (FirebaseHelper.Product p : source) {
            if (query.isEmpty() || p.name.toLowerCase(Locale.ROOT).contains(query)) {
                String type = (p.type != null) ? p.type : "OTHER";
                grouped.computeIfAbsent(type, k -> new ArrayList<>()).add(p);
            }
        }

        renderSections(grouped);
    }

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
        // Section header
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

        // ✅ Picasso reuses its internal disk+memory cache automatically.
        //    noPlaceholder() avoids a layout pass on placeholder swap.
        //    stableKey() lets Picasso reuse the same bitmap across cards.
        try {
            Picasso.get()
                    .load(imageUrl)
                    .resize(thumbSize, thumbSize)
                    .centerCrop()
                    .noFade()
                    .placeholder(placeholder)   // pre-built, not rebuilt per card
                    .error(error)
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
        TextView counter = card.findViewById(R.id.textViewCount);
        Button minus     = card.findViewById(R.id.minusButton);
        Button plus      = card.findViewById(R.id.plusButton);
        Button buy       = card.findViewById(R.id.buyButton);

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
                    .placeholder(placeholder)
                    .error(error)
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
        activePopupWindow.setBackgroundDrawable(placeholder);
        activePopupWindow.showAtLocation(requireView(), Gravity.CENTER, 0, 0);

        popupView.findViewById(R.id.btnClosePopup)
                .setOnClickListener(v -> activePopupWindow.dismiss());
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

    /**
     * Builds a rounded rectangle drawable for placeholder / error states.
     * Called once at fragment creation, not once per card.
     */
    private Drawable buildDrawable(String hex) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setColor(Color.parseColor(hex));
        d.setCornerRadius(16f);
        return d;
    }
}