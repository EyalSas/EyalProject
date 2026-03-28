package com.example.eyalproject;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


import com.example.eyalproject.ui.cart.CartReminderReceiver;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "e123d.db";
    private Context context;

    // User table constants
    private static final String TABLE_USERS = "users";
    private static final String COL_USER_ID = "ID";
    private static final String COL_USERNAME = "USERNAME";
    private static final String COL_PASSWORD = "PASSWORD";
    private static final String COL_EMAIL = "EMAIL";
    private static final String ADMIN_USERNAME = "or";
    // Service table constants
    private static final String TABLE_SERVICES = "services";
    private static final String COL_SERVICE_ID = "SERVICE_ID";
    private static final String COL_SERVICE_NAME = "SERVICE_NAME";
    private static final String COL_SERVICE_STATUS = "SERVICE_STATUS"; // "waiting", "in_progress", "completed"
    private static final String COL_SERVICE_CREATED_AT = "CREATED_AT";
    private static final String COL_USER_ID_FK = "USER_ID_FK";

    // Product table constants
    private static final String TABLE_PRODUCTS = "products";
    private static final String COL_PRODUCT_NAME = "PRODUCT_NAME";
    private static final String COL_PRODUCT_PRICE = "PRODUCT_PRICE";
    private static final String COL_PRODUCT_IMAGE = "PRODUCT_IMAGE";
    private static final String COL_PRODUCT_TYPE = "PRODUCT_TYPE";

    private static final String TABLE_ORDERS = "orders";
    private static final String COL_ORDER_ID = "ORDER_ID";
    private static final String COL_ORDER_PRODUCT_NAME = "ORDER_PRODUCT_NAME";
    private static final String COL_ORDER_PRODUCT_PRICE = "ORDER_PRODUCT_PRICE";
    private static final String COL_ORDER_TIME = "ORDER_TIME";

    private static final String COL_USER_ID_FK_ORDER = "USER_ID_FK";

    private static final String TABLE_RECEIPTS = "receipts";
    private static final String COL_RECEIPT_ID = "RECEIPT_ID";
    private static final String COL_RECEIPT_DATE = "RECEIPT_DATE";
    private static final String COL_RECEIPT_TOTAL_PRICE = "RECEIPT_TOTAL_PRICE";
    private static final String COL_RECEIPT_CONTENT = "RECEIPT_CONTENT";
    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, 1); // Fixed version to 1
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create users table
        String createUserTableQuery = "CREATE TABLE IF NOT EXISTS " + TABLE_USERS +
                " (" + COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USERNAME + " TEXT, " +
                COL_PASSWORD + " TEXT, " +
                COL_EMAIL + " TEXT)";
        db.execSQL(createUserTableQuery);

        // Create services table
        String createServiceTableQuery = "CREATE TABLE IF NOT EXISTS " + TABLE_SERVICES +
                " (" + COL_SERVICE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_SERVICE_NAME + " TEXT, " +
                COL_SERVICE_STATUS + " TEXT DEFAULT 'waiting', " +
                COL_SERVICE_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                COL_USER_ID_FK + " INTEGER, " +
                "FOREIGN KEY(" + COL_USER_ID_FK + ") REFERENCES " +
                TABLE_USERS + "(" + COL_USER_ID + "))";
        db.execSQL(createServiceTableQuery);

        // Create products table
        String createProductTableQuery = "CREATE TABLE " + TABLE_PRODUCTS +
                " (ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "PRODUCT_NAME TEXT, " +
                "PRODUCT_PRICE REAL, " +
                "PRODUCT_IMAGE TEXT, " +
                "PRODUCT_TYPE TEXT)";
        db.execSQL(createProductTableQuery);

        String createOrdersTableQuery = "CREATE TABLE " + TABLE_ORDERS +
                " (" + COL_ORDER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_ORDER_PRODUCT_NAME + " TEXT, " +
                COL_ORDER_PRODUCT_PRICE + " REAL, " +
                COL_ORDER_TIME + " TEXT, " +
                // 💡 ADDED: Foreign key column
                COL_USER_ID_FK_ORDER + " INTEGER, " +
                // 💡 ADDED: Foreign key constraint (linking to users)
                "FOREIGN KEY(" + COL_USER_ID_FK_ORDER + ") REFERENCES " +
                TABLE_USERS + "(" + COL_USER_ID + "))";
        db.execSQL(createOrdersTableQuery);

        String createReceiptsTableQuery = "CREATE TABLE " + TABLE_RECEIPTS +
                " (" + COL_RECEIPT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_RECEIPT_DATE + " TEXT, " +
                COL_RECEIPT_TOTAL_PRICE + " REAL, " +
                COL_RECEIPT_CONTENT + " TEXT, " +
                // 💡 ADDED: Foreign key column
                COL_USER_ID_FK_ORDER + " INTEGER, " +
                // 💡 ADDED: Foreign key constraint
                "FOREIGN KEY(" + COL_USER_ID_FK_ORDER + ") REFERENCES " +
                TABLE_USERS + "(" + COL_USER_ID + "))";
        db.execSQL(createReceiptsTableQuery);

        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PRODUCTS, null);
        if (cursor != null) {
            cursor.moveToFirst();
            int count = cursor.getInt(0);
            cursor.close();
            if (count == 0) {
                initializeSampleProducts(db);
            }
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SERVICES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ORDERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECEIPTS);
        onCreate(db);
    }

    // 💡 FIX: Helper method to hash passwords using SHA-256 for basic security
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return password; // Fallback only if something goes horribly wrong
        }
    }

    // 💡 NEW: Save receipt data to history
    public boolean insertReceipt(String date, double totalPrice, String content, int userId) { // 💡 ADDED: userId
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_RECEIPT_DATE, date);
        values.put(COL_RECEIPT_TOTAL_PRICE, totalPrice);
        values.put(COL_RECEIPT_CONTENT, content);
        values.put(COL_USER_ID_FK_ORDER, userId); // 💡 INSERTED: userId
        long result = db.insert(TABLE_RECEIPTS, null, values);
        db.close();
        return result != -1;
    }

    // 💡 NEW: Model class for returning receipt history data
    public static class ReceiptHistoryItem {
        public int id;
        public String date;
        public double totalPrice;
        public String content;

        public ReceiptHistoryItem(int id, String date, double totalPrice, String content) {
            this.id = id;
            this.date = date;
            this.totalPrice = totalPrice;
            this.content = content;
        }
    }

    // 💡 NEW: Retrieve all receipt history (must be present)
    public List<ReceiptHistoryItem> getAllReceiptHistory(int userId) { // 💡 ADDED: userId
        List<ReceiptHistoryItem> history = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // 💡 FILTERED: Filtered query using WHERE USER_ID_FK = ?
        Cursor cursor = db.rawQuery(
                "SELECT " + COL_RECEIPT_ID + ", " + COL_RECEIPT_DATE + ", " + COL_RECEIPT_TOTAL_PRICE + ", " + COL_RECEIPT_CONTENT +
                        " FROM " + TABLE_RECEIPTS +
                        " WHERE " + COL_USER_ID_FK_ORDER + " = ?" + // <-- Filter clause added here
                        " ORDER BY " + COL_RECEIPT_ID + " DESC", new String[]{String.valueOf(userId)});

        if (cursor != null) {
            // Note: Column index retrieval needs to be robust, using cursor.getColumnIndexOrThrow() is best practice
            // We rely on the implicit order or correct getColumnIndex for this block to function.
            int idIndex = cursor.getColumnIndex(COL_RECEIPT_ID);
            int dateIndex = cursor.getColumnIndex(COL_RECEIPT_DATE);
            int priceIndex = cursor.getColumnIndex(COL_RECEIPT_TOTAL_PRICE);
            int contentIndex = cursor.getColumnIndex(COL_RECEIPT_CONTENT);

            while (cursor.moveToNext()) {
                history.add(new ReceiptHistoryItem(
                        cursor.getInt(idIndex),
                        cursor.getString(dateIndex),
                        cursor.getDouble(priceIndex),
                        cursor.getString(contentIndex)
                ));
            }
            cursor.close();
        }
        return history;
    }
    ////////////////////////
    // User management methods
    public Boolean checkusername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + COL_USERNAME + "=?", new String[]{username});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public Boolean checkemail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + COL_EMAIL + "=?", new String[]{email});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public Boolean checkusernamepassword(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String hashedPass = hashPassword(password); // 💡 FIX: Hash entered password before checking
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + COL_USERNAME + "=? AND " + COL_PASSWORD + "=?", new String[]{username, hashedPass});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public Boolean insertUser(String username, String password, String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_USERNAME, username);
        contentValues.put(COL_PASSWORD, hashPassword(password)); // 💡 FIX: Store hashed password, not plain text
        contentValues.put(COL_EMAIL, email);
        long result = db.insert(TABLE_USERS, null, contentValues);
        return result != -1;
    }

    public int getUserId(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_USER_ID + " FROM " + TABLE_USERS + " WHERE " + COL_USERNAME + "=?", new String[]{username});
        int userId = -1;
        if (cursor.moveToFirst()) {
            userId = cursor.getInt(0);
        }
        cursor.close();
        return userId;
    }

    public String getUserEmail(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        String email = null;

        Cursor cursor = db.rawQuery(
                "SELECT " + COL_EMAIL + " FROM " + TABLE_USERS +
                        " WHERE " + COL_USERNAME + " = ?",
                new String[]{username}
        );

        if (cursor != null && cursor.moveToFirst()) {
            int emailColumnIndex = cursor.getColumnIndex(COL_EMAIL);
            if (emailColumnIndex != -1) {
                email = cursor.getString(emailColumnIndex);
            }
            cursor.close();
        }

        db.close();
        return email;
    }

    // In DBHelper.java, add this method:
    public List<String[]> getRandomProducts(int limit) {
        List<String[]> products = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // NOTE: This SQL uses ORDER BY RANDOM() to get random items.
        // Ensure TABLE_PRODUCTS and column names (PRODUCT_NAME, PRODUCT_PRICE, PRODUCT_IMAGE) are correct.
        String query = "SELECT " + COL_PRODUCT_NAME + ", " + COL_PRODUCT_PRICE + ", " + COL_PRODUCT_IMAGE +
                " FROM " + TABLE_PRODUCTS +
                " ORDER BY RANDOM() LIMIT " + limit;

        Cursor cursor = db.rawQuery(query, null);

        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex("PRODUCT_NAME");
            int priceIndex = cursor.getColumnIndex("PRODUCT_PRICE");
            int imageIndex = cursor.getColumnIndex("PRODUCT_IMAGE");

            while (cursor.moveToNext()) {
                // Store as String array: [Name, Price, ImageUrl]
                products.add(new String[]{
                        cursor.getString(nameIndex),
                        String.valueOf(cursor.getDouble(priceIndex)), // Convert double to string for array
                        cursor.getString(imageIndex)
                });
            }
            cursor.close();
        }
        db.close();
        return products;
    }
    // Service management methods
    public Boolean addService(String serviceName, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_SERVICE_NAME, serviceName);
        contentValues.put(COL_USER_ID_FK, userId);
        contentValues.put(COL_SERVICE_STATUS, "waiting");
        long result = db.insert(TABLE_SERVICES, null, contentValues);
        return result != -1;
    }
    public void notifyAdminNewService(String serviceName, String username) {
        // Prevent notification if the admin submits a request themselves
        if (ADMIN_USERNAME.equalsIgnoreCase(username)) {
            return;
        }

        // Prepare the notification intent
        Intent intent = new Intent(context, CartReminderReceiver.class);
        intent.setAction("NEW_SERVICE_REQUEST");
        intent.putExtra("message", username + " has requested a new service: " + serviceName);
        intent.putExtra("title", "🔔 New Service Request!");

        // Send broadcast immediately (this will be picked up by CartReminderReceiver)
        context.sendBroadcast(intent);

        Log.d("AdminNotification", "Sent notification for: " + username);
    }
    // In your DBHelper class, make sure the getUserServices method returns the correct status:
    public List<String> getUserServices(String username) {
        List<String> services = new ArrayList<>();
        int userId = getUserId(username);

        if (userId == -1) return services;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_SERVICE_NAME + ", " + COL_SERVICE_STATUS +
                " FROM " + TABLE_SERVICES + " WHERE " + COL_USER_ID_FK + "=? ORDER BY " +
                "CASE WHEN " + COL_SERVICE_STATUS + " = 'in_progress' THEN 1 " +
                "WHEN " + COL_SERVICE_STATUS + " = 'waiting' THEN 2 " +
                "WHEN " + COL_SERVICE_STATUS + " = 'completed' THEN 3 END, " +
                COL_SERVICE_CREATED_AT + " ASC", new String[]{String.valueOf(userId)});

        while (cursor.moveToNext()) {
            String serviceName = cursor.getString(0);
            String status = cursor.getString(1);
            services.add(serviceName + " - " + status);
        }
        cursor.close();
        return services;
    }
    public int[] getServiceCounts(String username) {
        int userId = getUserId(username);
        int[] counts = new int[3]; // waiting, in_progress, completed

        if (userId == -1) return counts;

        SQLiteDatabase db = this.getReadableDatabase();

        // Count waiting services
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SERVICES +
                        " WHERE " + COL_USER_ID_FK + "=? AND " + COL_SERVICE_STATUS + "='waiting'",
                new String[]{String.valueOf(userId)});
        if (cursor.moveToFirst()) counts[0] = cursor.getInt(0);
        cursor.close();

        // Count in_progress services
        cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SERVICES +
                        " WHERE " + COL_USER_ID_FK + "=? AND " + COL_SERVICE_STATUS + "='in_progress'",
                new String[]{String.valueOf(userId)});
        if (cursor.moveToFirst()) counts[1] = cursor.getInt(0);
        cursor.close();

        // Count completed services
        cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SERVICES +
                        " WHERE " + COL_USER_ID_FK + "=? AND " + COL_SERVICE_STATUS + "='completed'",
                new String[]{String.valueOf(userId)});
        if (cursor.moveToFirst()) counts[2] = cursor.getInt(0);
        cursor.close();

        return counts;
    }
    public void updateServiceStatus(int serviceId, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_SERVICE_STATUS, status);
        db.update(TABLE_SERVICES, values, COL_SERVICE_ID + "=?", new String[]{String.valueOf(serviceId)});
    }


    // 💡 NEW: Method for Admin ("or") to see all services from all users.
    public List<String> getAllServicesForAllUsers() {
        List<String> services = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // SQL Query to join services table (s) and users table (u)
        // We select the service name, status, and the username (owner)
        String query = "SELECT s." + COL_SERVICE_NAME + ", s." + COL_SERVICE_STATUS + ", u." + COL_USERNAME +
                " FROM " + TABLE_SERVICES + " s " +
                "JOIN " + TABLE_USERS + " u ON s." + COL_USER_ID_FK + " = u." + COL_USER_ID +
                // The sorting below is based on the original logic but should only
                // prioritize 'waiting' over 'completed' if the original logic is desired.
                // Since 'in_progress' is removed from the fragment, we simplify the sort.
                " ORDER BY " + COL_SERVICE_CREATED_AT + " ASC";

        Cursor cursor = db.rawQuery(query, null);

        while (cursor.moveToNext()) {
            String serviceName = cursor.getString(0);
            String status = cursor.getString(1);
            String ownerUsername = cursor.getString(2);

            // Format: "ServiceName by OwnerUsername - Status"
            // This format is required by ServiceFragment.java's handleServiceClick logic.
            services.add(serviceName + " by " + ownerUsername + " - " + status);
        }
        cursor.close();
        return services;
    }
    // In your DBHelper class, update the getServiceId method if needed:
    public int getServiceId(String serviceName, int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_SERVICE_ID + " FROM " + TABLE_SERVICES +
                        " WHERE " + COL_SERVICE_NAME + "=? AND " + COL_USER_ID_FK + "=? " +
                        "ORDER BY " + COL_SERVICE_CREATED_AT + " DESC LIMIT 1",
                new String[]{serviceName, String.valueOf(userId)});
        int serviceId = -1;
        if (cursor.moveToFirst()) {
            serviceId = cursor.getInt(0);
        }
        cursor.close();
        return serviceId;
    }
    // ✅ ADMIN: Get all services (with usernames)
    public List<String> getAllServices() {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT s." + COL_SERVICE_NAME + ", s." + COL_SERVICE_STATUS + ", u." + COL_USERNAME +
                        " FROM " + TABLE_SERVICES + " s " +
                        "JOIN " + TABLE_USERS + " u ON s." + COL_USER_ID_FK + " = u." + COL_USER_ID +
                        " ORDER BY CASE " +
                        "WHEN s." + COL_SERVICE_STATUS + " = 'in_progress' THEN 1 " +
                        "WHEN s." + COL_SERVICE_STATUS + " = 'waiting' THEN 2 " +
                        "WHEN s." + COL_SERVICE_STATUS + " = 'completed' THEN 3 END, " +
                        "s." + COL_SERVICE_CREATED_AT + " ASC",
                null
        );

        while (cursor.moveToNext()) {
            String name = cursor.getString(0);
            String status = cursor.getString(1);
            String user = cursor.getString(2);
            list.add(name + " - " + status + " (" + user + ")");
        }
        cursor.close();
        return list;
    }

    // ✅ ADMIN: Get all service counts (for all users)
    public int[] getAllServiceCounts() {
        SQLiteDatabase db = this.getReadableDatabase();
        int[] counts = new int[2]; // [0]=in_progress, [1]=completed

        Cursor cursor = db.rawQuery(
                "SELECT " + COL_SERVICE_STATUS + ", COUNT(*) FROM " + TABLE_SERVICES + " GROUP BY " + COL_SERVICE_STATUS,
                null
        );

        if (cursor.moveToFirst()) {
            do {
                String status = cursor.getString(0);
                int count = cursor.getInt(1);
                if ("in_progress".equals(status)) counts[0] = count;
                if ("completed".equals(status)) counts[1] = count;
            } while (cursor.moveToNext());
        }
        cursor.close();
        return counts;
    }

    // ✅ ADMIN: Get username for a service
    public String getUsernameForService(String serviceName) {
        SQLiteDatabase db = this.getReadableDatabase();
        String username = null;
        Cursor cursor = db.rawQuery(
                "SELECT u." + COL_USERNAME +
                        " FROM " + TABLE_SERVICES + " s " +
                        "JOIN " + TABLE_USERS + " u ON s." + COL_USER_ID_FK + " = u." + COL_USER_ID +
                        " WHERE s." + COL_SERVICE_NAME + " = ? LIMIT 1",
                new String[]{serviceName}
        );
        if (cursor.moveToFirst()) {
            username = cursor.getString(0);
        }
        cursor.close();
        return username;
    }

    // ✅ ADMIN: Get service ID by name and username
    public int getServiceIdByNameAndUser(String serviceName, String username) {
        int userId = getUserId(username);
        if (userId == -1) return -1;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + COL_SERVICE_ID + " FROM " + TABLE_SERVICES +
                        " WHERE " + COL_SERVICE_NAME + "=? AND " + COL_USER_ID_FK + "=? " +
                        "ORDER BY " + COL_SERVICE_CREATED_AT + " DESC LIMIT 1",
                new String[]{serviceName, String.valueOf(userId)}
        );

        int id = -1;
        if (cursor.moveToFirst()) id = cursor.getInt(0);
        cursor.close();
        return id;
    }

    private List<Double> getProductPricesByName(List<String> productNames) {
        List<Double> productPrices = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COL_PRODUCT_NAME + " IN (" + makePlaceholders(productNames.size()) + ")";
        Cursor cursor = db.query(TABLE_PRODUCTS, new String[]{COL_PRODUCT_PRICE}, selection, productNames.toArray(new String[0]), null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                double price = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PRODUCT_PRICE));
                productPrices.add(price);
            }
            cursor.close();
        }
        return productPrices;
    }
    private String makePlaceholders(int len) {
        if (len < 1) {
            // It will lead to an invalid query anyway ..
            throw new RuntimeException("No placeholders");
        } else {
            StringBuilder sb = new StringBuilder(len * 2 - 1);
            sb.append("?");
            for (int i = 1; i < len; i++) {
                sb.append(",?");
            }
            return sb.toString();
        }
    }
    private double calculateTotalSum(List<String> productNames) {
        double totalSum = 0.0;
        // Assuming product prices are available in the database, retrieve them and calculate the total sum
        List<Double> productPrices = getProductPricesByName(productNames);
        for (Double price : productPrices) {
            totalSum += price;
        }
        return totalSum;
    }
    public String[] getProductDetailsByName(String productName) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] projection = {COL_PRODUCT_PRICE, COL_PRODUCT_IMAGE};
        String selection = COL_PRODUCT_NAME + " = ?";
        String[] selectionArgs = {productName};

        Cursor cursor = db.query(TABLE_PRODUCTS, projection, selection, selectionArgs, null, null, null);

        String[] details = null;
        if (cursor != null && cursor.moveToFirst()) {
            int priceIndex = cursor.getColumnIndexOrThrow(COL_PRODUCT_PRICE);
            int imageIndex = cursor.getColumnIndexOrThrow(COL_PRODUCT_IMAGE);

            String price = String.valueOf(cursor.getDouble(priceIndex));
            String imageUrl = cursor.getString(imageIndex);

            details = new String[]{price, imageUrl};
        }

        if (cursor != null) {
            cursor.close();
        }
        return details;
    }
    private void insertProduct(SQLiteDatabase db, String productName, double productPrice, String imgUrl, ProductType type) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_PRODUCT_NAME, productName);
        contentValues.put(COL_PRODUCT_PRICE, productPrice);
        contentValues.put(COL_PRODUCT_IMAGE, imgUrl);
        contentValues.put(COL_PRODUCT_TYPE, type.name()); // Store the enum as string
        db.insert(TABLE_PRODUCTS, null, contentValues);
    }
    public List<String> getProductImageUrlsByType(String productType) {
        List<String> imageUrls = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String[] projection = {COL_PRODUCT_IMAGE};
        String selection = COL_PRODUCT_TYPE + " = ?";
        String[] selectionArgs = {productType};
        Cursor cursor = db.query(TABLE_PRODUCTS, projection, selection, selectionArgs, null, null, null);
        if (cursor != null) {
            int productImageIndex = cursor.getColumnIndex(COL_PRODUCT_IMAGE);
            if (productImageIndex != -1) {
                while (cursor.moveToNext()) {
                    String productImage = cursor.getString(productImageIndex);
                    imageUrls.add(productImage);
                }
            }
            cursor.close();
        }
        return imageUrls;
    }

    public List<String> getProductNamesByType(ProductType productType) {
        List<String> productNames = new ArrayList<>();
        productNames.add(productType.toString());
        return  productNames;
    }
    public List<String> getAllProductTypes() {
        List<String> productTypes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT DISTINCT " + COL_PRODUCT_TYPE + " FROM " + TABLE_PRODUCTS, null);
        if (cursor != null) {
            int productTypeIndex = cursor.getColumnIndex(COL_PRODUCT_TYPE);
            if (productTypeIndex != -1) {
                while (cursor.moveToNext()) {
                    String productType = cursor.getString(productTypeIndex);
                    productTypes.add(productType);
                }
            }
            cursor.close();
        }
        return productTypes;
    }


    public List<Double> getProductsPricesByType(String productType) {
        List<Double> productPrices = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String[] projection = {COL_PRODUCT_PRICE};
        String selection = COL_PRODUCT_TYPE + " = ?";
        String[] selectionArgs = {productType};
        Cursor cursor = db.query(TABLE_PRODUCTS, projection, selection, selectionArgs, null, null, null);
        if (cursor != null) {
            int productPriceIndex = cursor.getColumnIndex(COL_PRODUCT_PRICE);
            if (productPriceIndex != -1) {
                while (cursor.moveToNext()) {
                    double productPrice = cursor.getDouble(productPriceIndex);
                    productPrices.add(productPrice);
                }
            }
            cursor.close();
        }
        return productPrices;
    }
    public List<String> getProductsNamesByType(String productType) {
        List<String> productNames = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String[] projection = {COL_PRODUCT_NAME};
        String selection = COL_PRODUCT_TYPE + " = ?";
        String[] selectionArgs = {productType};
        Cursor cursor = db.query(TABLE_PRODUCTS, projection, selection, selectionArgs, null, null, null);
        if (cursor != null) {
            int productNameIndex = cursor.getColumnIndex(COL_PRODUCT_NAME);
            if (productNameIndex != -1) {
                while (cursor.moveToNext()) {
                    String productName = cursor.getString(productNameIndex);
                    productNames.add(productName);
                }
            }
            cursor.close();
        }
        return productNames;
    }
    //order methods
    public long insertOrder(String productName, double productPrice, String orderTime, int userId) { // 💡 ADDED: userId
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_ORDER_PRODUCT_NAME, productName);
        values.put(COL_ORDER_PRODUCT_PRICE, productPrice);
        values.put(COL_ORDER_TIME, orderTime);
        values.put(COL_USER_ID_FK_ORDER, userId); // 💡 INSERTED: userId
        long id = db.insert(TABLE_ORDERS, null, values);
        db.close();
        return id;
    }

    // 💡 FIX: Lightweight method to count items without loading massive data into the UI thread
    public int getCartItemCount(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_ORDERS +
                " WHERE " + COL_USER_ID_FK_ORDER + " = ?", new String[]{String.valueOf(userId)});
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        return count;
    }

    public double calculateTotalOrderSum(int userId) { // 💡 ADDED: userId
        double totalSum = 0.0;
        SQLiteDatabase db = this.getReadableDatabase();
        // 💡 FILTERED: Filtered query using WHERE USER_ID_FK = ?
        Cursor cursor = db.rawQuery("SELECT " + COL_ORDER_PRODUCT_PRICE + " FROM " + TABLE_ORDERS +
                " WHERE " + COL_USER_ID_FK_ORDER + " = ?", new String[]{String.valueOf(userId)});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                totalSum += cursor.getDouble(0);
            }
            cursor.close();
        }
        Log.d("TotalOrderSum", "Total Order Sum: " + totalSum);
        return totalSum;
    }
    public boolean deleteOrder(String orderId, int userId) { // 💡 ADDED: userId
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            // 💡 FILTERED: Filtered deletion using ORDER_ID AND USER_ID_FK
            int deletedRows = db.delete(TABLE_ORDERS,
                    COL_ORDER_ID + "=? AND " + COL_USER_ID_FK_ORDER + "=?",
                    new String[]{orderId, String.valueOf(userId)});
            db.close();
            if (deletedRows > 0) {
                return true;
            } else {
                Log.e("DeleteOrder", "No order was deleted.");
                return false;
            }
        } catch (Exception e) {
            Log.e("DeleteOrder", "Error deleting order: " + e.getMessage());
            return false;
        }
    }

    public void deleteAllOrders(int userId) { // 💡 ADDED: userId
        SQLiteDatabase db = this.getWritableDatabase();
        // 💡 FILTERED: Delete using WHERE USER_ID_FK = ?
        db.delete(TABLE_ORDERS, COL_USER_ID_FK_ORDER + "=?", new String[]{String.valueOf(userId)});
        db.close();
    }

    public List<String> getAllOrderNames(int userId) { // 💡 ADDED: userId
        List<String> orderNames = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        // 💡 FILTERED: Filtered query using WHERE USER_ID_FK = ?
        Cursor cursor = db.rawQuery("SELECT " + COL_ORDER_PRODUCT_NAME + " FROM " + TABLE_ORDERS +
                " WHERE " + COL_USER_ID_FK_ORDER + " = ?", new String[]{String.valueOf(userId)});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                orderNames.add(cursor.getString(0));
            }
            cursor.close();
        }
        return orderNames;
    }

    public List<Double> getAllOrderPrices(int userId) { // 💡 ADDED: userId
        List<Double> orderPrices = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        // 💡 FILTERED: Filtered query using WHERE USER_ID_FK = ?
        Cursor cursor = db.rawQuery("SELECT " + COL_ORDER_PRODUCT_PRICE + " FROM " + TABLE_ORDERS +
                " WHERE " + COL_USER_ID_FK_ORDER + " = ?", new String[]{String.valueOf(userId)});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                orderPrices.add(cursor.getDouble(0));
            }
            cursor.close();
        }
        return orderPrices;
    }

    public List<Integer> getAllOrderIds(int userId) { // 💡 ADDED: userId
        List<Integer> orderIds = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        // 💡 FILTERED: Filtered query using WHERE USER_ID_FK = ?
        Cursor cursor = db.rawQuery("SELECT " + COL_ORDER_ID + " FROM " + TABLE_ORDERS +
                " WHERE " + COL_USER_ID_FK_ORDER + " = ?", new String[]{String.valueOf(userId)});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                orderIds.add(cursor.getInt(0));
            }
            cursor.close();
        }
        return orderIds;
    }


    // 💡 NEW: Helper method to get product prices by name (Needed for dynamic list)
    public List<Double> getProductsPricesByName(List<String> productNames) {
        List<Double> productPrices = new ArrayList<>();
        if (productNames.isEmpty()) return productPrices;

        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COL_PRODUCT_NAME + " IN (" + makePlaceholders(productNames.size()) + ")";

        // Convert productNames list to an array of Strings for selectionArgs
        String[] selectionArgs = productNames.toArray(new String[0]);

        Cursor cursor = db.query(TABLE_PRODUCTS, new String[]{COL_PRODUCT_PRICE}, selection, selectionArgs, null, null, null);

        if (cursor != null) {
            int priceIndex = cursor.getColumnIndexOrThrow(COL_PRODUCT_PRICE);
            while (cursor.moveToNext()) {
                double price = cursor.getDouble(priceIndex);
                productPrices.add(price);
            }
            cursor.close();
        }
        return productPrices;
    }

    // 💡 NEW: Helper method to get product image URLs by name (Needed for dynamic list)
    public List<String> getProductImageUrlsByName(List<String> productNames) {
        List<String> imageUrls = new ArrayList<>();
        if (productNames.isEmpty()) return imageUrls;

        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COL_PRODUCT_NAME + " IN (" + makePlaceholders(productNames.size()) + ")";

        // Convert productNames list to an array of Strings for selectionArgs
        String[] selectionArgs = productNames.toArray(new String[0]);

        Cursor cursor = db.query(TABLE_PRODUCTS, new String[]{COL_PRODUCT_IMAGE}, selection, selectionArgs, null, null, null);

        if (cursor != null) {
            int imageIndex = cursor.getColumnIndexOrThrow(COL_PRODUCT_IMAGE);
            while (cursor.moveToNext()) {
                String imageUrl = cursor.getString(imageIndex);
                imageUrls.add(imageUrl);
            }
            cursor.close();
        }
        return imageUrls;
    }

    // 💡 NEW: The main method to get product names filtered by type AND search query
    public List<String> getProductsNamesByTypeAndSearch(String productType, String searchQuery) {
        List<String> productNames = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String[] projection = {COL_PRODUCT_NAME};

        // Start with the basic filter by type
        String selection = COL_PRODUCT_TYPE + " = ?";
        List<String> selectionArgsList = new ArrayList<>();
        selectionArgsList.add(productType);

        // Add search condition if a query exists
        if (!searchQuery.isEmpty()) {
            selection += " AND " + COL_PRODUCT_NAME + " LIKE ?";
            selectionArgsList.add("%" + searchQuery + "%");
        }

        String[] selectionArgs = selectionArgsList.toArray(new String[0]);

        Cursor cursor = db.query(TABLE_PRODUCTS, projection, selection, selectionArgs, null, null, COL_PRODUCT_NAME);

        if (cursor != null) {
            int productNameIndex = cursor.getColumnIndexOrThrow(COL_PRODUCT_NAME);
            while (cursor.moveToNext()) {
                String productName = cursor.getString(productNameIndex);
                productNames.add(productName);
            }
            cursor.close();
        }
        return productNames;
    }

    public enum ProductType {
        COMPUTERS,
        GAMING_CONSOLES,
        TVS_AND_DISPLAYS,
        VIDEO_GAMES,
        HOME_APPLIANCES,
        AUDIO_EQUIPMENT,
        SMARTPHONES,
        CAMERAS,
        NETWORKING,
        SMART_HOME
    }
    // Add these methods to your DBHelper.java
    public List<String> getProductsNamesByTypeAndSearchPaginated(String productType, String searchQuery, int limit, int offset) {
        List<String> productNames = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String[] projection = {COL_PRODUCT_NAME};

        String selection = COL_PRODUCT_TYPE + " = ?";
        List<String> selectionArgsList = new ArrayList<>();
        selectionArgsList.add(productType);

        if (!searchQuery.isEmpty()) {
            selection += " AND " + COL_PRODUCT_NAME + " LIKE ?";
            selectionArgsList.add("%" + searchQuery + "%");
        }

        String[] selectionArgs = selectionArgsList.toArray(new String[0]);
        String limitClause = limit + " OFFSET " + offset;

        Cursor cursor = db.query(TABLE_PRODUCTS, projection, selection, selectionArgs, null, null, COL_PRODUCT_NAME, limitClause);

        if (cursor != null) {
            int productNameIndex = cursor.getColumnIndexOrThrow(COL_PRODUCT_NAME);
            while (cursor.moveToNext()) {
                productNames.add(cursor.getString(productNameIndex));
            }
            cursor.close();
        }
        return productNames;
    }

    public int getProductCountByTypeAndSearch(String productType, String searchQuery) {
        SQLiteDatabase db = this.getReadableDatabase();

        String selection = COL_PRODUCT_TYPE + " = ?";
        List<String> selectionArgsList = new ArrayList<>();
        selectionArgsList.add(productType);

        if (!searchQuery.isEmpty()) {
            selection += " AND " + COL_PRODUCT_NAME + " LIKE ?";
            selectionArgsList.add("%" + searchQuery + "%");
        }

        String[] selectionArgs = selectionArgsList.toArray(new String[0]);

        Cursor cursor = db.query(TABLE_PRODUCTS, new String[]{"COUNT(*)"}, selection, selectionArgs, null, null, null);

        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }
    // Initialize with KSP-style sample data
    public void initializeSampleProducts(SQLiteDatabase db) {
        // COMPUTERS - 30 products
        insertProduct(db, "Gaming PC Ryzen 7", 1299.99, "https://m.media-amazon.com/images/I/715ey5-SgiL.jpg", ProductType.COMPUTERS);
        insertProduct(db, "MacBook Pro 16\" M3", 2499.99, "https://www.istoreil.co.il/media/catalog/product/m/a/macbook_pro_16_in_m3_pro_max_space_black_pdp_image_position-1__wwen_2.jpg", ProductType.COMPUTERS);
        insertProduct(db, "Dell XPS 13 Laptop", 1199.99, "https://m.media-amazon.com/images/I/710EGJBdIML._AC_SL1500_.jpg", ProductType.COMPUTERS);
        insertProduct(db, "ASUS ROG Gaming Laptop", 1799.99, "https://m.media-amazon.com/images/I/81XZXFH-RZL._AC_SX466_.jpg", ProductType.COMPUTERS);
        insertProduct(db, "HP Pavilion Desktop", 899.99, "https://m.media-amazon.com/images/I/81Lp4dVJDdL._AC_SX300_SY300_QL70_FMwebp_.jpg", ProductType.COMPUTERS);
        insertProduct(db, "Lenovo ThinkPad", 1099.99, "https://m.media-amazon.com/images/I/61IRRQ2gWPL.jpg", ProductType.COMPUTERS);
        insertProduct(db, "Alienware Aurora R15", 2199.99, "https://i.dell.com/is/image/DellContent/content/dam/ss2/product-images/dell-client-products/desktops/alienware-desktops/alienware-aurora-r15-intel/media-gallery/lunar-light-wh-clear-cryo-tech/desktop-alienware-aurora-r15-white-cryo-clear-panel-gallery-1.psd?fmt=png-alpha&pscan=auto&scl=1&wid=3398&hei=3941&qlt=100,1&resMode=sharp2&size=3398,3941&chrss=full&imwidth=5000", ProductType.COMPUTERS);
        insertProduct(db, "Microsoft Surface Pro", 1299.99, "https://www.adcs.co.il/media/catalog/product/cache/0d7e15299f2e9c150ef66e46509ba14b/z/d/zdt-00001_132150_.jpeg", ProductType.COMPUTERS);
        insertProduct(db, "Acer Predator Helios", 1499.99, "https://m.media-amazon.com/images/I/71sS7G5ZpQL._AC_SX300_SY300_QL70_FMwebp_.jpg", ProductType.COMPUTERS);
        insertProduct(db, "Custom Water Cooled PC", 2999.99, "https://i.ytimg.com/vi/-LDwgCbwcJ0/maxresdefault.jpg", ProductType.COMPUTERS);
        insertProduct(db, "Mac Mini M2 Pro", 1299.99, "https://mikicom.co.il/Cat_499090_4267.webp", ProductType.COMPUTERS);
        insertProduct(db, "Razer Blade 15", 2299.99, "https://m.media-amazon.com/images/I/71kBeFDgCkL._AC_SX466_.jpg", ProductType.COMPUTERS);
        insertProduct(db, "MSI Gaming Desktop", 1599.99, "https://storage-asset.msi.com/us/picture/feature/desktop/aegis_r_10th/components.png", ProductType.COMPUTERS);
        insertProduct(db, "Google Pixelbook Go", 999.99, "https://m.media-amazon.com/images/I/61y1WjZMBXL.jpg", ProductType.COMPUTERS);
        insertProduct(db, "Framework Laptop 13", 1049.99, "https://i.pcmag.com/imagery/reviews/03b2FfXfg7dHwHmcZWgeWjz-1.fit_lim.size_1050x591.v1689714556.jpg", ProductType.COMPUTERS);
        insertProduct(db, "Intel NUC Mini PC", 699.99, "https://m.media-amazon.com/images/I/71g2bpsrkkL._UF894,1000_QL80_.jpg", ProductType.COMPUTERS);
        insertProduct(db, "Gaming PC Intel i9", 1899.99, "https://m.media-amazon.com/images/I/71A8cat9MuL.jpg", ProductType.COMPUTERS);
        insertProduct(db, "MacBook Air 15\" M2", 1299.99, "https://img.zap.co.il/pics/8/6/5/1/92221568d.gif", ProductType.COMPUTERS);
        insertProduct(db, "Dell Alienware Laptop", 1999.99, "https://imageio.forbes.com/b-i-forbesimg/jasonevangelho/files/2013/06/Alienware-14-back-angle1.jpg?height=455&width=590&fit=bounds", ProductType.COMPUTERS);
        insertProduct(db, "ASUS ZenBook Pro", 1699.99, "https://d3m9l0v76dty0.cloudfront.net/system/photos/11958509/original/2393556a581afef24867bdecec2caa92.jpg", ProductType.COMPUTERS);
        insertProduct(db, "HP Spectre x360", 1399.99, "https://d3m9l0v76dty0.cloudfront.net/system/photos/7261614/large/29d61295ca291e27c6e24464314e94f3.jpg", ProductType.COMPUTERS);
        insertProduct(db, "Lenovo Yoga 9i", 1299.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSKFg7ODX9H048bT7d_FIKaCkDnmmLvaYgkdw&s", ProductType.COMPUTERS);
        insertProduct(db, "Microsoft Surface Laptop", 1199.99, "https://cdn-dynmedia-1.microsoft.com/is/image/microsoftcorp/MSFT-Surface-Laptop-6-Sneak-Curosel-Pivot-3?scl=1", ProductType.COMPUTERS);
        insertProduct(db, "All-in-One Desktop", 1299.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcT86XPxDxo6rmFQbyAizWFBPXbif378kLYuQQ&s", ProductType.COMPUTERS);
        insertProduct(db, "PlayStation 5 Pro", 599.99, "https://hotstore.hotmobile.co.il/media/catalog/product/cache/a73c0d5d6c75fbb1966fe13af695aeb7/p/s/ps5_cfi2000_pr_01_cmyk_copy_3.png", ProductType.GAMING_CONSOLES);
        insertProduct(db, "Xbox Series X", 499.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRYFRrbQ9WDJ6hiIreMaoOfVVLfR6gzKlr5bw&s", ProductType.GAMING_CONSOLES);
        insertProduct(db, "Nintendo Switch OLED", 349.99, "https://media.gamestop.com/i/gamestop/11149258/Nintendo-Switch-OLED-Console", ProductType.GAMING_CONSOLES);
        insertProduct(db, "PlayStation VR2", 549.99, "https://gmedia.playstation.com/is/image/SIEPDC/PSVR2-thumbnail-01-en-22feb22?$facebook$", ProductType.GAMING_CONSOLES);
        insertProduct(db, "Xbox Elite Controller", 179.99, "https://www.dominator.co.il/images/itempics/3227_18062019165512_large.jpg", ProductType.GAMING_CONSOLES);
        insertProduct(db, "Steam Deck 512GB", 649.99, "https://www.dominator.co.il/images/itempics/12366_03072024173002_large.jpg", ProductType.GAMING_CONSOLES);
        insertProduct(db, "Nintendo Switch Lite", 199.99, "https://encrypted-tbn2.gstatic.com/shopping?q=tbn:ANd9GcTy8mm4z3vOKMemAHBbih3UGOM13-7Qg2JcL88uMymzsfeh7RX2jweD-vubGKjdwK3JuQBUpUPZgtXjaNoorUIhfjdU1Wn5IbkMI7C__9_5tHnygA9VZW7CEQ", ProductType.GAMING_CONSOLES);
        insertProduct(db, "PS5 DualSense Edge", 199.99, "https://img.zap.co.il/pics/5/1/9/5/76005915d.gif", ProductType.GAMING_CONSOLES);
        insertProduct(db, "Gaming Chair Pro", 299.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSpEfoIexWYAA0dgJip7eXjd4gYUPv5Hms6Vw&s", ProductType.GAMING_CONSOLES);
        insertProduct(db, "Console Carrying Case", 49.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSUnttcWqSlelKeiIyWsQq3xfjYaRMqR0O8Ug&s", ProductType.GAMING_CONSOLES);
        insertProduct(db, "Xbox Series S", 299.99, "https://katom.shop/wp-content/uploads/2024/03/0b2854b9-a7e7-47dd-b4f8-a371567854b2.png", ProductType.GAMING_CONSOLES);
        insertProduct(db, "PlayStation 5 Digital", 449.99, "https://m.media-amazon.com/images/I/31MgKgiwAeL._SX342_SY445_QL70_FMwebp_.jpg", ProductType.GAMING_CONSOLES);
        insertProduct(db, "Nintendo Switch Pro", 399.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTNgSwgqn9zt306XQueH68sOZ3tZMe8mtXw9Q&s", ProductType.GAMING_CONSOLES);
        insertProduct(db, "Gaming Headset Pro", 149.99, "https://www.ocpc.co.il/wp-content/uploads/2021/07/61XjjJovijL._AC_SL1500_.jpg", ProductType.GAMING_CONSOLES);
        insertProduct(db, "Controller Charging Dock", 39.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTWiPsXOb7-hc_Qz6ckt6lFVuHIkEfEOvjqog&s", ProductType.GAMING_CONSOLES);
        insertProduct(db, "Gaming Keyboard Mechanical", 129.99, "https://cdn.mos.cms.futurecdn.net/XMDNCcbVWnrYj3zdapKrGb.jpg", ProductType.GAMING_CONSOLES);
        insertProduct(db, "Gaming Mouse Wireless", 89.99, "https://m.media-amazon.com/images/I/61Mk3YqYHpL._UF894,1000_QL80_.jpg", ProductType.GAMING_CONSOLES);
        insertProduct(db, "Console Skin Protector", 24.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQKWcSF6ITgabtaNQcK1l2teypLiMrssgb7-A&s", ProductType.GAMING_CONSOLES);
        insertProduct(db, "Gaming Monitor Stand", 79.99, "https://m.media-amazon.com/images/I/61x6rzhViDL.jpg", ProductType.GAMING_CONSOLES);
        insertProduct(db, "Console Cooling Fan", 34.99, "https://i5.walmartimages.com/seo/Cooling-Fan-Dust-Proof-for-Xbox-Series-X-Console-with-Colorful-Light-Strip-Dust-Cover-Filter-Low-Noise-Top-Fan_95429b84-e533-4b88-a0ce-02465cc65a1e.09800b96c5f3298a98f8a4179098db74.jpeg?odnHeight=768&odnWidth=768&odnBg=FFFFFF", ProductType.GAMING_CONSOLES);
        insertProduct(db, "Gaming Desk Large", 249.99, "https://m.media-amazon.com/images/I/81JnWF-jqPL.jpg", ProductType.GAMING_CONSOLES);
        insertProduct(db, "Controller Thumb Grips", 14.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcS-IFiGkWnMjS2Jba73LuN9JlR7PsG7n_htlQ&s", ProductType.GAMING_CONSOLES);
        insertProduct(db, "Console Vertical Stand", 29.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQmKajugOkZTRY95PQXT6pzPZCxE8J3eYQuIA&s", ProductType.GAMING_CONSOLES);
        insertProduct(db, "Gaming Microphone", 99.99, "https://m.media-amazon.com/images/I/71jfzOmq6dL.jpg", ProductType.GAMING_CONSOLES);
        insertProduct(db, "Console Travel Bag", 59.99, "https://m.media-amazon.com/images/I/8173csq7edL._UY1000_.jpg", ProductType.GAMING_CONSOLES);
        insertProduct(db, "Gaming Console Bundle", 699.99, "https://i5.walmartimages.com/seo/Latest-Xbox-Series-X-Gaming-Console-Bundle-1TB-SSD-Black-Xbox-Console-and-Wireless-Controller-with-HALO-Infinity-and-Mytrix-HDMI-Cable_7bfe8527-c6ae-42b3-a403-30a7655ae0c1.9c3ed2a067f4ac136194010f919fe3c4.jpeg", ProductType.GAMING_CONSOLES);
        insertProduct(db, "Console Storage Expansion", 199.99, "https://www.seagate.com/content/dam/seagate/assets/products/gaming-drive/xbox/storage-expansion-for-xbox-series-x/storage-expansion-card-for-xbox-series-x-s-pdp-row-2-4-640x640.png/_jcr_content/renditions/1-1-large-640x640.png", ProductType.GAMING_CONSOLES);
        insertProduct(db, "Samsung 85\" 8K QLED TV", 3999.99, "https://img.zap.co.il/pics/9/4/2/8/77838249d.gif", ProductType.TVS_AND_DISPLAYS);
        insertProduct(db, "LG 77\" OLED C3", 2499.99, "https://media.us.lg.com/transform/ecomm-PDPGallery-1100x730/af63d767-92db-4c8a-8200-75c2dfadf1c8/md08003930-DZ-2-jpg?io=transform:fill,width:596", ProductType.TVS_AND_DISPLAYS);
        insertProduct(db, "Sony Bravia 65\" 4K", 1799.99, "https://www.traklin.co.il/images/gallery/15696/x75wl.jpg", ProductType.TVS_AND_DISPLAYS);
        insertProduct(db, "Gaming Monitor 32\" 240Hz", 699.99, "https://www.pc365.co.il/wp-content/uploads/2024/12/32GS95UV-B.webp", ProductType.TVS_AND_DISPLAYS);
        insertProduct(db, "UltraWide Curved Monitor", 899.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSnsRWvqXYGN_0jtQ-bR7AuTtEXLdKq5SZ4Qw&s", ProductType.TVS_AND_DISPLAYS);
        insertProduct(db, "4K Projector", 1299.99, "https://d3m9l0v76dty0.cloudfront.net/system/photos/15520467/original/6505e1735e8c43a1d98836b9fc588d23.jpg", ProductType.TVS_AND_DISPLAYS);
        insertProduct(db, "Smart TV 55\" 4K UHD", 799.99, "https://www.galeykor.co.il/images/itempics/6328_090620241754362.jpg", ProductType.TVS_AND_DISPLAYS);
        insertProduct(db, "Portable Monitor 15.6\"", 299.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTJqOBSD2BFfH9jJbG8aL8Ni5rcrTAwlIxG9Q&s", ProductType.TVS_AND_DISPLAYS);
        insertProduct(db, "Dual Monitor Stand", 149.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRN0g9Ea5f4DOYrrXzz1_z58-9qCzDbEKvUZw&s", ProductType.TVS_AND_DISPLAYS);
        insertProduct(db, "TV Soundbar System", 399.99, "https://m.media-amazon.com/images/I/7113LuUzdBL.jpg", ProductType.TVS_AND_DISPLAYS);
        insertProduct(db, "Samsung 75\" 4K QLED", 1999.99, "https://www.citydeal.co.il/images/itempics/18510-3_24042024155704.jpg", ProductType.TVS_AND_DISPLAYS);
        insertProduct(db, "LG 65\" NanoCell TV", 1199.99, "https://www.electricshop.co.il/images/itempics/65NANO846QA_141220221137541_large.jpg", ProductType.TVS_AND_DISPLAYS);
        insertProduct(db, "Sony 55\" 4K HDR", 899.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSzGFr-xFc0_zWQWt8blPFCzCAjf5ipqvgUcg&s", ProductType.TVS_AND_DISPLAYS);
        insertProduct(db, "Monitor 27\" 4K IPS", 349.99, "https://m.media-amazon.com/images/I/71GRpZb6+vL.jpg", ProductType.TVS_AND_DISPLAYS);
        insertProduct(db, "Gaming Monitor 49\" Super", 1299.99, "https://www.xgaming.co.il/images/itempics/971_130720251344583366_large.jpg", ProductType.TVS_AND_DISPLAYS);
        insertProduct(db, "TV Wall Mount Full Motion", 129.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTGvHTigFHKAPkBKAyzWurjxRs9IlUAGZ9niw&s", ProductType.TVS_AND_DISPLAYS);
        insertProduct(db, "Monitor Arm Single", 89.99, "https://www.hexcal.com/cdn/shop/files/Hexcal_Single_Monitor_Arm_Front_View_1000x.jpg?v=1752211670", ProductType.TVS_AND_DISPLAYS);
        insertProduct(db, "TV Cabinet Stand", 199.99, "https://avfgroup.com/us/wp-content/uploads/sites/3/2022/12/fs900varwb-a_04_large.jpg", ProductType.TVS_AND_DISPLAYS);
        insertProduct(db, "Outdoor TV 55\" Weatherproof", 2499.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR25-DQN3JrwFqi9MeCufG1GawH1SWeiaI9LQ&s", ProductType.TVS_AND_DISPLAYS);
        insertProduct(db, "Touch Screen Monitor", 599.99, "https://m.media-amazon.com/images/I/61HFY+Ji+JL.jpg", ProductType.TVS_AND_DISPLAYS);
        insertProduct(db, "Monitor Calibration Tool", 249.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQ8jhNA2qjCs5bxYE0tvRSTcu_uO2B1sZSKRw&s", ProductType.TVS_AND_DISPLAYS);
        insertProduct(db, "TV LED Backlight Kit", 49.99, "https://m.media-amazon.com/images/I/71B4RM9iriL.jpg", ProductType.TVS_AND_DISPLAYS);
        insertProduct(db, "Monitor Privacy Screen", 79.99, "https://us.targus.com/cdn/shop/products/0027620_4vu-privacy-screen-for-235-widescreen-monitors-169-706848.jpg?v=1625678313", ProductType.TVS_AND_DISPLAYS);
        insertProduct(db, "Call of Duty: Modern Warfare III", 69.99, "https://m.media-amazon.com/images/I/71nWtjjUz-L._AC_UF1000,1000_QL80_.jpg", ProductType.VIDEO_GAMES);
        insertProduct(db, "EA Sports FC 24", 59.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTws4C1Pyl0xqmMUql2fbCZC0RFkl8eRPQA2Q&s", ProductType.VIDEO_GAMES);
        insertProduct(db, "Spider-Man 2 PS5", 69.99, "https://m.media-amazon.com/images/I/81WUPcfQ9OL._AC_UF1000,1000_QL80_.jpg", ProductType.VIDEO_GAMES);
        insertProduct(db, "The Legend of Zelda: Tears", 59.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSCcGP83ZMklsXMvEoYk5k3au7iJlzDz9QMRA&s", ProductType.VIDEO_GAMES);
        insertProduct(db, "Grand Theft Auto VI", 79.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR5bff_9PYPv8dgHVkEKbj2RgHFMXijTqxNDA&s", ProductType.VIDEO_GAMES);
        insertProduct(db, "FIFA 24 Xbox", 59.99, "https://www.king-games.co.il/files/products/product21489_image1_2023-07-31_14-17-16.webp", ProductType.VIDEO_GAMES);
        insertProduct(db, "Cyberpunk 2077 Phantom", 49.99, "https://upload.wikimedia.org/wikipedia/en/d/de/Cyberpunk_2077_Phantom_Liberty_cover_art.jpg", ProductType.VIDEO_GAMES);
        insertProduct(db, "Mario Kart 8 Deluxe", 54.99, "https://www.amazon.com/-/he/Mario-Kart-Deluxe-%D7%90%D7%9E%D7%A8%D7%99%D7%A7%D7%90%D7%99%D7%AA-Nintendo-Switch/dp/B01N1037CV", ProductType.VIDEO_GAMES);
        insertProduct(db, "God of War Ragnarok", 59.99, "https://upload.wikimedia.org/wikipedia/en/e/ee/God_of_War_Ragnar%C3%B6k_cover.jpg", ProductType.VIDEO_GAMES);
        insertProduct(db, "NBA 2K24", 69.99, "https://upload.wikimedia.org/wikipedia/en/thumb/4/48/NBA_2K24_cover_art.jpg/250px-NBA_2K24_cover_art.jpg", ProductType.VIDEO_GAMES);
        insertProduct(db, "Elden Ring Shadow", 59.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTB8NuBEuxyQPvHryc549LN9A2OV6H7UO4xJg&s", ProductType.VIDEO_GAMES);
        insertProduct(db, "Starfield Premium Edition", 89.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQaIL3BkPrzyBHlUyTuh4a1Bw_TPABLwr9oOg&s", ProductType.VIDEO_GAMES);
        insertProduct(db, "Ratchet & Clank: Rift", 69.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQ5J0NWCTzkx_yi__zBQjReYu28i7v1JSTipQ&s", ProductType.VIDEO_GAMES);
        insertProduct(db, "Returnal PS5", 69.99, "https://m.media-amazon.com/images/I/71kEwiRf03L._AC_UF1000,1000_QL80_.jpg", ProductType.VIDEO_GAMES);
        insertProduct(db, "Samsung Smart Refrigerator", 1899.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSmxjLim8WDBjt6seA0AXWVK2jzSapLujqqJg&s", ProductType.HOME_APPLIANCES);
        insertProduct(db, "LG Inverter Washing Machine", 899.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTsjZhOhdrakoeECEtMjhiujKrlhSxjlKg_6Q&s", ProductType.HOME_APPLIANCES);
        insertProduct(db, "KitchenAid Stand Mixer", 429.99, "https://m.media-amazon.com/images/I/615kwOY9+3L.jpg", ProductType.HOME_APPLIANCES);
        insertProduct(db, "Dyson V15 Vacuum Cleaner", 749.99, "https://m.media-amazon.com/images/I/517RFNhcMJL.jpg", ProductType.HOME_APPLIANCES);
        insertProduct(db, "Ninja Air Fryer", 199.99, "https://m.media-amazon.com/images/I/71+8uTMDRFL.jpg", ProductType.HOME_APPLIANCES);
        insertProduct(db, "Instant Pot Pro", 149.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQgOraL3ytV5TBJGTy-Htqv5shbXOZaarqcpA&s", ProductType.HOME_APPLIANCES);
        insertProduct(db, "Breville Smart Oven", 349.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRLe6SP8X2XG29J8mw_nLknF2Z0cUAyycj1Hw&s", ProductType.HOME_APPLIANCES);
        insertProduct(db, "Philips Air Purifier", 299.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR9ChpB0jFkfqs_j_kXCKHSMmyu22w3CiHpLQ&s", ProductType.HOME_APPLIANCES);
        insertProduct(db, "Keurig K-Elite Coffee Maker", 189.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTLIPwbwfjNd9RWcJZfq9a2ysR7A9_lx7WhRg&s", ProductType.HOME_APPLIANCES);
        insertProduct(db, "Robot Vacuum S9", 999.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSSy8r2jdtntIMuFZFu6Cl7KMLitgjV_Wy89A&s", ProductType.HOME_APPLIANCES);
        insertProduct(db, "Bosch Dishwasher 800", 1099.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRcCurcc_BuZz5DEKUaJfjsLELV8YZtM5sTtQ&s", ProductType.HOME_APPLIANCES);
        insertProduct(db, "Sony WH-1000XM5 Headphones", 399.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSOux4xPx67B3OnDxjDlNlRkvE8msB70vjl7Q&s", ProductType.AUDIO_EQUIPMENT);
        insertProduct(db, "Apple AirPods Pro 2", 249.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRktbsvMfnEHG6TJVW2uH37hubZQb2SKSjrCw&s", ProductType.AUDIO_EQUIPMENT);
        insertProduct(db, "Bose QuietComfort Ultra", 429.99, "https://beingbetter-bose.co.il/cdn/shop/files/86_1000x.png?v=1749727895", ProductType.AUDIO_EQUIPMENT);
        insertProduct(db, "JBL Flip 6 Bluetooth Speaker", 129.99, "https://pcmaster.co.il/image/cache/catalog/i/fg/bi/d30f20564441ee7524f19456b26bfd5f-1000x1000.jpg", ProductType.AUDIO_EQUIPMENT);
        insertProduct(db, "Sonos Beam Soundbar", 449.99, "https://m.media-amazon.com/images/I/51kIR1gKWYL._AC_UF1000,1000_QL80_.jpg", ProductType.AUDIO_EQUIPMENT);
        insertProduct(db, "Sennheiser HD 660S", 499.99, "https://la-bama.co.il/wp-content/uploads/2024/07/Screenshot_57-2-253x298.jpg", ProductType.AUDIO_EQUIPMENT);
        insertProduct(db, "Marshall Stanmore III Speaker", 399.99, "https://img.zap.co.il/pics/8/4/3/7/53717348d.gif", ProductType.AUDIO_EQUIPMENT);
        insertProduct(db, "Audio-Technica AT-LP120X", 349.99, "https://m.media-amazon.com/images/I/61SAte9QzkL.jpg", ProductType.AUDIO_EQUIPMENT);
        insertProduct(db, "Beats Studio Pro", 349.99, "https://m.media-amazon.com/images/I/61u-OaDSfQL._AC_UF894,1000_QL80_.jpg", ProductType.AUDIO_EQUIPMENT);
        insertProduct(db, "iPhone 15 Pro Max 1TB", 1599.99, "https://img.zap.co.il/pics/0/7/9/6/79366970c.gif", ProductType.SMARTPHONES);
        insertProduct(db, "Samsung Galaxy S24 Ultra", 1299.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQKNkvqurPMB8FabyOSgTelVqy1y8HX1Mui5Q&s", ProductType.SMARTPHONES);
        insertProduct(db, "Google Pixel 8 Pro", 999.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQdyH2Om768gdygNfEci_RqXtR3Z0wx-oDVDA&s", ProductType.SMARTPHONES);
        insertProduct(db, "OnePlus 12", 899.99, "https://gadget-mobile.co.il/wp-content/uploads/2024/02/oneplus-12-600x600.jpg", ProductType.SMARTPHONES);
        insertProduct(db, "Xiaomi 14 Pro", 849.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQEDn_0HnKL5GueEeejSVvM3SwZtvA8pHqXQw&s", ProductType.SMARTPHONES);
        insertProduct(db, "Samsung Galaxy Z Fold5", 1799.99, "https://img.zap.co.il/pics/4/1/4/5/78635414c.gif", ProductType.SMARTPHONES);
        insertProduct(db, "iPhone 14 Plus", 899.99, "https://m.media-amazon.com/images/I/61KXZ+vfu3L.jpg", ProductType.SMARTPHONES);
        insertProduct(db, "Google Pixel 7a", 499.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR61FjwJCam-SxmhUQqmkg56ez-oVVLJFgYLQ&s", ProductType.SMARTPHONES);
        insertProduct(db, "Samsung Galaxy A54", 449.99, "https://m.media-amazon.com/images/I/51orKJJMfTL.jpg", ProductType.SMARTPHONES);
        insertProduct(db, "Nothing Phone (2)", 599.99, "https://www.gadgety.co.il/wp-content/themes/main/thumbs/2023/07/Nothing-Phone-2.jpg", ProductType.SMARTPHONES);
        insertProduct(db, "iPhone SE 2024", 429.99, "https://i.redd.it/iphone-se-4th-gen-2024-v0-x939lzbxtfic1.jpg?width=4000&format=pjpg&auto=webp&s=966f57f15908082e025350bc0314f0b021c1eca6", ProductType.SMARTPHONES);
        insertProduct(db, "Samsung Galaxy Z Flip5", 999.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTBrlcCse_oWXr-PepYJKa8083gPqVqOetJnw&s", ProductType.SMARTPHONES);
        insertProduct(db, "Canon EOS R5 Mirrorless", 3899.99, "https://d3m9l0v76dty0.cloudfront.net/system/photos/5240671/large/8f1981cd0e63635905fd91c6d1f468c1.jpg", ProductType.CAMERAS);
        insertProduct(db, "Sony A7 IV Camera", 2499.99, "https://m.media-amazon.com/images/I/71BaBwNek-L.jpg", ProductType.CAMERAS);
        insertProduct(db, "Nikon Z9 Mirrorless", 5499.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSDsKvz44GiTrdLgm4poJZhxCdq4-ydaoBE2w&s", ProductType.CAMERAS);
        insertProduct(db, "GoPro HERO12 Black", 399.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQ-IWL_3tLQkqZq4UYN-TAZzLMawV6KUp0wYg&s", ProductType.CAMERAS);
        insertProduct(db, "DJI Mini 4 Pro Drone", 759.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTCtuIsnCCD1fcln5vLJplbDtwfJ-wMU8dmoA&s", ProductType.CAMERAS);
        insertProduct(db, "Fujifilm X-T5", 1699.99, "https://img.zap.co.il/pics/3/3/9/0/74890933c.gif", ProductType.CAMERAS);
        insertProduct(db, "Canon 70-200mm f/2.8 Lens", 2099.99, "https://cdn.media.amplience.net/i/canon/zoom-lens-ef-70-200mm-l-is-ii-usm-fsl-w-cap_7fb75a0151624ddfadc3cec199378c96", ProductType.CAMERAS);
        insertProduct(db, "Sony 24-70mm f/2.8 Lens", 2199.99, "https://www.sony.co.il/image/9a3029fb7027dcc88601afba0d8c6bf9?fmt=pjpeg&wid=1200&hei=470&bgcolor=F1F5F9&bgc=F1F5F9", ProductType.CAMERAS);
        insertProduct(db, "Insta360 X3 Camera", 449.99, "https://cellfi.co.il/wp-content/uploads/2025/02/insta360.jpg", ProductType.CAMERAS);
        insertProduct(db, "Canon PowerShot G7 X", 749.99, "https://img.zap.co.il/pics/2/2/4/0/92820422c.gif", ProductType.CAMERAS);
        insertProduct(db, "Sony A7C II Camera", 2199.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcST3abZLTSbHyIO5RCxECoRpw33fxJvHJicgw&s", ProductType.CAMERAS);
        insertProduct(db, "ASUS ROG Rapture WiFi 6", 449.99, "https://www.payngo.co.il/cdn-cgi/image/format=auto,metadata=none,quality=90,width=700,height=700/media/catalog/product/c/b/cbb222d7-c677-42b8-8faa-gfhbr.png", ProductType.NETWORKING);
        insertProduct(db, "TP-Link Archer AXE95", 399.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTj3S444qF684K8_Tqg0F3qAPnFjZaxaHVuBQ&s", ProductType.NETWORKING);
        insertProduct(db, "Netgear Orbi WiFi 6E", 899.99, "https://www.netgear.com/uk/media/RBKE963B-NEW_tcm158-152296.webp", ProductType.NETWORKING);
        insertProduct(db, "Google Nest Wifi Pro", 399.99, "https://i.pcmag.com/imagery/reviews/04xrWbRQrmZhC7GG0tgLVlL-7.fit_lim.size_1050x.jpg", ProductType.NETWORKING);
        insertProduct(db, "Ubiquiti Dream Machine", 379.99, "https://gfx3.senetic.com/akeneo-catalog/9/b/2/3/9b23d554efd43a1beac2fa7dce01118f6f0f4a92_1033520_UDM_image4.jpg", ProductType.NETWORKING);
        insertProduct(db, "NETGEAR Nighthawk M6 Pro", 899.99, "https://ksp.co.il/shop/items/512/409979.jpg", ProductType.NETWORKING);
        insertProduct(db, "TP-Link Deco XE75", 299.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQlODZdQZMHOHoifHAyPuk_RuqLfJKkgrPraA&s", ProductType.NETWORKING);
        insertProduct(db, "ASUS ZenWiFi Pro ET12", 799.99, "https://eilat.payngo.co.il/cdn-cgi/image/format=auto,metadata=none,quality=90,width=700,height=700/media/catalog/product/t/g/tguyfhygfdr.png", ProductType.NETWORKING);
        insertProduct(db, "Network Switch 8-Port", 89.99, "https://comservice.co.il/up/gallery/70060_TL-SG108(UN)30_01_normal_15166172.jpg", ProductType.NETWORKING);
        insertProduct(db, "WiFi Extender AC1200", 79.99, "https://www.mi-il.co.il/images/site/products/a2597fbd-a8fd-4937-945e-25675913ff05.jpg", ProductType.NETWORKING);
        insertProduct(db, "Ethernet Cable Cat8", 29.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSt6jHKVyuvebYyiWgETWnKhlSOfE4hj_qLdA&s", ProductType.NETWORKING);
        insertProduct(db, "Network Patch Panel", 149.99, "https://9233480.fs1.hubspotusercontent-na1.net/hubfs/9233480/tailwind-feat-whatisapatchpanel.jpg", ProductType.NETWORKING);
        insertProduct(db, "Powerline Adapter Kit", 99.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRhEUFdkNwbx_BuK97vACjHXE9Kqr4t6LWNDw&s", ProductType.NETWORKING);
        insertProduct(db, "Network Cabinet Wall", 199.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTG9IpyyJy6NPDRuHMORYWEZPbkvcZ2Og7vog&s", ProductType.NETWORKING);
        insertProduct(db, "PoE Injector 48V", 39.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTpHbmXsKcsB79Dn9yQvWGbdstQ_T7NTjvCAw&s", ProductType.NETWORKING);
        insertProduct(db, "Network Cable Tester", 49.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTkQiwwVXd4iE_C2kZQctX4GzFLQxaKNVPdFw&s", ProductType.NETWORKING);
        insertProduct(db, "Wireless Access Point", 129.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcT_0bWIvNkvL64AN8syVoZ838RNEovpfk0Ymg&s", ProductType.NETWORKING);
        insertProduct(db, "Network Attached Storage", 599.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR4xrQibQu590kPiy9bqdXaWt8wa6a1e53clA&s", ProductType.NETWORKING);
        insertProduct(db, "Amazon Echo Show 15", 249.99, "https://m.media-amazon.com/images/I/61xQl81iYQL._UF1000,1000_QL80_.jpg", ProductType.SMART_HOME);
        insertProduct(db, "Google Nest Hub Max", 229.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRL2hc4dlUPbIlkwWNvy8ibBTvvRTg9XVW7vw&s", ProductType.SMART_HOME);
        insertProduct(db, "Philips Hue Starter Kit", 199.99, "https://www.assets.signify.com/is/image/Signify/046677563080-929002469109-Philips-Hue_W-10_5W-A19-E26-2set-US-RTP", ProductType.SMART_HOME);
        insertProduct(db, "Ring Video Doorbell Pro 2", 249.99, "https://images.ctfassets.net/a3peezndovsu/variant-31961428492377/e8d3f08c98ee484eef46c383b85cb785/variant-31961428492377.jpg", ProductType.SMART_HOME);
        insertProduct(db, "Nest Learning Thermostat", 249.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQVNC7RaDNQ6JSPYj8kLargzhWInRGhlGjBIw&s", ProductType.SMART_HOME);
        insertProduct(db, "August Smart Lock Pro", 279.99, "https://m.media-amazon.com/images/I/519AkRwE2pL.jpg", ProductType.SMART_HOME);
        insertProduct(db, "Arlo Pro 4 Security Camera", 199.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSRvg2BLNJdCfLYbeInGAx97zBny9K3SXXfAg&s", ProductType.SMART_HOME);
        insertProduct(db, "Samsung SmartThings Hub", 129.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTl152bc6-WkwKUuj5WaZhN5DQq8-RSHaOksQ&s", ProductType.SMART_HOME);
        insertProduct(db, "Wyze Cam Pan v3", 49.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcS07mM2cvm50q45yU9hjRg_faDBdeRPcpVapQ&s", ProductType.SMART_HOME);
        insertProduct(db, "Smart Plug 4-Pack", 39.99, "https://m.media-amazon.com/images/I/51zoLDBO0wL.jpg", ProductType.SMART_HOME);
        insertProduct(db, "Smart Light Bulb RGB", 24.99, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcStvjmQbF2CsJD6xeMaJy5s81EOF25CgjHPNg&s", ProductType.SMART_HOME);
    }}
