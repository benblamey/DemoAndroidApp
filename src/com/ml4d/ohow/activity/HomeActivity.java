package com.ml4d.ohow.activity;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONException;

import com.ml4d.core.WebImageView;
import com.ml4d.core.exceptions.UnexpectedEnumValueException;
import com.ml4d.ohow.CredentialStore;
import com.ml4d.ohow.ITaskFinished;
import com.ml4d.ohow.MultiLocationProvider;
import com.ml4d.ohow.Moment;
import com.ml4d.ohow.OHOWAPIResponseHandler;
import com.ml4d.ohow.OfficialBuild;
import com.ml4d.ohow.R;
import com.ml4d.ohow.exceptions.ApiViaHttpException;
import com.ml4d.ohow.exceptions.NoResponseAPIException;
import com.ml4d.ohow.tasks.MomentLocationRecentSearchTask;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/*
 * Interactive logic for the sign in activity.
 */
public class HomeActivity extends Activity implements ITaskFinished, LocationListener {

	// These fields are not persisted.
	private State _state;
	private String _ohowAPIError; // If the state is 'API_ERROR_RESPONSE', details of the error. 
	private MomentLocationRecentSearchTask _getMomentTask;
	private MultiLocationProvider _multiLocationProvider;
	
	// These fields are persisted.
	private Moment _moment; 
	private Date _momentTimestamp;

	private enum State {
		LOCATION_SERVICES_DISABLED,
		WAITING_FOR_FIRST_LOCATION_UPDATE,
		WAITING_FOR_API,
		HAVE_MOMENT,
		API_HAS_NO_MOMENTS,
		NO_API_RESPONSE, 
		API_ERROR_RESPONSE, 
		API_GARBAGE_RESPONSE };
	
