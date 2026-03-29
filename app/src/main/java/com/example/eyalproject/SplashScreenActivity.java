package com.example.eyalproject;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

public class SplashScreenActivity extends AppCompatActivity {
    private static final long SPLASH_SCREEN_TIMEOUT = 4000;

    // bgCircle3, loadingSpinner, loadingContainer removed — not in new layout
    private ImageView bgCircle1, bgCircle2;
    private TextView welcomeText, taglineText, loadingText, versionText;
    private LinearProgressIndicator progressBar;
    private MaterialCardView logoCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initializeViews();
        setupAnimations();
    }

    private void initializeViews() {
        logoCard      = findViewById(R.id.logoCard);
        welcomeText   = findViewById(R.id.welcomeText);
        taglineText   = findViewById(R.id.taglineText);
        loadingText   = findViewById(R.id.loadingText);
        versionText   = findViewById(R.id.versionText);
        progressBar   = findViewById(R.id.progressBar);
        bgCircle1     = findViewById(R.id.bgCircle1);
        bgCircle2     = findViewById(R.id.bgCircle2);

        // Hide elements that animate in
        welcomeText.setAlpha(0f);
        taglineText.setAlpha(0f);
        loadingText.setAlpha(0f);
        versionText.setAlpha(0f);
        progressBar.setAlpha(0f);
    }

    private void setupAnimations() {
        logoCard.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                logoCard.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                startLogoAnimation();
            }
        });
    }

    private void startLogoAnimation() {
        // Logo card entrance
        AnimatorSet logoSet = new AnimatorSet();
        logoSet.playTogether(
                ObjectAnimator.ofFloat(logoCard, "scaleX",   0f, 1f),
                ObjectAnimator.ofFloat(logoCard, "scaleY",   0f, 1f),
                ObjectAnimator.ofFloat(logoCard, "rotation", -180f, 0f),
                ObjectAnimator.ofFloat(logoCard, "alpha",    0f, 1f)
        );
        logoSet.setDuration(1200);
        logoSet.setInterpolator(new OvershootInterpolator(1.2f));
        logoSet.start();

        // Staggered text + loading animations
        new Handler().postDelayed(this::animateWelcomeText,  400);
        new Handler().postDelayed(this::animateTaglineText,  800);
        new Handler().postDelayed(this::animateLoadingSection, 1200);
        new Handler().postDelayed(this::animateProgressAndVersion, 1600);

        startBackgroundAnimations();

        new Handler().postDelayed(this::navigateToNextActivity, SPLASH_SCREEN_TIMEOUT);
    }

    private void animateWelcomeText() {
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(welcomeText, "alpha",        0f, 1f),
                ObjectAnimator.ofFloat(welcomeText, "translationY", 50f, 0f)
        );
        set.setDuration(800);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.start();
    }

    private void animateTaglineText() {
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(taglineText, "alpha",  0f, 1f),
                ObjectAnimator.ofFloat(taglineText, "scaleX", 0.8f, 1f),
                ObjectAnimator.ofFloat(taglineText, "scaleY", 0.8f, 1f)
        );
        set.setDuration(600);
        set.setInterpolator(new BounceInterpolator());
        set.start();
    }

    private void animateLoadingSection() {
        // Fade in loading text (replaces old loadingContainer)
        ObjectAnimator textAlpha = ObjectAnimator.ofFloat(loadingText, "alpha", 0f, 1f);
        textAlpha.setDuration(600);
        textAlpha.start();

        startPulseAnimation(loadingText);
    }

    private void animateProgressAndVersion() {
        ObjectAnimator progressAlpha = ObjectAnimator.ofFloat(progressBar, "alpha", 0f, 1f);
        progressAlpha.setDuration(600);
        progressAlpha.start();

        ObjectAnimator versionAlpha = ObjectAnimator.ofFloat(versionText, "alpha", 0f, 1f);
        versionAlpha.setDuration(600);
        versionAlpha.start();
    }

    private void startBackgroundAnimations() {
        if (bgCircle1 != null) {
            ObjectAnimator c1 = ObjectAnimator.ofFloat(bgCircle1, "rotation", 0f, 360f);
            c1.setDuration(20000);
            c1.setRepeatCount(ObjectAnimator.INFINITE);
            c1.setInterpolator(new AccelerateDecelerateInterpolator());
            c1.start();
        }
        if (bgCircle2 != null) {
            ObjectAnimator c2 = ObjectAnimator.ofFloat(bgCircle2, "rotation", 360f, 0f);
            c2.setDuration(15000);
            c2.setRepeatCount(ObjectAnimator.INFINITE);
            c2.setInterpolator(new AccelerateDecelerateInterpolator());
            c2.start();
        }
    }

    private void startPulseAnimation(android.view.View view) {
        ObjectAnimator sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f);
        sx.setDuration(2000);
        sy.setDuration(2000);
        sx.setRepeatCount(ObjectAnimator.INFINITE);
        sy.setRepeatCount(ObjectAnimator.INFINITE);
        sx.setInterpolator(new AccelerateDecelerateInterpolator());
        sy.setInterpolator(new AccelerateDecelerateInterpolator());
        sx.start();
        sy.start();
    }

    private void navigateToNextActivity() {
        AnimatorSet exitSet = new AnimatorSet();
        exitSet.playTogether(
                ObjectAnimator.ofFloat(logoCard, "alpha",  1f, 0f),
                ObjectAnimator.ofFloat(logoCard, "scaleX", 1f, 0.8f),
                ObjectAnimator.ofFloat(logoCard, "scaleY", 1f, 0.8f)
        );
        exitSet.setDuration(500);
        exitSet.setInterpolator(new AccelerateInterpolator());
        exitSet.start();

        exitSet.addListener(new android.animation.Animator.AnimatorListener() {
            @Override public void onAnimationStart(android.animation.Animator a) {}
            @Override public void onAnimationCancel(android.animation.Animator a) {}
            @Override public void onAnimationRepeat(android.animation.Animator a) {}

            @Override
            public void onAnimationEnd(android.animation.Animator a) {
                Intent intent = new Intent(SplashScreenActivity.this, WelcomeActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (logoCard  != null) logoCard.clearAnimation();
        if (loadingText != null) loadingText.clearAnimation();
        if (bgCircle1 != null) bgCircle1.clearAnimation();
        if (bgCircle2 != null) bgCircle2.clearAnimation();
        super.onDestroy();
    }
}