package com.engbergenterprises.transitassistant;


import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

/**
 * These utilities will be used to communicate with the network.
 */
public class TruetimeUtils {

    //final static String TRUETIME_BASE_URL = "http://realtime.portauthority.org/bustime/api/v1/getvehicles";
    final static String TRUETIME_BASE_URL = "http://realtime.portauthority.org/bustime/api/v1/getvehicles";

    final static String PARAM_QUERY = "rt";

    /*
     * The sort field. One of stars, forks, or updated.
     * Default: results are sorted by best match if no field is specified.
     */
    final static String PARAM_KEY = "key";
    final static String key = "eivdgXd5xUAHrANjdNi22iCCc";
    final static String PARAM_FORMAT = "format";
    final static String format = "json";

    public static URL buildUrl(String function, String searchParam, String searchVal, String direction) {
        Uri builtUri = Uri.parse(TRUETIME_BASE_URL).buildUpon()
                .appendPath(function)
                .appendQueryParameter(searchParam, searchVal)
                .appendQueryParameter("dir", direction)
                .appendQueryParameter(PARAM_KEY, key)
                .appendQueryParameter(PARAM_FORMAT, format)
                .build();

        URL url = null;
        try {
            url = new URL(builtUri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return url;
    }

    public static URL buildUrlOld(String routeSearch) {
        Uri builtUri = Uri.parse(TRUETIME_BASE_URL).buildUpon()
                .appendQueryParameter(PARAM_QUERY, routeSearch)
                .appendQueryParameter(PARAM_KEY, key)
                .appendQueryParameter(PARAM_FORMAT, format)
                .build();

        URL url = null;
        try {
            url = new URL(builtUri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return url;
    }

    /**
     * This method returns the entire result from the HTTP response.
     *
     * @param url The URL to fetch the HTTP response from.
     * @return The contents of the HTTP response.
     * @throws IOException Related to network and stream reading
     */
    public static String getResponseFromHttpUrl(URL url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            InputStream in = urlConnection.getInputStream();

            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\A");

            boolean hasInput = scanner.hasNext();
            if (hasInput) {
                return scanner.next();
            } else {
                return null;
            }
        } finally {
            urlConnection.disconnect();
        }
    }
}