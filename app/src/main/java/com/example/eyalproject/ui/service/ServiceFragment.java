package com.example.eyalproject.ui.service;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.eyalproject.DBHelper;
import com.example.eyalproject.MainActivity;
import com.example.eyalproject.R;
// Correct binding class for fragment_service.xml
import com.example.eyalproject.databinding.FragmentServiceBinding;

import java.util.List;

public class ServiceFragment extends Fragment {

    private FragmentServiceBinding binding;
    private DBHelper dbHelper;
    private String username;

    // Define the admin username
    private static final String ADMIN_USERNAME = "or";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentServiceBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        if (getActivity() != null) {
            username = ((MainActivity) getActivity()).getUsername();
        }
        dbHelper = new DBHelper(getContext());

        initializeUI();

        loadServicesFromDatabase();

        return root;
    }

    private void initializeUI() {
        Button addServiceBtn = binding.btnAddService;
        EditText serviceNameEt = binding.editTextServiceName;
        LinearLayout statsLayout = binding.statsOverviewLayout;

        boolean isAdmin = ADMIN_USERNAME.equalsIgnoreCase(username);

        if (isAdmin) {
            addServiceBtn.setVisibility(View.GONE);
            serviceNameEt.setVisibility(View.GONE);
            statsLayout.setVisibility(View.GONE);
            binding.servicesListTitle.setText("All User Service Requests (Admin View)");
        } else {
            addServiceBtn.setVisibility(View.VISIBLE);
            serviceNameEt.setVisibility(View.VISIBLE);
            statsLayout.setVisibility(View.VISIBLE);
            binding.servicesListTitle.setText("Your Service Requests");

            addServiceBtn.setOnClickListener(v -> {
                String serviceName = serviceNameEt.getText().toString().trim();
                if (!serviceName.isEmpty()) {
                    addService(serviceName);
                    serviceNameEt.setText("");
                } else {
                    Toast.makeText(getContext(), "Please enter a service name", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadServicesFromDatabase() {
        updateServiceCounts();
        loadServiceItems();
    }

    private void addService(String serviceName) {
        int userId = dbHelper.getUserId(username);
        if (userId == -1) {
            Toast.makeText(getContext(), "User not found", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean success = dbHelper.addService(serviceName, userId);
        if (success) {
            // 💡 FIXED: Notify the admin about the new service request
            dbHelper.notifyAdminNewService(serviceName, username);

            Toast.makeText(getContext(), "Service added successfully (Waiting)", Toast.LENGTH_SHORT).show();
            loadServicesFromDatabase();
        } else {
            Toast.makeText(getContext(), "Failed to add service", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateServiceCounts() {
        if (ADMIN_USERNAME.equalsIgnoreCase(username)) {
            return;
        }
        int[] counts = dbHelper.getServiceCounts(username);

        // 🟢 FIX APPLIED HERE: Use .getRoot() on the included layout bindings to get the View.
        View waitingCard = binding.cardWaiting.getRoot();
        View completedCard = binding.cardCompleted.getRoot();

        TextView waitingCount = waitingCard.findViewById(R.id.waitingCount);
        TextView completedCount = completedCard.findViewById(R.id.completedCount);

        waitingCount.setText(String.valueOf(counts[0]));
        completedCount.setText(String.valueOf(counts[2]));
    }

    private void loadServiceItems() {
        LinearLayout servicesContainer = binding.servicesContainer;
        servicesContainer.removeAllViews();

        boolean isAdmin = ADMIN_USERNAME.equalsIgnoreCase(username);
        List<String> rawServicesList;

        if (isAdmin) {
            rawServicesList = dbHelper.getAllServicesForAllUsers();
        } else {
            rawServicesList = dbHelper.getUserServices(username);
        }

        for (String item : rawServicesList) {
            View serviceCard = createServiceCard(item, isAdmin);
            servicesContainer.addView(serviceCard);
        }
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private View createServiceCard(String item, boolean isAdmin) {
        Context context = getContext();

        // 1. Root Card Layout
        LinearLayout rootLayout = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 0, dpToPx(10));
        rootLayout.setLayoutParams(layoutParams);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        rootLayout.setBackgroundColor(Color.parseColor("#2C2C2C"));

        // 2. Parse Data
        String serviceName = "";
        String status = "";
        String ownerUsername = "";

        try {
            String[] parts = item.split(" - ");
            status = parts[1].trim();

            // 💡 FIXED: Ensure correct parsing for Admin view
            if (isAdmin) {
                // Format: "ServiceName by OwnerUsername - Status"
                String[] userParts = parts[0].split(" by ");
                serviceName = userParts[0].trim();
                ownerUsername = userParts[1].trim();
            } else {
                serviceName = parts[0].trim();
            }
        } catch (Exception e) {
            serviceName = "Error Parsing Service";
            status = "unknown";
        }

        // Determine status color
        int statusColor = Color.parseColor("#FFFFFF");
        if ("completed".equalsIgnoreCase(status)) {
            statusColor = Color.parseColor("#4CAF50"); // Green
        } else if ("waiting".equalsIgnoreCase(status)) {
            statusColor = Color.parseColor("#FF6347"); // Red
        }

        // 3. Service Name (Title)
        TextView nameTextView = new TextView(context);
        nameTextView.setText(serviceName);
        nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        nameTextView.setTextColor(Color.WHITE);
        nameTextView.setTypeface(null, android.graphics.Typeface.BOLD);
        rootLayout.addView(nameTextView);

        // 4. Owner (Admin Only)
        if (isAdmin) {
            TextView ownerTextView = new TextView(context);
            // 💡 FIXED: Display the username clearly
            ownerTextView.setText("Requested by: " + ownerUsername);
            ownerTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            ownerTextView.setTextColor(Color.parseColor("#AAAAAA"));
            rootLayout.addView(ownerTextView);
        }

        // 5. Status and Action Button Row
        LinearLayout statusRow = new LinearLayout(context);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dpToPx(8);
        statusRow.setLayoutParams(rowParams);

        // Status Label
        TextView statusLabel = new TextView(context);
        statusLabel.setText("Status: ");
        statusLabel.setTextColor(Color.parseColor("#999999"));
        statusLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        statusRow.addView(statusLabel);

        // Status Text
        TextView statusTextView = new TextView(context);
        statusTextView.setText(status.toUpperCase());
        statusTextView.setTextColor(statusColor);
        statusTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        statusTextView.setTypeface(null, android.graphics.Typeface.BOLD);
        statusRow.addView(statusTextView);

        // 6. Admin Action Button
        if (isAdmin && !"completed".equalsIgnoreCase(status)) {
            Button adminButton = new Button(context);
            adminButton.setText("Complete");
            adminButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            adminButton.setBackgroundColor(Color.parseColor("#4CAF50"));
            adminButton.setTextColor(Color.WHITE);

            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                    0, dpToPx(35));
            buttonParams.leftMargin = dpToPx(16);
            buttonParams.weight = 1.0f;
            adminButton.setLayoutParams(buttonParams);

            final String finalServiceName = serviceName;
            final String finalOwnerUsername = ownerUsername;
            adminButton.setOnClickListener(v -> handleAdminAction(finalServiceName, finalOwnerUsername));

            statusRow.addView(adminButton);
        }

        rootLayout.addView(statusRow);

        return rootLayout;
    }

    private void handleAdminAction(String serviceName, String ownerUsername) {
        int userId = dbHelper.getUserId(ownerUsername);
        if (userId == -1) {
            Toast.makeText(getContext(), "Error: Service owner not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        int serviceId = dbHelper.getServiceIdByNameAndUser(serviceName, ownerUsername);

        if (serviceId != -1) {
            String newStatus = "completed";
            dbHelper.updateServiceStatus(serviceId, newStatus);
            loadServicesFromDatabase();
            Toast.makeText(getContext(), serviceName + " status set to " + newStatus + " for " + ownerUsername, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Error: Service ID not found. Service might be a duplicate.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (dbHelper != null && username != null) {
            loadServicesFromDatabase();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}