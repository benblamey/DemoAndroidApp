package com.ml4d.ohow.activity;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
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
import com.ml4d.ohow.OHOWAPIResponseHandler;
import com.ml4d.ohow.OfficialBuild;
import com.ml4d.ohow.R;
import com.ml4d.ohow.exceptions.ApiViaHttpException;
import com.ml4d.ohow.exceptions.NoResponseAPIException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

/*
 * Interactive logic for the sign in activity.
 */
public class HomeActivity extends Activity implements LocationListener {

	// These fields are not persisted.
	private State _state;
	private String _ohowAPIError; // If the state is 'API_ERROR_RESPONSE', details of the error. 
	private boolean _subscribedToLocationUpdates;
	private GetMomentTask _getMomentTask;
	
	// These fields are persisted.
	private Location _gpsLocation;
	private Moment _moment; 
	private Date _momentTimestamp;

	private enum State {
		COULD_NOT_START_GPS,
		WAITING_FOR_FIRST_GPS_UPDATE,
		WAITING_FOR_API,
		HAVE_MOMENT,
		API_HAS_NO_MOMENTS,
		NO_API_RESPONSE, 
		API_ERROR_RESPONSE, 
		API_GARBAGE_RESPONSE };
	
	/** 
	 * A hint for the GPS location update interval, in milliseconds.
	 */
	private static final int _gpsSuggestedUpdateIntervalMS = 5000;

	/**
	 * The minimum distance interval for update, in metres.
	 */
	private static final int _gpsSuggestedUpdateDistanceMetres = 1;
	
