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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A fragment that displays the main store interface.
 * It manages product fetching, categorization, search filtering,
 * and user interactions such as viewing product details and adding items to the cart.
 */
public class StoreFragment extends Fragment {

    private RecyclerView recyclerViewMain;
    private LinearLayout skeletonLayout;
    private Spinner spinnerFilter;
    private TextInputEditText editTextSearch;
    private PopupWindow activePopupWindow;

    private CategoryAdapter categoryAdapter;

    private static FirebaseHelper fbHelper;
    private static final Map<String, List<FirebaseHelper.Product>> productCache = new LinkedHashMap<>();
    private static final Set<String> prewarmedFilters = new HashSet<>();

    private String selectedFilter = "All";
    private String currentSearchQuery = "";

    private RequestOptions glideOptions;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private final Runnable searchRunnable = () -> {
        List<FirebaseHelper.Product> cached = productCache.get(selectedFilter);
        if (cached != null) filterAndRender(cached);
    };

    private ValueAnimator shimmerAnimator;

    private float dp;

    /**
     * Called to do initial creation of the fragment.
     * Retains the instance across configuration changes.
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //noinspection deprecation
        setRetainInstance(true);
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * Initializes views, sets up adapters, configures glide, and triggers the initial product load.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return Return the View for the fragment's UI, or null.
     */
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
        buildSkeletonLayout();
        setupSpinner();
        setupSearch();
        loadProducts();

