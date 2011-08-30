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
import com.ml4d.core.exceptions.UnexpectedEnumValueException;
import com.ml4d.ohow.App;
import com.ml4d.ohow.CredentialStore;
import com.ml4d.ohow.Moment;
import com.ml4d.ohow.MomentArrayAdapter;
import com.ml4d.ohow.OHOWAPIResponseHandler;
import com.ml4d.ohow.R;
import com.ml4d.ohow.exceptions.ApiViaHttpException;
import com.ml4d.ohow.exceptions.NoResponseAPIException;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

/*
 * Interactive logic for the 'LocalTimeline' activity.
 */
public class LocalTimelineActivity extends ListActivity implements AdapterView.OnItemClickListener {

	// These fields are persisted.
	private State _state;
	private String _ohowAPIError; // If the state is 'API_ERROR_RESPONSE', details of the error.
	private ArrayList<Moment> _moments; 
	
	// These fields are not persisted.
	private GetMomentsTask _getMomentTask;
	private Dialog _dialog;

	private enum State {
		WAITING_FOR_API,
		HAVE_MOMENT,
		API_HAS_NO_MOMENTS,
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
		
		ListView listView = getListView();
		listView.setTextFilterEnabled(false); // We don't support text-filtering for moments.
		listView.setOnItemClickListener(this);
		
		startSignInActivityIfNotSignedIn();

		if (null != savedInstanceState) {
			_moments = (ArrayList<Moment>)savedInstanceState.getSerializable("_moments");
			_state = Enum.valueOf(State.class, savedInstanceState.getString("_state"));
			_ohowAPIError = savedInstanceState.getString("_ohowAPIError");
			
			if (State.WAITING_FOR_API == _state) {
				_state = State.FAILED_ROTATE;
			}
		} else {
			Intent startingIntent = getIntent();		
			double latitude = startingIntent.getDoubleExtra(EXTRA_LATITUDE, -1);
			double longitude = startingIntent.getDoubleExtra(EXTRA_LONGITUDE, -1);

			_getMomentTask = new GetMomentsTask(this, latitude, longitude);
			_getMomentTask.execute((Void[])null);
			_state = State.WAITING_FOR_API;
		}

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
		
		outState.putSerializable("_moments", _moments);
		outState.putString("_state", _state.name());
		outState.putString(_ohowAPIError, _ohowAPIError);
	}

	private void tearEverythingDown() {
		// We don't cancel() the task, as the results are difficult to predict.
		_getMomentTask = null;
	}

	private void showState() {
		if (null != _dialog) {
			_dialog.dismiss();
			_dialog = null;
		}
		
		Resources resources = getResources();
		
		if (null != _moments) {
			ListAdapter locationAdapter = new MomentArrayAdapter(this, 
				R.layout.local_timeline_item, 
				_moments.toArray(new Moment[_moments.size()]));
			
			setListAdapter(locationAdapter);
		} else if (State.WAITING_FOR_API == _state) { 
			// Show a 'waiting' dialog.
			_dialog = ProgressDialog.show(this, resources.getString(R.string.local_timeline_activity_label),
					resources.getString(R.string.general_waiting), true, // Indeterminate.
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
				case API_HAS_NO_MOMENTS:
					messsage = resources.getString(R.string.home_no_history_here);
					break;
				case FAILED_ROTATE:
					messsage = resources.getString(R.string.dialog_error_rotate_when_busy);
					break;
				case WAITING_FOR_API:
				case HAVE_MOMENT:
					throw new RuntimeException("Case has been handled above (programmer mistake).");
				default:
					throw new UnexpectedEnumValueException(_state);
			}
			
			// Show a 'failed' dialog.
			AlertDialog failedDialog = new AlertDialog.Builder(this).create();
			failedDialog.setTitle(resources.getString(R.string.local_timeline_activity_label));
			failedDialog.setMessage(messsage);
			failedDialog.show();
			_dialog = failedDialog;
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// Show the particular moment.
		Intent i = new Intent(this, ShowMomentActivity.class);
		i.putExtra(ShowMomentActivity.EXTRA_MOMENT_ID, _moments.get(position).getId());
		startActivity(i);
	}
	
	/**
	 * Asynchronously performs the get places HTTP request.
	 */
	private class GetMomentsTask extends AsyncTask<Void, Void, HttpResponse> {
		
		private WeakReference<LocalTimelineActivity> _parent;
		private double _longitude;
		private double _latitude;
		private HttpGet _get;

		public GetMomentsTask(LocalTimelineActivity parent, double latitude, double longitude) {
			// Use a weak-reference for the parent activity. This prevents a memory leak should the activity be destroyed.
			_parent = new WeakReference<LocalTimelineActivity>(parent);
			_latitude = latitude;
			_longitude = longitude;

			_get = new HttpGet(OHOWAPIResponseHandler.getBaseUrlIncludingTrailingSlash(false) + "moment_location_recent_search.php"
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
			
			LocalTimelineActivity parent = _parent.get();
			
			if (null != parent) {
				// 'parent' will be null if it has already been garbage collected.
				if (parent._getMomentTask == this) {
					
					State error; 
					String apiErrorMessage = "";
					
					try {
						// ProcessJSONResponse() appropriately handles a null result.
						Object result = OHOWAPIResponseHandler.ProcessJSONResponse(response, getResources());
						
						if (result instanceof JSONArray) {
							JSONArray resultArray = (JSONArray)result;
							
							if (resultArray.length() > 0) {
								error = State.HAVE_MOMENT;
								ArrayList<Moment> moments = new ArrayList<Moment>();
								for (int i = 0; i < resultArray.length(); i++) {
									Object resultItem = resultArray.get(i);
									if (resultItem instanceof JSONObject) {
										JSONObject resultItemObject = (JSONObject)resultItem;
										moments.add(new Moment(resultItemObject));
									} else {
										Log.d("OHOW", "Result array item not an object..");
										error = State.API_GARBAGE_RESPONSE;
										break;
									}
								}
								if (State.HAVE_MOMENT == error) {
									parent._moments = moments;
								}
									
							} else {
								Log.d("OHOW", "Result array has zero moments.");
								error = State.API_HAS_NO_MOMENTS;
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
					parent._getMomentTask = null;
					parent._ohowAPIError = apiErrorMessage;
					parent._state = error;
					 
					parent.showState();
				}
			}
		}
	}
}
