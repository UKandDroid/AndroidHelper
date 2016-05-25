package com.helper.lib;
import android.content.Context;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// we have an Json array of item
// item[0]                       get array element / JSON Object
// item[0].object                get JSON Object at index 0
// item[0].array[]               get JSON Array at item index 0
// item[0].object.array[]        get array nested in object, if using getArray() method don't add '[]'
// item[0].object.size_width     get/set a property of JSON Object
// object1.object2.property      get nested objects and properties
// METHODS:
// get() -- Returns JSON Object/Array as JsonHelper object
// getArray()  -- returns a Json array at given the path
// getObject() -- returns a Json Object at given the path
// getRoot()   -- Returns Root JSON
// isRootArray() -- tells if root JSON is Array or Object

/* ********************************************************************************************
* CLASS -- Helper class to get nester JSON objects by string
* *********************************************************************************************
*/
public class JsonHelper {
    private JSONObject rootJsonObj;
    private JSONArray rootJsonArray;
    private String rootName;    // name of root node
    private String varName;     // Variable name
    private boolean bIsArray = false;
    private final String LOG_TAG = "JsonHelper";
    private final String VERSION = "1.1";

    public JsonHelper(){
        JSONObject temp = new JSONObject();
        try {
            temp.put("Empty", "JsonHelper root json not set");
            setRoot(temp);
        } catch (JSONException e) {e.printStackTrace(); }

    }
    public JsonHelper(JSONObject jObject){ setRoot(jObject); }
    public JsonHelper(JSONArray jArray){ setRoot(jArray); }

    // METHOD - Returns a JSON Array based on path, array string is added "[]"
    public JSONArray getArray(String strPath) {
        return (JSONArray) getJson(strPath+"[]", false);
    }
    // METHOD - Returns JSON Object
    public JSONObject getObject(String strPath) {
        return (JSONObject) getJson(strPath, false);
    }
    // METHOD - Returns boolean, false if variable not found
    public boolean getBoolean(String strPath) {
        JSONObject temp = (JSONObject) getJson(strPath, true);
        if(temp == null){
            Log.e(LOG_TAG, "Invalid path: getBoolean() " + strPath);
            return false;
        }
        return temp.optBoolean(varName);
    }
    // METHOD - Returns integer, 0 if variable not found
    public int getInteger(String strPath) {
        JSONObject temp = (JSONObject) getJson(strPath, true);
        if(temp == null){
            Log.e(LOG_TAG, "Invalid path: getInteger() " + strPath);
            return 0;
        }
        return temp.optInt(varName);
    }
    // METHOD - Returns string, "" if variable not found
    public String getString(String strPath) {
        JSONObject temp = (JSONObject) getJson(strPath, true);
        if(temp == null){
            Log.e(LOG_TAG, "Invalid path: getString() " + strPath);
            return "";
        }
        return temp.optString(varName);
    }
    // METHOD - Returns double, NaN if variable not found
    public double getDouble(String strPath) {
        JSONObject temp = (JSONObject) getJson(strPath, true);
        if(temp == null){
            Log.e(LOG_TAG, "Invalid path: getDouble() " + strPath);
            return Double.NaN;
        }
        return temp.optDouble(varName);
    }
    // METHOD - Sets a boolean
    public JsonHelper setBoolean(String strPath, boolean value) {
        JSONObject jObj = null;
        JsonHelper jTemp = new JsonHelper();
        try {
            jObj = (JSONObject)getJson(strPath, true);
            jObj.put(varName, value);
        } catch (JSONException e) { Log.e(LOG_TAG, "Invalid path: setBoolean() " + strPath); }
        jTemp.setRoot(jObj);
        return jTemp;
    }
    // METHOD - Sets a Json Object
    public JsonHelper setJson(String strPath, JSONObject value) {
        JSONObject jObj = null;
        JsonHelper jTemp = new JsonHelper();
        try {
            jObj = (JSONObject)getJson(strPath, true);
            jObj.put(varName, value);
        } catch (JSONException e) {  Log.e(LOG_TAG, "Invalid path: setJson() " + strPath); }
        jTemp.setRoot(jObj);
        return jTemp;
    }
    // METHOD - Sets a Json array
    public JsonHelper setJson(String strPath, JSONArray value) {
        JSONObject jObj = null;
        JsonHelper jTemp = new JsonHelper();
        try {
            jObj = (JSONObject) getJson(strPath, true);
            jObj.put(varName, value);
        } catch (JSONException e) {  Log.e(LOG_TAG, "Invalid path: setJson() " + strPath); }
        jTemp.setRoot(jObj);
        return jTemp;
    }
    // METHOD - Sets integer
    public JsonHelper setInteger(String strPath, int value) {
        JSONObject jObj = null;
        JsonHelper jTemp = new JsonHelper();
        try {
            jObj = (JSONObject)getJson(strPath, true);
            jObj.put(varName, value);
        } catch (JSONException e) { Log.e(LOG_TAG, "Invalid path: setInteger() " + strPath); }
        jTemp.setRoot(jObj);
        return jTemp;
    }
    // METHOD - Sets string
    public JsonHelper setString(String strPath, String value) {
        JSONObject jObj = null;
        JsonHelper jTemp = new JsonHelper();
        try {
            jObj = (JSONObject)getJson(strPath, true);
            jObj.put(varName, value);
        } catch (JSONException e) { Log.e(LOG_TAG, "Invalid path: setString() " + strPath); }
        jTemp.setRoot(jObj);
        return jTemp;
    }
    // METHOD - Sets double
    public JsonHelper setDouble(String strPath, double value) {
        JSONObject jObj = null;
        JsonHelper jTemp = new JsonHelper();
        try {
            jObj = (JSONObject)getJson(strPath, true);
            jObj.put(varName, value);
        } catch (JSONException e) { Log.e(LOG_TAG, "Invalid path: setDouble() " + strPath); }
        jTemp.setRoot(jObj);
        return jTemp;
    }
    // METHOD - Adds a json object to root array
    public boolean add(JSONObject value) {
        if(isJsonArray()){
            JSONArray jArray = (JSONArray)getRoot();
            jArray.put(value);
            return true;
        } else {
            Log.d(LOG_TAG, "JsonHelper::add() root is not a JSON Array ");
        }
        return false;
    }
    public Object getRoot(){
        return bIsArray ?  rootJsonArray : rootJsonObj;
    }

