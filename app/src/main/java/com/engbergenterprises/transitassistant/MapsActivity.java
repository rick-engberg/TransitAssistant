package com.engbergenterprises.transitassistant;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
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

import java.io.IOException;
import java.net.URL;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ProgressBar mLoadingIndicator;
    private EditText mSearchBoxEditText;
    private String busQueryResponse;
    private Marker busMarker;


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


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        LatLng eastCarsonBirminghamBridge = new LatLng(40.4284087647783,-79.97431994499044);
        busMarker = mMap.addMarker(new MarkerOptions().position(eastCarsonBirminghamBridge).title("East Carson St @ Birmingham Bridge"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(eastCarsonBirminghamBridge,15));
        mMap.animateCamera(CameraUpdateFactory.zoomIn());
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15),2000,null);
    }

    public void getBusPosition(View view) {
        String busQuery = mSearchBoxEditText.getText().toString();
        URL busSearchURL = NetworkUtils.buildUrl(busQuery);
        new BusQueryTask().execute(busSearchURL);
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
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(busPosition,15));
        mMap.animateCamera(CameraUpdateFactory.zoomIn());
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15),2000,null);
    }

    public String getMarkerTitle(String busQueryResponse) {
        try {
            JSONObject main = new JSONObject(busQueryResponse);
            JSONObject busTimeResponse = main.getJSONObject("bustime-response");
            JSONArray vehicles = busTimeResponse.getJSONArray("vehicle");
            JSONObject bus = (JSONObject) vehicles.get(0);
            String vid = bus.getString("vid");
            String rt = bus.getString("rt");
            return "Route = "+rt+", VID="+vid;
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
            return -34;
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
            return 151;
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
                busSearchResults = NetworkUtils.getResponseFromHttpUrl(searchUrl);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return busSearchResults;
        }

        @Override
        protected void onPostExecute(String busSearchResults) {
            // COMPLETED (27) As soon as the loading is complete, hide the loading indicator
            mLoadingIndicator.setVisibility(View.INVISIBLE);
            if (busSearchResults != null && !busSearchResults.equals("")) {
                busQueryResponse = busSearchResults;
                updateMap();
            } else {
                //showErrorMessage();
            }
        }
    }

}
