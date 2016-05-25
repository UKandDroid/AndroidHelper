package com.helper.lib;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class WebHelper {

    private Context context;
    private RequestQueue reqQueue;
    private boolean bWebAuth = false;
    private static String USERNAME = "";  // Used for web authentication
    private static String PASSWORD = "";

    // INTERFACE for web response
    public interface Response {
        public void onSuccess(Object result);
        public void onError(int httpCode, Object result);
    }

    // CONSTRUCTORS
    public WebHelper(Context vContext){ this(vContext, "", ""); }
    public WebHelper(Context vContext, String sUserName, String sPassword){
        context = vContext;
        reqQueue = Volley.newRequestQueue(context);
        if(sUserName.isEmpty() && sPassword.isEmpty()){
            setWebAuth(sUserName,sPassword);
        }
    }

    // METHOD - sets user credentials for web authentication
    public void setWebAuth(String sUserName, String sPassword){
        if(sUserName.isEmpty() && sPassword.isEmpty()){
            USERNAME = sUserName;
            PASSWORD = sPassword;
            bWebAuth = true;
        }
    }

    // METHOD -- GET request, expects response to be JSON String
    public Object GETRequest (String url, final Response response, final Map<String, String> mapParams){
        if(mapParams != null) {
            try {
                for (String key : mapParams.keySet()) {
                    url += key + "=" + URLEncoder.encode(mapParams.get(key), "utf-8") + "&";
                }
            } catch (UnsupportedEncodingException e) { Log.e("WebHelper", "Parameter Encoding Exception for URL:" + url); }
        }

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new com.android.volley.Response.Listener<String>() { @Override public void onResponse(String arg) {response.onSuccess(arg);}} ,
                new com.android.volley.Response.ErrorListener() { @Override public void onErrorResponse(VolleyError error) {
                    response.onError(error.networkResponse.statusCode, error.toString());}})
        {
            @Override public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> params = new HashMap<String, String>();
                params.put("Content-Type","application/json");
                if(bWebAuth){
                    String credentials = USERNAME + ":" + PASSWORD;
                    String encodedCredentials = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                    params.put("Authorization", "Basic " + encodedCredentials);
                }
                return params;
            }
        };
        reqQueue.add(stringRequest);
        return  stringRequest;
    }

    // METHOD -- GET request, response listener provided by the caller
    public Object GETRequest (String url, final Response response){
        return  GETRequest(url, response, null);
    }


    // METHOD -- POST request, response listener provided by the caller
    public void POSTRequest(String url, final String params, final Response apiResponse){
        StringRequest request = new StringRequest(Request.Method.POST, url,
                new com.android.volley.Response.Listener<String>() {
                    @Override public void onResponse(String response) {
                        apiResponse.onSuccess(response);
                    }},
                new com.android.volley.Response.ErrorListener() {
                    @Override  public void onErrorResponse(VolleyError error) {
                        apiResponse.onError(error.networkResponse == null ? 0 : error.networkResponse.statusCode, error);
                    }})
        {
            @Override public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> params = new HashMap<String, String>();
                params.put("Content-Type","application/json");
                if(bWebAuth){
                    String credentials = USERNAME + ":" + PASSWORD;
                    String encodedCredentials = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                    params.put("Authorization", "Basic " + encodedCredentials);
                }
                return params;
            }

            @Override public byte[] getBody() throws AuthFailureError {
                return params.getBytes();
            }
        };

        reqQueue.add(request);
    }
}
