package com.example.eyalproject.ui.store;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.eyalproject.FirebaseHelper;
import com.example.eyalproject.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class StoreFragment extends Fragment {

    // ── Views ──────────────────────────────────────────────────────────────────
    private LinearLayout tableLayout;
    private LinearLayout skeletonLayout;   // built entirely in Java — no XML file
    private NestedScrollView nestedScrollView;
    private Spinner spinnerFilter;
    private TextInputEditText editTextSearch;
    private PopupWindow activePopupWindow;

    // ── App-level singletons (survive fragment destroy/recreate) ───────────────
    private static FirebaseHelper fbHelper;
    private static final Map<String, List<FirebaseHelper.Product>> productCache = new LinkedHashMap<>();
    private static final Set<String> prewarmedFilters = new HashSet<>();

    // ── State ──────────────────────────────────────────────────────────────────
    private String selectedFilter     = "All";
    private String currentSearchQuery = "";

    // Glide options built once, reused for every card
    private RequestOptions glideOptions;

    // ── Threading ──────────────────────────────────────────────────────────────
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable searchRunnable = () -> {
        List<FirebaseHelper.Product> cached = productCache.get(selectedFilter);
        if (cached != null) filterAndRender(cached);
    };

    // ── Shimmer animator ───────────────────────────────────────────────────────
    private ValueAnimator shimmerAnimator;

    // ── dp helper (set once in onCreateView) ──────────────────────────────────
    private float dp;

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Keeps this object alive across tab switches — productCache survives,
        // so every re-entry after the first costs zero network calls.
        //noinspection deprecation
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_store, container, false);

        dp = requireContext().getResources().getDisplayMetrics().density;

        if (fbHelper == null) fbHelper = new FirebaseHelper();

        glideOptions = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .override(240, 240)
                .centerCrop()
                .placeholder(roundRect("#F0F0F0", 12))
                .error(roundRect("#FFCDD2", 12));

        initViews(root);
        buildSkeletonLayout();   // ← creates skeleton purely in Java, no XML
        setupSpinner();
        setupSearch();
        loadProducts();

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mainHandler.removeCallbacksAndMessages(null);
        stopShimmer();
        if (activePopupWindow != null && activePopupWindow.isShowing()) {
            activePopupWindow.dismiss();
            activePopupWindow = null;
        }
    }

    // ── Init ───────────────────────────────────────────────────────────────────

    private void initViews(View root) {
        tableLayout      = root.findViewById(R.id.tableLayout);
        nestedScrollView = root.findViewById(R.id.nestedScrollView);
        spinnerFilter    = root.findViewById(R.id.spinnerFilter);
        editTextSearch   = root.findViewById(R.id.editTextSearch);
    }

    /**
     * Builds the skeleton loading UI entirely in Java — zero new XML or drawable
     * files. Creates two rows of grey pulsing card placeholders that match the
     * shape of real product cards. Inserted into the root layout above the
     * NestedScrollView, hidden by default (GONE), shown only on first load.
     */
    private void buildSkeletonLayout() {
        skeletonLayout = new LinearLayout(requireContext());
        skeletonLayout.setOrientation(LinearLayout.VERTICAL);
        skeletonLayout.setVisibility(View.GONE);

        LinearLayout.LayoutParams fullWidth = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        skeletonLayout.setLayoutParams(fullWidth);

        // Build 2 skeleton sections
        skeletonLayout.addView(buildSkeletonSection(110));  // wider title bar
        skeletonLayout.addView(buildSkeletonSection(80));   // narrower title bar

        // Insert into root LinearLayout, right above the NestedScrollView
        ViewGroup root = (ViewGroup) nestedScrollView.getParent();
        int scrollIndex = root.indexOfChild(nestedScrollView);
        root.addView(skeletonLayout, scrollIndex);
    }

    /** One skeleton section = a grey title bar + a horizontal row of 3 grey cards. */
    private LinearLayout buildSkeletonSection(int titleWidthDp) {
        LinearLayout section = new LinearLayout(requireContext());
        section.setOrientation(LinearLayout.VERTICAL);
        section.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // Title placeholder bar
        View titleBar = new View(requireContext());
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                px(titleWidthDp), px(16));
        titleParams.setMargins(px(16), px(20), 0, px(10));
        titleBar.setLayoutParams(titleParams);
        titleBar.setBackground(roundRect("#E0E0E0", 8));
        section.addView(titleBar);

        // Horizontal row of 3 skeleton cards
        HorizontalScrollView hScroll = new HorizontalScrollView(requireContext());
        hScroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        hScroll.setHorizontalScrollBarEnabled(false);

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setPadding(px(8), 0, px(8), 0);

        for (int i = 0; i < 3; i++) row.addView(buildSkeletonCard());

        hScroll.addView(row);
        section.addView(hScroll);
        return section;
    }

    /** One skeleton card — a white rounded card containing grey placeholder blocks. */
    private LinearLayout buildSkeletonCard() {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(px(140), ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(px(6), 0, px(6), px(8));
        card.setLayoutParams(cardParams);
        card.setBackground(roundRect("#FFFFFF", 14));
        card.setPadding(px(10), px(10), px(10), px(10));

        card.addView(skeletonBlock(ViewGroup.LayoutParams.MATCH_PARENT, 110, 0, "#EBEBEB", 10));  // image
        card.addView(skeletonBlock(100, 13, 10, "#EBEBEB", 6));   // name
        card.addView(skeletonBlock(55,  11, 6,  "#EBEBEB", 6));   // price
        card.addView(skeletonBlock(ViewGroup.LayoutParams.MATCH_PARENT, 30, 10, "#EBEBEB", 8));   // button

        return card;
    }

    /** Creates a single grey placeholder block with given dimensions and top margin. */
    private View skeletonBlock(int widthDp, int heightDp, int topMarginDp, String color, int radiusDp) {
        View v = new View(requireContext());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                widthDp == ViewGroup.LayoutParams.MATCH_PARENT ? widthDp : px(widthDp),
                px(heightDp));
        p.topMargin = px(topMarginDp);
        v.setLayoutParams(p);
        v.setBackground(roundRect(color, radiusDp));
        return v;
    }

    // ── Spinner + Search ───────────────────────────────────────────────────────

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
                if (productCache.containsKey(selectedFilter)) {
                    mainHandler.postDelayed(searchRunnable, 150);
                }
            }
        });
    }

    // ── Data ───────────────────────────────────────────────────────────────────

    private void loadProducts() {
        List<FirebaseHelper.Product> cached = productCache.get(selectedFilter);
        if (cached != null) {
            filterAndRender(cached);
            return;
        }

        showSkeleton(true);

        FirebaseHelper.ProductsCallback callback = new FirebaseHelper.ProductsCallback() {
            @Override
            public void onProductsLoaded(List<FirebaseHelper.Product> products) {
                if (!isAdded()) return;
                productCache.put(selectedFilter, products);
                prewarmImages(products);
                mainHandler.post(() -> {
                    showSkeleton(false);
                    filterAndRender(products);
                });
            }
            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                mainHandler.post(() -> {
                    showSkeleton(false);
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

    /**
     * Pre-decodes every product image into Glide's disk+memory cache on a
     * background thread. When the cards are rendered, Glide finds the bitmap
     * already decoded in RAM → images appear instantly with no grey flash.
     */
    private void prewarmImages(List<FirebaseHelper.Product> products) {
        if (prewarmedFilters.contains(selectedFilter)) return;
        prewarmedFilters.add(selectedFilter);

        new Thread(() -> {
            for (FirebaseHelper.Product p : products) {
                if (p.imageUrl == null || p.imageUrl.isEmpty()) continue;
                try {
                    Glide.with(requireContext())
                            .load(p.imageUrl)
                            .apply(glideOptions)
                            .preload(240, 240);
                } catch (Exception ignored) {}
            }
        }, "GlidePrewarm").start();
    }

    // ── Filter + Render ────────────────────────────────────────────────────────

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
        TextView header = new TextView(getContext());
        String display  = title.replace('_', ' ').toLowerCase(Locale.ROOT);
        display = Character.toUpperCase(display.charAt(0)) + display.substring(1);
        header.setText(display);
        header.setTextSize(20);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setTextColor(Color.parseColor("#1976D2"));
        header.setPadding(16, 24, 16, 8);
        tableLayout.addView(header);

        HorizontalScrollView hScroll = new HorizontalScrollView(getContext());
        hScroll.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        hScroll.setHorizontalScrollBarEnabled(false);

        LinearLayout row = new LinearLayout(getContext());
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
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
        View card           = inflater.inflate(R.layout.table_row_products, container, false);
        ImageView imageView = card.findViewById(R.id.imageViewProduct);

        try {
            Glide.with(requireContext())
                    .load(imageUrl)
                    .apply(glideOptions)
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
        plus.setOnClickListener(v ->
                counter.setText(String.valueOf(getSafeQuantity(counter) + 1)));
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
            Glide.with(requireContext())
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(roundRect("#F0F0F0", 12))
                    .error(roundRect("#FFCDD2", 12))
                    .centerCrop()
                    .into(img);
        } catch (Exception e) {
            img.setBackgroundColor(Color.LTGRAY);
        }

        activePopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        activePopupWindow.setElevation(100);
        activePopupWindow.setOutsideTouchable(true);
        activePopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
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

    // ── Skeleton shimmer ───────────────────────────────────────────────────────

    private void showSkeleton(boolean show) {
        if (skeletonLayout == null) return;
        if (show) {
            skeletonLayout.setVisibility(View.VISIBLE);
            nestedScrollView.setVisibility(View.GONE);
            startShimmer();
        } else {
            stopShimmer();
            skeletonLayout.setVisibility(View.GONE);
            nestedScrollView.setVisibility(View.VISIBLE);
        }
    }

    private void startShimmer() {
        shimmerAnimator = ValueAnimator.ofFloat(0.3f, 1f);
        shimmerAnimator.setDuration(850);
        shimmerAnimator.setRepeatMode(ValueAnimator.REVERSE);
        shimmerAnimator.setRepeatCount(ValueAnimator.INFINITE);
        shimmerAnimator.addUpdateListener(anim -> {
            if (skeletonLayout != null)
                skeletonLayout.setAlpha((float) anim.getAnimatedValue());
        });
        shimmerAnimator.start();
    }

    private void stopShimmer() {
        if (shimmerAnimator != null) {
            shimmerAnimator.cancel();
            shimmerAnimator = null;
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void showSnackbar(String message) {
        if (getView() != null) {
            Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG)
                    .setAction("OK", v -> {})
                    .show();
        }
    }

    /** Rounded rectangle drawable — used for placeholders and skeleton blocks. */
    private Drawable roundRect(String hex, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setColor(Color.parseColor(hex));
        d.setCornerRadius(radiusDp * dp);
        return d;
    }

    /** Converts dp to pixels using the screen density. */
    private int px(int dp) { return Math.round(dp * this.dp); }
}