        return root;
    }

    /**
     * Called when the view previously created by onCreateView has been detached from the fragment.
     * Cleans up handlers, animations, and open popups to prevent memory leaks.
     */
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

    /**
     * Initializes the core UI components and sets up the primary vertical RecyclerView.
     *
     * @param root The root view of the fragment's layout.
     */
    private void initViews(View root) {
        recyclerViewMain = root.findViewById(R.id.recyclerViewMain);
        spinnerFilter    = root.findViewById(R.id.spinnerFilter);
        editTextSearch   = root.findViewById(R.id.editTextSearch);

        recyclerViewMain.setLayoutManager(new LinearLayoutManager(requireContext()));
        categoryAdapter = new CategoryAdapter(new ArrayList<>());
        recyclerViewMain.setAdapter(categoryAdapter);
    }

    /**
     * Programmatically constructs the skeleton loading layout to provide
     * visual feedback while product data is being fetched.
     */
    private void buildSkeletonLayout() {
        skeletonLayout = new LinearLayout(requireContext());
        skeletonLayout.setOrientation(LinearLayout.VERTICAL);
        skeletonLayout.setVisibility(View.GONE);

        LinearLayout.LayoutParams fullWidth = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        skeletonLayout.setLayoutParams(fullWidth);

        skeletonLayout.addView(buildSkeletonSection(110));
        skeletonLayout.addView(buildSkeletonSection(80));

        ViewGroup root = (ViewGroup) recyclerViewMain.getParent();
        int scrollIndex = root.indexOfChild(recyclerViewMain);
        root.addView(skeletonLayout, scrollIndex);
    }

    /**
     * Builds a single horizontal section of the skeleton layout, representing a product category.
     *
     * @param titleWidthDp The width in density-independent pixels for the category title placeholder.
     * @return A LinearLayout configured as a skeleton section.
     */
    private LinearLayout buildSkeletonSection(int titleWidthDp) {
        LinearLayout section = new LinearLayout(requireContext());
        section.setOrientation(LinearLayout.VERTICAL);
        section.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        View titleBar = new View(requireContext());
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                px(titleWidthDp), px(16));
        titleParams.setMargins(px(16), px(20), 0, px(10));
        titleBar.setLayoutParams(titleParams);
        titleBar.setBackground(roundRect("#E0E0E0", 8));
        section.addView(titleBar);

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

    /**
     * Builds an individual skeleton product card.
     *
     * @return A LinearLayout representing a placeholder product card.
     */
    private LinearLayout buildSkeletonCard() {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(px(140), ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(px(6), 0, px(6), px(8));
        card.setLayoutParams(cardParams);
        card.setBackground(roundRect("#FFFFFF", 14));
        card.setPadding(px(10), px(10), px(10), px(10));

        card.addView(skeletonBlock(ViewGroup.LayoutParams.MATCH_PARENT, 110, 0, "#EBEBEB", 10));
        card.addView(skeletonBlock(100, 13, 10, "#EBEBEB", 6));
        card.addView(skeletonBlock(55,  11, 6,  "#EBEBEB", 6));
        card.addView(skeletonBlock(ViewGroup.LayoutParams.MATCH_PARENT, 30, 10, "#EBEBEB", 8));

        return card;
    }

    /**
     * Creates a generic skeleton block to act as a placeholder for text or images.
     *
     * @param widthDp     The block width in density-independent pixels, or a LayoutParams constant.
     * @param heightDp    The block height in density-independent pixels.
     * @param topMarginDp The top margin in density-independent pixels.
     * @param color       The hex color code for the block background.
     * @param radiusDp    The corner radius in density-independent pixels.
     * @return The configured View object.
     */
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

    /**
     * Configures the category filter spinner and attaches an item selection listener
     * to trigger product reloads upon category change.
     */
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

    /**
     * Sets up the search input field with a text watcher. Triggers a delayed search
     * routine to filter the currently loaded product list.
     */
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

    /**
     * Initiates the loading of products from the Firebase database or retrieves them
     * from the local cache if previously fetched. Manages the skeleton loading state.
     */
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
     * Preloads product images in the background to ensure smoother scrolling
     * when the user interacts with the horizontal lists.
     *
     * @param products The list of products whose images should be preloaded.
     */
    private void prewarmImages(List<FirebaseHelper.Product> products) {
        if (prewarmedFilters.contains(selectedFilter)) return;
        prewarmedFilters.add(selectedFilter);

        backgroundExecutor.execute(() -> {
            for (FirebaseHelper.Product p : products) {
                if (p.imageUrl == null || p.imageUrl.isEmpty()) continue;
                try {
                    Glide.with(requireContext())
                            .load(p.imageUrl)
                            .apply(glideOptions)
                            .preload(240, 240);
                } catch (Exception ignored) {}
            }
        });
    }

    /**
     * Filters a list of products based on the current search query and organizes them
     * into categorized groupings before dispatching them to the main adapter.
     *
     * @param source The unfiltered list of products to process.
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

        if (grouped.isEmpty()) {
            categoryAdapter.updateData(new ArrayList<>());
            showSnackbar("No products found.");
            return;
        }

        List<CategoryData> newCategories = new ArrayList<>();
        for (Map.Entry<String, List<FirebaseHelper.Product>> entry : grouped.entrySet()) {
            String display = entry.getKey().replace('_', ' ').toLowerCase(Locale.ROOT);
            display = Character.toUpperCase(display.charAt(0)) + display.substring(1);
            newCategories.add(new CategoryData(display, entry.getValue()));
        }

        categoryAdapter.updateData(newCategories);
    }

    /**
     * A simple data object representing a categorized group of products.
     */
    private static class CategoryData {
        String name;
        List<FirebaseHelper.Product> products;

        /**
         * Constructs a new CategoryData instance.
         *
         * @param name     The display name of the category.
         * @param products The list of products belonging to this category.
         */
        CategoryData(String name, List<FirebaseHelper.Product> products) {
            this.name = name;
            this.products = products;
        }
    }

    /**
     * An adapter for the main vertical RecyclerView, responsible for displaying
     * category headers and initializing nested horizontal RecyclerViews for products.
     */
    private class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
        private List<CategoryData> categories;

        /**
         * Constructs a CategoryAdapter.
         *
         * @param categories The initial list of category data to display.
         */
        CategoryAdapter(List<CategoryData> categories) {
            this.categories = categories;
        }

        /**
         * Updates the adapter's underlying data set and refreshes the layout.
         *
         * @param newData The new list of category data.
         */
        void updateData(List<CategoryData> newData) {
            this.categories = newData;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout layout = new LinearLayout(parent.getContext());
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView header = new TextView(parent.getContext());
            header.setTextSize(20);
            header.setTypeface(Typeface.DEFAULT_BOLD);
            header.setTextColor(Color.parseColor("#1976D2"));
            header.setPadding(px(16), px(24), px(16), px(8));
            layout.addView(header);

            RecyclerView horizontalRecycler = new RecyclerView(parent.getContext());
            horizontalRecycler.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            horizontalRecycler.setClipToPadding(false);
            horizontalRecycler.setPadding(px(8), 0, px(8), 0);

            horizontalRecycler.setLayoutManager(new LinearLayoutManager(
                    parent.getContext(), LinearLayoutManager.HORIZONTAL, false));

            layout.addView(horizontalRecycler);

            return new CategoryViewHolder(layout, header, horizontalRecycler);
        }

        @Override
        public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
            CategoryData data = categories.get(position);
            holder.title.setText(data.name);

            ProductAdapter productAdapter = new ProductAdapter(data.products);
            holder.productRecycler.setAdapter(productAdapter);
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        /**
         * View holder pattern implementation for category rows.
         */
        class CategoryViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            RecyclerView productRecycler;

            /**
             * Constructs a CategoryViewHolder.
             *
             * @param itemView        The parent layout containing the category UI.
             * @param title           The TextView displaying the category name.
             * @param productRecycler The nested RecyclerView handling the product list.
             */
            CategoryViewHolder(View itemView, TextView title, RecyclerView productRecycler) {
                super(itemView);
                this.title = title;
                this.productRecycler = productRecycler;
            }
        }
    }

    /**
     * An adapter for the nested horizontal RecyclerViews, responsible for managing
     * individual product cards, quantities, and purchase actions.
     */
    private class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
        private final List<FirebaseHelper.Product> products;

        /**
         * Constructs a ProductAdapter.
         *
         * @param products The list of products to populate within the nested list.
         */
        ProductAdapter(List<FirebaseHelper.Product> products) {
            this.products = products;
        }

        @NonNull
        @Override
        public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.table_row_products, parent, false);
            return new ProductViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
            FirebaseHelper.Product p = products.get(position);

            holder.name.setText(p.name);
            holder.price.setText(String.format(Locale.ROOT, "$%.2f", p.price));
            holder.counter.setText("0");

            try {
                Glide.with(StoreFragment.this)
                        .load(p.imageUrl)
                        .apply(glideOptions)
                        .into(holder.image);
            } catch (Exception e) {
                holder.image.setBackgroundColor(Color.LTGRAY);
            }

            holder.image.setOnClickListener(v -> showProductPopup(p.name, p.imageUrl, p.price));

            holder.btnMinus.setOnClickListener(v -> {
                int n = getSafeQuantity(holder.counter);
                if (n > 0) holder.counter.setText(String.valueOf(n - 1));
            });

            holder.btnPlus.setOnClickListener(v ->
                    holder.counter.setText(String.valueOf(getSafeQuantity(holder.counter) + 1)));

            holder.btnBuy.setOnClickListener(v -> {
                int n = getSafeQuantity(holder.counter);
                if (n > 0) {
                    purchaseProduct(p.name, p.price, n);
                    holder.counter.setText("0");
                } else {
                    showSnackbar("Please specify a quantity greater than zero");
                }
            });
        }

        @Override
        public int getItemCount() {
            return products.size();
        }

        /**
         * View holder pattern implementation for individual product items.
         */
        class ProductViewHolder extends RecyclerView.ViewHolder {
            ImageView image;
            TextView name, price, counter;
            Button btnMinus, btnPlus, btnBuy;

            /**
             * Constructs a ProductViewHolder.
             *
             * @param view The root view of the product card layout.
             */
            ProductViewHolder(View view) {
                super(view);
                image = view.findViewById(R.id.imageViewProduct);
                name = view.findViewById(R.id.textViewName);
                price = view.findViewById(R.id.textViewPrice);
                counter = view.findViewById(R.id.textViewCount);
                btnMinus = view.findViewById(R.id.minusButton);
                btnPlus = view.findViewById(R.id.plusButton);
                btnBuy = view.findViewById(R.id.buyButton);
            }
        }
    }

    /**
     * Extracts an integer quantity from the provided TextView, defaulting to zero
     * if the text is invalid or malformed.
     *
     * @param counter The TextView containing the quantity value.
     * @return The parsed integer quantity, or 0 if parsing fails.
     */
    private int getSafeQuantity(TextView counter) {
        try {
            return Integer.parseInt(counter.getText().toString().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Displays a centered popup window featuring an enlarged product image and details.
     *
     * @param name     The product name.
     * @param imageUrl The URL of the product image.
     * @param price    The product price.
     */
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

    /**
     * Initiates the process of adding a specified quantity of a product to the user's cart.
     * Warns the user if they are not authenticated.
     *
     * @param name  The product name.
     * @param price The unit price of the product.
     * @param qty   The quantity to add to the cart.
     */
    private void purchaseProduct(String name, double price, int qty) {
        if (fbHelper.getCurrentUserId() == null) {
            showSnackbar("Error: User session not found. Please log in.");
            return;
        }
        fbHelper.addToCart(name, price, qty);
        Toast.makeText(getContext(), qty + " × " + name + " added to cart!",
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Toggles the visibility state between the skeleton loading layout and the
     * main product RecyclerView. Controls the shimmer animation status accordingly.
     *
     * @param show True to display the skeleton layout, false to show the actual content.
     */
    private void showSkeleton(boolean show) {
        if (skeletonLayout == null) return;
        if (show) {
            skeletonLayout.setVisibility(View.VISIBLE);
            recyclerViewMain.setVisibility(View.GONE);
            startShimmer();
        } else {
            stopShimmer();
            skeletonLayout.setVisibility(View.GONE);
            recyclerViewMain.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Instantiates and starts the pulsing opacity animation attached to the skeleton layout.
     */
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

    /**
     * Cancels the active skeleton shimmer animation and nullifies its reference.
     */
    private void stopShimmer() {
        if (shimmerAnimator != null) {
            shimmerAnimator.cancel();
            shimmerAnimator = null;
        }
    }

    /**
     * Displays a brief Snackbar message to the user at the bottom of the view.
     *
     * @param message The text message to display.
     */
    private void showSnackbar(String message) {
        if (getView() != null) {
            Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG)
                    .setAction("OK", v -> {})
                    .show();
        }
    }

    /**
     * Creates a rounded rectangle drawable dynamically for use as view backgrounds.
     *
     * @param hex      The hex color string.
     * @param radiusDp The corner radius in density-independent pixels.
     * @return A styled GradientDrawable.
     */
    private Drawable roundRect(String hex, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setColor(Color.parseColor(hex));
        d.setCornerRadius(radiusDp * dp);
        return d;
    }

    /**
     * Converts a density-independent pixel (dp) value into an exact pixel count based
     * on the current device screen density.
     *
     * @param dp The dimension in dp.
     * @return The rounded integer pixel equivalent.
     */
    private int px(int dp) { return Math.round(dp * this.dp); }
}