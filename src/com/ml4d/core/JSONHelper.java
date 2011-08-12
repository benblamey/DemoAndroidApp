package com.ml4d.core;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Various utility functions for JSON.
 */
public class JSONHelper {

	/**
	 * Gets a string from a JSONObject. Throws a JSONException if there is no property of that name, or it is not a string.
	 * If the value is JSON-null, then null is returned. This is unlike the JSONObject.GetString method that returns the string "null"
	 * in this case!
	 * ** IT IS RECOMMENDED NOT TO USE JSONObject.GetString() FOR THIS REASON AND USE THIS HELPER METHOD INSTEAD **
	 * @param obj
	 * @param key
	 * @return
	 * @throws JSONException
	 */
	public static String getStringOrNull(JSONObject obj, String key) throws JSONException {
		
		String result;
		
		// Throws a JSONException if the key is not found.
		Object resultObject = obj.get(key);
		//JSONObject.
		
		if (JSONObject.NULL.equals(resultObject)) {
			result = null;
		} else if (resultObject instanceof String) {
			result = (String) resultObject;
		} else {
			throw new JSONException("Not a string!");
		}
		
		return result;
	}
}
