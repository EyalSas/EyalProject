package com.example.eyalproject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.eyalproject.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private String username;
    private String email;
    private NavController navController;
    private DBHelper dbHelper;

    // 💡 FIX: Define SharedPreferences name for session persistence
    private static final String PREFS_NAME = "AppPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Retrieve the username passed from Login/RegisterActivity FIRST
        username = getIntent().getStringExtra("USERNAME");

        // 💡 FIX: Session Persistence using SharedPreferences
        if (username == null) {
            // App started without intent (e.g., from home screen), try to restore session
            username = prefs.getString("USERNAME", null);
        } else {
            // Logged in via Intent (just typed password), save to session for next time
            prefs.edit().putString("USERNAME", username).apply();
        }

        // 💡 CRITICAL FIX: Session Check
        // If still no username (not passed via intent AND not saved in prefs), redirect
        if (username == null) {
            Intent welcomeIntent = new Intent(MainActivity.this, WelcomeActivity.class);
            // Flags ensure a clean break and prevent the back button from returning here
            welcomeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(welcomeIntent);
            finish(); // Close this MainActivity instance
            return; // STOP execution of onCreate
        }

        // --- ONLY execute the following UI setup IF username is NOT null (user is logged in) ---

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        dbHelper = new DBHelper(this);

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_store,
                R.id.navigation_service)
                .build();

        navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        if (username != null) {
            TextView usernameTextView = findViewById(R.id.usernameTextView);
            if (usernameTextView != null) {
                usernameTextView.setText("Welcome, " + username + "!");
                usernameTextView.setTextColor(Color.WHITE);
            }

            // Get user email from database
            email = dbHelper.getUserEmail(username);

            // Set up profile icon click listener
            setupProfileIcon();

            // Set up global action to pass username to ServiceFragment
            setupNavigationWithUsername();
        }

        navView.getMenu().findItem(R.id.navigation_cart).setVisible(true);

        // Set a listener to handle the navigation to the "Order" fragment
        navView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_cart) {

                // 1. Get the current user ID
                int userId = -1;
                if (username != null) {
                    userId = dbHelper.getUserId(username);
                }

                // 2. CHECK: Only proceed if user is valid and cart is not empty
                if (userId != -1) {
                    // 💡 FIX: Use a lightweight COUNT query instead of fetching all strings to the UI thread
                    int cartCount = dbHelper.getCartItemCount(userId);

                    // Check if the cart is not empty before navigating
                    if (cartCount == 0) {
                        // Show a custom toast message indicating that the cart is empty
                        LayoutInflater inflater = getLayoutInflater();
                        View layout = inflater.inflate(R.layout.toast_layout,
                                (ViewGroup) findViewById(R.id.custom_toast_container));

                        TextView text = layout.findViewById(R.id.toast_text);
                        text.setText("Cart is empty");

                        Toast toast = new Toast(getApplicationContext());
                        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                        toast.setDuration(Toast.LENGTH_SHORT);
                        toast.setView(layout);
                        toast.show();
                        return false; // Do not perform navigation
                    }
                } else {
                    // Safety check if username was somehow null
                    Toast.makeText(this, "Error: User data missing.", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
            return NavigationUI.onNavDestinationSelected(item, navController);
        });
    }

    private void setupProfileIcon() {
        ImageView profileIcon = findViewById(R.id.user_profile_image);
        if (profileIcon != null) {
            profileIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showProfileDialog();
                }
            });
        }
    }


    private void logoutUser() {
        // 💡 FIX: Clear the SharedPreferences session on logout
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().clear().apply();

        // Navigate back to WelcomeActivity
        Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Close MainActivity
    }

    private void setupNavigationWithUsername() {
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.navigation_service) {
                // Create a new instance of ServiceFragment with arguments
                Bundle bundle = new Bundle();
                bundle.putString("USERNAME", username);
            }
        });
    }

    // Method to get the current username (can be used by other fragments)
    public String getUsername() {
        return username;
    }

    // Handle back button press for navigation
    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
    private void showProfileDialog() {
        // Inflate the dropdown layout
        View dropdownView = getLayoutInflater().inflate(R.layout.profile_dropdown, null);

        // Initialize views
        TextView tvUsername = dropdownView.findViewById(R.id.tvDropdownUsername);
        TextView tvEmail = dropdownView.findViewById(R.id.tvDropdownEmail);
        Button btnLogout = dropdownView.findViewById(R.id.btnDropdownLogout);

        // Set user data
        tvUsername.setText(username);
        tvEmail.setText(email != null ? email : "Not available");

        // Create PopupWindow
        PopupWindow popupWindow = new PopupWindow(
                dropdownView,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true
        );

        // Set background and animation
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setElevation(16f);
        popupWindow.setAnimationStyle(android.R.style.Animation_Dialog);

        // Get profile icon location
        ImageView profileIcon = findViewById(R.id.user_profile_image);

        // Calculate position - show below the profile icon
        popupWindow.showAsDropDown(profileIcon, -180, 10); // Adjust x-offset and y-offset as needed

        // Set logout button click listener
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
                logoutUser();
            }
        });

        // Dismiss when touching outside
        dropdownView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    popupWindow.dismiss();
                    return true;
                }
                return false;
            }
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}