package com.example.eyalproject.ui.service;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.eyalproject.DBHelper;
import com.example.eyalproject.MainActivity;
import com.example.eyalproject.R;
import com.example.eyalproject.databinding.FragmentServiceBinding;

import java.util.List;

public class ServiceFragment extends Fragment {
    private FragmentServiceBinding binding;
    private DBHelper dbHelper;
    private String username;
    private ArrayAdapter<String> adapter;
    private List<String> servicesList;
    private CountDownTimer currentTimer;
    private String currentProcessingService;
    private int currentServiceId = -1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentServiceBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Get username from arguments
        Bundle args = getArguments();
        if (args != null) {
            username = args.getString("USERNAME");
        }
        // If username is null, try to get it from MainActivity
        if (username == null && getActivity() != null) {
            username = ((MainActivity) getActivity()).getUsername();
        }
        dbHelper = new DBHelper(getContext());

        // Initialize UI components
        initializeUI();

        // Load all services from database for this user
        loadServicesFromDatabase();

        return root;
    }

    private void initializeUI() {
        Button addServiceBtn = binding.addServiceBtn;
        EditText searchView = binding.searchView;

        addServiceBtn.setOnClickListener(v -> {
            String serviceName = searchView.getText().toString().trim();
            if (!serviceName.isEmpty()) {
                addService(serviceName);
                searchView.setText("");
            } else {
                Toast.makeText(getContext(), "Please enter a service name", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadServicesFromDatabase() {
        updateServiceCounts();
        loadServicesList();
        checkAndStartProcessing();
    }

    private void addService(String serviceName) {
        int userId = dbHelper.getUserId(username);
        if (userId == -1) {
            Toast.makeText(getContext(), "User not found", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean success = dbHelper.addService(serviceName, userId);
        if (success) {
            Toast.makeText(getContext(), "Service added successfully", Toast.LENGTH_SHORT).show();
            loadServicesFromDatabase();
        } else {
            Toast.makeText(getContext(), "Failed to add service", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkAndStartProcessing() {
        int userId = dbHelper.getUserId(username);
        if (userId == -1) return;

        // Do not start new service if one is already running
        if (currentTimer != null || currentProcessingService != null) {
            return;
        }

        // Find the first waiting service
        List<String> services = dbHelper.getUserServices(username);
        String nextService = null;
        int nextServiceId = -1;

        for (String service : services) {
            String[] parts = service.split(" - ");
            if (parts.length >= 2) {
                String serviceName = parts[0];
                String status = parts[1];

                if ("waiting".equals(status)) {
                    nextService = serviceName;
                    nextServiceId = dbHelper.getServiceId(serviceName, userId);
                    break;
                }
            }
        }

        if (nextService != null && nextServiceId != -1) {
            startServiceProcessing(nextService, nextServiceId);
        }
    }

    private void startServiceProcessing(String serviceName, int serviceId) {
        // Update status to "in_progress"
        dbHelper.updateServiceStatus(serviceId, "in_progress");
        currentProcessingService = serviceName;
        currentServiceId = serviceId;

        // Create timer BEFORE refreshing UI to prevent concurrent starts
        currentTimer = new CountDownTimer(40000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> updateProcessingStatus(serviceName));
                }
            }

            @Override
            public void onFinish() {
                dbHelper.updateServiceStatus(serviceId, "completed");
                currentProcessingService = null;
                currentServiceId = -1;
                currentTimer = null;

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        loadServicesFromDatabase();
                        checkAndStartProcessing();
                    });
                }
            }
        };

        currentTimer.start();
        loadServicesFromDatabase();
    }

    private void updateProcessingStatus(String serviceName) {
        if (servicesList != null && adapter != null) {
            for (int i = 0; i < servicesList.size(); i++) {
                String service = servicesList.get(i);
                if (service.startsWith(serviceName + " - ")) {
                    servicesList.set(i, serviceName + " - in_progress");
                    adapter.notifyDataSetChanged();
                    break;
                }
            }
        }
    }

    private void updateServiceCounts() {
        int[] counts = dbHelper.getServiceCounts(username);

        TextView inProgressCount = binding.inProgressCount;
        TextView completedCount = binding.completedCount;
        TextView waitingCount = binding.waitingCount;

        inProgressCount.setText(String.valueOf(counts[1]));
        completedCount.setText(String.valueOf(counts[2]));
        waitingCount.setText(String.valueOf(counts[0]));
    }

    private void loadServicesList() {
        servicesList = dbHelper.getUserServices(username);
        ListView servicesListView = binding.servicesListView;

        adapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_list_item_1, servicesList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);

                String item = servicesList.get(position);
                if (item.contains("completed")) {
                    textView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                } else if (item.contains("in_progress")) {
                    textView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                } else {
                    textView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                }

                return view;
            }
        };

        servicesListView.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (dbHelper != null && username != null) {
            loadServicesFromDatabase();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (currentTimer != null) {
            currentTimer.cancel();
            currentTimer = null;

            if (currentProcessingService != null && currentServiceId != -1) {
                dbHelper.updateServiceStatus(currentServiceId, "waiting");
                currentProcessingService = null;
                currentServiceId = -1;
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (currentTimer != null) {
            currentTimer.cancel();
            currentTimer = null;
        }
        binding = null;
    }
}
