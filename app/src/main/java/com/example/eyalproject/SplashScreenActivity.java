package com.example.eyalproject;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

public class SplashScreenActivity extends AppCompatActivity {
    private static final long SPLASH_SCREEN_TIMEOUT = 4000;

    private ImageView logoImageView, loadingSpinner, bgCircle1, bgCircle2, bgCircle3;
    private TextView welcomeText, taglineText, loadingText, versionText;
    private LinearProgressIndicator progressBar;
    private MaterialCardView logoCard;
    private LinearLayout loadingContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initializeViews();
        setupAnimations();
    }

    private void initializeViews() {
        logoCard = findViewById(R.id.logoCard);
        logoImageView = findViewById(R.id.logoImageView);
        welcomeText = findViewById(R.id.welcomeText);
        taglineText = findViewById(R.id.taglineText);
        loadingSpinner = findViewById(R.id.loadingSpinner);
        loadingText = findViewById(R.id.loadingText);
        versionText = findViewById(R.id.versionText);
        progressBar = findViewById(R.id.progressBar);
        bgCircle1 = findViewById(R.id.bgCircle1);
        bgCircle2 = findViewById(R.id.bgCircle2);
        bgCircle3 = findViewById(R.id.bgCircle3);
        loadingContainer = findViewById(R.id.loadingContainer);

        // Initially hide elements that will be animated in
        welcomeText.setAlpha(0f);
        taglineText.setAlpha(0f);
        loadingContainer.setAlpha(0f); // Hide the entire loading container
        versionText.setAlpha(0f);
        progressBar.setAlpha(0f);

        // BUT keep loadingText visible initially since it's inside loadingContainer
        // We'll animate it separately for the pulse effect
    }

    private void setupAnimations() {
        // Wait for layout to be drawn before starting animations
        logoCard.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                logoCard.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                startLogoAnimation();
            }
        });
    }

    private void startLogoAnimation() {
        // Logo card entrance animation
        AnimatorSet logoAnimatorSet = new AnimatorSet();

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(logoCard, "scaleX", 0f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(logoCard, "scaleY", 0f, 1f);
        ObjectAnimator rotation = ObjectAnimator.ofFloat(logoCard, "rotation", -180f, 0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(logoCard, "alpha", 0f, 1f);

        logoAnimatorSet.playTogether(scaleX, scaleY, rotation, alpha);
        logoAnimatorSet.setDuration(1200);
        logoAnimatorSet.setInterpolator(new OvershootInterpolator(1.2f));
        logoAnimatorSet.start();

        // Logo image pulse animation
        startPulseAnimation(logoImageView);

        // Staggered text animations
        new Handler().postDelayed(this::animateWelcomeText, 400);
        new Handler().postDelayed(this::animateTaglineText, 800);
        new Handler().postDelayed(this::animateLoadingSection, 1200);
        new Handler().postDelayed(this::animateProgressAndVersion, 1600);

        // Start background animations
        startBackgroundAnimations();

        // Navigate to next activity
        new Handler().postDelayed(this::navigateToNextActivity, SPLASH_SCREEN_TIMEOUT);
    }

    private void animateWelcomeText() {
        ObjectAnimator alpha = ObjectAnimator.ofFloat(welcomeText, "alpha", 0f, 1f);
        ObjectAnimator translationY = ObjectAnimator.ofFloat(welcomeText, "translationY", 50f, 0f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(alpha, translationY);
        animatorSet.setDuration(800);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.start();
    }

    private void animateTaglineText() {
        ObjectAnimator alpha = ObjectAnimator.ofFloat(taglineText, "alpha", 0f, 1f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(taglineText, "scaleX", 0.8f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(taglineText, "scaleY", 0.8f, 1f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(alpha, scaleX, scaleY);
        animatorSet.setDuration(600);
        animatorSet.setInterpolator(new BounceInterpolator());
        animatorSet.start();
    }

    private void animateLoadingSection() {
        // First, fade in the entire loading container
        ObjectAnimator containerAlpha = ObjectAnimator.ofFloat(loadingContainer, "alpha", 0f, 1f);
        containerAlpha.setDuration(600);
        containerAlpha.start();

        // Then start the spinner rotation
        ObjectAnimator rotation = ObjectAnimator.ofFloat(loadingSpinner, "rotation", 0f, 360f);
        rotation.setDuration(1000);
        rotation.setRepeatCount(ObjectAnimator.INFINITE);
        rotation.setInterpolator(new AccelerateDecelerateInterpolator());
        rotation.start();

        // Start loading text pulse animation
        startPulseAnimation(loadingText);
    }

    private void animateProgressAndVersion() {
        // Progress bar animation
        ObjectAnimator progressAlpha = ObjectAnimator.ofFloat(progressBar, "alpha", 0f, 1f);
        progressAlpha.setDuration(600);
        progressAlpha.start();

        // Version text animation
        ObjectAnimator versionAlpha = ObjectAnimator.ofFloat(versionText, "alpha", 0f, 1f);
        versionAlpha.setDuration(600);
        versionAlpha.start();
    }

    private void startBackgroundAnimations() {
        // Background circle 1 animation
        ObjectAnimator circle1Rotate = ObjectAnimator.ofFloat(bgCircle1, "rotation", 0f, 360f);
        circle1Rotate.setDuration(20000);
        circle1Rotate.setRepeatCount(ObjectAnimator.INFINITE);
        circle1Rotate.setInterpolator(new AccelerateDecelerateInterpolator());
        circle1Rotate.start();

        // Background circle 2 animation
        ObjectAnimator circle2Rotate = ObjectAnimator.ofFloat(bgCircle2, "rotation", 360f, 0f);
        circle2Rotate.setDuration(15000);
        circle2Rotate.setRepeatCount(ObjectAnimator.INFINITE);
        circle2Rotate.setInterpolator(new AccelerateDecelerateInterpolator());
        circle2Rotate.start();

        // Background circle 3 pulse animation
        startCirclePulseAnimation(bgCircle3);
    }

    private void startCirclePulseAnimation(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.5f, 1f);
        scaleX.setDuration(3000);
        scaleX.setRepeatCount(ObjectAnimator.INFINITE);

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.5f, 1f);
        scaleY.setDuration(3000);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);

        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 0.08f, 0.15f, 0.08f);
        alpha.setDuration(3000);
        alpha.setRepeatCount(ObjectAnimator.INFINITE);

        scaleX.start();
        scaleY.start();
        alpha.start();
    }

    private void startPulseAnimation(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f);
        scaleX.setDuration(2000);
        scaleX.setRepeatCount(ObjectAnimator.INFINITE);

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f);
        scaleY.setDuration(2000);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);

        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());

        scaleX.start();
        scaleY.start();
    }

    private void navigateToNextActivity() {
        // Create exit animations before transitioning
        AnimatorSet exitAnimator = new AnimatorSet();

        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(logoCard, "alpha", 1f, 0f);
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(logoCard, "scaleX", 1f, 0.8f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(logoCard, "scaleY", 1f, 0.8f);

        exitAnimator.playTogether(fadeOut, scaleDownX, scaleDownY);
        exitAnimator.setDuration(500);
        exitAnimator.setInterpolator(new AccelerateInterpolator());
        exitAnimator.start();

        exitAnimator.addListener(new android.animation.Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(android.animation.Animator animation) {}

            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                Intent intent = new Intent(SplashScreenActivity.this, WelcomeActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }

            @Override
            public void onAnimationCancel(android.animation.Animator animation) {}

            @Override
            public void onAnimationRepeat(android.animation.Animator animation) {}
        });
    }

    @Override
    protected void onDestroy() {
        // Clean up animations to prevent memory leaks
        if (logoCard != null) logoCard.clearAnimation();
        if (loadingSpinner != null) loadingSpinner.clearAnimation();
        if (loadingText != null) loadingText.clearAnimation();
        if (bgCircle1 != null) bgCircle1.clearAnimation();
        if (bgCircle2 != null) bgCircle2.clearAnimation();
        if (bgCircle3 != null) bgCircle3.clearAnimation();
        super.onDestroy();
    }
}