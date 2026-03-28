package com.example.eyalproject;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FirebaseHelper {

    private static final String TAG = "FirebaseHelper";

    // ── Singletons ─────────────────────────────────────────────────────────────
    // Reusing the same Auth + Firestore instances across all FirebaseHelper
    // objects eliminates repeated getInstance() overhead and listener re-registration.

    private static FirebaseFirestore dbInstance;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public FirebaseHelper() {
        auth = FirebaseAuth.getInstance();
        db   = getDb();
    }

    /**
     * Returns a singleton Firestore instance with offline persistence enabled.
     *
     * Offline persistence means:
     *  • First launch reads from disk cache instantly (0 ms), then syncs in background.
     *  • Subsequent launches are instant even with no network.
     *  • No duplicate Settings configuration if called multiple times.
     */
    private static synchronized FirebaseFirestore getDb() {
        if (dbInstance == null) {
            dbInstance = FirebaseFirestore.getInstance();
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)       // ✅ local disk cache
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build();
            dbInstance.setFirestoreSettings(settings);
        }
        return dbInstance;
    }

    // ==========================================================================
    // 1. AUTHENTICATION
    // ==========================================================================

    public interface AuthCallback {
        void onSuccess(String username);
        void onFailure(String error);
    }

    public void registerUser(String username, String email, String password,
                             AuthCallback callback) {
        db.collection("users").whereEqualTo("username", username).get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        callback.onFailure("Username is already taken. Please choose another.");
                        return;
                    }
                    auth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener(result -> {
                                FirebaseUser user = result.getUser();
                                if (user == null) return;
                                Map<String, Object> data = new HashMap<>();
                                data.put("username", username);
                                data.put("email", email);
                                db.collection("users").document(user.getUid()).set(data)
                                        .addOnSuccessListener(v -> callback.onSuccess(username))
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Profile save failed, Auth ok");
                                            callback.onSuccess(username);
                                        });
                            })
                            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure("Database connection failed."));
    }

    public void loginUserByUsername(String username, String password, AuthCallback callback) {
        db.collection("users").whereEqualTo("username", username).get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        callback.onFailure("Username not found.");
                        return;
                    }
                    String email = snap.getDocuments().get(0).getString("email");
                    if (email == null) {
                        callback.onFailure("User data is corrupted.");
                        return;
                    }
                    auth.signInWithEmailAndPassword(email, password)
                            .addOnSuccessListener(r -> callback.onSuccess(username))
                            .addOnFailureListener(e -> callback.onFailure("Invalid password."));
                })
                .addOnFailureListener(e -> callback.onFailure("Database connection failed."));
    }

    public String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    // ==========================================================================
    // 2. PRODUCTS
    // ==========================================================================

    public static class Product {
        public String name;
        public double price;
        public String imageUrl;
        public String type;
    }

    public interface ProductsCallback {
        void onProductsLoaded(List<Product> products);
        void onError(String error);
    }

    /**
     * Cache-first product fetch strategy:
     *
     *  1. Try disk/memory cache immediately (Source.CACHE) → near-instant response.
     *  2. If cache miss, fall back to network automatically.
     *
     * This makes the store feel instant on every subsequent open.
     */
    public void getAllProducts(ProductsCallback callback) {
        db.collection("products")
                .get(Source.CACHE)                         // ✅ Try cache first
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        callback.onProductsLoaded(parseProducts(snap));
                    } else {
                        // Cache empty → go to network
                        fetchAllProductsFromNetwork(callback);
                    }
                })
                .addOnFailureListener(e -> fetchAllProductsFromNetwork(callback)); // cache miss
    }

    private void fetchAllProductsFromNetwork(ProductsCallback callback) {
        db.collection("products")
                .get(Source.SERVER)
                .addOnSuccessListener(snap -> callback.onProductsLoaded(parseProducts(snap)))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getProductsByType(String productType, ProductsCallback callback) {
        db.collection("products")
                .whereEqualTo("type", productType)
                .get(Source.CACHE)                         // ✅ Try cache first
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        callback.onProductsLoaded(parseProducts(snap));
                    } else {
                        fetchProductsByTypeFromNetwork(productType, callback);
                    }
                })
                .addOnFailureListener(e -> fetchProductsByTypeFromNetwork(productType, callback));
    }

    private void fetchProductsByTypeFromNetwork(String productType, ProductsCallback callback) {
        db.collection("products")
                .whereEqualTo("type", productType)
                .get(Source.SERVER)
                .addOnSuccessListener(snap -> callback.onProductsLoaded(parseProducts(snap)))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /** Shared parser — avoids duplicated field-reading code across all product queries. */
    private List<Product> parseProducts(com.google.firebase.firestore.QuerySnapshot snap) {
        List<Product> list = new ArrayList<>(snap.size());
        for (QueryDocumentSnapshot doc : snap) {
            Product p  = new Product();
            p.name     = doc.getString("name");
            p.price    = doc.getDouble("price") != null ? doc.getDouble("price") : 0.0;
            p.imageUrl = doc.getString("imageUrl");
            p.type     = doc.getString("type");
            list.add(p);
        }
        return list;
    }

    public void uploadSingleProduct(String name, double price, String imageUrl, String type) {
        Map<String, Object> product = new HashMap<>();
        product.put("name", name);
        product.put("price", price);
        product.put("imageUrl", imageUrl);
        product.put("type", type);
        // Replace forward slashes so Firebase doesn't treat them as path separators.
        String safeId = name.replace("/", "-");
        db.collection("products").document(safeId).set(product);
    }

    // ==========================================================================
    // 3. CART & CHECKOUT
    // ==========================================================================

    public interface ActionCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public static class CartItem {
        public String documentId;
        public String productName;
        public double price;
        public int quantity;
    }

    public interface CartCallback {
        void onCartLoaded(List<CartItem> items, double totalSum);
        void onError(String error);
    }

    public void addToCart(String productName, double price, int quantity) {
        String uid = getCurrentUserId();
        if (uid == null) return;

        Map<String, Object> item = new HashMap<>();
        item.put("productName", productName);
        item.put("price", price);
        item.put("quantity", quantity);
        item.put("timestamp", System.currentTimeMillis());

        db.collection("users").document(uid).collection("cart").add(item)
                .addOnSuccessListener(ref -> Log.d(TAG, "Cart item added"))
                .addOnFailureListener(e -> Log.e(TAG, "Cart add failed", e));
    }

    public void getCartItems(CartCallback callback) {
        String uid = getCurrentUserId();
        if (uid == null) { callback.onError("User not logged in"); return; }

        db.collection("users").document(uid).collection("cart")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<CartItem> items = new ArrayList<>(snap.size());
                    double total = 0;
                    for (QueryDocumentSnapshot doc : snap) {
                        CartItem ci  = new CartItem();
                        ci.documentId   = doc.getId();
                        ci.productName  = doc.getString("productName");
                        ci.price        = doc.getDouble("price") != null ? doc.getDouble("price") : 0.0;
                        ci.quantity     = doc.getLong("quantity") != null
                                ? doc.getLong("quantity").intValue() : 1;
                        total += ci.price * ci.quantity;
                        items.add(ci);
                    }
                    callback.onCartLoaded(items, total);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void deleteCartItem(String documentId, ActionCallback callback) {
        String uid = getCurrentUserId();
        if (uid == null) { callback.onFailure("User not logged in"); return; }

        db.collection("users").document(uid).collection("cart").document(documentId)
                .delete()
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public interface CartCountCallback {
        void onCountRetrieved(int count);
    }

    public void getCartItemCount(CartCountCallback callback) {
        String uid = getCurrentUserId();
        if (uid == null) { callback.onCountRetrieved(0); return; }

        db.collection("users").document(uid).collection("cart").get()
                .addOnSuccessListener(snap -> callback.onCountRetrieved(snap.size()))
                .addOnFailureListener(e -> callback.onCountRetrieved(0));
    }

    public interface CheckoutCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public void checkoutCart(String receiptContent, double totalSum, CheckoutCallback callback) {
        String uid = getCurrentUserId();
        if (uid == null) { callback.onFailure("User not logged in"); return; }

        Map<String, Object> receipt = new HashMap<>();
        receipt.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()).format(new Date()));
        receipt.put("totalPrice", totalSum);
        receipt.put("content", receiptContent);
        receipt.put("timestamp", System.currentTimeMillis());

        db.collection("users").document(uid).collection("receipts").add(receipt)
                .addOnSuccessListener(ref ->
                        db.collection("users").document(uid).collection("cart").get()
                                .addOnSuccessListener(snap -> {
                                    WriteBatch batch = db.batch();
                                    for (QueryDocumentSnapshot doc : snap) batch.delete(doc.getReference());
                                    batch.commit()
                                            .addOnSuccessListener(v -> callback.onSuccess())
                                            .addOnFailureListener(e -> callback.onFailure(
                                                    "Receipt saved, but cart clear failed: " + e.getMessage()));
                                })
                                .addOnFailureListener(e -> callback.onFailure(
                                        "Receipt saved, but cart fetch failed.")))
                .addOnFailureListener(e -> callback.onFailure("Failed to save receipt: " + e.getMessage()));
    }

    // ==========================================================================
    // 4. HISTORY (Receipts)
    // ==========================================================================

    public static class ReceiptItem {
        public String date;
        public double totalPrice;
        public String content;
    }

    public interface HistoryCallback {
        void onHistoryLoaded(List<ReceiptItem> historyList);
        void onError(String error);
    }

    public void getReceiptHistory(HistoryCallback callback) {
        String uid = getCurrentUserId();
        if (uid == null) { callback.onError("User not logged in"); return; }

        db.collection("users").document(uid).collection("receipts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<ReceiptItem> list = new ArrayList<>(snap.size());
                    for (QueryDocumentSnapshot doc : snap) {
                        ReceiptItem item  = new ReceiptItem();
                        item.date         = doc.getString("date");
                        item.totalPrice   = doc.getDouble("totalPrice") != null
                                ? doc.getDouble("totalPrice") : 0.0;
                        item.content      = doc.getString("content");
                        list.add(item);
                    }
                    callback.onHistoryLoaded(list);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}