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
import com.ml4d.core.exceptions.ImprobableCheckedExceptionException;
import com.ml4d.core.exceptions.UnexpectedEnumValueException;
import com.ml4d.ohow.CredentialStore;
import com.ml4d.ohow.Entry;
import com.ml4d.ohow.OHOWAPIResponseHandler;
import com.ml4d.ohow.R;
import com.ml4d.ohow.exceptions.ApiViaHttpException;
import com.ml4d.ohow.exceptions.NoResponseAPIException;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

/*
 * Interactive logic for the sign in activity.
 */
public class ShowMomentActivity extends Activity {

	// These fields are not persisted.
	private GetEntryTask _getEntryTask;
	
	// These fields are persisted.
	private State _state;
	private String _ohowAPIError; // If the state is 'API_ERROR_RESPONSE', details of the error.
	private Entry _entry; 
	private int _entryId;

	/**
	 * The name of the intent extra that needs to be set to the ID of the entry to show when
	 * starting the activity.
	 */
	public static String EXTRA_ENTRY_ID = "entry_id"; 

	private enum State {
		WAITING_FOR_API,
		HAVE_ENTRY,
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
			_entryId = savedInstanceState.getInt("_entryId");
			_entry = (Entry)savedInstanceState.getSerializable("_entry");
			_state = Enum.valueOf(State.class, savedInstanceState.getString("_state"));
			_ohowAPIError = savedInstanceState.getString("_ohowAPIError");
		} else {
			// The activity is being started.
			Intent startingIntent = getIntent();
			_entryId = startingIntent.getIntExtra(EXTRA_ENTRY_ID, -1); // Moments always have positive IDs.
			
			if (-1 == _entryId) {
				throw new RuntimeException("This activity should only be started by the with the intent extra set specifying the entry ID.");
			}
			
			_getEntryTask = new GetEntryTask(this, _entryId);
			_getEntryTask.execute((Void[])null);
			_state = State.WAITING_FOR_API;
			_ohowAPIError = "";
		}
		
		startSignInActivityIfNotSignedIn();
		showState();
	}
	
	private void startSignInActivityIfNotSignedIn() {
		if (!CredentialStore.getInstance(this).getHaveVerifiedCredentials()) {
			// Start the sign in activity.
			startActivity(new Intent(this, SignIn.class));
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
			outState.putInt("_entryId", _entryId);
			outState.putSerializable("_entry", _entry);
			outState.putString("_state", _state.name());
			outState.putString("_ohowAPIError", _ohowAPIError);
		}
	}
	
	/**
	 * Ensures the asynchronous HTTP task is stopped.
	 */
	private void ensureTaskIsStopped() {
		if (State.WAITING_FOR_API == _state) {
			if (this._getEntryTask != null) {
				_getEntryTask.cancel(false); // Don't interrupt the operation if
												// it has started. The results
												// are difficult to predict.
				_getEntryTask = null;
			}
			
			_ohowAPIError = getResources().getString(R.string.dialog_error_task_canceled);
			_state = State.API_ERROR_RESPONSE;
		}
	}
		
	private void showState() {
		String location;
		String body;
		String details;
		Resources resources = getResources();
		
		if (null != _entry) {
			// Otherwise, if we have an entry, we show it. We do this even if we failed to get a new entry.

			location = _entry.getLocationName();
			if ((null == location) || (0 == location.length())) {
				location = Double.toString(_entry.getLongitude()) + ", " + 
					Double.toString(_entry.getLatitude());
			}

			// Not that the 'default' locale means the 'local culture'. We format the string in the same way as the home activity.
			body = String.format(Locale.getDefault(), resources.getString(R.string.home_body_format), _entry.getBody()); 
			
			// The 'default' locale (used by getDateTimeInstance()) is suitable for the local culture, and should not be used for persistence, etc.
			DateFormat localDateFormat = DateFormat.getDateTimeInstance(
					DateFormat.SHORT, // Date.
					DateFormat.MEDIUM); // Time.
			localDateFormat.setTimeZone(TimeZone.getDefault());
			// We format the string in the same way as the home activity.
			details = String.format(Locale.getDefault(), resources.getString(R.string.home_detail_format), _entry.getUsername(), localDateFormat.format( _entry.getDateCreatedUTC())); 
		} else {
			
			switch (_state) {
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
				case HAVE_ENTRY:
					throw new RuntimeException("We shouldn't be in the HAVE_ENTRY state if we have no entry (programmer mistake).");
				default:
					throw new UnexpectedEnumValueException(_state);
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
	 * Asynchronously performs the get places HTTP request.
	 */
	private class GetEntryTask extends AsyncTask<Void, Void, HttpResponse> {
		
		private WeakReference<ShowMomentActivity> _parent;
		private String _userAgent;
		private int _entryId;
		private HttpGet _get;

		public GetEntryTask(ShowMomentActivity parent, int entryId) {
			// Use a weak-reference for the parent activity. This prevents a memory leak should the activity be destroyed.
			_parent = new WeakReference<ShowMomentActivity>(parent);
			_entryId = entryId;

			// While we are on the UI thread, build a user-agent string from
			// the package details.
			PackageInfo packageInfo;
			try {
				packageInfo = parent.getPackageManager().getPackageInfo(parent.getPackageName(), 0);
			} catch (NameNotFoundException e1) {
				throw new ImprobableCheckedExceptionException(e1);
			}
			_userAgent = packageInfo.packageName + " Android App, version: " + packageInfo.versionName;
			_get = new HttpGet(OHOWAPIResponseHandler.getBaseUrlIncludingTrailingSlash(parent, false) + "show_entry.php"
					+ "?" + "id=" + Double.toString(_entryId));
			_get.setHeader("Accept", "application/json");
		}

		@Override
		protected HttpResponse doInBackground(Void... arg0) {
			// This is executed on a background thread.
			HttpClient client = new DefaultHttpClient();
			HttpProtocolParams.setUserAgent(client.getParams(), _userAgent);
			
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
				if (parent._getEntryTask == this) {
					
					State error; 
					String apiErrorMessage = "";
					
					try {
						// ProcessJSONResponse() appropriately handles a null result.
						Object result = OHOWAPIResponseHandler.ProcessJSONResponse(response, getResources());
						
						if (result instanceof JSONObject) {
							JSONObject resultObject = (JSONObject)result;
							_entry = new Entry(resultObject);
							error = State.HAVE_ENTRY;
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
					parent._getEntryTask = null;
					parent._ohowAPIError = apiErrorMessage;
					parent._state = error; 
					parent.showState();
				}
			}
		}
	}
	
}