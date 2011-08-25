package com.ml4d.ohow.activity;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpProtocolParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.ml4d.core.exceptions.ImprobableCheckedExceptionException;
import com.ml4d.core.exceptions.UnexpectedEnumValueException;
import com.ml4d.ohow.CredentialStore;
import com.ml4d.ohow.Entry;
import com.ml4d.ohow.EntryArrayAdapter;
import com.ml4d.ohow.OHOWAPIResponseHandler;
import com.ml4d.ohow.R;
import com.ml4d.ohow.exceptions.ApiViaHttpException;
import com.ml4d.ohow.exceptions.NoResponseAPIException;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;

/*
 * Interactive logic for the 'LocalTimeline' activity.
 */
public class LocalTimeline extends ListActivity implements AdapterView.OnItemClickListener {

	// These fields are persisted.
	private State _state;
	private String _ohowAPIError; // If the state is 'API_ERROR_RESPONSE', details of the error.
	private ArrayList<Entry> _entries; 
	
	// These fields are not persisted.
	private GetEntriesTask _getEntryTask;
	private Dialog _dialog;

	private enum State {
		WAITING_FOR_API,
		HAVE_ENTRY,
		API_HAS_NO_ENTRIES,
		NO_API_RESPONSE, 
		API_ERROR_RESPONSE, 
		API_GARBAGE_RESPONSE,
		FAILED_ROTATE};
	
	public static String EXTRA_LATITUDE = "latitude";
	public static String EXTRA_LONGITUDE = "longitude";

	/** Called when the activity is first created. */
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		startSignInActivityIfNotSignedIn();

