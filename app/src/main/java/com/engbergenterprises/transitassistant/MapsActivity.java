package com.engbergenterprises.transitassistant;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

import fr.arnaudguyon.xmltojsonlib.XmlToJson;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ProgressBar mLoadingIndicator;
    private EditText mSearchBoxEditText;
    private String busQueryResponse;
    private Marker busMarker;
    static boolean primed = false;
    private long updates = 0;
    private String direction;
    private LocationService locater;
    private boolean bound = false;
    private LatLng location;
    private String stopsQueryResponse;

    private final int PERMISSION_REQUEST_CODE = 698;
    private final int NOTIFICATION_ID = 423;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            LocationService.LocationBinder LocationBinder = (LocationService.LocationBinder) binder;
            locater = LocationBinder.getLocater();
            bound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mLoadingIndicator = (ProgressBar) findViewById(R.id.pb_loading_indicator);
        mSearchBoxEditText = (EditText) findViewById(R.id.et_search_box);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, LocationService.PERMISSION_STRING)!= PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{LocationService.PERMISSION_STRING},PERMISSION_REQUEST_CODE);
        } else {
            Intent intent = new Intent(this, LocationService.class);
            bindService(intent, connection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (bound) {
            unbindService(connection);
            bound = false;
        }
    }

    private void updatePositionHandler() {
        Log.d("***DEBUG***","updatePositionHandler started");
        final GoogleMap mMapUp = mMap;
        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
            getBusPosition(null);
            updateMap();
            handler.postDelayed(this, 60000);
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        LatLng eastCarsonBirminghamBridge = new LatLng(40.4284087647783,-79.97431994499044);
        busMarker = mMap.addMarker(new MarkerOptions().position(eastCarsonBirminghamBridge).title("East Carson St @ Birmingham Bridge"));
        if (bound && locater !=null) {
            location = locater.getLocation();
            busMarker = mMap.addMarker(new MarkerOptions().position(location).title("Current Location"));
            busMarker.setPosition(location);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location,15));
        } else {
            busMarker = mMap.addMarker(new MarkerOptions().position(eastCarsonBirminghamBridge).title("East Carson St @ Birmingham Bridge"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(eastCarsonBirminghamBridge,15));
        }
        mMap.animateCamera(CameraUpdateFactory.zoomIn());
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15),2000,null);
    }

    public void getClosestStop(View view) {
        JSONObject currentStop;
        JSONObject bestStop;
        String route = mSearchBoxEditText.getText().toString();
        URL stopsURL = TruetimeUtils.buildUrl("getstops", "rt", route, direction);
        new ClosestStopTask().execute(stopsURL);
        try {
            JSONObject bustimeResponse = new JSONObject(stopsQueryResponse);
            Iterator<String> iter = bustimeResponse.keys();
            while (iter.hasNext()) {
                currentStop = new JSONObject(iter.next());
                // Need to convert truetime XML response to JSON
                Log.d("*** STOP ***",(String)currentStop.get("stopnm"));
            }
        } catch (Exception e) {
            Log.d("*** EXCEPTION ***",e.toString());
        }
    }

    public void displayClosestStop(View view) {

    }

    public void getBusPosition(View view) {
        String route = mSearchBoxEditText.getText().toString();
        URL busSearchURL = TruetimeUtils.buildUrl("getvehicles", "rt", route, direction);
        new BusQueryTask().execute(busSearchURL);
        if (!primed) {
            updatePositionHandler();
            primed = true;
        }
    }

    public void searchInbound(View view) {
        direction = "INBOUND";
        getClosestStop(view);
        getBusPosition(view);
    }

    public void searchOutbound(View view) {
        direction = "OUTBOUND";
        getClosestStop(view);
        getBusPosition(view);
    }

    public void updateMap() {
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        LatLng busPosition = new LatLng(getLat(busQueryResponse),getLon(busQueryResponse));
        busMarker.setPosition(busPosition);
        busMarker.setTitle(getMarkerTitle(busQueryResponse));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(busPosition));
        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(busPosition,15));
        //mMap.animateCamera(CameraUpdateFactory.zoomIn());
        //mMap.animateCamera(CameraUpdateFactory.zoomTo(15),2000,null);
    }

    public String getMarkerTitle(String busQueryResponse) {
        try {
            JSONObject main = new JSONObject(busQueryResponse);
            JSONObject busTimeResponse = main.getJSONObject("bustime-response");
            JSONArray vehicles = busTimeResponse.getJSONArray("vehicle");
            JSONObject bus = (JSONObject) vehicles.get(0);
            String vid = bus.getString("vid");
            String rt = bus.getString("rt");
            return "Route = "+rt+", VID="+vid+", updates="+updates+++", "+direction;
        } catch (Exception e) {
            Log.e("Exception",e.getMessage());
            return "error";
        }

    }

    public double getLat(String busQueryResponse) {
        try {
            JSONObject main = new JSONObject(busQueryResponse);
            Log.d("main",main.toString());
            JSONObject busTimeResponse = main.getJSONObject("bustime-response");
            Log.d("busTimeResponse",busTimeResponse.toString());
            JSONArray vehicles = busTimeResponse.getJSONArray("vehicle");
            Log.d("vehicles",vehicles.toString());
            JSONObject bus = (JSONObject) vehicles.get(0);
            Log.d("bus",bus.toString());
            double lat = bus.getDouble("lat");
            return lat;
        } catch (Exception e) {
            Log.e("Exception",e.getMessage());
            return -34; // return latitude for Sydney, Australia
        }
    }

    public double getLon(String busQueryResponse) {
        try {
            JSONObject main = new JSONObject(busQueryResponse);
            Log.d("main",main.toString());
            JSONObject busTimeResponse = main.getJSONObject("bustime-response");
            Log.d("busTimeResponse",busTimeResponse.toString());
            JSONArray vehicles = busTimeResponse.getJSONArray("vehicle");
            Log.d("vehicles",vehicles.toString());
            JSONObject bus = (JSONObject) vehicles.get(0);
            Log.d("bus",bus.toString());
            double lon = bus.getDouble("lon");
            return lon;
        } catch (Exception e) {
            Log.e("Exception",e.getMessage());
            return 151; // return longitude for Sydney, Australia
        }
    }

    public class ClosestStopTask extends AsyncTask<URL, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mLoadingIndicator.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(URL... params) {
            URL searchUrl = params[0];
            String stopSearchResults = null;
            try {
                stopSearchResults = TruetimeUtils.getResponseFromHttpUrl(searchUrl);
                location = locater.getLocation();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return stopSearchResults;
        }

        @Override
        protected void onPostExecute(String stopSearchResults) {
            mLoadingIndicator.setVisibility(View.INVISIBLE);
            if (stopSearchResults != null && !stopSearchResults.equals("")) {
                stopsQueryResponse = stopSearchResults;
            } else {
                //showErrorMessage();
            }
        }
    }

    public class BusQueryTask extends AsyncTask<URL, Void, String> {

        // COMPLETED (26) Override onPreExecute to set the loading indicator to visible
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mLoadingIndicator.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(URL... params) {
            URL searchUrl = params[0];
            String busSearchResults = null;
            try {
                busSearchResults = TruetimeUtils.getResponseFromHttpUrl(searchUrl);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return busSearchResults;
        }

        @Override
        protected void onPostExecute(String busSearchResults) {
            mLoadingIndicator.setVisibility(View.INVISIBLE);
            if (busSearchResults != null && !busSearchResults.equals("")) {
                //busQueryResponse = busSearchResults;
                XmlToJson xmlToJson = new XmlToJson.Builder(busSearchResults).build();
                busQueryResponse = xmlToJson.toString();
                updateMap();
            } else {
                //showErrorMessage();
            }
        }
    }

}
