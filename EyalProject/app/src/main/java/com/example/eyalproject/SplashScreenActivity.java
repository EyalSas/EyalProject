package com.example.eyalproject;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashScreenActivity extends AppCompatActivity {
    private static final long SPLASH_SCREEN_TIMEOUT = 4000; // 5 seconds
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);
        // Reference to the ImageView containing your app logo
        ImageView logoImageView = findViewById(R.id.logoImageView);
        // Define the scale animation
        ScaleAnimation scaleAnimation = new ScaleAnimation(
                1f, 1.5f, // Start and end scale X
                1f, 1.5f, // Start and end scale Y
                Animation.RELATIVE_TO_SELF, 0.5f, // Pivot X (center)
                Animation.RELATIVE_TO_SELF, 0.5f // Pivot Y (center)
        );
        scaleAnimation.setDuration(2500); // 2.5 seconds for each direction
        scaleAnimation.setRepeatCount(1); // Repeat the animation once
        scaleAnimation.setRepeatMode(Animation.REVERSE); // Reverse the animation after completing
        // Start the animation
        logoImageView.startAnimation(scaleAnimation);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Start your main activity after the splash screen timeout
                Intent intent = new Intent(SplashScreenActivity.this, WelcomeActivity.class);
                startActivity(intent);
                finish(); // Finish the splash screen activity to prevent it from being shown again on back press
            }
        }, SPLASH_SCREEN_TIMEOUT);
    }
}

