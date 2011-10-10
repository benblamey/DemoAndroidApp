package com.ml4d.ohow.tasks;
import java.io.IOException;
import java.lang.ref.WeakReference;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpProtocolParams;
import org.json.JSONException;
import org.json.JSONObject;
import android.os.AsyncTask;
import com.ml4d.ohow.App;
import com.ml4d.ohow.ITaskFinished;
import com.ml4d.ohow.Moment;
import com.ml4d.ohow.OHOWAPIResponseHandler;
import com.ml4d.ohow.activity.ShowMomentActivity;
import com.ml4d.ohow.exceptions.ApiViaHttpException;
import com.ml4d.ohow.exceptions.NoResponseAPIException;
	
/**
 * Asynchronously performs the get moment HTTP request.
 */
public class ShowMomentTask extends AsyncTask<Void, Void, Void> {
	
	private WeakReference<ITaskFinished> _parent;
	private int _momentId;
	private HttpGet _get;
	
	private Moment _result;
	private NoResponseAPIException _noResponseAPIException;
	private ApiViaHttpException _apiViaHttpException;
	private JSONException _jsonException; 
	private IOException _ioException;

	public ShowMomentTask(ShowMomentActivity parent, int momentId) {
		// Use a weak-reference for the parent activity. This prevents a memory leak should the activity be destroyed.
		_parent = new WeakReference<ITaskFinished>(parent);
		_momentId = momentId;

		_get = new HttpGet(OHOWAPIResponseHandler.getBaseUrlIncludingTrailingSlash(false) + "show_moment.php"
				+ "?" + "id=" + Double.toString(_momentId));

		_get.setParams(OHOWAPIResponseHandler.getHttpParams());
		_get.setHeader("Accept", "application/json");
	}

	@Override
	protected Void doInBackground(Void... arg0) {
		// This is executed on a background thread.
		HttpClient client = new DefaultHttpClient();
		HttpProtocolParams.setUserAgent(client.getParams(), App.Instance.getUserAgent());
		
		try {
			HttpResponse response = client.execute(_get);
			
			// ProcessJSONResponse() appropriately handles a null result.
			Object result = OHOWAPIResponseHandler.ProcessJSONResponse(response);
			
			if (result instanceof JSONObject) {
				JSONObject resultObject = (JSONObject)result;
				_result = new Moment(resultObject);
			} else {
				throw new JSONException("Result was not a JSONObject");
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
	protected void onPostExecute(Void response) {
		// On the main thread.
		ITaskFinished parent = _parent.get();
		
		if (null != parent) {
				parent.callMeBack(this);
		}
	}

	public Moment getResult() throws NoResponseAPIException, ApiViaHttpException, JSONException, IOException {
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