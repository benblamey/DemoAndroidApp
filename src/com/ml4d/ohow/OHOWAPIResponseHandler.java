package com.ml4d.ohow;

import java.io.IOException;
import java.lang.reflect.Field;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.ml4d.core.JSONHelper;
import com.ml4d.core.exceptions.ImprobableCheckedExceptionException;
import com.ml4d.ohow.exceptions.*;

import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.provider.Settings.Secure;
import android.util.Log;

/*
 * A utility class for handling responses from the OHOW API.
 */
public class OHOWAPIResponseHandler {

	/*
	 * Process a response from the OHOW API. Intended only where the API is
	 * expected to return JSON. (i.e. endpoints that return images are not
	 * appropriate).
	 */
	public static Object ProcessJSONResponse(HttpResponse response) throws NoResponseAPIException,
			ApiViaHttpException {

		if (null == response) {
			throw new NoResponseAPIException();
		} else {
			int statusCode = response.getStatusLine().getStatusCode();

			Log.d("OHOW", Integer.toString(statusCode));

			HttpEntity entity = response.getEntity();

			if (null == entity) {
				throw new NoResponseAPIException();
			}

			String content;
			try {
				content = EntityUtils.toString(entity);
			} catch (ParseException e) {
				throw new NoResponseAPIException();
			} catch (IOException e) {
				throw new NoResponseAPIException();
			}

			String contentString = content.toString();
			// The adb tool crashes if we give it strings that are too long - I'm not sure exactly what the limit is.
			Log.d("OHOW", contentString.substring(0, Math.min(1024, contentString.length())));
			
			JSONObject responseJson;
			Object result;
			String errorMessage;
			int exceptionCode = -1;

			try {
				responseJson = new JSONObject(content);
				exceptionCode = responseJson.getInt("exception_code");
				// There are some issues with JSONObject.GetString() - we use our helper instead.
				errorMessage = JSONHelper.getStringOrNull(responseJson, "error_message");
				result = responseJson.get("result");
			} catch (JSONException e) {
				String friendlyErrorMessage = response.getStatusLine().getReasonPhrase();
				throw new ApiViaHttpException(friendlyErrorMessage, statusCode, e);
			}

			if (200 != statusCode) {
				
				Resources resources = App.Instance.getResources();
				
				// Something went wrong with the request.

				// First, try to get a friendly message for the error. This is
				// the best option because it might have been localized
				// (the message returned by the API will always be English), and
				// it has been written to target a user of the app,
				// rather than a developer working with the API.
				String description = getFriendlyErrorDescription(statusCode, exceptionCode, resources);

				// If we don't have a friendly message for the error, use the
				// one returned by the API.
				if (0 == description.length()) {
					// Use a prefix to make the text seem a bit more friendly.
					description = resources.getString(R.string.unfriendly_error_prefix) + " " + errorMessage;
				}

				// If there wasn't a message returned by the API, it means the
				// response wasn't in JSON etc.
				// Fall back to the reason phrase in the header.
				if (0 == description.length()) {
					description = resources.getString(R.string.unfriendly_error_prefix) + " " + response.getStatusLine().getReasonPhrase();
				}

				throw new ApiViaHttpException(description, statusCode, exceptionCode);
			} else {
				return result;
			}
		}
	}
	
	private static String getFriendlyErrorDescription(int httpCode, int exceptionCode, Resources resources) {
		String friendlyMessage = "";
		String fieldName = "api_error_" + Integer.toString(httpCode) + "_" + Integer.toString(exceptionCode);

		for (Field field : R.string.class.getDeclaredFields()) {
			if (fieldName.equals(field.getName())) {
				try {
					friendlyMessage = resources.getString(field.getInt(null));
				} catch (NotFoundException e) {
					throw new ImprobableCheckedExceptionException(e);
				} catch (IllegalArgumentException e) {
					throw new ImprobableCheckedExceptionException(e);
				} catch (IllegalAccessException e) {
					throw new ImprobableCheckedExceptionException(e);
				}
			}
		}

		return friendlyMessage;
	}
	
	public static String getBaseUrlIncludingTrailingSlash(boolean secure) {

		String baseUrl;
		OfficialBuild officialBuild = OfficialBuild.getInstance();
		String dommain = "cpanel02.lhc.uk.networkeq.net/~soberfun/";
		
		if (officialBuild.useLiveOHOWApi()) {
			baseUrl = dommain + "live_v1/"; 
		} else if (officialBuild.isOfficialBuild()) {
			// All official builds that do not use the live server use the main dev server.
			baseUrl = dommain + "dev/";
		} else {
			// From experimentation, the 'ANDROID_ID' is based on which google account the phone is linked with - it is not the ID of the phone.
			String phoneID = Secure.getString(App.Instance.getContentResolver(), Secure.ANDROID_ID); 
			if ("20013fc7bad6deee".equals(phoneID)) {
				// Use Ben's dev server API stream when the phone is linked to Ben's ML4D google account.
				baseUrl = dommain + "dev_ben/";	
			} else {
				baseUrl = dommain + "dev/";
			}
		}
		
		if (secure) {
			baseUrl = "https://" + baseUrl;
		} else {
			baseUrl = "http://" + baseUrl;
		}
		
		return baseUrl;
	}

}
