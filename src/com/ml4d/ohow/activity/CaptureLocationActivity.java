package com.ml4d.ohow.activity;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpProtocolParams;
import com.ml4d.core.Charset2;
import com.ml4d.core.exceptions.ImprobableCheckedExceptionException;
import com.ml4d.core.exceptions.UnexpectedEnumValueException;
import com.ml4d.ohow.CapturedMoments;
import com.ml4d.ohow.OHOWAPIResponseHandler;
import com.ml4d.ohow.CredentialStore;
import com.ml4d.ohow.GooglePlacesAPI;
import com.ml4d.ohow.LocationForCapture;
import com.ml4d.ohow.LocationForCaptureArrayAdapter;
import com.ml4d.ohow.R;
import com.ml4d.ohow.exceptions.ApiViaHttpException;
import com.ml4d.ohow.exceptions.NoResponseAPIException;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

/*
 * Note regarding screen layout:
 * "ListActivity has a default layout that consists of a single, full-screen list in the center of the screen.
 * However, if you desire, you can customize the screen layout by setting your own view layout with setContentView() in onCreate(). 
 * To do this, your own view MUST contain a ListView object with the id "@android:id/list" (or list if it's in code)"
 * 
 * http://developer.android.com/reference/android/app/ListActivity.html
 */  

/**
 * Interactive logic for the 'capturelocation' activity.
 */
public class CaptureLocationActivity extends ListActivity implements DialogInterface.OnClickListener, AdapterView.OnItemClickListener {
	
	/**
	 * With the activity lifecycle an an asynchronous HTTP request to handle,
	 * this class is designed as a state machine at its core.
	 * http://en.wikipedia.org/wiki/State_machine
	 */
	private enum State {
		WAITING_FOR_PLACES, DATA_MOMENT, WAITING_FOR_CAPTURE, SUCCESS, FAILED, FAILED_INVALID_CREDENTIALS 
	}

	/**
	 * The message used only with the State.FAILED state.
	 */
	private String _errorMessage;
	private CaptureTask _captureTask;
	private GetPlacesTask _getPlacesTask;
	private State _state;
	private DialogInterface _dialog;
	private String _body;
	private double _latitude;
	private double _longitude;
	private double _fixAccuracyMeters;
	private File _photoFile;
	private ArrayList<LocationForCapture> _locations;
	private String _captureUniqueID;

	
	/** Called when the activity is first created. */
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		ListView listView = getListView();
		listView.setTextFilterEnabled(true);
		listView.setOnItemClickListener(this);
		
		if (!CredentialStore.getInstance().getHaveVerifiedCredentials()) {
			// Start the sign in activity.
			startActivity(new Intent(this, SignInActivity.class));
		}

		if (savedInstanceState != null) {
			// The activity is being restored.
			
			_state = Enum.valueOf(State.class, savedInstanceState.getString("_state"));
			_errorMessage = savedInstanceState.getString("_errorMessage");
			
			// If we have previously successfully logged in, go back to the data-moment state.
			// Otherwise we will redirect immediately back to the home activity.
			if (State.SUCCESS == _state) {
				_state = State.DATA_MOMENT;
				_errorMessage = "";
			} else if (State.FAILED_INVALID_CREDENTIALS == _state) {
				// When the credentials are invalid, we immediately redirect to the sign in page.
				// We don't want to do this automatically if the user reaches the activity from history.
				_errorMessage = "";
				_state = State.DATA_MOMENT;
			}
			
			_locations = (ArrayList<LocationForCapture>)(savedInstanceState.getSerializable("_locations"));
			_body = savedInstanceState.getString("_body");
			_latitude = savedInstanceState.getDouble("_latitude");
			_longitude = savedInstanceState.getDouble("_longitude");
			_fixAccuracyMeters = savedInstanceState.getDouble("_fixAccuracyMeters");
			_photoFile = (File)(savedInstanceState.getSerializable("_photoFile"));
			_captureUniqueID = savedInstanceState.getString("_captureUniqueID");

		} else {
			// The activity is being started.
			
			// Note that the 'savedInstanceState' bundle is not the 'extras' from the intent that launched the activity.
			// They are available in 'onNewIntent'.
			// Get some details from the previous capture step. We assume they are valid, the API will check for us.
			Intent startingIntent = getIntent();
			_body = startingIntent.getStringExtra("body");
			if (null == _body) {
				throw new RuntimeException("This activity should only be started by the CaptureTextPhoto activity (with the details from that step filled in as intent extras).");
			}
			_latitude = startingIntent.getDoubleExtra("latitude", 9999); // The default value will eventually be rejected by the API if it is used.
			_longitude = startingIntent.getDoubleExtra("longitude", 9999); // The default value will eventually be rejected by the API if it is used.
			_photoFile = (File)startingIntent.getSerializableExtra("photoFile");
			_fixAccuracyMeters = startingIntent.getDoubleExtra("fixAccuracyMeters", -1);
			_captureUniqueID = startingIntent.getStringExtra("captureUniqueID");
			
			// Start the Async task to retrieve the list of places.
			_state = State.WAITING_FOR_PLACES;
			_getPlacesTask = new GetPlacesTask(this, _latitude, _longitude, _fixAccuracyMeters);
			_getPlacesTask.execute((HttpPost[])null);
		}

