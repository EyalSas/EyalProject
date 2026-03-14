package com.example.eyalproject.Models;

/**
 * Model class representing a physical location or store ("Place").
 * This class holds geographical coordinates, contact information, and an address.
 */
public class Place {
    private long id;         // Unique identifier for the place in the database.
    private String name;     // Name of the place (e.g., "Main Street Store").
    private double latitude; // Latitude coordinate for the place location.
    private double longitude;// Longitude coordinate for the place location.
    private String phoneNumber; // Contact phone number for the place.
    private String address;  // Physical street address of the place.

    /**
     * Constructor for the Place model.
     *
     * @param id The unique database ID.
     * @param name The name of the store or location.
     * @param latitude The geographical latitude.
     * @param longitude The geographical longitude.
     * @param phoneNumber The contact phone number.
     * @param address The physical street address.
     */
    public Place(long id, String name, double latitude, double longitude, String phoneNumber, String address) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.phoneNumber = phoneNumber;
        this.address = address;
    }

    // Getters

    /**
     * Gets the unique database ID of the place.
     * @return The ID.
     */
    public long getId() { return id; }

    /**
     * Gets the name of the place.
     * @return The name.
     */
    public String getName() { return name; }

    /**
     * Gets the latitude coordinate.
     * @return The latitude.
     */
    public double getLatitude() { return latitude; }

    /**
     * Gets the longitude coordinate.
     * @return The longitude.
     */
    public double getLongitude() { return longitude; }

    /**
     * Gets the contact phone number.
     * @return The phone number.
     */
    public String getPhoneNumber() { return phoneNumber; }

    /**
     * Gets the physical street address.
     * @return The address string.
     */
    public String getAddress() { return address; }
}