		if (null != savedInstanceState) {
			_entries = (ArrayList<Entry>)savedInstanceState.getSerializable("_entries");
			_state = Enum.valueOf(State.class, savedInstanceState.getString("_state"));
			_ohowAPIError = savedInstanceState.getString("_ohowAPIError");
			
			if (State.WAITING_FOR_API == _state) {
				_state = State.FAILED_ROTATE;
			}
		} else {
			Intent startingIntent = getIntent();		
			double latitude = startingIntent.getDoubleExtra(EXTRA_LATITUDE, -1);
			double longitude = startingIntent.getDoubleExtra(EXTRA_LONGITUDE, -1);

			_getEntryTask = new GetEntriesTask(this, latitude, longitude);
			_getEntryTask.execute((Void[])null);
			_state = State.WAITING_FOR_API;
		}

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
		// Another activity is taking focus (this activity is about to be "paused").
		tearEverythingDown();
	}

	@Override
	protected void onStop() {
		super.onStop();
		// The activity is no longer visible (it is now "stopped").
		tearEverythingDown();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// The activity is about to be destroyed.
		tearEverythingDown();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putSerializable("_entries", _entries);
		outState.putString("_state", _state.name());
		outState.putString(_ohowAPIError, _ohowAPIError);
	}

	private void tearEverythingDown() {
		// We don't cancel() the task, as the results are difficult to predict.
		_getEntryTask = null;
	}

	private void showState() {
		if (null != _dialog) {
			_dialog.dismiss();
			_dialog = null;
		}
		
		Resources resources = getResources();
		
		if (null != _entries) {
			ListAdapter locationAdapter = new EntryArrayAdapter(this, 
				R.layout.local_timeline_item, 
				_entries.toArray(new Entry[_entries.size()]));
			setListAdapter(locationAdapter);
		} else if (State.WAITING_FOR_API == _state) { 
			// Show a 'waiting' dialog.
			_dialog = ProgressDialog.show(this, resources.getString(R.string.register_waiting_dialog_title),
					resources.getString(R.string.register_waiting_dialog_body), true, // Indeterminate.
					false); // Not cancellable.
		} else {
			String messsage;
			switch (_state) {
				case API_ERROR_RESPONSE:
					messsage = _ohowAPIError;
					break;
				case API_GARBAGE_RESPONSE:
					messsage = resources.getString(R.string.error_ohow_garbage_response);
					break;
				case NO_API_RESPONSE:
					messsage = resources.getString(R.string.comms_error);
					break;
				case API_HAS_NO_ENTRIES:
					messsage = resources.getString(R.string.home_no_history_here);
					break;
				case WAITING_FOR_API:
					messsage = resources.getString(R.string.general_waiting);
					break;
				case FAILED_ROTATE:
					messsage = resources.getString(R.string.dialog_error_rotate_when_busy);
					break;
				case HAVE_ENTRY:
					throw new RuntimeException("We shouldn't be in the HAVE_ENTRY state if we have no entry (programmer mistake).");
				default:
					throw new UnexpectedEnumValueException(_state);
			}
			
			// Show a 'failed' dialog.
			AlertDialog failedDialog = new AlertDialog.Builder(this).create();
			failedDialog.setTitle(resources.getString(R.string.register_error_dialog_title));
			failedDialog.setMessage(messsage);
			failedDialog.show();
			_dialog = failedDialog;
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// Do nothing.
	}
	
	/**
	 * Asynchronously performs the get places HTTP request.
	 */
	private class GetEntriesTask extends AsyncTask<Void, Void, HttpResponse> {
		
		private WeakReference<LocalTimeline> _parent;
		private String _userAgent;		 
		private double _longitude;
		private double _latitude;
		private HttpGet _get;

		public GetEntriesTask(LocalTimeline parent, double latitude, double longitude) {
			// Use a weak-reference for the parent activity. This prevents a memory leak should the activity be destroyed.
			_parent = new WeakReference<LocalTimeline>(parent);
			_latitude = latitude;
			_longitude = longitude;

			// While we are on the UI thread, build a user-agent string from
			// the package details.
			PackageInfo packageInfo;
			try {
				packageInfo = parent.getPackageManager().getPackageInfo(parent.getPackageName(), 0);
			} catch (NameNotFoundException e) {
				throw new ImprobableCheckedExceptionException(e);
			}
			_userAgent = packageInfo.packageName + " Android App, version: " + packageInfo.versionName;
			_get = new HttpGet(OHOWAPIResponseHandler.getBaseUrlIncludingTrailingSlash(parent, false) + "entry_location_recent_search.php"
					+ "?" + "latitude=" + Double.toString(_latitude)
					+ "&" + "longitude=" + Double.toString(_longitude)
					+ "&" + "max_results=30"
					+ "&" + "radius_meters=1000");
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
			
			LocalTimeline parent = _parent.get();
			
			if (null != parent) {
				// 'parent' will be null if it has already been garbage collected.
				if (parent._getEntryTask == this) {
					
					State error; 
					String apiErrorMessage = "";
					
					try {
						// ProcessJSONResponse() appropriately handles a null result.
						Object result = OHOWAPIResponseHandler.ProcessJSONResponse(response, getResources());
						
						if (result instanceof JSONArray) {
							JSONArray resultArray = (JSONArray)result;
							
							if (resultArray.length() > 0) {
								error = State.HAVE_ENTRY;
								ArrayList<Entry> entries = new ArrayList<Entry>();
								for (int i = 0; i < resultArray.length(); i++) {
									Object resultItem = resultArray.get(i);
									if (resultItem instanceof JSONObject) {
										JSONObject resultItemObject = (JSONObject)resultItem;
										entries.add(new Entry(resultItemObject));
									} else {
										Log.d("OHOW", "Result array item not an object..");
										error = State.API_GARBAGE_RESPONSE;
										break;
									}
								}
								if (State.HAVE_ENTRY == error) {
									parent._entries = entries;
								}
									
							} else {
								Log.d("OHOW", "Result array has zero entries.");
								error = State.API_HAS_NO_ENTRIES;
							}
						} else {
							Log.d("OHOW", "Result was not a JSONArray");
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