		showState();
	}

	@Override
	protected void onNewIntent (Intent intent) {
		Log.d("OHOW", "CaptureLocation.onNewIntent called - terminating");
		throw new RuntimeException("onNewIntent called!");
	}
	
	/**
	 * Updates the user-interface to represent the state of this activity
	 * object.
	 */
	private void showState() {

		// If a dialog is being displayed, remove it.
		if (_dialog != null) {
			_dialog.dismiss();
			_dialog = null;
		}
		
		Resources resources = getResources();

		switch (_state) {
		case WAITING_FOR_PLACES:
			// Show a 'waiting' dialog.
			_dialog = ProgressDialog.show(this, resources.getString(R.string.capture_location_places_waiting_dialog_title),
					resources.getString(R.string.capture_location_places_waiting_dialog_body), true, // Indeterminate.
					false); // Not cancellable.
			break;			
		case DATA_MOMENT:
			if (null != _locations) {
				ListAdapter locationAdapter = new LocationForCaptureArrayAdapter(this, 
						R.layout.location_item, 
						_locations.toArray(new LocationForCapture[_locations.size()]), 
						resources.getString(R.string.capture_location_unlisted_place_label));
				setListAdapter(locationAdapter);
			} else {
				setListAdapter(null);
			}
			break;
		case WAITING_FOR_CAPTURE:
			// Show a 'waiting' dialog.
			_dialog = ProgressDialog.show(this, resources.getString(R.string.capture_location_capture_waiting_dialog_title),
					resources.getString(R.string.capture_location_capture_waiting_dialog_body), true, // Indeterminate.
					false); // Not cancellable.
			break;
		case SUCCESS:
			_locations = null;
			
			// Start the 'home' activity.
			// Credentials/session key has already been stored.
			startActivity(new Intent(this, HomeActivity.class));
			
			// Show the user some toast to inform them of the success.
			Toast.makeText(this, resources.getString(R.string.capture_location_waiting_dialog_success), Toast.LENGTH_LONG).show();
			
			_state = State.DATA_MOMENT;
			break;
		case FAILED:
			// Show a 'failed' dialog.
			AlertDialog failedDialog = new AlertDialog.Builder(this).create();
			failedDialog.setTitle(resources.getString(R.string.error_dialog_title));
			failedDialog.setMessage(_errorMessage);
			failedDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", this);
			failedDialog.setCancelable(false); // Prevent the user from cancelling the dialog with the back key.
			failedDialog.show();
			_dialog = failedDialog;
			break;
		case FAILED_INVALID_CREDENTIALS:
			// Don't redirect more than once.
			_errorMessage = "";
			_state = State.DATA_MOMENT;
			
			// Clear credentials saved in the store.
			CredentialStore.getInstance().clear();
			
			// Go back to the sign in activity.
			startActivity(new Intent(this, SignInActivity.class));
			
			// Show the user some toast explaining why they have been redirected.
			Toast.makeText(this, resources.getString(R.string.sign_in_redirected_because_credentials_invalid), Toast.LENGTH_LONG).show();
			
			// We leave the location picker as-is (and let it get its state persisted), then if the user goes back, they can try again with their original info.
			break;
		default:
			throw new UnexpectedEnumValueException(_state);
		}
	}

	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		tearEverythingDown();

		// Values are allowed to be null.
		outState.putString("_state", _state.name());
		outState.putString("_errorMessage", _errorMessage);
		outState.putSerializable("_locations", _locations);
		outState.putSerializable("_photoFile", _photoFile);
		outState.putString("_body", _body);
		outState.putDouble("_latitude", _latitude);
		outState.putDouble("_longitude", _longitude);
		outState.putDouble("_fixAccuracyMeters", _fixAccuracyMeters);
		outState.putString("_captureUniqueID", _captureUniqueID);
	}

	@Override
	protected void onStart() {
		super.onStart();
		// The activity is about to become visible.
		showState();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// The activity has become visible (it is now "resumed").
		showState();
	}

	@Override
	protected void onPause() {
		super.onPause();
		// Another activity is taking focus (this activity is about to be
		// "paused").
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
	
	/**
	 * Ensures the asynchronous HTTP task is stopped.
	 */
	private void tearEverythingDown() {
				
		// Ensure the Async HTTP task is cleaned away.
		
		// Don't interrupt the operation if it has started. The results are difficult to predict.
		if (_captureTask != null) {
			_captureTask.cancel(false); 
			_captureTask = null;
		}
		
		if (State.WAITING_FOR_CAPTURE == _state) {
			_errorMessage = getResources().getString(R.string.dialog_error_task_canceled);
			_state = State.FAILED;
		}
		
		else if (State.WAITING_FOR_PLACES == _state) {
			_errorMessage = getResources().getString(R.string.dialog_error_task_canceled);
			_state = State.FAILED;
		}
	}
	
	private void captureButtonClicked(LocationForCapture location) {
		
		// The 'CaptureLocation' activity is marked in the manifest XML as not appearing
		// in history. Therefore, we should never arrive at this activity for a moment
		// that has already been captured.
		if (CapturedMoments.getInstance().hasMomentBeenCapturedRecently(_captureUniqueID)) {
			throw new IllegalStateException("This moment has already been captured.");
		}
		
		if (null == location) {
			throw new IllegalArgumentException("location cannot be null.");
		}

		CredentialStore store = CredentialStore.getInstance();
		if (!store.getHaveVerifiedCredentials()) {
			_errorMessage = "";
			_state = State.FAILED_INVALID_CREDENTIALS;
		} else {
						
			// Validate the user data the same as it will be validated by the OHOW API.
			String validationMessage = "";
			
			if (validationMessage.length() > 0) {
				Toast.makeText(this, validationMessage, Toast.LENGTH_LONG).show();
			} else {

				// The HttpClient will verify the certificate is signed by a trusted
				// source.
				HttpPost post = new HttpPost(OHOWAPIResponseHandler.getBaseUrlIncludingTrailingSlash(true) + "capture.php");
				post.setHeader("Accept", "application/json");

				// PHP doesn't seem to accept the post if we specify a character set in the 'MultipartEntity' constructor. 
				MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
				final String textMimeType = "text/plain";
				final Charset utf8 = Charset2.getUtf8();
				 
				try {
					entity.addPart(new FormBodyPart("username", new StringBody(store.getUsername(), textMimeType, utf8))); 
					entity.addPart(new FormBodyPart("password", new StringBody(store.getPassword(), textMimeType, utf8)));
					entity.addPart(new FormBodyPart("body", new StringBody(_body, textMimeType, utf8))); // The 'body' was validated by the previous activity. We don't validate it again because the API is going to validate it anyway.
					entity.addPart(new FormBodyPart("longitude", new StringBody(Double.toString(_longitude), textMimeType, utf8)));
					entity.addPart(new FormBodyPart("latitude", new StringBody(Double.toString(_latitude), textMimeType, utf8)));
					
					if (location.getIsListed()) {
						entity.addPart(new FormBodyPart("location_name", new StringBody(location.getLocationName(), textMimeType, utf8)));
						entity.addPart(new FormBodyPart("google_location_stable_ref", new StringBody(location.getGoogleLocationStableRef(), textMimeType, utf8)));
						entity.addPart(new FormBodyPart("google_location_retrieval_ref", new StringBody(location.getGoogleLocationRetrievalRef(), textMimeType, utf8)));
					}
					
				} catch (UnsupportedEncodingException e) {
					throw new ImprobableCheckedExceptionException(e);
				}
				
				if (null != _photoFile) {
					FileBody photoFilePart= new FileBody(_photoFile, _photoFile.getAbsolutePath(), CaptureTextPhotoActivity.MIME_TYPE_FOR_PHOTO, Charset2.getUtf8().name());
					entity.addPart("photo", photoFilePart);
				}
				post.setEntity(entity);
				
				_state = State.WAITING_FOR_CAPTURE;
				_captureTask = new CaptureTask(this);
				_captureTask.execute(post);
			}
		}

		showState();
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (_state) {
		case FAILED:
			// Something was wrong, go back to data-moment to let the user try again.
			_errorMessage = "";
			_state = State.DATA_MOMENT;
			showState();
			break;
		case SUCCESS:
		case DATA_MOMENT:
		case WAITING_FOR_CAPTURE:
		case FAILED_INVALID_CREDENTIALS:
		case WAITING_FOR_PLACES:
			throw new IllegalStateException();
		default:
			throw new UnexpectedEnumValueException(_state);
		}
	}

	/**
	 * Asynchronously performs the Capture HTTP request.
	 */
	private class CaptureTask extends AsyncTask<HttpPost, Void, HttpResponse> {
		private WeakReference<CaptureLocationActivity> _parent;
		private String _userAgent;		 

		public CaptureTask(CaptureLocationActivity parent) {
			// Use a weak-reference for the parent activity. This prevents a memory leak should the activity be destroyed.
			_parent = new WeakReference<CaptureLocationActivity>(parent);

			// While we are on the UI thread, build a user-agent string from
			// the package details.
			PackageInfo packageInfo;
			try {
				packageInfo = parent.getPackageManager().getPackageInfo(parent.getPackageName(), 0);
			} catch (NameNotFoundException e1) {
				throw new ImprobableCheckedExceptionException(e1);
			}
			_userAgent = packageInfo.packageName + " Android App, version: " + packageInfo.versionName;
		}

		@Override
		protected HttpResponse doInBackground(HttpPost... arg0) {
			// This is executed on a background thread.
			HttpClient client = new DefaultHttpClient();
			HttpProtocolParams.setUserAgent(client.getParams(), _userAgent);
			try {
				return client.execute(arg0[0]);
			} catch (ClientProtocolException e) {
				return null;
			} catch (IOException e) {
				return null;
			}
		}

		protected void onPostExecute(HttpResponse response) {
			// On the main thread.
			
			CaptureLocationActivity parent = _parent.get();
			
			// 'parent' will be null if it has already been garbage collected.
			if ((null != parent) && (parent._captureTask == this)) {

				CredentialStore auth = CredentialStore.getInstance();
				
				try {
					// ProcessJSONResponse() appropriately handles a null result.
					
					// We don't actually care about the response, we just need to ensure there are no errors.
					OHOWAPIResponseHandler.ProcessJSONResponse(response, getResources());

					// To complete without error is a success.
					parent._state = State.SUCCESS;
					// We want to record the fact that this moment has been captured -
					// This means we can prevent the user from capturing it again by going back through the activity history.
					CapturedMoments.getInstance().momentHasBeenCaptured(parent._captureUniqueID);
					
				} catch (ApiViaHttpException e) {
					parent._state = State.FAILED;
					
					if ((401 == e.getHttpCode()) && (3 == e.getExceptionCode())) {
						// The password was wrong. Clear any saved credentials or session keys.
						auth.clear();
						parent._state = State.FAILED_INVALID_CREDENTIALS;	
					}
					
					parent._errorMessage = e.getLocalizedMessage();
				} catch (NoResponseAPIException e) {
					parent._state = State.FAILED;
					parent._errorMessage = parent.getResources().getString(R.string.comms_error);
				}

				// Allow this task to be garbage-collected as it is no longer needed.
				// I think that for large requests (e.g. images) this helps bring down our memory footprint.
				parent._captureTask = null;
				parent.showState();
			}
		}

	}

	/**
	 * Asynchronously performs the get places HTTP request.
	 */
	private class GetPlacesTask extends AsyncTask<HttpPost, Void, HttpResponse> {
		
		private WeakReference<CaptureLocationActivity> _parent;
		private String _userAgent;		 
		private double _longitude;
		private double _latitude;
		@SuppressWarnings("unused")
		private double _fixAccuracyMeters;

		public GetPlacesTask(CaptureLocationActivity parent, double latitude, double longitude, double fixAccuracyMeters) {
			// Use a weak-reference for the parent activity. This prevents a memory leak should the activity be destroyed.
			_parent = new WeakReference<CaptureLocationActivity>(parent);
			_latitude = latitude;
			_longitude = longitude;
			_fixAccuracyMeters = fixAccuracyMeters;

			// While we are on the UI thread, build a user-agent string from
			// the package details.
			PackageInfo packageInfo;
			try {
				packageInfo = parent.getPackageManager().getPackageInfo(parent.getPackageName(), 0);
			} catch (NameNotFoundException e1) {
				throw new ImprobableCheckedExceptionException(e1);
			}
			_userAgent = packageInfo.packageName + " Android App, version: " + packageInfo.versionName;
		}

		@Override
		protected HttpResponse doInBackground(HttpPost... arg0) {
			
			HttpGet get = new HttpGet("https://maps.googleapis.com/maps/api/place/search/json"
					+ "?" + "location=" + Double.toString(_latitude) + "," + Double.toString(_longitude)
					// It is suggested that the GPS fix accuracy is used for the radius. However, under testing the search results were poor.
					// We'll see if using a larger search radius helps things.
					// See: PT#16732227
					+ "&" + "radius=300" 
					+ "&" + "sensor=true"
					+ "&" + "types=" + Uri.encode(GooglePlacesAPI.getCapturePlaceTypes())
					+ "&" + "key=AIzaSyBXytCoZm7Q5fecpiyMVPAup4zoc2a35VM");
			get.setHeader("Accept", "application/json");
			
			// This is executed on a background thread.
			HttpClient client = new DefaultHttpClient();
			HttpProtocolParams.setUserAgent(client.getParams(), _userAgent);
			
			try {
				return client.execute(get);
			} catch (ClientProtocolException e) {
				return null;
			} catch (IOException e) {
				return null;
			}
		}

		protected void onPostExecute(HttpResponse response) {
			// On the main thread.
			
			CaptureLocationActivity parent = _parent.get();
			
			if (null != parent) {
				// 'parent' will be null if it has already been garbage collected.
				if (parent._getPlacesTask == this) {
					
					ArrayList<LocationForCapture> locations = new ArrayList<LocationForCapture>();
					
					try {
						// ProcessJSONResponse() appropriately handles a null result.
						locations.addAll(GooglePlacesAPI.ProcessJSONResponse(response, getResources()));
		
						// To complete without error is a success.
						parent._state = State.DATA_MOMENT;
						
					} catch (ApiViaHttpException e) {
						parent._locations = null;
						parent._state = State.FAILED;
						parent._errorMessage = e.getLocalizedMessage();
					} catch (NoResponseAPIException e) {
						// If there is no response (possibly because we lost the connection), don't worry
						// and leave the locations empty. There is always at least one moment anyway - see below.
						parent._locations = null;
						parent._state = State.FAILED;
						parent._errorMessage = parent.getResources().getString(R.string.capture_location_google_no_response);
					}
					
					// Add the special 'unlisted' moment to the list. 
					// This means there is an moment to select if the Google request failed, or if the location
					// is not among the results.
					locations.add(LocationForCapture.getUnlisted());
					
					// Allow this task to be garbage-collected as it is no longer needed.
					// I think that for large requests (e.g. images) this helps bring down our memory footprint.
					parent._getPlacesTask = null;
					parent._locations = locations;
					parent.showState();
				}
			}
		}

	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		captureButtonClicked(_locations.get(position));
	}

}