    public boolean isJsonArray(){
        return bIsArray;
    }

    public int getLength(){
        return bIsArray ? rootJsonArray.length() : 0;
    }

    public JsonHelper getIndex(int index){
        JsonHelper jhObj = new JsonHelper();
        if(bIsArray){
            try {
                jhObj.setRoot(rootJsonArray.getJSONObject(index));
            } catch (JSONException e) {
                Log.d(LOG_TAG, "JsonHelper::setString() cannot get array index: "+ index);
                e.printStackTrace();
                return null;
            }
            return jhObj;
        }
        return null;
    }

    public String toString(){
        return  bIsArray ? rootJsonArray.toString() : rootJsonObj.toString();
    }

    /**
     * ***********************************************************************************************
     * METHOD : returns JsonHelper object for the path, note to get array use '[]' at the array name end
     * ************************************************************************************************
     */

    public JsonHelper get(String strPath){
        JsonHelper jhTemp = new JsonHelper();
        Object obj = getJson(strPath, false);
        if(obj instanceof  JSONObject){
            jhTemp.setRoot((JSONObject) obj);
        } else {
            jhTemp.setRoot((JSONArray) obj);
        }
        return jhTemp;
    }
    /**
     * ***********************************************************************************************
     * METHOD : returns JSON object/array or Variable based on the string provided
     * ************************************************************************************************
     */
    private Object getJson(String arg, boolean bVariable) {
        List<Token> tokens = new ArrayList<Token>();
        String token[] = arg.split("\\.");
        int tokenLength = 0;
        // Create tokens from string
        for (int i = 0; i < token.length; i++) {
            int start = token[i].indexOf("[");
            int end = token[i].indexOf("]");
            int type = end - start;

            switch (type) {
                case 0:  // JSON object
                    tokens.add(new Token(token[i], 0, 0));
                    break;
                case 1: // JSON array object
                    token[i] = token[i].replace("[]", "");
                    tokens.add(new Token(token[i], 1, 0));
                    break;
                default: // JSON array index
                    String index = token[i].substring(start + 1, end);
                    String temp = token[i].substring(0, start);
                    tokens.add(new Token(temp, 2, Integer.parseInt(index)));
                    break;
            }
        }

        if (bVariable) { // if String is for JSON variable, don't consider last token
            tokenLength = token.length - 1;
            varName = tokens.get(tokenLength).name;
        } else {
            tokenLength = token.length;
        }

        Object jsonResult = bIsArray ? rootJsonArray : rootJsonObj;

        try {
            for (int i = 0; i < tokenLength; i++) {
                if (i == 0) {
                    switch (tokens.get(i).type) {
                        case 0:
                            if (bIsArray) {
                                jsonResult = ((JSONArray) (jsonResult));
                            } else {
                                jsonResult = ((JSONObject) (jsonResult)).getJSONObject(tokens.get(i).name);
                            }
                            break;
                        case 1:
                            if (bIsArray) {
                                jsonResult = ((JSONArray) (jsonResult));
                            } else {
                                jsonResult = ((JSONObject) (jsonResult)).getJSONArray(tokens.get(i).name);
                            }
                            break;
                        default:
                            if (bIsArray) {
                                jsonResult = ((JSONArray) (jsonResult)).getJSONObject(tokens.get(i).index);
                            } else {
                                jsonResult = ((JSONObject) (jsonResult)).getJSONArray(tokens.get(i).name).get(tokens.get(i).index);
                            }
                    }
                } else {
                    switch (tokens.get(i).type) {
                        case 0:
                            jsonResult = ((JSONObject) (jsonResult)).getJSONObject(tokens.get(i).name);
                            break;
                        case 1:
                            jsonResult = ((JSONObject) (jsonResult)).getJSONArray(tokens.get(i).name);
                            break;
                        default:
                            jsonResult = ((JSONObject) (jsonResult)).getJSONArray(tokens.get(i).name).get(tokens.get(i).index);
                    }
                }
            }
        } catch (JSONException e) {
            Log.d(LOG_TAG, "JsonHelper::getJson() error getting JSON " );
            return null;
        }
        return jsonResult;
    }

