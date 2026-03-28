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
import android.widget.ImageButton;
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

import com.example.eyalproject.DBHelper;
import com.example.eyalproject.MainActivity;
import com.example.eyalproject.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StoreFragment extends Fragment {

    private DBHelper dbHelper;
    private LinearLayout tableLayout;
    private Spinner spinnerFilter;
    private TextInputEditText editTextSearch;
    private ProgressBar progressBar;
    private NestedScrollView nestedScrollView;

    private String selectedFilter = "All";
    private String currentSearchQuery = "";

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private List<String> initiallyLoadedCategories = new ArrayList<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_store, container, false);

        dbHelper = new DBHelper(getContext());
        initializeViews(root);
        setupSpinner();
        setupSearch();

        // Load initial data (2 products per category)
        loadInitialData();

        return root;
    }

    private void initializeViews(View root) {
        tableLayout = root.findViewById(R.id.tableLayout);
        spinnerFilter = root.findViewById(R.id.spinnerFilter);
        editTextSearch = root.findViewById(R.id.editTextSearch);
        progressBar = root.findViewById(R.id.progressBar);
        nestedScrollView = root.findViewById(R.id.nestedScrollView);
    }

    private void setupSpinner() {
        try {
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                    R.array.filter_options, R.layout.spinner_item);
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            spinnerFilter.setAdapter(adapter);
        } catch (Exception e) {
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                    R.array.filter_options, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerFilter.setAdapter(adapter);
        }

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedFilter = parent.getItemAtPosition(position).toString();
                resetAndLoadData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupSearch() {
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();
                // Add delay to avoid too many searches while typing
                mainHandler.removeCallbacks(searchRunnable);
                mainHandler.postDelayed(searchRunnable, 500);
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private Runnable searchRunnable = new Runnable() {
        @Override
        public void run() {
            resetAndLoadData();
        }
    };

    private void resetAndLoadData() {
        tableLayout.removeAllViews();
        initiallyLoadedCategories.clear();
        loadInitialData();
    }

    private void loadInitialData() {
        progressBar.setVisibility(View.VISIBLE);

        executorService.execute(() -> {
            try {
                List<String> productTypes = getProductTypesToLoad();

                mainHandler.post(() -> {
                    // 💡 FIX: Check if fragment is still attached
                    if (!isAdded() || getContext() == null) return;

                    // First load 2 products per category for quick display
                    loadLimitedProducts(productTypes, 2);
                    progressBar.setVisibility(View.GONE);

                    // Then immediately load the rest in background
                    loadRemainingProducts(productTypes);
                });

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    // 💡 FIX: Check if fragment is still attached
                    if (!isAdded() || getContext() == null) return;

                    progressBar.setVisibility(View.GONE);
                    showSnackbar("Error loading products");
                });
            }
        });
    }

    private List<String> getProductTypesToLoad() {
        if (!selectedFilter.equals("All")) {
            return List.of(selectedFilter);
        } else {
            // NOTE: dbHelper.getAllProductTypes() is used here
            return dbHelper.getAllProductTypes();
        }
    }

    private void loadLimitedProducts(List<String> productTypes, int limit) {
        for (String productType : productTypes) {
            // Get all products for this type
            List<String> allProductNames = dbHelper.getProductsNamesByTypeAndSearch(productType, currentSearchQuery);

            if (!allProductNames.isEmpty()) {
                // Take only the first 'limit' products
                List<String> limitedProductNames = allProductNames.subList(0, Math.min(limit, allProductNames.size()));

                // NOTE: Relying on DBHelper to return lists in order matching limitedProductNames
                List<Double> limitedProductPrices = dbHelper.getProductsPricesByName(limitedProductNames);
                List<String> limitedProductImageUrls = dbHelper.getProductImageUrlsByName(limitedProductNames);

                addHorizontalProductSection(productType, limitedProductNames, limitedProductPrices, limitedProductImageUrls);
                initiallyLoadedCategories.add(productType);
            }
        }
    }

    private void loadRemainingProducts(List<String> productTypes) {
        executorService.execute(() -> {
            try {
                for (String productType : productTypes) {
                    // Get all products for this type
                    List<String> allProductNames = dbHelper.getProductsNamesByTypeAndSearch(productType, currentSearchQuery);

                    if (allProductNames.size() > 2) {
                        // Take products starting from index 2 (skip the first 2 we already loaded)
                        List<String> remainingProductNames = allProductNames.subList(2, allProductNames.size());

                        // NOTE: Relying on DBHelper to return lists in order matching remainingProductNames
                        List<Double> remainingProductPrices = dbHelper.getProductsPricesByName(remainingProductNames);
                        List<String> remainingProductImageUrls = dbHelper.getProductImageUrlsByName(remainingProductNames);

                        // Update the UI on main thread
                        mainHandler.post(() -> {
                            // 💡 FIX: Check if fragment is attached
                            if (!isAdded() || getContext() == null) return;
                            addProductsToExistingCategory(productType, remainingProductNames, remainingProductPrices, remainingProductImageUrls);
                        });

                        // Small delay to make the loading feel smooth
                        Thread.sleep(100);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void addProductsToExistingCategory(String productType, List<String> names, List<Double> prices, List<String> urls) {
        // 💡 FIX: Empty check to prevent StringIndexOutOfBoundsException
        if (getContext() == null || productType == null || productType.isEmpty()) return;

        // Find the existing HorizontalScrollView for this category
        String searchTitle = productType.replace('_', ' ').toLowerCase(Locale.ROOT);
        searchTitle = searchTitle.substring(0, 1).toUpperCase(Locale.ROOT) + searchTitle.substring(1);

        for (int i = 0; i < tableLayout.getChildCount(); i++) {
            View child = tableLayout.getChildAt(i);
            if (child instanceof TextView) {
                TextView titleView = (TextView) child;

                if (titleView.getText().toString().equals(searchTitle)) {
                    // Found the title, next view should be the HorizontalScrollView
                    if (i + 1 < tableLayout.getChildCount()) {
                        View nextChild = tableLayout.getChildAt(i + 1);
                        if (nextChild instanceof HorizontalScrollView) {
                            HorizontalScrollView hScrollView = (HorizontalScrollView) nextChild;
                            LinearLayout horizontalContainer = (LinearLayout) hScrollView.getChildAt(0);

                            if (horizontalContainer != null) {
                                // Add the remaining products to the existing container
                                LayoutInflater inflater = getLayoutInflater();
                                for (int j = 0; j < names.size(); j++) {
                                    if (j < prices.size() && j < urls.size()) {
                                        // 💡 FIX: Ensure we use the correct element index from the matched lists
                                        addProductCardToContainer(inflater, horizontalContainer, names.get(j), prices.get(j), urls.get(j));
                                    }
                                }
                            }
                        }
                    }
                    break;
                }
            }
        }
    }

    private void addHorizontalProductSection(String title, List<String> names, List<Double> prices, List<String> urls) {
        if (getContext() == null) return;

        LayoutInflater inflater = getLayoutInflater();

        // 1. Title TextView
        TextView textViewTitle = new TextView(getContext());
        String displayTitle = title.replace('_', ' ').toLowerCase(Locale.ROOT);
        displayTitle = displayTitle.substring(0, 1).toUpperCase(Locale.ROOT) + displayTitle.substring(1);
        textViewTitle.setText(displayTitle);
        textViewTitle.setTextSize(20);
        textViewTitle.setTypeface(Typeface.DEFAULT_BOLD);
        textViewTitle.setTextColor(Color.parseColor("#1976D2"));
        textViewTitle.setPadding(16, 24, 16, 8);
        tableLayout.addView(textViewTitle);

        // 2. Horizontal Scroll View (allows horizontal swiping)
        HorizontalScrollView hScrollView = new HorizontalScrollView(getContext());
        hScrollView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // 3. Inner Horizontal LinearLayout (holds all the product cards side-by-side)
        LinearLayout horizontalContainer = new LinearLayout(getContext());
        horizontalContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        horizontalContainer.setOrientation(LinearLayout.HORIZONTAL);
        horizontalContainer.setPadding(8, 0, 8, 0);

        // 4. Populate the Horizontal LinearLayout with Product Cards
        for (int i = 0; i < names.size(); i++) {
            if (i < prices.size() && i < urls.size()) {
                addProductCardToContainer(inflater, horizontalContainer, names.get(i), prices.get(i), urls.get(i));
            }
        }

        // 5. Add container to ScrollView, and ScrollView to main vertical layout
        hScrollView.addView(horizontalContainer);
        tableLayout.addView(hScrollView);
    }

    private void addProductCardToContainer(LayoutInflater inflater, ViewGroup container, final String productName, double productPrice, final String imageUrl) {
        // Inflate the product card layout (table_row_products.xml)
        View productView = inflater.inflate(R.layout.table_row_products, container, false);

        final ImageView imageView = productView.findViewById(R.id.imageViewProduct);

        // Determine the target size for the thumbnail (e.g., 100dp size in XML)
        // NOTE: Requires a dimension resource: <dimen name="product_thumbnail_size">100dp</dimen>
        final int thumbnailSize = (int) getResources().getDimension(R.dimen.product_thumbnail_size);

        try {
            // 💡 FIX: Explicitly resize the image to match the view's size.
            // This is Priority 2 fix for fast image decoding and smooth scrolling.
            Picasso.get()
                    .load(imageUrl)
                    .resize(thumbnailSize, thumbnailSize)
                    .centerCrop()
                    .placeholder(createPlaceholderDrawable())
                    .error(createErrorDrawable())
                    .into(imageView);
        } catch (Exception e) {
            imageView.setBackgroundColor(Color.LTGRAY);
        }

        TextView textViewName = productView.findViewById(R.id.textViewName);
        textViewName.setText(productName);

        TextView textViewPrice = productView.findViewById(R.id.textViewPrice);
        textViewPrice.setText(String.format("$%.2f", productPrice));

        imageView.setOnClickListener(v -> {
            showProductDetailsPopup(productName, imageUrl);
        });

        // Setup quantity controls
        setupQuantityControls(productView, productName, productPrice);

        container.addView(productView);
    }

    private void setupQuantityControls(View productView, String productName, double productPrice) {
        TextView textViewCount = productView.findViewById(R.id.textViewCount);
        Button minusButton = productView.findViewById(R.id.minusButton);
        Button plusButton = productView.findViewById(R.id.plusButton);
        Button buyButton = productView.findViewById(R.id.buyButton);

        minusButton.setOnClickListener(v -> {
            int count = Integer.parseInt(textViewCount.getText().toString());
            if (count > 0) {
                textViewCount.setText(String.valueOf(--count));
            }
        });

        plusButton.setOnClickListener(v -> {
            int count = Integer.parseInt(textViewCount.getText().toString());
            textViewCount.setText(String.valueOf(++count));
        });

        buyButton.setOnClickListener(v -> {
            int count = Integer.parseInt(textViewCount.getText().toString());
            if (count > 0) {
                buyProduct(productName, productPrice, count);
                textViewCount.setText("0"); // Reset count
            } else {
                showSnackbar("Please specify a quantity greater than zero");
            }
        });
    }

    private void showProductDetailsPopup(String productName, String productImageUrl) {
        if (getContext() == null || getView() == null) return;

        // Fetch details again, but only for the price, as the image URL is reliable now.
        String[] details = dbHelper.getProductDetailsByName(productName);

        String productPriceString = (details != null && details.length > 0) ? details[0] : "0.00";

        // 1. Inflate the popup layout (product_details_popup.xml)
        View popupView = getLayoutInflater().inflate(R.layout.product_details_popup, null);

        // 2. Find popup views
        TextView name = popupView.findViewById(R.id.textViewPopupName);
        TextView price = popupView.findViewById(R.id.textViewPopupPrice);
        ImageView image = popupView.findViewById(R.id.imageViewPopupProduct);
        ImageButton closeButton = popupView.findViewById(R.id.btnClosePopup);

        // 3. Set product data
        name.setText(productName);
        try {
            double priceValue = Double.parseDouble(productPriceString);
            price.setText(String.format("$%.2f", priceValue));
        } catch (NumberFormatException e) {
            price.setText("$0.00"); // Safety fallback
        }

        // Load image into the popup using the reliable URL
        try {
            Picasso.get()
                    .load(productImageUrl)
                    .placeholder(createPlaceholderDrawable())
                    .error(createErrorDrawable())
                    .fit()
                    .centerCrop()
                    .into(image);
        } catch (Exception e) {
            image.setBackgroundColor(Color.LTGRAY);
        }

        // 4. Create and show the PopupWindow
        final PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        popupWindow.setElevation(100);
        popupWindow.setOutsideTouchable(true);
        // Need a background to handle outside touch dismissal correctly
        popupWindow.setBackgroundDrawable(createPlaceholderDrawable());

        // Show in the center of the fragment's root view
        popupWindow.showAtLocation(requireView(), Gravity.CENTER, 0, 0);

        // 5. Close button action
        closeButton.setOnClickListener(v -> popupWindow.dismiss());
    }

    private int getCurrentUserId() {
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            String username = ((MainActivity) getActivity()).getUsername();
            // Assuming dbHelper is initialized in StoreFragment's onCreateView
            return dbHelper.getUserId(username);
        }
        return -1;
    }

    private void buyProduct(String productName, double productPrice, int quantity) {
        int userId = getCurrentUserId();
        if (userId == -1) {
            showSnackbar("Error: User session not found. Please log in.");
            return;
        }

        String orderTime = String.valueOf(System.currentTimeMillis());

        // 💡 FIX: Move the database write operation to the background thread
        executorService.execute(() -> {
            boolean success = false;
            for (int i = 0; i < quantity; i++) {
                long orderId = dbHelper.insertOrder(productName, productPrice, orderTime, userId);
                if (orderId != -1) {
                    success = true;
                }
            }

            final boolean finalSuccess = success;

            // Update UI on main thread
            mainHandler.post(() -> {
                // 💡 FIX: Check if fragment is attached
                if (!isAdded() || getContext() == null) return;

                if (finalSuccess) {
                    Toast.makeText(getContext(), quantity + " x " + productName + " added to orders!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Failed to add products!", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showSnackbar(String message) {
        if (getView() != null) {
            Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG)
                    .setAction("OK", v -> {})
                    .show();
        }
    }

    private Drawable createPlaceholderDrawable() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(Color.parseColor("#F5F5F5"));
        drawable.setCornerRadius(16f);
        return drawable;
    }

    private Drawable createErrorDrawable() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(Color.parseColor("#FFCDD2"));
        drawable.setCornerRadius(16f);
        return drawable;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
        mainHandler.removeCallbacksAndMessages(null);
    }
}