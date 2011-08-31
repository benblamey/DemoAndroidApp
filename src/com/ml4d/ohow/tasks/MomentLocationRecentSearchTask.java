package com.ml4d.ohow.tasks;
import java.io.IOException;
import java.lang.ref.WeakReference;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpProtocolParams;
import org.json.JSONArray;
import org.json.JSONException;
import android.os.AsyncTask;
import com.ml4d.ohow.App;
import com.ml4d.ohow.ITaskFinished;
import com.ml4d.ohow.OHOWAPIResponseHandler;
import com.ml4d.ohow.exceptions.ApiViaHttpException;
import com.ml4d.ohow.exceptions.NoResponseAPIException;

/**
 * Asynchronously performs the get places HTTP request.
 */
public class MomentLocationRecentSearchTask extends AsyncTask<Void, Void, Void> {
	
	private WeakReference<ITaskFinished> _parent;		 
	private HttpGet _get;
	
	private JSONArray _result;
	private NoResponseAPIException _noResponseAPIException;
	private ApiViaHttpException _apiViaHttpException;
	private JSONException _jsonException; 
	private IOException _ioException;

	public MomentLocationRecentSearchTask(ITaskFinished parent, double latitude, double longitude, int maxResults, int radiusMeters) {
		// Use a weak-reference for the parent activity. This prevents a memory leak should the activity be destroyed.
			_parent = new WeakReference<ITaskFinished>(parent);
 
			_get = new HttpGet(OHOWAPIResponseHandler.getBaseUrlIncludingTrailingSlash(false) + "moment_location_recent_search.php"
				+ "?" + "latitude=" + Double.toString(latitude)
				+ "&" + "longitude=" + Double.toString(longitude)
				+ "&" + "max_results=" + Integer.toString(maxResults)
				+ "&" + "radius_meters=" + Integer.toString(radiusMeters));
		_get.setHeader("Accept", "application/json");
	}

	@Override
	protected Void doInBackground(Void... arg0) {
		// This is executed on a background thread.
		HttpClient client = new DefaultHttpClient();
		HttpProtocolParams.setUserAgent(client.getParams(), App.Instance.getUserAgent());
		HttpResponse response;
		
		try {
			response = client.execute(_get);
			
			// ProcessJSONResponse() appropriately handles a null result.
			Object result = OHOWAPIResponseHandler.ProcessJSONResponse(response);
			
			if (result instanceof JSONArray) {
				_result = (JSONArray)result;
			} else {
				throw new JSONException("Result was not a JSONArray");
			}
		} catch (NoResponseAPIException e) {
			_noResponseAPIException = e;
		} catch (ApiViaHttpException e) {
			_apiViaHttpException = e;
		} catch (JSONException e) {
			_jsonException = e;
		} catch (IOException e) {
			_ioException = e;
		}

		return null;
	}
	
	@Override
	protected void onPostExecute(Void unused) {
		// On the main thread.
		ITaskFinished parent = _parent.get();

		// 'parent' will be null if it has already been garbage collected.
		if (null != parent) {
			parent.CallMeBack(this);
		}
	}
	
	public JSONArray getResult() throws NoResponseAPIException, ApiViaHttpException, JSONException, IOException {
		if (null != _noResponseAPIException) {
			throw _noResponseAPIException;
		} else if (null != _apiViaHttpException) {
			throw _apiViaHttpException;
		} else if (null != _jsonException) {
			throw _jsonException;
		} else if (null != _ioException) {
			throw _ioException;
		} else {
			return _result;
		}    
	}
}