    /**
     * ***********************************************************************************************
     * METHOD : to return array index string
     * ************************************************************************************************
     */
    public static String I(int index) {
        return "[" + Integer.toString(index) + "]";
    }

    /**
     * ***********************************************************************************************
     * METHOD : set JSON root object and root name
     * ************************************************************************************************
     */
    public void setRoot(JSONObject json) {
        rootJsonObj = json;
        bIsArray = false;
        Iterator<?> keys = rootJsonObj.keys();
        if (keys.hasNext()) {
            String rootNode = (String) keys.next();
            rootName = rootNode;
        }
    }

    public void setRoot(JSONArray json) {
        rootJsonArray = json;
        bIsArray = true;
   /*     Iterator<?> keys = rootJsonObj.keys();
        if (keys.hasNext()) {
            String rootNode = (String) keys.next();
            rootName = rootNode;
            Log.d(LOG_TAG, "RootNode: " + rootNode);
        }*/
    }

    public String getRootName() {
        return rootName;
    }

    /**
     * ***********************************************************************************************
     * CLASS : to store JSON tokens that will be used to find json object
     * ************************************************************************************************
     */
    public class Token {
        public String name;  // Name of the Json object or Variable
        public int type;    // Type Json object or array
        public int index;   // index for array

        Token(String tag, int type, int index) {
            this.name = tag;
            this.type = type;
            this.index = index;
        }
    }

    /**
     * **********************************************************************************
     * METHOD -- Reads a file in asset folder and loads it as root object
     * ***********************************************************************************
     */
    public boolean loadJSONFile(Context context, String fileName) {

        boolean bResult = true;
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(context.getAssets().open(fileName)));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
            }
            br.close();
        } catch (IOException e) {
            Log.d(LOG_TAG, "JsonHelper::loadJSONFile() opening file: " + fileName);
            bResult = false;
        }

        try {
            JSONObject jObj = new JSONObject(text.toString());
            setRoot(jObj);
        } catch (JSONException e) {
            Log.d(LOG_TAG, "JsonHelper::loadJSONFile() Error creating JSONObject");
            try {
                JSONArray jArray = new JSONArray(text.toString());
                setRoot(jArray);
            } catch (JSONException e1) {
                Log.d(LOG_TAG, "JsonHelper::loadJSONFile() Error creating JSONArray");
                bResult = false;
            }
        }

        return bResult;
    }



}