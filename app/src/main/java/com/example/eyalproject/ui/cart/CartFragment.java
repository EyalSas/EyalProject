package com.example.eyalproject.ui.cart;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.eyalproject.DBHelper;
import com.example.eyalproject.MainActivity;
import com.example.eyalproject.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fragment responsible for managing the user's shopping cart, checkout process,
 * and displaying purchase history.
 */
public class CartFragment extends Fragment {

    private DBHelper dbHelper;
    private TableLayout tableLayout;        // Layout to display cart items.
    private TextView textViewTotalSum;      // Displays the total sum of the cart.
    private View btnBuyAll;                 // Button to initiate the checkout process.
    private Button btnViewHistory;          // Button to view purchase history.

    private PopupWindow receiptPopup;        // Popup window for viewing the digital receipt.
    private AlertDialog paymentDialog;       // Dialog for entering payment details.

    // 💡 Priority 1 Fix: Thread management fields for asynchronous DB operations
    // ExecutorService runs tasks in a background thread pool (single thread used here for serial DB access).
    private ExecutorService cartExecutor = Executors.newSingleThreadExecutor();
    // Handler for posting results back to the main (UI) thread.
    private Handler mainHandler = new Handler(Looper.getMainLooper());


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_cart, container, false);

        dbHelper = new DBHelper(getContext());

        // Initialize UI views
        tableLayout = root.findViewById(R.id.tableLayout);
        textViewTotalSum = root.findViewById(R.id.textViewTotalSum);
        btnBuyAll = root.findViewById(R.id.btnBuyAll);
        btnViewHistory = root.findViewById(R.id.btnViewHistory);

        return root;
    }

    /**
     * Retrieves the ID of the currently logged-in user from MainActivity.
     * @return The User ID, or -1 if the user is not logged in.
     */
    private int getCurrentUserId() {
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            String username = mainActivity.getUsername();
            return dbHelper.getUserId(username);
        }
        return -1;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Load data asynchronously every time the fragment is resumed (e.g., returning from another tab).
        loadCartDataAsync();

        // Set click listener for the "Buy All" button.
        btnBuyAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int userId = getCurrentUserId();
                if (userId == -1) {
                    showCustomToast("User not logged in", R.drawable.ic_store, R.color.error);
                    return;
                }

                // Calculate sum (DB read) before showing the payment dialog.
                double totalSum = dbHelper.calculateTotalOrderSum(userId);

                if (totalSum > 0.0) {
                    showPaymentPopup(totalSum);
                } else {
                    showCustomToast("Your cart is empty", R.drawable.ic_store, R.color.info_color);
                }
            }
        });

        // Set click listener for the History Button.
        btnViewHistory.setOnClickListener(v -> showHistoryPopup());
    }

    // 💡 Priority 1 Fix: Asynchronous Cart Data Loader
    /**
     * Starts a background thread to fetch cart data from the database.
     */
    private void loadCartDataAsync() {
        int userId = getCurrentUserId();
        if (userId == -1) {
            // If user is not logged in, show empty state immediately on the UI thread.
            mainHandler.post(this::showEmptyCartState);
            return;
        }

        // Execute the database reads on the background thread.
        cartExecutor.execute(() -> {
            // --- Background Work (DB Access - User Isolated) ---
            final List<Integer> orderIds = dbHelper.getAllOrderIds(userId);
            final List<Double> orderPrices = dbHelper.getAllOrderPrices(userId);
            final List<String> orderNames = dbHelper.getAllOrderNames(userId);
            final double totalSum = dbHelper.calculateTotalOrderSum(userId);

            // --- Post Result to Main Thread (UI Update) ---
            mainHandler.post(() -> {
                // Update UI based on fetched data.
                displayOrdersUI(orderIds, orderNames, orderPrices);
                updateTotalSumUI(totalSum);
            });
        });
    }

    // 💡 Priority 1 Fix: UI update method for cart items
    /**
     * Clears the current table and populates it with new order rows.
     */
    private void displayOrdersUI(List<Integer> orderIds, List<String> orderNames, List<Double> orderPrices) {
        tableLayout.removeAllViews();
        if (orderIds.isEmpty()) {
            showEmptyCartState();
        } else {
            for (int i = 0; i < orderIds.size(); i++) {
                TableRow tableRow = createOrderRow(orderIds.get(i), orderNames.get(i), orderPrices.get(i));
                tableLayout.addView(tableRow);
            }
        }
    }

    // 💡 Priority 1 Fix: UI update method for total sum
    /**
     * Updates the total sum TextView and the state of the Buy All button.
     * @param totalSum The calculated total sum.
     */
    private void updateTotalSumUI(double totalSum) {
        textViewTotalSum.setText(String.format("$%.2f", totalSum));
        if (btnBuyAll != null) {
            boolean hasItems = totalSum > 0;
            btnBuyAll.setEnabled(hasItems);
            // Visually dim the button if the cart is empty
            btnBuyAll.setAlpha(hasItems ? 1f : 0.5f);
        }
    }

    // --- START: Payment/Checkout Methods ---

    /**
     * Displays the payment dialog for users to enter card details.
     * @param totalAmount The total sum due.
     */
    private void showPaymentPopup(double totalAmount) {
        View popupView = LayoutInflater.from(getContext()).inflate(R.layout.payment_popup, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(popupView);

        // Initialize payment input fields and error TextView
        final EditText editTextCardNumber = popupView.findViewById(R.id.editTextCardNumber);
        final EditText editTextExpiryDate = popupView.findViewById(R.id.editTextExpiryDate);
        final EditText editTextCVV = popupView.findViewById(R.id.editTextCVV);
        final TextView textViewPaymentError = popupView.findViewById(R.id.textViewPaymentError);
        final Button btnPayNow = popupView.findViewById(R.id.btnPayNow);
        final Button btnCancelPayment = popupView.findViewById(R.id.btnCancelPayment);

        // Update the pay button text with the total amount
        btnPayNow.setText(String.format("Pay $%.2f", totalAmount));
        addExpiryDateTextWatcher(editTextExpiryDate);
        paymentDialog = builder.create();

        btnPayNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String cardNumber = editTextCardNumber.getText().toString().replaceAll("\\s", "");
                String expiryDate = editTextExpiryDate.getText().toString();
                String cvv = editTextCVV.getText().toString();

                if (validatePaymentDetails(cardNumber, expiryDate, cvv, textViewPaymentError)) {
                    showCustomToast("Payment Processing...", R.drawable.ic_store, R.color.primary_color);
                    buyAll(); // Initiates the asynchronous purchase process
                    paymentDialog.dismiss();
                }
            }
        });

        btnCancelPayment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paymentDialog.dismiss();
            }
        });

        paymentDialog.setCanceledOnTouchOutside(false);
        paymentDialog.show();
    }

    /**
     * Validates the format and validity of credit card details.
     */
    private boolean validatePaymentDetails(String cardNumber, String expiryDate, String cvv, TextView errorTextView) {
        // ... (validation logic remains the same)
        if (TextUtils.isEmpty(cardNumber) || TextUtils.isEmpty(expiryDate) || TextUtils.isEmpty(cvv)) {
            errorTextView.setText("All fields are required.");
            errorTextView.setVisibility(View.VISIBLE);
            return false;
        }
        if (cardNumber.length() != 16 || !TextUtils.isDigitsOnly(cardNumber)) {
            errorTextView.setText("Invalid Card Number (must be 16 digits).");
            errorTextView.setVisibility(View.VISIBLE);
            return false;
        }
        if ((cvv.length() < 3 || cvv.length() > 4) || !TextUtils.isDigitsOnly(cvv)) {
            errorTextView.setText("Invalid CVV (must be 3 or 4 digits).");
            errorTextView.setVisibility(View.VISIBLE);
            return false;
        }
        if (!expiryDate.matches("^(0[1-9]|1[0-2])/([0-9]{2})$")) {
            errorTextView.setText("Invalid Expiry Date format (MM/YY).");
            errorTextView.setVisibility(View.VISIBLE);
            return false;
        }
        try {
            String[] parts = expiryDate.split("/");
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt(parts[1]) + 2000;
            Calendar currentCal = Calendar.getInstance();
            int currentMonth = currentCal.get(Calendar.MONTH) + 1;
            int currentYear = currentCal.get(Calendar.YEAR);
            if (year < currentYear || (year == currentYear && month < currentMonth)) {
                errorTextView.setText("Card is expired.");
                errorTextView.setVisibility(View.VISIBLE);
                return false;
            }
        } catch (Exception e) {
            errorTextView.setText("Error parsing expiry date.");
            errorTextView.setVisibility(View.VISIBLE);
            return false;
        }
        errorTextView.setVisibility(View.GONE);
        return true;
    }

    /**
     * Adds a TextWatcher to automatically format the expiry date as MM/YY.
     */
    private void addExpiryDateTextWatcher(EditText et) {
        et.addTextChangedListener(new TextWatcher() {
            private String current = "";
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                if (text.equals(current)) {
                    return;
                }
                String digits = text.replaceAll("\\D", "");
                int len = digits.length();
                String formatted;
                if (len <= 2) {
                    formatted = digits;
                } else {
                    formatted = digits.substring(0, 2) + "/" + digits.substring(2);
                }
                if (formatted.length() > 5) {
                    formatted = formatted.substring(0, 5);
                }
                current = formatted;
                et.setText(formatted);
                et.setSelection(formatted.length());
            }
        });
    }

    /**
     * Handles the final purchase confirmation, moving data to history and clearing the cart.
     */
    private void buyAll() {
        int userId = getCurrentUserId();
        if (userId == -1) {
            showCustomToast("User session expired. Please log in.", R.drawable.ic_store, R.color.error_color);
            return;
        }

        // --- Synchronous reads before starting the background process ---
        final double totalSum = dbHelper.calculateTotalOrderSum(userId);
        final List<String> orderNames = dbHelper.getAllOrderNames(userId);
        final List<Double> orderPrices = dbHelper.getAllOrderPrices(userId);
        final String receipt = generateDigitalReceipt(orderNames, orderPrices, totalSum);
        final String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        // 💡 Priority 1 Fix: Move all database writes (insert/delete) to the background thread
        cartExecutor.execute(() -> {

            // 1. Database Writes (Blocking I/O)
            boolean isReceiptSaved = dbHelper.insertReceipt(currentDate, totalSum, receipt, userId);
            dbHelper.deleteAllOrders(userId);

            // 2. UI Update and Next Steps
            mainHandler.post(() -> {
                if (isReceiptSaved) {
                    showReceiptPopup(receipt);
                    schedulePurchaseNotification(); // Schedule the notification after purchase
                    loadCartDataAsync(); // Reload cart asynchronously to refresh UI
                    showCustomToast("Purchase successful! History saved.", R.drawable.ic_store, R.color.success_color);
                } else {
                    saveReceiptToFile(receipt); // Fallback to saving as a file if DB save failed
                    showCustomToast("Payment successful but failed to save history.",
                            R.drawable.ic_store, R.color.error);
                }
            });
        });
    }

    // --- END: Payment/Checkout Methods ---

    // --- START: Order Display Methods ---

    /**
     * Deprecated method; use loadCartDataAsync() instead.
     */
    private void displayOrders() {
        loadCartDataAsync();
    }

    /**
     * Displays a message and icon indicating the cart is empty.
     */
    private void showEmptyCartState() {
        tableLayout.removeAllViews();
        // ... (UI creation for empty cart state)
        TableRow emptyRow = new TableRow(getContext());
        TableLayout.LayoutParams params = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 100, 0, 0);
        emptyRow.setLayoutParams(params);

        TextView emptyText = new TextView(getContext());
        emptyText.setText("🛒\nYour Cart is Empty");
        emptyText.setTextColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
        emptyText.setGravity(Gravity.CENTER);
        emptyText.setTextSize(18);
        emptyText.setLineSpacing(1.2f, 1.2f);
        emptyText.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
        ));

        emptyRow.addView(emptyText);
        tableLayout.addView(emptyRow);
        // ...
    }

    /**
     * Creates a TableRow representing a single order item with product details and a remove button.
     */
    private TableRow createOrderRow(int orderId, String orderName, double orderPrice) {
        TableRow tableRow = new TableRow(getContext());
        tableRow.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.rounded_card));

        // ... (UI setup for Name, Price TextViews)
        TableLayout.LayoutParams layoutParams = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, 0, 0, 8);
        tableRow.setLayoutParams(layoutParams);

        // Product name (60% width)
        TextView textViewOrderName = new TextView(getContext());
        textViewOrderName.setText(orderName);
        textViewOrderName.setTextColor(ContextCompat.getColor(getContext(), R.color.text_primary));
        textViewOrderName.setTextSize(16);
        textViewOrderName.setPadding(16, 20, 16, 20);
        textViewOrderName.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 3f));

        // Price (20% width)
        TextView textViewOrderPrice = new TextView(getContext());
        textViewOrderPrice.setText(String.format("$%.2f", orderPrice));
        textViewOrderPrice.setTextColor(ContextCompat.getColor(getContext(), R.color.primary_color));
        textViewOrderPrice.setTextSize(16);
        textViewOrderPrice.setTypeface(null, Typeface.BOLD);
        textViewOrderPrice.setPadding(16, 20, 16, 20);
        textViewOrderPrice.setGravity(Gravity.CENTER);
        textViewOrderPrice.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));

        // Remove button (20% width)
        Button btnRemoveItem = new Button(getContext());
        btnRemoveItem.setText("Remove");
        btnRemoveItem.setTextSize(12);
        btnRemoveItem.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.error));
        btnRemoveItem.setTextColor(Color.WHITE);
        btnRemoveItem.setPadding(16, 8, 16, 8);
        btnRemoveItem.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
        // ...

        // Set the click listener for the remove button
        btnRemoveItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int userId = getCurrentUserId();
                int orderId = (int) tableRow.getTag();

                // 💡 Priority 1 Fix: Move deletion to background thread
                cartExecutor.execute(() -> {
                    boolean deletionSuccessful = dbHelper.deleteOrder(String.valueOf(orderId), userId);
                    mainHandler.post(() -> {
                        if (deletionSuccessful) {
                            showCustomToast("Item removed from cart", R.drawable.ic_store, R.color.success_color);
                            loadCartDataAsync(); // Reload the data asynchronously to refresh UI
                        } else {
                            showCustomToast("Failed to remove item", R.drawable.ic_store, R.color.error);
                        }
                    });
                });
            }
        });

        tableRow.addView(textViewOrderName);
        tableRow.addView(textViewOrderPrice);
        tableRow.addView(btnRemoveItem);
        tableRow.setTag(orderId); // Store the unique order ID in the row's tag

        return tableRow;
    }

    // --- END: Order Display Methods ---

    // --- START: History Popup Methods ---

    /**
     * Fetches purchase history and displays it in a PopupWindow.
     */
    private void showHistoryPopup() {
        if (getContext() == null || getView() == null) return;

        int userId = getCurrentUserId();
        if (userId == -1) {
            showCustomToast("User session required to view history.", R.drawable.ic_store, R.color.error);
            return;
        }

        // Fetch history data synchronously (DB read)
        List<DBHelper.ReceiptHistoryItem> historyList = dbHelper.getAllReceiptHistory(userId);

        View popupView = LayoutInflater.from(getContext()).inflate(R.layout.history_popup, null);
        LinearLayout historyContainer = popupView.findViewById(R.id.historyPopupContainer);
        Button btnCloseHistory = popupView.findViewById(R.id.btnCloseHistory);

        // 1. Setup History Display
        if (historyList.isEmpty()) {
            // ... (Empty history message UI)
            TextView emptyText = new TextView(getContext());
            emptyText.setText("No purchase history yet. Buy something!");
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setPadding(0, 50, 0, 50);
            emptyText.setTextColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
            historyContainer.addView(emptyText);
        } else {
            // Create a structured TableLayout for history items.
            TableLayout historyTable = new TableLayout(getContext());
            historyTable.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            historyTable.setStretchAllColumns(true);

            // Add header and rows.
            historyTable.addView(createHistoryHeader());
            for (DBHelper.ReceiptHistoryItem item : historyList) {
                historyTable.addView(createHistoryRow(item));
            }

            historyContainer.addView(historyTable);
        }

        // 2. Create and show the PopupWindow
        final PopupWindow historyPopup = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                true // Focusable
        );
        historyPopup.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        historyPopup.setElevation(20f);
        historyPopup.showAtLocation(requireView(), Gravity.CENTER, 0, 0);

        // 3. Close button
        btnCloseHistory.setOnClickListener(v -> historyPopup.dismiss());
    }

    /**
     * Creates the header row for the purchase history table.
     */
    private TableLayout createHistoryHeader() {
        TableLayout headerLayout = new TableLayout(getContext());
        headerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        headerLayout.setStretchAllColumns(true);

        TableRow headerRow = new TableRow(getContext());
        headerRow.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.light_gray));
        headerRow.setPadding(16, 16, 16, 16);

        String[] headers = {"ID", "Date", "Price", "Action"};
        int[] weights = {1, 2, 2, 2};
        for (int i = 0; i < headers.length; i++) {
            TextView tv = new TextView(getContext());
            tv.setText(headers[i]);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tv.setTextColor(ContextCompat.getColor(getContext(), R.color.text_primary));
            TableRow.LayoutParams params = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, weights[i]);
            tv.setLayoutParams(params);
            headerRow.addView(tv);
        }
        headerLayout.addView(headerRow);
        return headerLayout;
    }

    /**
     * Helper function for creating TextView with specific properties.
     */
    private TextView createTextView(Context context, String text, float weight, int color, boolean isBold) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        if (isBold) tv.setTypeface(null, Typeface.BOLD);

        TableRow.LayoutParams params = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, weight);
        tv.setLayoutParams(params);
        return tv;
    }

    /**
     * Creates a TableRow for a single receipt history item.
     */
    private TableRow createHistoryRow(DBHelper.ReceiptHistoryItem item) {
        Context context = getContext();
        if (context == null) return new TableRow(requireContext());

        TableRow row = new TableRow(context);
        row.setBackgroundColor(Color.WHITE);
        row.setPadding(16, 10, 16, 10);

        // Define colors once
        int textColorPrimary = ContextCompat.getColor(context, R.color.text_primary);
        int textColorSecondary = ContextCompat.getColor(context, R.color.text_secondary);
        int primaryColor = ContextCompat.getColor(context, R.color.primary_color);

        // 1. ID (Weight 1)
        TextView tvId = createTextView(context, String.valueOf(item.id), 1f, textColorPrimary, false);
        row.addView(tvId);

        // 2. Date (Weight 2)
        String shortDate = item.date.substring(0, 10);
        TextView tvDate = createTextView(context, shortDate, 2f, textColorSecondary, false);
        row.addView(tvDate);

        // 3. Price (Weight 2)
        TextView tvPrice = createTextView(context, String.format("$%.2f", item.totalPrice), 2f, primaryColor, true);
        row.addView(tvPrice);

        // 4. Action Button (Weight 2)
        Button btnViewReceipt = new Button(context);
        btnViewReceipt.setText("View");
        btnViewReceipt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnViewReceipt.setBackgroundColor(primaryColor);
        btnViewReceipt.setTextColor(Color.WHITE);
        btnViewReceipt.setPadding(8, 0, 8, 0);

        // Set the click listener to show the full receipt content
        btnViewReceipt.setOnClickListener(v -> showReceiptPopup(item.content));

        // Custom button parameters (fixed height: 36dip)
        TableRow.LayoutParams buttonParams = new TableRow.LayoutParams(0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, getResources().getDisplayMetrics()), 2f);
        btnViewReceipt.setLayoutParams(buttonParams);
        row.addView(btnViewReceipt);

        return row;
    }

    // --- END: History Popup Methods ---

    // --- START: Receipt/File Utility Methods ---

    /**
     * Schedules a local notification (via CartReminderReceiver) to confirm the purchase after a delay.
     */
    private void schedulePurchaseNotification() {
        try {
            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(requireContext(), CartReminderReceiver.class);
            intent.setAction("PURCHASE_CONFIRMATION");
            intent.putExtra("message", "Thank you for your purchase! Your order is being processed.");
            intent.putExtra("title", "Purchase Confirmation");

            // Create PendingIntent that will be broadcast when the alarm triggers
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    requireContext(),
                    (int) System.currentTimeMillis(), // Unique request code
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            long triggerTime = System.currentTimeMillis() + 5000; // Trigger in 5 seconds

            // Set the alarm using different methods based on Android version for reliability
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showCustomToast("Purchase successful but failed to schedule notification", R.drawable.ic_store, R.color.info_color);
        }
    }

    /**
     * Displays the full digital receipt content in a full-screen PopupWindow.
     * @param receiptContent The full text of the receipt.
     */
    private void showReceiptPopup(String receiptContent) {
        View popupView = LayoutInflater.from(getContext()).inflate(R.layout.receipt_popup, null);
        TextView textViewReceipt = popupView.findViewById(R.id.textViewReceipt);
        textViewReceipt.setText(receiptContent);

        receiptPopup = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                true
        );
        // ... (UI setup for popup)
        receiptPopup.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        receiptPopup.setElevation(20f);

        View rootView = getView();
        if (rootView != null) {
            receiptPopup.showAtLocation(rootView, Gravity.CENTER, 0, 0);
        }

        // Close button
        Button btnClose = popupView.findViewById(R.id.btnCloseReceipt);
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (receiptPopup != null && receiptPopup.isShowing()) {
                    receiptPopup.dismiss();
                }
            }
        });

        // Share button
        Button btnShare = popupView.findViewById(R.id.btnShareReceipt);
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareReceipt(receiptContent);
            }
        });

        receiptPopup.setOutsideTouchable(true);
        receiptPopup.setFocusable(true);
    }

    /**
     * Initiates a system share Intent for the digital receipt text.
     */
    private void shareReceipt(String receiptContent) {
        try {
            android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Purchase Receipt");
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, receiptContent);
            startActivity(android.content.Intent.createChooser(shareIntent, "Share receipt via"));
        } catch (Exception e) {
            showCustomToast("No sharing app available", R.drawable.ic_store, R.color.info_color);
        }
    }

    /**
     * Generates the structured text content for the digital receipt.
     */
    private String generateDigitalReceipt(List<String> orderNames, List<Double> orderPrices, double totalSum) {
        StringBuilder receipt = new StringBuilder();
        String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        receipt.append("🛍️ DIGITAL RECEIPT 🛍️\n");
        receipt.append("======================\n");
        receipt.append("Date: ").append(currentDate).append("\n");
        receipt.append("Order ID: ORD").append(System.currentTimeMillis()).append("\n");
        receipt.append("Status: PAID ✅\n\n");

        receipt.append("ITEMS PURCHASED:\n");
        receipt.append("----------------\n");

        double subtotal = 0;
        for (int i = 0; i < orderNames.size(); i++) {
            String itemName = orderNames.get(i);
            double itemPrice = orderPrices.get(i);
            subtotal += itemPrice;
            receipt.append(String.format("• %-25s $%.2f\n", itemName, itemPrice));
        }

        receipt.append("----------------\n");
        receipt.append(String.format("SUBTOTAL:       $%.2f\n", subtotal));
        receipt.append(String.format("TOTAL:          $%.2f\n", totalSum));
        receipt.append("======================\n");
        receipt.append("Thank you for your purchase! 🎉\n");
        receipt.append("We hope to see you again soon!\n\n");
        receipt.append("Generated by Eyal Project App");

        return receipt.toString();
    }

    /**
     * Gets or creates the directory where receipt files are saved (fallback).
     */
    private File getReceiptsDirectory() {
        File receiptsDir = new File(requireContext().getFilesDir(), "Receipts");
        if (!receiptsDir.exists()) {
            receiptsDir.mkdirs();
        }
        return receiptsDir;
    }

    /**
     * Saves the receipt text content to a file in the app's private storage (fallback).
     */
    private boolean saveReceiptToFile(String receipt) {
        FileOutputStream fos = null;
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "receipt_" + timeStamp + ".txt";

            File receiptsDir = getReceiptsDirectory();
            File receiptFile = new File(receiptsDir, fileName);

            fos = new FileOutputStream(receiptFile);
            fos.write(receipt.getBytes());
            fos.flush();

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // --- END: Receipt/File Utility Methods ---

    // --- START: Toast/Lifecycle Methods ---

    /**
     * Displays a custom-styled Toast message with an icon and colored background.
     */
    private void showCustomToast(String message, int iconResId, int typeColorResId) {
        if (getContext() == null || getActivity() == null) return;

        try {
            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.custom_toast,
                    (ViewGroup) getActivity().findViewById(R.id.custom_toast_container));

            TextView text = layout.findViewById(R.id.toast_text);
            ImageView icon = layout.findViewById(R.id.toast_icon);
            LinearLayout container = layout.findViewById(R.id.custom_toast_container);

            // Apply color filter to the background drawable
            int color = ContextCompat.getColor(getContext(), typeColorResId);
            container.getBackground().mutate().setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);

            text.setText(message);
            icon.setImageResource(iconResId);


            Toast toast = new Toast(getContext());
            // Position the toast at the bottom center
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 150);
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setView(layout);
            toast.show();
        } catch (Exception e) {
            // Fallback to simple Toast if custom styling fails
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    /**
     * Cleans up resources when the fragment view is destroyed.
     */
    @Override
    public void onDestroyView() {
        // Dismiss any active popups to prevent window leaks
        if (receiptPopup != null && receiptPopup.isShowing()) {
            receiptPopup.dismiss();
        }
        if (paymentDialog != null && paymentDialog.isShowing()) {
            paymentDialog.dismiss();
        }
        // 💡 Priority 1 Fix: Shutdown ExecutorService to prevent memory leaks and threading issues
        cartExecutor.shutdown();
        super.onDestroyView();
    }

    // --- END: Toast/Lifecycle Methods ---
}