	/**
	 * The minimum interval in seconds for fetching a new moment from OHOW.
	 */
	private static final int _minimumFetchMomentIntervalSeconds = 19;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.show_moment_activity);

		if (null != savedInstanceState) {
			_gpsLocation = savedInstanceState.getParcelable("_gpsLocation");
			_momentTimestamp = (Date)savedInstanceState.getSerializable("_momentTimestamp");
			_moment = (Moment)savedInstanceState.getSerializable("_moment");
		}
		
		startSignInActivityIfNotSignedIn();
		ensureSubscribedToGpsUpdates();
		getMomentIfAppropriate();
		showState();
	}
	
	private void startSignInActivityIfNotSignedIn() {
		if (!CredentialStore.getInstance().getHaveVerifiedCredentials()) {
			// Start the sign in activity.
			startActivity(new Intent(this, SignInActivity.class));
		}
	}

	private void signOutButtonClicked() {
		// Clear the saved credentials.
		CredentialStore.getInstance().clear();
		
		// Start the sign in activity.
		startActivity(new Intent(this, SignInActivity.class));
	}
	
	private void captureButtonClicked() {	
		// Start the sign in activity.
		startActivity(new Intent(this, CaptureTextPhotoActivity.class));
	}

	@Override
	protected void onStart() {
		super.onStart();
		// The activity is about to become visible.
		startSignInActivityIfNotSignedIn();
		ensureSubscribedToGpsUpdates();
		showState();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// The activity has become visible (it is now "resumed").
		startSignInActivityIfNotSignedIn();
		ensureSubscribedToGpsUpdates();
		showState();
	}

	@Override
	protected void onPause() {
		super.onPause();
		// Another activity is taking focus (this activity is about to be "paused").
		ensureNotSubscribedToGpsUpdates();
	}

	@Override
	protected void onStop() {
		super.onStop();
		// The activity is no longer visible (it is now "stopped").
		ensureNotSubscribedToGpsUpdates();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// The activity is about to be destroyed.
		ensureNotSubscribedToGpsUpdates();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		if (null != outState) {
			outState.putParcelable("_gpsLocation", _gpsLocation);
			outState.putSerializable("_momentTimestamp", _momentTimestamp);
			outState.putSerializable("_moment", _moment);
		}
	}
	
	// 'LocationListener' interface members.

	@Override
	public void onLocationChanged(Location location) {
		// A GPS fix has been obtained, store it.
		_gpsLocation = location;
		getMomentIfAppropriate();
		showState();
	}

	@Override
	public void onProviderDisabled(String provider) {
		// Nothing to do.
	}

	@Override
	public void onProviderEnabled(String provider) {
		// Nothing to do.
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// Nothing to do.
	}
	
	// The Options menu.
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case R.id.menu_item_slideshow:
	    	showSlideShow();
	        return true;
	    case R.id.menu_item_local_timeline:
	    	showLocalTimeline();
	    	return true;
	    case R.id.menu_item_sign_out:
	    	this.signOutButtonClicked();
	    	return true;
	    case R.id.menu_item_capture:
	    	this.captureButtonClicked();
	    	return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}

	private void showSlideShow() {
		Intent i = new Intent(this, SlideShowActivity.class);
		// When the slide show is finished, come back to this activity.
		i.putExtra(SlideShowActivity.CALLBACK_INTENT_EXTRA_KEY, new Intent(this, HomeActivity.class));
		startActivity(i);
	}
	
	private void showLocalTimeline() {
		double longitude = 999;
		double latitude = 999;
		boolean startActivity;
		
		if (null != _gpsLocation) {
			latitude = _gpsLocation.getLatitude();
			longitude = _gpsLocation.getLongitude();
			startActivity = true;
		} else if (!OfficialBuild.getInstance().isOfficialBuild()) {
			// This is an unofficial (i.e. developer) build. Provide some dummy co-ordinates.
			longitude = -2.599488; // (Coordinates of Bristol Office.)
			latitude = 51.453956;
			Log.d("OHOW", "NO suitable fix - using dummy GPS coordinates instead (this feature is only enabled on developer builds).");
			startActivity = true;
		} else {
			Toast.makeText(this, this.getResources().getString(R.string.error_gps_no_fix), Toast.LENGTH_SHORT);
			startActivity = false;
		}
		
		if (startActivity) {
			Intent i = new Intent(this, LocalTimelineActivity.class);
			i.putExtra(LocalTimelineActivity.EXTRA_LATITUDE, latitude);
			i.putExtra(LocalTimelineActivity.EXTRA_LONGITUDE, longitude);
			startActivity(i);	
		}
	}
	
	private void getMomentIfAppropriate() {
		if ((null != _gpsLocation) && (null == _getMomentTask))
		{
			// Create a new culture-independent calendar initialised to the current date and time.
			GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.US); 
			Date now = calendar.getTime();
			
			boolean needToGetMoment;
			if ((null == _momentTimestamp)) {
				needToGetMoment = true;
			} else {
				calendar.setTime(_momentTimestamp);
				calendar.add(Calendar.SECOND, _minimumFetchMomentIntervalSeconds);
				needToGetMoment = calendar.before(now);
			}
			
			if (needToGetMoment) {
				_getMomentTask = new GetMomentTask(this, _gpsLocation.getLatitude(), _gpsLocation.getLongitude());
				_getMomentTask.execute((Void[])null);
				_state = State.WAITING_FOR_API;
				_momentTimestamp = now;
			}
		}
	}
	
	private void ensureSubscribedToGpsUpdates() {
		// Obtaining a GPS can take about 30 seconds, so we start the GPS provider as soon as we start the activity,
		// this way, a fix is hopefully available by the time the user is ready to capture. 
		
		if (!_subscribedToLocationUpdates) {
		
			// Get the most recent GPS fix (this might be null or out of date).
			LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
			
			if ((locationManager == null) || (!locationManager.isProviderEnabled("gps"))) {
				_state = State.COULD_NOT_START_GPS;
			} else {
				// Begin listening for further GPS location updates.
				locationManager.requestLocationUpdates("gps", _gpsSuggestedUpdateIntervalMS, _gpsSuggestedUpdateDistanceMetres, this, getMainLooper());
				_subscribedToLocationUpdates = true;
				_state = State.WAITING_FOR_FIRST_GPS_UPDATE;
			}
			
			_gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		}
	}
	
	/**
	 * Ensures that we are no longer watching GPS location updates.
	 */
	private void ensureNotSubscribedToGpsUpdates() {
		
		// Ensure we no longer listen to GPS location updates.
		if (_subscribedToLocationUpdates) {
			LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
			if (null != locationManager) {
				locationManager.removeUpdates(this);
			}
			_subscribedToLocationUpdates = false;
		}
	}
	
	private void showState() {
		
		String location;
		String body;
		String details;
		Resources resources = getResources();
		
		if (_state == State.COULD_NOT_START_GPS) {
			// If the user has disabled GPS - tell them to turn it back on.
			location = "";
			details = "";
			body = resources.getString(R.string.error_gps_no_gps);
		}
		else if (null != _moment) {
			// Otherwise, if we have a moment, we show it. We do this even if we failed to get a new moment.

			location = _moment.getLocationName();
			if ((null == location) || (0 == location.length())) {
				location = Double.toString(_moment.getLongitude()) + ", " + 
					Double.toString(_moment.getLatitude());
			}

			// Not that the 'default' locale means the 'local culture'.
			body = String.format(Locale.getDefault(), resources.getString(R.string.moment_body_format), _moment.getBody()); 
			
			// The 'default' locale (used by getDateTimeInstance()) is suitable for the local culture, and should not be used for persistence, etc.
			DateFormat localDateFormat = DateFormat.getDateTimeInstance(
					DateFormat.SHORT, // Date.
					DateFormat.MEDIUM); // Time.
			localDateFormat.setTimeZone(TimeZone.getDefault());
			details = String.format(Locale.getDefault(), resources.getString(R.string.moment_detail_format), _moment.getUsername(), localDateFormat.format( _moment.getDateCreatedUTC())); 
		} else {
			
			switch (_state) {
				case API_ERROR_RESPONSE:
					body = _ohowAPIError;
					break;
				case API_GARBAGE_RESPONSE:
					body = resources.getString(R.string.error_ohow_garbage_response);
					break;
				case COULD_NOT_START_GPS:
					throw new RuntimeException("This case has been handled further up (programmer mistake).");
				case NO_API_RESPONSE:
					body = resources.getString(R.string.comms_error);
					break;
				case API_HAS_NO_MOMENTS:
					body = resources.getString(R.string.home_no_history_here);
					break;
				case WAITING_FOR_FIRST_GPS_UPDATE:
					body = resources.getString(R.string.error_gps_no_fix);
					break;
				case WAITING_FOR_API:
					body = resources.getString(R.string.general_waiting);
					break;
				case HAVE_MOMENT:
					throw new RuntimeException("We shouldn't be in the HAVE_MOMENT state if we have no moment (programmer mistake).");
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
	private class GetMomentTask extends AsyncTask<Void, Void, HttpResponse> {
		
		private WeakReference<HomeActivity> _parent;		 
		private double _longitude;
		private double _latitude;
		private HttpGet _get;

		public GetMomentTask(HomeActivity parent, double latitude, double longitude) {
			// Use a weak-reference for the parent activity. This prevents a memory leak should the activity be destroyed.
			_parent = new WeakReference<HomeActivity>(parent);
			_latitude = latitude;
			_longitude = longitude;
 
			_get = new HttpGet(OHOWAPIResponseHandler.getBaseUrlIncludingTrailingSlash(false) + "moment_location_recent_search.php"
					+ "?" + "latitude=" + Double.toString(_latitude)
					+ "&" + "longitude=" + Double.toString(_longitude)
					+ "&" + "max_results=1"
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
			
			HomeActivity parent = _parent.get();
			
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
								Object resultItem = resultArray.get(0);
								
								if (resultItem instanceof JSONObject) {
									JSONObject resultItemObject = (JSONObject)resultItem;
									_moment = new Moment(resultItemObject);
									error = State.HAVE_MOMENT;
								} else {
									Log.d("OHOW", "Result array 1st item not an object..");
									error = State.API_GARBAGE_RESPONSE;
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