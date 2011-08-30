package com.ml4d.ohow.activity;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.Locale;
import java.util.TimeZone;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpProtocolParams;
import org.json.JSONException;
import org.json.JSONObject;

import com.ml4d.core.WebImageView;
import com.ml4d.core.exceptions.UnexpectedEnumValueException;
import com.ml4d.ohow.App;
import com.ml4d.ohow.CredentialStore;
import com.ml4d.ohow.Moment;
import com.ml4d.ohow.OHOWAPIResponseHandler;
import com.ml4d.ohow.R;
import com.ml4d.ohow.exceptions.ApiViaHttpException;
import com.ml4d.ohow.exceptions.NoResponseAPIException;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

/**
 * Interactive logic for the sign in activity.
 */
public class ShowMomentActivity extends Activity {

	// These fields are not persisted.
	private GetMomentTask _getMomentTask;
	
	// These fields are persisted.
	private State _entryState;
	private String _ohowAPIError; // If the state is 'API_ERROR_RESPONSE', details of the error.
	private Moment _moment; 
	private int _momentId;

	/**
	 * The name of the intent extra that needs to be set to the ID of the moment to show when
	 * starting the activity.
	 */
	public static String EXTRA_MOMENT_ID = "moment_id"; 

	private enum State {
		WAITING_FOR_API,
		HAVE_MOMENT,
		NO_API_RESPONSE, 
		API_ERROR_RESPONSE, 
		API_GARBAGE_RESPONSE };
		
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.show_moment_activity);

		if (null != savedInstanceState) {
			// The activity is being restored from serialised state.
			_momentId = savedInstanceState.getInt("_momentId");
			_moment = (Moment)savedInstanceState.getSerializable("_moment");
			_entryState = Enum.valueOf(State.class, savedInstanceState.getString("_entryState"));
			_ohowAPIError = savedInstanceState.getString("_ohowAPIError");
		} else {
			// The activity is being started.
			Intent startingIntent = getIntent();
			_momentId = startingIntent.getIntExtra(EXTRA_MOMENT_ID, -1); // Moments always have positive IDs.
			
			if (-1 == _momentId) {
				throw new RuntimeException("This activity should only be started by the with the intent extra set specifying the moment ID.");
			}
			
			_getMomentTask = new GetMomentTask(this, _momentId);
			_getMomentTask.execute((Void[])null);

			// Fetch the photo - there might not be one, but it is faster to try immediately and risk the wasted effort than
			// wait until we have fetched the moment.
			String url = OHOWAPIResponseHandler.getBaseUrlIncludingTrailingSlash(false) + "photo.php"
				+ "?" 
				+ "id=" + Double.toString(_momentId)
				+ "&thumbnail=false"; // Get the full-sized image.
			((WebImageView)findViewById(R.id.show_moment_activity_image_view_photo)).setUrl(url);
			
			_entryState = State.WAITING_FOR_API;
			_ohowAPIError = "";
		}
		
		startSignInActivityIfNotSignedIn();
		showState();
	}
	
	private void startSignInActivityIfNotSignedIn() {
		if (!CredentialStore.getInstance().getHaveVerifiedCredentials()) {
			// Start the sign in activity.
			startActivity(new Intent(this, SignInActivity.class));
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		// The activity is about to become visible.
		startSignInActivityIfNotSignedIn();
		showState();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// The activity has become visible (it is now "resumed").
		startSignInActivityIfNotSignedIn();
		showState();
	}

	@Override
	protected void onPause() {
		super.onPause();
		ensureTaskIsStopped();
	}

	@Override
	protected void onStop() {
		super.onStop();
		ensureTaskIsStopped();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		ensureTaskIsStopped();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		if (null != outState) {
			outState.putInt("_momentId", _momentId);
			outState.putSerializable("_moment", _moment);
			outState.putString("_entryState", _entryState.name());
			outState.putString("_ohowAPIError", _ohowAPIError);
		}
	}
	
	/**
	 * Ensures the asynchronous HTTP task is stopped.
	 */
	private void ensureTaskIsStopped() {
		if (State.WAITING_FOR_API == _entryState) {
			if (this._getMomentTask != null) {
				_getMomentTask.cancel(false); // Don't interrupt the operation if
												// it has started. The results
												// are difficult to predict.
				_getMomentTask = null;
			}
			
			_ohowAPIError = getResources().getString(R.string.dialog_error_task_canceled);
			_entryState = State.API_ERROR_RESPONSE;
		}
	}
		
	private void showState() {
		Resources resources = getResources();
			
		String location;
		String body;
		String details;
		if (null != _moment) {
			// Otherwise, if we have an moment, we show it. We do this even if we failed to get a new moment.

			location = _moment.getLocationName();
			if ((null == location) || (0 == location.length())) {
				location = Double.toString(_moment.getLongitude()) + ", " + 
					Double.toString(_moment.getLatitude());
			}

			// Not that the 'default' locale means the 'local culture'. We format the string in the same way as the home activity.
			body = String.format(Locale.getDefault(), resources.getString(R.string.moment_body_format), _moment.getBody()); 
			
			// The 'default' locale (used by getDateTimeInstance()) is suitable for the local culture, and should not be used for persistence, etc.
			DateFormat localDateFormat = DateFormat.getDateTimeInstance(
					DateFormat.SHORT, // Date.
					DateFormat.MEDIUM); // Time.
			localDateFormat.setTimeZone(TimeZone.getDefault());
			// We format the string in the same way as the home activity.
			details = String.format(Locale.getDefault(), resources.getString(R.string.moment_detail_format), _moment.getUsername(), localDateFormat.format( _moment.getDateCreatedUTC())); 
		} else {
			
			switch (_entryState) {
				case API_ERROR_RESPONSE:
					body = _ohowAPIError;
					break;
				case API_GARBAGE_RESPONSE:
					body = resources.getString(R.string.error_ohow_garbage_response);
					break;
				case NO_API_RESPONSE:
					body = resources.getString(R.string.comms_error);
					break;
				case WAITING_FOR_API:
					body = resources.getString(R.string.general_waiting);
					break;
				case HAVE_MOMENT:
					throw new RuntimeException("We shouldn't be in the HAVE_MOMENT state if we have no moment (programmer mistake).");
				default:
					throw new UnexpectedEnumValueException(_entryState);
			}
			
			location = "";
			details = "";
		}
		
		TextView textViewLocation = (TextView)findViewById(R.id.show_moment_activity_text_view_capture_location);
		TextView textViewBody = (TextView)findViewById(R.id.show_moment_activity_activity_text_view_body);
		TextView textViewDetails = (TextView)findViewById(R.id.show_moment_activity_text_view_details);
		
		textViewLocation.setText(location);
		textViewBody.setText(body);
		textViewDetails.setText(details);
	}
	
	/**
	 * Asynchronously performs the get moment HTTP request.
	 */
	private class GetMomentTask extends AsyncTask<Void, Void, HttpResponse> {
		
		private WeakReference<ShowMomentActivity> _parent;
		private int _momentId;
		private HttpGet _get;

		public GetMomentTask(ShowMomentActivity parent, int momentId) {
			// Use a weak-reference for the parent activity. This prevents a memory leak should the activity be destroyed.
			_parent = new WeakReference<ShowMomentActivity>(parent);
			_momentId = momentId;

			_get = new HttpGet(OHOWAPIResponseHandler.getBaseUrlIncludingTrailingSlash(false) + "show_moment.php"
					+ "?" + "id=" + Double.toString(_momentId));
			_get.setHeader("Accept", "application/json");
		}

		@Override
		protected HttpResponse doInBackground(Void... arg0) {
			// This is executed on a background thread.
			HttpClient client = new DefaultHttpClient();
			HttpProtocolParams.setUserAgent(client.getParams(), App.Instance.getUserAgent());
			
			try {
				return client.execute(_get);
			} catch (ClientProtocolException e) {
				return null;
			} catch (IOException e) {
				return null;
			}
		}

		protected void onPostExecute(HttpResponse response) {
			// On the main thread.
			
			ShowMomentActivity parent = _parent.get();
			
			if (null != parent) {
				// 'parent' will be null if it has already been garbage collected.
				if (parent._getMomentTask == this) {
					
					State error; 
					String apiErrorMessage = "";
					
					try {
						// ProcessJSONResponse() appropriately handles a null result.
						Object result = OHOWAPIResponseHandler.ProcessJSONResponse(response, getResources());
						
						if (result instanceof JSONObject) {
							JSONObject resultObject = (JSONObject)result;
							_moment = new Moment(resultObject);
							error = State.HAVE_MOMENT;
						} else {
							Log.d("OHOW", "Result was not a JSONObject");
							error = State.API_GARBAGE_RESPONSE;
						}
					} catch (JSONException e) {
						Log.d("OHOW", e.toString());
						error = State.API_GARBAGE_RESPONSE;
						
					} catch (ApiViaHttpException e) {
						Log.d("OHOW", e.toString());
						error = State.API_ERROR_RESPONSE;
						apiErrorMessage = e.getLocalizedMessage();
						
					} catch (NoResponseAPIException e) {
						Log.d("OHOW", e.toString());
						error = State.NO_API_RESPONSE;
					}
					
					// Allow this task to be garbage-collected as it is no longer needed.
					// I think that for large requests (e.g. images) this helps bring down our memory footprint.
					parent._getMomentTask = null;
					parent._ohowAPIError = apiErrorMessage;
					parent._entryState = error; 
					parent.showState();
				}
			}
		}
	}

}
