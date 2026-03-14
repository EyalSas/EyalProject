package com.example.eyalproject.ui.home;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.eyalproject.DBHelper;
import com.example.eyalproject.MainActivity;
import com.example.eyalproject.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private MaterialButton buttonAbout, buttonFeatures, buttonContact;
    private TextView welcomeText, subtitleText, statsValue1, statsValue2, statsValue3;
    private MaterialCardView statsCard1, statsCard2, statsCard3, logoCard;
    private LinearProgressIndicator loadingProgress;
    private ImageView logoImage;
    private Handler animationHandler = new Handler();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        initializeViews(rootView);
        setupClickListeners();
        startAnimations();

        return rootView;
    }

    private void initializeViews(View rootView) {
        buttonAbout = rootView.findViewById(R.id.buttonAbout);
        buttonFeatures = rootView.findViewById(R.id.buttonFeatures);
        buttonContact = rootView.findViewById(R.id.buttonContact);
        welcomeText = rootView.findViewById(R.id.welcomeText);
        subtitleText = rootView.findViewById(R.id.subtitleText);
        statsValue1 = rootView.findViewById(R.id.statsValue1);
        statsValue2 = rootView.findViewById(R.id.statsValue2);
        statsValue3 = rootView.findViewById(R.id.statsValue3);
        statsCard1 = rootView.findViewById(R.id.statsCard1);
        statsCard2 = rootView.findViewById(R.id.statsCard2);
        statsCard3 = rootView.findViewById(R.id.statsCard3);
        loadingProgress = rootView.findViewById(R.id.loadingProgress);
        logoImage = rootView.findViewById(R.id.logoImage);
        logoCard = rootView.findViewById(R.id.logoCard);
    }

    private void setupClickListeners() {
        buttonAbout.setOnClickListener(v -> {
            showLoadingProgress();
            animateModernButtonClick(v, () -> {
                hideLoadingProgress();
                Navigation.findNavController(v).navigate(R.id.action_navigation_home_to_aboutFragment);
            });
        });

        buttonFeatures.setOnClickListener(v -> {
            // 💡 FIX: Open the product carousel
            animateModernButtonClick(v, this::showRandomProductCarousel);
        });

        buttonContact.setOnClickListener(v -> {
            animateModernButtonClick(v, () -> {
                // Navigate directly to ServiceFragment
                Navigation.findNavController(v).navigate(R.id.navigation_service);
            });
        });

        setupStatsCardsInteractions();
    }

    private void setupStatsCardsInteractions() {
        statsCard1.setOnClickListener(v -> {
            animateStatsCardClick(v, () -> showDesignedToast(
                    "📍 9 Stores Nationwide", // 💡 FIXED TOAST CONTENT
                    "Click 'About Our Stores' to find directions and contact info.",
                    R.color.stats_card_blue,
                    R.drawable.ic_store
            ));
        });

        statsCard2.setOnClickListener(v -> {
            animateStatsCardClick(v, () -> showDesignedToast(
                    "🎮 170+ Products Available", // 💡 FIXED TOAST CONTENT
                    "Explore our extensive catalog covering all your gaming needs.",
                    R.color.stats_card_green,
                    R.drawable.ic_store
            ));
        });

        statsCard3.setOnClickListener(v -> {
            animateStatsCardClick(v, () -> showDesignedToast(
                    "🛡️ 24/7 Support", // 💡 FIXED TOAST CONTENT
                    "Need help? Contact support directly via the Service tab.",
                    R.color.stats_card_purple,
                    R.drawable.ic_store
            ));
        });
    }

    private void animateStatsCardClick(View v, Runnable action) {
        v.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(150)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .withEndAction(action)
                            .start();
                })
                .start();
    }

    private void showDesignedToast(String title, String message, int colorRes, int iconRes) {
        if (getContext() == null) return;

        try {
            // Create custom toast layout
            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.custom_toast_layout, null);

            // Configure toast appearance
            MaterialCardView toastCard = layout.findViewById(R.id.toastCard);
            ImageView toastIcon = layout.findViewById(R.id.toastIcon);
            TextView toastTitle = layout.findViewById(R.id.toastTitle);
            TextView toastMessage = layout.findViewById(R.id.toastMessage);

            // Set background color
            int color = ContextCompat.getColor(requireContext(), colorRes);
            toastCard.setCardBackgroundColor(color);

            // Set content
            toastTitle.setText(title);
            toastMessage.setText(message);
            toastIcon.setImageResource(iconRes);

            // Create and show toast
            Toast toast = new Toast(requireContext());
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setView(layout);

            // 💡 FIXED: Set gravity to CENTER to place the toast in the middle of the screen
            toast.setGravity(Gravity.CENTER, 0, 0);

            // Add entrance animation
            layout.setAlpha(0f);
            layout.setTranslationY(-50f);

            toast.show();

            // Animate toast entrance
            layout.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(300)
                    .start();

        } catch (Exception e) {
            // Fallback to simple toast if custom layout fails
            showSimpleToast(title + ": " + message);
        }
    }

    private void showSimpleToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void startAnimations() {
        animateLogoEntrance();
        // 💡 FIXED: Call loadStaticStats first
        loadStaticStats();
        new Handler().postDelayed(this::loadServiceStats, 500);
        new Handler().postDelayed(this::animateContentEntrance, 800);
        new Handler().postDelayed(this::startPulseAnimation, 2000);
    }

    // 💡 NEW METHOD: Load static stats (Stores and Products)
    private void loadStaticStats() {
        // Stats Card 1: Stores
        statsValue1.setText("9");
        animateNumberCounter(statsValue1, 0, 9, 800);

        // Stats Card 2: Products
        statsValue2.setText("170");
        animateNumberCounter(statsValue2, 0, 170, 1000);

        // Stats Card 3: Support
        statsValue3.setText("24/7");
    }

    private void animateLogoEntrance() {
        logoCard.setScaleX(0f);
        logoCard.setScaleY(0f);
        logoCard.setAlpha(0f);
        logoCard.setRotation(-180f);

        logoCard.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .rotation(0f)
                .setDuration(1000)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        startLogoFloatingAnimation();
    }

    private void startLogoFloatingAnimation() {
        ValueAnimator floatAnimator = ValueAnimator.ofFloat(0f, 1f);
        floatAnimator.setDuration(2000);
        floatAnimator.setRepeatCount(ValueAnimator.INFINITE);
        floatAnimator.setRepeatMode(ValueAnimator.REVERSE);
        floatAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            float translationY = (float) Math.sin(value * Math.PI * 2) * 10f;
            logoCard.setTranslationY(translationY);
        });
        floatAnimator.start();
    }

    private void startPulseAnimation() {
        ScaleAnimation pulse = new ScaleAnimation(1f, 1.05f, 1f, 1.05f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        pulse.setDuration(1500);
        pulse.setRepeatMode(Animation.REVERSE);
        pulse.setRepeatCount(Animation.INFINITE);
        logoCard.startAnimation(pulse);
    }

    private void animateContentEntrance() {
        animateTextEntrance(welcomeText, 0);
        animateTextEntrance(subtitleText, 100);
        animateStatsCardEntrance(statsCard1, 200);
        animateStatsCardEntrance(statsCard2, 300);
        animateStatsCardEntrance(statsCard3, 400);
        animateButtonEntrance(buttonAbout, 500);
        animateButtonEntrance(buttonFeatures, 600);
        animateButtonEntrance(buttonContact, 700);
    }

    private void animateTextEntrance(TextView textView, long delay) {
        textView.setAlpha(0f);
        textView.setTranslationY(30f);
        textView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setStartDelay(delay)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private void animateStatsCardEntrance(MaterialCardView card, long delay) {
        card.setScaleX(0f);
        card.setScaleY(0f);
        card.setAlpha(0f);

        card.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(500)
                .setStartDelay(delay)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private void animateButtonEntrance(View button, long delay) {
        button.setAlpha(0f);
        button.setTranslationY(50f);
        button.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setStartDelay(delay)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    // 💡 NEW HELPER METHOD: Get username from MainActivity
    private String getUsername() {
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            return ((MainActivity) getActivity()).getUsername();
        }
        return null;
    }

    // 💡 NEW METHOD: Load and display dynamic service stats
    private void loadServiceStats() {
        String username = getUsername();
        if (username == null || getContext() == null) return;

        DBHelper dbHelper = new DBHelper(getContext());
        // counts[0]=waiting, counts[1]=in_progress, counts[2]=completed
        int[] counts = dbHelper.getServiceCounts(username);

        new Handler().postDelayed(this::animateStatsCelebration, 2000);
    }
    private void animateStatsCelebration() {
        // This method triggers a subtle bounce effect on the three stat cards
        animateBounceEffect(statsCard1);
        new Handler().postDelayed(() -> animateBounceEffect(statsCard2), 150);
        new Handler().postDelayed(() -> animateBounceEffect(statsCard3), 300);
    }
    private void animateNumberCounter(TextView textView, int start, int end, long duration) {
        ValueAnimator animator = ValueAnimator.ofInt(start, end);
        animator.setDuration(duration);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(valueAnimator -> {
            String value = String.format(Locale.US, "%,d", (int)valueAnimator.getAnimatedValue());

            textView.setText(value + (end > 50 && !value.contains("24/7") ? "+" : ""));
        });
        animator.start();
    }


    private void animateBounceEffect(View view) {
        view.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(200)
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .start())
                .start();
    }

    private void animateModernButtonClick(View v, Runnable action) {
        v.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(80)
                .withEndAction(() -> {
                    v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(120)
                            .withEndAction(action)
                            .start();
                })
                .start();
    }

    private void showLoadingProgress() {
        loadingProgress.setVisibility(View.VISIBLE);
        loadingProgress.setAlpha(0f);
        loadingProgress.animate()
                .alpha(1f)
                .setDuration(300)
                .start();

        animationHandler.postDelayed(this::hideLoadingProgress, 1500);
    }

    private void hideLoadingProgress() {
        loadingProgress.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> loadingProgress.setVisibility(View.INVISIBLE))
                .start();
    }

    private void showModernFeatureDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("GameZone Features")
                .setMessage("• Latest Gaming Consoles\n• VR Gaming Experiences\n• Gaming Accessories\n• Repair Services\n• Tournament Hosting\n• Exclusive Merchandise\n• Gaming Lounge")
                .setPositiveButton("Explore", (dialog, which) -> {
                    showDesignedToast("🚀 Features", "Exploring all amazing GameZone features!", R.color.stats_card_purple, R.drawable.ic_store);
                })
                .setNegativeButton("Later", null)
                .show();
    }
    // In HomeFragment.java, add this block of methods:

    private void showRandomProductCarousel() {
        if (getContext() == null || getView() == null) return;

        // 1. Fetch random products (synchronous call for now, runs fast enough for 10 items)
        DBHelper dbHelper = new DBHelper(getContext());
        // Set a reasonable limit (e.g., 10 random products)
        List<String[]> randomProducts = dbHelper.getRandomProducts(10);

        if (randomProducts.isEmpty()) {
            showSimpleToast("No products available to display.");
            return;
        }

        // 2. Inflate the custom layout for the carousel popup
        // R.layout.product_carousel_popup MUST exist.
        View popupView = getLayoutInflater().inflate(R.layout.product_carousel_popup, null);

        // 💡 FIX: Resolve symbols using the inflated popupView
        LinearLayout horizontalContainer = popupView.findViewById(R.id.horizontalProductContainer);
        Button btnClose = popupView.findViewById(R.id.btnCloseCarousel);

        // 3. Populate the horizontal container
        LayoutInflater productInflater = getLayoutInflater();
        for (String[] product : randomProducts) {
            // product[0]=Name, product[2]=ImageUrl
            // The method should handle null or malformed arrays gracefully
            if (product.length >= 3) {
                addProductCardToCarousel(productInflater, horizontalContainer, product[0], product[2]);
            }
        }

        // 4. Create and show the PopupWindow
        PopupWindow carouselPopup = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true // Focusable
        );

        carouselPopup.setElevation(20f);
        carouselPopup.setBackgroundDrawable(new ColorDrawable(Color.WHITE));

        // Show in the center of the fragment's root view
        carouselPopup.showAtLocation(requireView(), Gravity.CENTER, 0, 0);

        // 5. Close button action
        btnClose.setOnClickListener(v -> carouselPopup.dismiss());
    }

    // Helper method to create and add a single product card to the carousel
    private void addProductCardToCarousel(LayoutInflater inflater, LinearLayout container, String name, String imageUrl) {
        // Reusing the table_row_products.xml layout (the existing product card)
        View productView = inflater.inflate(R.layout.table_row_products, container, false);

        // Set layout parameters for horizontal display
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                (int) getResources().getDimension(R.dimen.product_card_width), // 160dp width
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMarginEnd((int) getResources().getDimension(R.dimen.activity_horizontal_margin));
        productView.setLayoutParams(params);


        // 1. Image
        ImageView imageView = productView.findViewById(R.id.imageViewProduct);
        if (imageView != null) {
            try {
                // Using Picasso for loading image from URL
                Picasso.get()
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_store)
                        .error(R.drawable.ic_store)
                        .fit()
                        .centerCrop()
                        .into(imageView);
            } catch (Exception e) {
                imageView.setImageResource(R.drawable.ic_store);
            }
        }

        // 2. Name
        TextView textViewName = productView.findViewById(R.id.textViewName);
        if (textViewName != null) {
            textViewName.setText(name);
        }

        // 💡 Hide unnecessary views (Price, Quantity Controls, Buy Button)
        if (productView.findViewById(R.id.textViewPrice) != null) productView.findViewById(R.id.textViewPrice).setVisibility(View.GONE);
        if (productView.findViewById(R.id.buyButton) != null) productView.findViewById(R.id.buyButton).setVisibility(View.GONE);

        // Hide the LinearLayout containing the quantity buttons
        View quantityContainer = productView.findViewById(R.id.minusButton).getParent() instanceof LinearLayout ?
                (View) productView.findViewById(R.id.minusButton).getParent() : null;
        if (quantityContainer != null) {
            quantityContainer.setVisibility(View.GONE);
        }


        container.addView(productView);
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        animationHandler.removeCallbacksAndMessages(null);
    }
}