package com.engbergenterprises.transitassistant;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.maps.model.LatLng;

public class LocationService extends Service {
    public LocationService() {
    }

    private final IBinder binder = new LocationBinder();
    private LocationListener listener;
    private LocationManager locationManager;
    private static double distanceInMeters;
    private static Location lastLocation;
    public static final String PERMISSION_STRING = Manifest.permission.ACCESS_FINE_LOCATION;
    private LatLng latLng;

    @Override
    public void onCreate() {
        super.onCreate();
        latLng = new LatLng(-34, 151);
        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                latLng = new LatLng(location.getLatitude(),location.getLongitude());
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(this,PERMISSION_STRING)== PackageManager.PERMISSION_GRANTED) {
            String provider = locationManager.getBestProvider(new Criteria(), true);
            if (provider!=null) {
                locationManager.requestLocationUpdates(provider,1000,1,listener);
            }
        }
    }

    public class LocationBinder extends Binder {
        LocationService getLocater() {
            return LocationService.this;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationManager!=null && listener!=null) {
            if (ContextCompat.checkSelfPermission(this, PERMISSION_STRING)==PackageManager.PERMISSION_GRANTED) {
                locationManager.removeUpdates(listener);
            }
            locationManager = null;
            listener = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public LatLng getLocation() {
        return this.latLng;
    }

}
