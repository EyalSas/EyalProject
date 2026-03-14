package com.example.eyalproject.ui.store;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.eyalproject.DBHelper;
import com.example.eyalproject.Models.Product;
import com.example.eyalproject.Models.ProductAdapter;
import com.example.eyalproject.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class StoreFragment extends Fragment {

    private DBHelper dbHelper;
    private TableLayout tableLayout;
    private Spinner spinnerFilter;
    private String selectedFilter = "All";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_store, container, false);

        dbHelper = new DBHelper(getContext());
        tableLayout = root.findViewById(R.id.tableLayout);
        spinnerFilter = root.findViewById(R.id.spinnerFilter);

        setupSpinner();
        refreshTable();

        return root;
    }

    private void setupSpinner() {
        try {
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                    R.array.filter_options, R.layout.spinner_item);
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            spinnerFilter.setAdapter(adapter);
        } catch (Exception e) {
            // Fallback to default spinner style
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                    R.array.filter_options, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerFilter.setAdapter(adapter);
        }

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedFilter = parent.getItemAtPosition(position).toString();
                refreshTable();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void refreshTable() {
        tableLayout.removeAllViews();

        List<String> productTypes;
        switch (selectedFilter) {
            case "Home":
                productTypes = dbHelper.getProductNamesByType(DBHelper.ProductType.Home);
                break;
            case "Clothing":
                productTypes = dbHelper.getProductNamesByType(DBHelper.ProductType.Clothing);
                break;
            case "Electronics":
                productTypes = dbHelper.getProductNamesByType(DBHelper.ProductType.Electronics);
                break;
            case "Sports":
                productTypes = dbHelper.getProductNamesByType(DBHelper.ProductType.Sports);
                break;
            default:
                productTypes = dbHelper.getAllProductTypes();
                break;
        }

        for (String productType : productTypes) {
            addTitleToTable(productType);

            List<String> productNames = dbHelper.getProductsNamesByType(productType);
            List<Double> productPrices = dbHelper.getProductsPricesByType(productType);
            List<String> productImageUrls = dbHelper.getProductImageUrlsByType(productType);

            int numProducts = Math.min(productNames.size(), Math.min(productPrices.size(), productImageUrls.size()));

            for (int i = 0; i < numProducts; i++) {
                addProductToTable(productNames.get(i), productPrices.get(i), productImageUrls.get(i));
            }
        }
    }

    private void addTitleToTable(String title) {
        try {
            TableRow titleRow = (TableRow) getLayoutInflater().inflate(R.layout.table_row_title, null);
            TextView textViewTitle = titleRow.findViewById(R.id.textViewTitle);
            textViewTitle.setText(title);
            tableLayout.addView(titleRow);
        } catch (Exception e) {
            // Fallback title view
            TextView textView = new TextView(getContext());
            textView.setText(title);
            textView.setTextSize(20);
            textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
            textView.setTextColor(Color.parseColor("#1976D2"));
            textView.setPadding(16, 16, 16, 16);
            textView.setBackgroundColor(Color.parseColor("#E3F2FD"));
            tableLayout.addView(textView);
        }
    }

    private void addProductToTable(String productName, double productPrice, String imageUrl) {
        try {
            TableRow tableRow = (TableRow) getLayoutInflater().inflate(R.layout.table_row_products, null);

            // Load image with error handling
            ImageView imageView = tableRow.findViewById(R.id.imageViewProduct);
            try {
                Picasso.get()
                        .load(imageUrl)
                        .placeholder(createPlaceholderDrawable())
                        .error(createErrorDrawable())
                        .fit()
                        .centerCrop()
                        .into(imageView);
            } catch (Exception e) {
                imageView.setBackgroundColor(Color.LTGRAY);
            }

            TextView textViewName = tableRow.findViewById(R.id.textViewName);
            textViewName.setText(productName);

            TextView textViewPrice = tableRow.findViewById(R.id.textViewPrice);
            textViewPrice.setText(String.format("$%.2f", productPrice));

            setupQuantityControls(tableRow, productName, productPrice);
            tableLayout.addView(tableRow);

        } catch (Exception e) {
            // Fallback simple product row
            addFallbackProductRow(productName, productPrice, imageUrl);
        }
    }

    private void addFallbackProductRow(String productName, double productPrice, String imageUrl) {
        TableRow tableRow = new TableRow(getContext());
        tableRow.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT));
        tableRow.setPadding(16, 16, 16, 16);

        // Product name
        TextView nameText = new TextView(getContext());
        nameText.setText(productName);
        nameText.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
        tableRow.addView(nameText);

        // Price
        TextView priceText = new TextView(getContext());
        priceText.setText(String.format("$%.2f", productPrice));
        priceText.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
        tableRow.addView(priceText);

        tableLayout.addView(tableRow);
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

    private void setupQuantityControls(TableRow tableRow, String productName, double productPrice) {
        TextView textViewCount = tableRow.findViewById(R.id.textViewCount);
        Button minusButton = tableRow.findViewById(R.id.minusButton);
        Button plusButton = tableRow.findViewById(R.id.plusButton);
        Button buyButton = tableRow.findViewById(R.id.buyButton);

        if (minusButton != null) {
            minusButton.setOnClickListener(v -> {
                int count = Integer.parseInt(textViewCount.getText().toString());
                if (count > 0) {
                    count--;
                    textViewCount.setText(String.valueOf(count));
                }
            });
        }

        if (plusButton != null) {
            plusButton.setOnClickListener(v -> {
                int count = Integer.parseInt(textViewCount.getText().toString());
                count++;
                textViewCount.setText(String.valueOf(count));
            });
        }

        if (buyButton != null) {
            buyButton.setOnClickListener(v -> {
                int count = Integer.parseInt(textViewCount.getText().toString());
                if (count > 0) {
                    buyProduct(productName, productPrice, count);
                } else {
                    showSnackbar("Please specify a quantity greater than zero");
                }
            });
        }
    }

    private void buyProduct(String productName, double productPrice, int quantity) {
        double total = productPrice * quantity;
        String message = String.format("Added %d x %s to cart - Total: $%.2f",
                quantity, productName, total);

        showSnackbar(message);
        refreshTable();
    }

    private void showSnackbar(String message) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG)
                .setAction("OK", v -> {})
                .show();
    }
}