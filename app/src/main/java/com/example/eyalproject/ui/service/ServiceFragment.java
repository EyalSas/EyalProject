package com.example.eyalproject.ui.service;

import android.content.Context;
import android.content.Intent;
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

import com.example.eyalproject.MainActivity;
import com.example.eyalproject.R;
import com.example.eyalproject.databinding.FragmentServiceBinding;
import com.example.eyalproject.ui.cart.CartReminderReceiver;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class ServiceFragment extends Fragment {

    private FragmentServiceBinding binding;
    private String username;

    private static final String ADMIN_USERNAME = "admin";
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentServiceBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        if (getActivity() != null) {
            username = ((MainActivity) getActivity()).getUsername();
        }

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
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // 💡 FIX: Save service request to Firebase
        Map<String, Object> serviceRequest = new HashMap<>();
        serviceRequest.put("serviceName", serviceName);
        serviceRequest.put("ownerUid", uid);
        serviceRequest.put("ownerUsername", username);
        serviceRequest.put("status", "waiting");
        serviceRequest.put("timestamp", System.currentTimeMillis());

        FirebaseFirestore.getInstance().collection("services").add(serviceRequest)
                .addOnSuccessListener(documentReference -> {
                    if (!isAdded() || getContext() == null) return;

                    // Trigger notification via Broadcast Receiver
                    Intent intent = new Intent(getContext(), CartReminderReceiver.class);
                    intent.setAction("NEW_SERVICE_REQUEST");
                    intent.putExtra("message", username + " has requested a new service: " + serviceName);
                    intent.putExtra("title", "🔔 New Service Request!");
                    getContext().sendBroadcast(intent);

                    Toast.makeText(getContext(), "Service added successfully (Waiting)", Toast.LENGTH_SHORT).show();
                    loadServicesFromDatabase();
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to add service", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateServiceCounts() {
        if (ADMIN_USERNAME.equalsIgnoreCase(username) || FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // 💡 FIX: Fetch counts asynchronously from Firebase
        FirebaseFirestore.getInstance().collection("services")
                .whereEqualTo("ownerUid", uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded() || getContext() == null) return;

                    int waiting = 0;
                    int completed = 0;

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String status = doc.getString("status");
                        if ("waiting".equals(status)) waiting++;
                        if ("completed".equals(status)) completed++;
                    }

                    View waitingCard = binding.cardWaiting.getRoot();
                    View completedCard = binding.cardCompleted.getRoot();
                    TextView waitingCount = waitingCard.findViewById(R.id.waitingCount);
                    TextView completedCount = completedCard.findViewById(R.id.completedCount);

                    waitingCount.setText(String.valueOf(waiting));
                    completedCount.setText(String.valueOf(completed));
                });
    }

    private void loadServiceItems() {
        LinearLayout servicesContainer = binding.servicesContainer;
        servicesContainer.removeAllViews();

        boolean isAdmin = ADMIN_USERNAME.equalsIgnoreCase(username);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Query query;

        // 💡 FIX: Query logic branches for Admin vs User
        if (isAdmin) {
            query = db.collection("services").orderBy("timestamp", Query.Direction.ASCENDING);
        } else {
            if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            query = db.collection("services").whereEqualTo("ownerUid", uid).orderBy("timestamp", Query.Direction.DESCENDING);
        }

        query.get().addOnSuccessListener(querySnapshot -> {
            if (!isAdded() || getContext() == null) return;
            servicesContainer.removeAllViews();

            for (QueryDocumentSnapshot doc : querySnapshot) {
                String docId = doc.getId();
                String serviceName = doc.getString("serviceName");
                String status = doc.getString("status");
                String ownerUsername = doc.getString("ownerUsername");

                View serviceCard = createServiceCard(docId, serviceName, status, ownerUsername, isAdmin);
                servicesContainer.addView(serviceCard);
            }
        }).addOnFailureListener(e -> {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Failed to load services", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    // 💡 FIX: Pass Firebase Document ID so the Admin can update the exact record
    private View createServiceCard(String docId, String serviceName, String status, String ownerUsername, boolean isAdmin) {
        Context context = getContext();

        LinearLayout rootLayout = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 0, dpToPx(10));
        rootLayout.setLayoutParams(layoutParams);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        rootLayout.setBackgroundColor(Color.parseColor("#2C2C2C"));

        if (serviceName == null) serviceName = "Unknown Service";
        if (status == null) status = "unknown";

        int statusColor = Color.parseColor("#FFFFFF");
        if ("completed".equalsIgnoreCase(status)) {
            statusColor = Color.parseColor("#4CAF50");
        } else if ("waiting".equalsIgnoreCase(status)) {
            statusColor = Color.parseColor("#FF6347");
        }

        TextView nameTextView = new TextView(context);
        nameTextView.setText(serviceName);
        nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        nameTextView.setTextColor(Color.WHITE);
        nameTextView.setTypeface(null, android.graphics.Typeface.BOLD);
        rootLayout.addView(nameTextView);

        if (isAdmin) {
            TextView ownerTextView = new TextView(context);
            ownerTextView.setText("Requested by: " + (ownerUsername != null ? ownerUsername : "Unknown"));
            ownerTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            ownerTextView.setTextColor(Color.parseColor("#AAAAAA"));
            rootLayout.addView(ownerTextView);
        }

        LinearLayout statusRow = new LinearLayout(context);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dpToPx(8);
        statusRow.setLayoutParams(rowParams);

        TextView statusLabel = new TextView(context);
        statusLabel.setText("Status: ");
        statusLabel.setTextColor(Color.parseColor("#999999"));
        statusLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        statusRow.addView(statusLabel);

        TextView statusTextView = new TextView(context);
        statusTextView.setText(status.toUpperCase());
        statusTextView.setTextColor(statusColor);
        statusTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        statusTextView.setTypeface(null, android.graphics.Typeface.BOLD);
        statusRow.addView(statusTextView);

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

            // Pass the Document ID to uniquely identify what to update in Firebase
            adminButton.setOnClickListener(v -> handleAdminAction(docId, finalServiceName, finalOwnerUsername));

            statusRow.addView(adminButton);
        }

        rootLayout.addView(statusRow);
        return rootLayout;
    }

    // 💡 FIX: Update status directly in Firebase
    private void handleAdminAction(String docId, String serviceName, String ownerUsername) {
        FirebaseFirestore.getInstance().collection("services").document(docId)
                .update("status", "completed")
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded() || getContext() == null) return;
                    loadServicesFromDatabase();
                    Toast.makeText(getContext(), serviceName + " status set to completed for " + ownerUsername, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to update service.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (username != null) {
            loadServicesFromDatabase();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}