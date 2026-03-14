package com.example.eyalproject.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.eyalproject.R;

public class HomeFragment extends Fragment {

    private LinearLayout categoriesLayout;
    private LinearLayout featuredProductsLayout;
    private LinearLayout newArrivalsLayout;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize views
        categoriesLayout = root.findViewById(R.id.categoriesLayout);
        featuredProductsLayout = root.findViewById(R.id.featuredProductsLayout);
        newArrivalsLayout = root.findViewById(R.id.newArrivalsLayout);

        // Set up categories
        setupCategories();

        return root;
    }

    private void setupCategories() {
        // Create category names and icons
        String[] categoryNames = {"TVs", "PCs", "Laptops", "Phones", "Accessories", "Gaming"};
        int[] categoryIcons = {
                R.drawable.ic_home_black_24dp,
                R.drawable.ic_home_black_24dp,
                R.drawable.ic_home_black_24dp,
                R.drawable.ic_home_black_24dp,
                R.drawable.ic_home_black_24dp,
                R.drawable.ic_home_black_24dp
        };

        // Add category views dynamically
        for (int i = 0; i < categoryNames.length; i++) {
            View categoryView = LayoutInflater.from(getContext()).inflate(R.layout.item_category, categoriesLayout, false);

            ImageView categoryImage = categoryView.findViewById(R.id.categoryImage);
            TextView categoryName = categoryView.findViewById(R.id.categoryName);

            categoryImage.setImageResource(categoryIcons[i]);
            categoryName.setText(categoryNames[i]);

            final int position = i;
            categoryView.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Category: " + categoryNames[position], Toast.LENGTH_SHORT).show();
            });

            categoriesLayout.addView(categoryView);
        }
    }
}