package com.example.eyalproject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "or.db";
    private Context context;

    // User table constants
    private static final String TABLE_USERS = "users";
    private static final String COL_USER_ID = "ID";
    private static final String COL_USERNAME = "USERNAME";
    private static final String COL_PASSWORD = "PASSWORD";
    private static final String COL_EMAIL = "EMAIL";

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
        onCreate(db);
    }

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
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + COL_USERNAME + "=? AND " + COL_PASSWORD + "=?", new String[]{username, password});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public Boolean insertUser(String username, String password, String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_USERNAME, username);
        contentValues.put(COL_PASSWORD, password);
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

    // Initialize with sample data
    public void initializeSampleProducts(SQLiteDatabase db) {

        // Add sample products
        insertProduct(db, "iPhone 15", 999.99, "https://www.orchidsandsweettea.com/wp-content/uploads/2019/05/Veggie-Pizza-2-of-5-e1691215701129.jpg", ProductType.Electronics);
        insertProduct(db, "Samsung Galaxy S24", 899.99, "https://www.orchidsandsweettea.com/wp-content/uploads/2019/05/Veggie-Pizza-2-of-5-e1691215701129.jpg", ProductType.Electronics);
        insertProduct(db, "MacBook Pro", 2499.99, "https://www.orchidsandsweettea.com/wp-content/uploads/2019/05/Veggie-Pizza-2-of-5-e1691215701129.jpg", ProductType.Electronics);
        insertProduct(db, "Wireless Headphones", 199.99, "https://www.orchidsandsweettea.com/wp-content/uploads/2019/05/Veggie-Pizza-2-of-5-e1691215701129.jpg", ProductType.Electronics);

        insertProduct(db, "T-Shirt", 29.99, "https://www.orchidsandsweettea.com/wp-content/uploads/2019/05/Veggie-Pizza-2-of-5-e1691215701129.jpg\"", ProductType.Home);
        insertProduct(db, "Jeans", 59.99, "https://www.orchidsandsweettea.com/wp-content/uploads/2019/05/Veggie-Pizza-2-of-5-e1691215701129.jpg", ProductType.Clothing);
        insertProduct(db, "Jacket", 129.99, "https://www.orchidsandsweettea.com/wp-content/uploads/2019/05/Veggie-Pizza-2-of-5-e1691215701129.jpg", ProductType.Clothing);
        insertProduct(db, "Sneakers", 89.99, "https://www.orchidsandsweettea.com/wp-content/uploads/2019/05/Veggie-Pizza-2-of-5-e1691215701129.jpg", ProductType.Sports);
    }

    public enum ProductType {
        Electronics,
        Clothing,
        Home,
        Sports
    }
}