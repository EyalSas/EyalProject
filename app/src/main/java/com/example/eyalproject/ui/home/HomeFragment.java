package com.example.eyalproject.ui.home;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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

import com.example.eyalproject.MainActivity;
import com.example.eyalproject.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.squareup.picasso.Picasso;

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
            animateModernButtonClick(v, this::showRandomProductCarousel);
        });

        buttonContact.setOnClickListener(v -> {
            animateModernButtonClick(v, () -> {
                Navigation.findNavController(v).navigate(R.id.navigation_service);
            });
        });

        setupStatsCardsInteractions();
    }

    private void setupStatsCardsInteractions() {
        statsCard1.setOnClickListener(v -> {
            animateStatsCardClick(v, () -> showDesignedToast(
                    "📍 9 Stores Nationwide",
                    "Click 'About Our Stores' to find directions and contact info.",
                    R.color.stats_card_blue,
                    R.drawable.ic_store
            ));
        });

        statsCard2.setOnClickListener(v -> {
            animateStatsCardClick(v, () -> showDesignedToast(
                    "🎮 170+ Products Available",
                    "Explore our extensive catalog covering all your gaming needs.",
                    R.color.stats_card_green,
                    R.drawable.ic_store
            ));
        });

        statsCard3.setOnClickListener(v -> {
            animateStatsCardClick(v, () -> showDesignedToast(
                    "🛡️ 24/7 Support",
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
            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.custom_toast_layout, null);

            MaterialCardView toastCard = layout.findViewById(R.id.toastCard);
            ImageView toastIcon = layout.findViewById(R.id.toastIcon);
            TextView toastTitle = layout.findViewById(R.id.toastTitle);
            TextView toastMessage = layout.findViewById(R.id.toastMessage);

            int color = ContextCompat.getColor(requireContext(), colorRes);
            toastCard.setCardBackgroundColor(color);

            toastTitle.setText(title);
            toastMessage.setText(message);
            toastIcon.setImageResource(iconRes);

            Toast toast = new Toast(requireContext());
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setView(layout);
            toast.setGravity(Gravity.CENTER, 0, 0);

            layout.setAlpha(0f);
            layout.setTranslationY(-50f);

            toast.show();

            layout.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(300)
                    .start();

        } catch (Exception e) {
            showSimpleToast(title + ": " + message);
        }
    }

    private void showSimpleToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void startAnimations() {
        animateLogoEntrance();
        loadStaticStats();
        new Handler().postDelayed(this::loadServiceStats, 500);
        new Handler().postDelayed(this::animateContentEntrance, 800);
        new Handler().postDelayed(this::startPulseAnimation, 2000);
    }

    private void loadStaticStats() {
        statsValue1.setText("9");
        animateNumberCounter(statsValue1, 0, 9, 800);

        statsValue2.setText("170");
        animateNumberCounter(statsValue2, 0, 170, 1000);

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

    private String getUsername() {
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            return ((MainActivity) getActivity()).getUsername();
        }
        return null;
    }

    private void loadServiceStats() {
        String username = getUsername();
        if (username == null || getContext() == null) return;
        new Handler().postDelayed(this::animateStatsCelebration, 2000);
    }

    private void animateStatsCelebration() {
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

    private void showRandomProductCarousel() {
        if (getContext() == null || getView() == null) return;

        // 💡 FIX: Fetch products from Firebase Firestore asynchronously
        FirebaseFirestore.getInstance().collection("products")
                .limit(10)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded() || getView() == null) return;
                    if (querySnapshot.isEmpty()) {
                        showSimpleToast("No products available to display.");
                        return;
                    }

                    View popupView = getLayoutInflater().inflate(R.layout.product_carousel_popup, null);
                    LinearLayout horizontalContainer = popupView.findViewById(R.id.horizontalProductContainer);
                    Button btnClose = popupView.findViewById(R.id.btnCloseCarousel);

                    LayoutInflater productInflater = getLayoutInflater();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String name = doc.getString("name");
                        String imageUrl = doc.getString("imageUrl");
                        if (name != null && imageUrl != null) {
                            addProductCardToCarousel(productInflater, horizontalContainer, name, imageUrl);
                        }
                    }

                    PopupWindow carouselPopup = new PopupWindow(
                            popupView,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            true
                    );

                    carouselPopup.setElevation(20f);
                    carouselPopup.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
                    carouselPopup.showAtLocation(requireView(), Gravity.CENTER, 0, 0);

                    btnClose.setOnClickListener(v -> carouselPopup.dismiss());
                })
                .addOnFailureListener(e -> showSimpleToast("Failed to load products."));
    }

    private void addProductCardToCarousel(LayoutInflater inflater, LinearLayout container, String name, String imageUrl) {
        View productView = inflater.inflate(R.layout.table_row_products, container, false);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                (int) getResources().getDimension(R.dimen.product_card_width),
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMarginEnd((int) getResources().getDimension(R.dimen.activity_horizontal_margin));
        productView.setLayoutParams(params);

        ImageView imageView = productView.findViewById(R.id.imageViewProduct);
        if (imageView != null) {
            try {
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

        TextView textViewName = productView.findViewById(R.id.textViewName);
        if (textViewName != null) {
            textViewName.setText(name);
        }

        if (productView.findViewById(R.id.textViewPrice) != null) productView.findViewById(R.id.textViewPrice).setVisibility(View.GONE);
        if (productView.findViewById(R.id.buyButton) != null) productView.findViewById(R.id.buyButton).setVisibility(View.GONE);

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