	/**
	 * The minimum interval in seconds for fetching a new moment from OHOW.
	 */
	private static final int _minimumFetchMomentIntervalSeconds = 19;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.show_moment_activity);

		// We don't use the previous and next buttons for this activity.
		findViewById(R.id.show_moment_activity_button_next).setVisibility(View.INVISIBLE);
		findViewById(R.id.show_moment_activity_button_previous).setVisibility(View.INVISIBLE);

		_state = State.WAITING_FOR_FIRST_LOCATION_UPDATE;
		
		if (null != savedInstanceState) {
			_momentTimestamp = (Date)savedInstanceState.getSerializable("_momentTimestamp");
			_moment = (Moment)savedInstanceState.getSerializable("_moment");
		}
		
		startSignInActivityIfNotSignedIn();
		ensureSubscribedToLocationUpdates();
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
		ensureSubscribedToLocationUpdates();
		showState();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// The activity has become visible (it is now "resumed").
		startSignInActivityIfNotSignedIn();
		ensureSubscribedToLocationUpdates();
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
		
		if (null != outState) {
			outState.putSerializable("_momentTimestamp", _momentTimestamp);
			outState.putSerializable("_moment", _moment);
		}
	}
	
	// 'LocationListener' interface members.

	@Override
	public void onLocationChanged(Location location) {
		// A GPS fix has been obtained - fetch a moment from the API.
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
	    	signOutButtonClicked();
	    	return true;
	    case R.id.menu_item_capture:
	    	captureButtonClicked();
	    	return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}

	private void showSlideShow() {
		// The first activity will finish when all the slides have been shown.
		Intent i = new Intent(this, SlideShowActivity.class);
		startActivity(i);
	}
	
	private void showLocalTimeline() {
		double longitude = 999;
		double latitude = 999;
		boolean startActivity = false;
		
		if (null != _multiLocationProvider) {
			Location location = _multiLocationProvider.getLocation();
			if (null != location) {
				latitude = location.getLatitude();
				longitude = location.getLongitude();
				startActivity = true;
			}
		}
		
		if (!startActivity && !OfficialBuild.getInstance().isOfficialBuild()) {
			// This is an unofficial (i.e. developer) build. Provide some dummy co-ordinates.
			longitude = -2.599488; // (Coordinates of Bristol Office.)
			latitude = 51.453956;
			Log.d("OHOW", "NO suitable fix - using dummy GPS coordinates instead (this feature is only enabled on developer builds).");
				startActivity = true;
		}

		if (!startActivity) {
				Toast.makeText(this, getResources().getString(R.string.error_no_location_fix), Toast.LENGTH_SHORT).show();
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
		
		if (null != _multiLocationProvider) {
			Location location = _multiLocationProvider.getLocation();
			
			if ((null != location) && (null == _getMomentTask))
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
					// Get a maximum of one moment, within 1000 metres. 
					_getMomentTask = new MomentLocationRecentSearchTask(this, location.getLatitude(), location.getLongitude(), 1, 1000);
					_getMomentTask.execute((Void[])null);
					_state = State.WAITING_FOR_API;
					_momentTimestamp = now;
				}
			}
		}
	}
	
	private void ensureSubscribedToLocationUpdates() {
		if (null == _multiLocationProvider) {
			// We are both the listener and the context.
			_multiLocationProvider = new MultiLocationProvider(this, this);
			_multiLocationProvider.start();
			if (!_multiLocationProvider.getIsEnabled()) {
				_state = State.LOCATION_SERVICES_DISABLED;
			}
		}
	}
	
	private void tearEverythingDown() {
		if (null != _multiLocationProvider) {
			_multiLocationProvider.stop();
			_multiLocationProvider = null;
		}
		
		// We don't cancel() the task, as the results are difficult to predict.
		_getMomentTask = null;
	}
	
	private void showState() {
		
		String location;
		String body;
		String details;
		Resources resources = getResources();
		
		if (_state == State.LOCATION_SERVICES_DISABLED) {
			// If the user has disabled GPS - tell them to turn it back on.
			location = "";
			details = "";
			body = resources.getString(R.string.error_no_location_services);
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

			// Get the photo associated with the moment.			
			String photoUrl;
			if (_moment.getHasPhoto()) {
				photoUrl = OHOWAPIResponseHandler.getBaseUrlIncludingTrailingSlash(false) + "photo.php"
					+ "?" 
					+ "id=" + Double.toString(_moment.getId())
					+ "&photo_size=medium"; // Get the full-sized image.
				
			} else {
				// Clear any existing image.
				photoUrl = null;
			}
			((WebImageView)findViewById(R.id.show_moment_activity_image_view_photo)).setUrl(photoUrl);
			
		} else {
			
			switch (_state) {
				case API_ERROR_RESPONSE:
					body = _ohowAPIError;
					break;
				case API_GARBAGE_RESPONSE:
					body = resources.getString(R.string.error_ohow_garbage_response);
					break;
				case LOCATION_SERVICES_DISABLED:
					throw new RuntimeException("This case has been handled further up (programmer mistake).");
				case NO_API_RESPONSE:
					body = resources.getString(R.string.comms_error);
					break;
				case API_HAS_NO_MOMENTS:
					body = resources.getString(R.string.home_no_history_here);
					break;
				case WAITING_FOR_FIRST_LOCATION_UPDATE:
					body = resources.getString(R.string.error_no_location_fix);
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
	
	@Override
	public void CallMeBack(Object sender) {

		if (sender == _getMomentTask) {
			try {
				List<Moment> resultArray = _getMomentTask.getResult();
				
				if (resultArray.size() > 0) {
					_moment = resultArray.get(0);
					_state = State.HAVE_MOMENT;
				} else {
					_moment = null;
					_state = State.API_HAS_NO_MOMENTS;
				}
			} catch (NoResponseAPIException e) {
				_state = State.NO_API_RESPONSE;
			} catch (ApiViaHttpException e) {
				_state  = State.API_ERROR_RESPONSE;
				_ohowAPIError = e.getLocalizedMessage();
			} catch (JSONException e) {
				_state = State.API_GARBAGE_RESPONSE;
			} catch (IOException e) {
				_state = State.NO_API_RESPONSE;
			}

			_getMomentTask = null;
			showState();
		}
	}
	
}
