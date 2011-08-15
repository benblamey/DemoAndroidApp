package com.ml4d.ohow.activity;

import java.io.*;
import java.util.UUID;

import com.ml4d.core.exceptions.UnexpectedEnumValueException;
import com.ml4d.core.exceptions.UnknownClickableItemException;
import com.ml4d.ohow.APIConstants;
import com.ml4d.ohow.CapturedMoments;
import com.ml4d.ohow.CredentialStore;
import com.ml4d.ohow.ExternalStorageUtilities;
import com.ml4d.ohow.OfficialBuild;
import com.ml4d.ohow.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/*
 * Interactive logic for the 'capturetextphoto' activity.
 */
public class CaptureTextPhoto extends Activity implements OnClickListener, DialogInterface.OnClickListener, LocationListener {

	/**
	 * With the activity lifecycle an an asynchronous HTTP request to handle,
	 * this class is designed as a state machine at its core.
	 * http://en.wikipedia.org/wiki/State_machine
	 */
	private enum State {
		DATA_ENTRY, FAILED_ALLOW_ACK, FAILED_ALREADY_CAPTURED, FAILED_INVALID_CREDENTIALS, FAILED_NO_GPS_SERVICE
	}

	/**
	 * The message used only with the State.FAILED state.
	 */
	private String _errorMessage;
	private State _state;
	private DialogInterface _dialog;
	private Location _location;
	private boolean _gettingLocationUpdates;
	private File _photoFile;
	private String _captureUniqueId;
	
	private static final String _jpegExtensionWithoutDot = "jpg";
	
	/**
	 * The Mime-type that should be used for HTTP-posting photos created by this activity.
	 */
	public static final String MIME_TYPE_FOR_PHOTO = "image/jpeg";
	
	/** 
	 * A hint for the GPS location update interval, in milliseconds.
	 */
	private static final int _gpsSuggestedUpdateIntervalMS = 5000;

	/**
	 * The minimum distance interval for update, in metres.
	 */
	private static final int _gpsSuggestedUpdateDistanceMetres = 1;
	
	/**
	 * The unique ID that this class uses to identify the task of obtaining a photo.
	 */
	private static final int _takePhotoIntentUId = 0x65C45B8;
	
	/**
	 * The maximum age for a GPS fix allowed in a capture that we permit.
	 */
	private static final int _maximumGpsFixAgeMs = 3 * 60 * 1000;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.capturetextphoto);
		
		startSignInActivityIfNotSignedIn();

		findViewById(R.id.capture_text_photo_button_capture).setOnClickListener(this);
		findViewById(R.id.capture_text_photo_button_toggle_photo).setOnClickListener(this);

		if (savedInstanceState != null) {
			_state = Enum.valueOf(State.class, savedInstanceState.getString("_state"));
			_errorMessage = savedInstanceState.getString("_errorMessage");
			_photoFile = (File)savedInstanceState.getSerializable("_photoFile");
			_captureUniqueId = savedInstanceState.getString("_captureUniqueId");
			
			if (State.FAILED_INVALID_CREDENTIALS == _state) {
				// When the credentials are invalid, we immediately redirect to the sign in page.
				// We don't want to do this automatically if the user reaches the activity from history.
				_errorMessage = "";
				_state = State.DATA_ENTRY;
			} else if (State.FAILED_NO_GPS_SERVICE == _state) {
				// There was a problem with GPS the last time this activity was running - that
				// may no longer be the case.
				_errorMessage = "";
				_state = State.DATA_ENTRY;
			}

			// Because we may have different layouts for portrait and landscape
			// views, we need to manually save and restore the state of the
			// TextViews.
			restoreTextViewInstanceState(savedInstanceState, R.id.capture_text_photo_edittext_body);

			// Restore the focused view.
			View focusTarget = findViewById(savedInstanceState.getInt("focused_view"));
			if (null != focusTarget) {
				focusTarget.requestFocus();
			}
			
			ensureGettingGPSUpdates();

		} else {
			_state = State.DATA_ENTRY;
			// This is a new moment - generate a new unique ID to associate with it.
			_captureUniqueId = UUID.randomUUID().toString();
		}

		showState();
	}

	private void startSignInActivityIfNotSignedIn() {
		if (!CredentialStore.getInstance(this).getHaveVerifiedCredentials()) {
			// Start the sign in activity.
			startActivity(new Intent(this, SignIn.class));
		}
	}
	
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		tearEverythingDown();

		// Because we have different layouts for portrait and landscape views,
		// we need to manually save and restore the state of the TextViews.
		saveTextViewInstanceState(outState, R.id.capture_text_photo_edittext_body);

		// Save which view is focused.
		View focusedView = getCurrentFocus();
		if (null != focusedView) {
			outState.putInt("focused_view", focusedView.getId());
		}

		outState.putString("_state", _state.name());
		outState.putString("_errorMessage", _errorMessage);
		
		if (null != _photoFile) {
			outState.putSerializable("_photoFile", _photoFile);
		}
	}

	/**
	 * Saves the state of the specified TextView.
	 */
	private void saveTextViewInstanceState(Bundle state, int textViewId) {
		Parcelable instanceState = ((TextView) findViewById(textViewId)).onSaveInstanceState();
		state.putParcelable("textView_id_" + Integer.toString(textViewId), instanceState);
	}

	/**
	 * Restores the state of the specified TextView.
	 */
	private void restoreTextViewInstanceState(Bundle state, int textViewId) {
		Parcelable instanceState = state.getParcelable("textView_id_" + Integer.toString(textViewId));
		if (null != instanceState) {
			((TextView) findViewById(textViewId)).onRestoreInstanceState(instanceState);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		startSignInActivityIfNotSignedIn();
		// The activity is about to become visible.
		ensureGettingGPSUpdates();
		showState();
	}

	@Override
	protected void onResume() {
		super.onResume();
		startSignInActivityIfNotSignedIn();
		// The activity has become visible (it is now "resumed").
		ensureGettingGPSUpdates();
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

	/**
	 * Updates the user-interface to represent the state of this activity
	 * object.
	 */
	private void showState() {

		if (CapturedMoments.getInstance(this).hasMomentBeenCapturedRecently(_captureUniqueId)) {
			_state = State.FAILED_ALREADY_CAPTURED;
		}
		
		// If a dialog is being displayed, remove it.
		if (_dialog != null) {
			_dialog.dismiss();
			_dialog = null;
		}
		
		Resources resources = getResources();
		
		// Update the photo and the photo button.
		String togglePhotoButtonText = resources.getString(R.string.capture_text_photo_photo_button_toggle_add);
		if ((null != _photoFile) && (_photoFile.exists())) {
			Options bitmapOptions = new Options();
			bitmapOptions.inSampleSize = 4; // Open the bitmap as 1/4 its original size to save memory.
			Bitmap photoBitmap = BitmapFactory.decodeFile(_photoFile.getAbsolutePath(), bitmapOptions);
			((ImageView)findViewById(R.id.capture_text_photo_imageview_photo)).setImageBitmap(photoBitmap);
			togglePhotoButtonText = resources.getString(R.string.capture_text_photo_photo_button_toggle_remove);
		} else { 
			((ImageView)findViewById(R.id.capture_text_photo_imageview_photo)).setImageBitmap(null);
		}
		((android.widget.Button)findViewById(R.id.capture_text_photo_button_toggle_photo)).setText(togglePhotoButtonText); 

		switch (_state) {
		case DATA_ENTRY:
			// Nothing to do.
			break;
		case FAILED_ALLOW_ACK:
			// Show a 'failed' dialog.
			AlertDialog failedDialog = new AlertDialog.Builder(this).create();
			failedDialog.setTitle(resources.getString(R.string.error_dialog_title));
			failedDialog.setMessage(_errorMessage);
			failedDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", this);
			failedDialog.setCancelable(false); // Prevent the user from cancelling the dialog with the back key.
			failedDialog.show();
			_dialog = failedDialog;
			break;
		case FAILED_ALREADY_CAPTURED:
			// The user has navigated back in history - we need to prevent them re-capturing the moment.
			// We can't show a dialog, because we would need to make it non-cancellable,
			// and that prevents the user from navigating any further back using the back button.
			// Neither do we re-direct to a different activity, because this prevents the user accessing previous items in the history stack.
			// Instead, we disable all the controls on the page and show some toast.
			findViewById(R.id.capture_text_photo_edittext_body).setEnabled(false);
			findViewById(R.id.capture_text_photo_button_capture).setEnabled(false);
			findViewById(R.id.capture_text_photo_button_toggle_photo).setEnabled(false);
			
			Toast alreadyCapturedToast = Toast.makeText(this, resources.getString(R.string.capture_already_captured), Toast.LENGTH_LONG);
			alreadyCapturedToast.show();
			break;
		case FAILED_INVALID_CREDENTIALS:
			
			// Don't redirect more than once.
			_errorMessage = "";
			_state = State.DATA_ENTRY;
			
			// Clear credentials saved in the store.
			CredentialStore.getInstance(this).clear();
			
			// Go back to the sign in activity.
			startActivity(new Intent(this, SignIn.class));
			
			// Show the user some toast explaining why they have been redirected.
			Toast.makeText(this, resources.getString(R.string.sign_in_redirected_because_credentials_invalid), Toast.LENGTH_LONG).show();
			
			// We leave the text field as-is (and let it get its state persisted), then if the user goes back, they can try again with their original text.
			break;
		case FAILED_NO_GPS_SERVICE:
			// Show a dialog.
			AlertDialog noGpsfailedDialog = new AlertDialog.Builder(this).create();
			noGpsfailedDialog.setTitle(resources.getString(R.string.error_dialog_title));
			noGpsfailedDialog.setMessage(resources.getString(R.string.error_gps_no_gps));
			noGpsfailedDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", this);
			noGpsfailedDialog.setCancelable(false); // Prevent the user from cancelling the dialog with the back key.
			noGpsfailedDialog.show();
			_dialog = noGpsfailedDialog;
			break;
		default:
			throw new UnexpectedEnumValueException(_state);
		}
	}

	/**
	 * Ensure we are getting GPS location updates.
	 */
	private void ensureGettingGPSUpdates() {
		// Obtaining a GPS can take about 30 seconds, so we start the GPS provider as soon as we start the activity,
		// this way, a fix is hopefully available by the time the user is ready to capture. 
		
		if (!_gettingLocationUpdates) {
		
			// Get the most recent GPS fix (this might be null or out of date).
			LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
			
			// If GPS is not available, fail outright immediately (unless we're busy, showing another error, etc.).
			if (!locationManager.isProviderEnabled("gps")) {
				if (State.DATA_ENTRY == _state) {
					_state = State.FAILED_NO_GPS_SERVICE;
				}
			} else {
				// Begin listening for further GPS location updates.
				locationManager.requestLocationUpdates("gps", _gpsSuggestedUpdateIntervalMS, _gpsSuggestedUpdateDistanceMetres, this, getMainLooper());
				_gettingLocationUpdates = true;
			}
			
			_location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		}
	}
	
	/**
	 * Ensures that we are no longer watching GPS location updates.
	 */
	private void tearEverythingDown() {
		
		// Ensure we no longer listen to GPS location updates.
		if (_gettingLocationUpdates) {
			LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
			if (null != locationManager) {
				locationManager.removeUpdates(this);
			}
			_gettingLocationUpdates = false;
		}
	}
	
	private void captureButtonClicked() {
		
		if (CapturedMoments.getInstance(this).hasMomentBeenCapturedRecently(_captureUniqueId)) {
			throw new IllegalStateException("This entry has already been captured.");
		}
		
		Resources resources = getResources();

		CredentialStore store = CredentialStore.getInstance(this);
		if (!store.getHaveVerifiedCredentials()) {
			_errorMessage = "";
			_state = State.FAILED_INVALID_CREDENTIALS;
		} else {
			
			boolean allowCapture;
			double latitude = -1234;
			double longitude = -1234;
			double fixAccuracyMeters = 0;
			long unixTimestampMs = 0;

			if (null == _location) {
				allowCapture = false;
			} else {
				latitude = _location.getLatitude();
				longitude = _location.getLongitude();
				fixAccuracyMeters = _location.getAccuracy();
				unixTimestampMs = _location.getTime();
				// Allow capture only if the fix is 'fresh'.
				allowCapture = ((System.currentTimeMillis() - unixTimestampMs) < _maximumGpsFixAgeMs);
			}
			
			if (!OfficialBuild.getInstance(this).isOfficialBuild() && !allowCapture) {
				// This is an unofficial (i.e. developer) build. Provide some dummy co-ordinates and allow the capture to proceed.
				longitude = -2.599488; // (Coordinates of Bristol Office.)
				latitude = 51.453956;
				unixTimestampMs = System.currentTimeMillis() - 1;
				fixAccuracyMeters = 200;
				allowCapture = true;
				Log.d("OHOW", "NO suitable fix - using dummy GPS coordinates instead (this feature is only enabled on developer builds).");
			}
			
			if (!allowCapture) {
				// There was no GPS fix or else it was too old.
				_errorMessage = resources.getString(R.string.error_gps_no_fix);
				_state = State.FAILED_ALLOW_ACK;
			} else {
				// Validate the user data the same as it will be validated by the OHOW API.
				String body = ((TextView) findViewById(R.id.capture_text_photo_edittext_body)).getText().toString();
				
				// There is a problem with the UI that we seem unable to get wrapping to work, so we use allow multi-line input.
				// However the API does not allow multi-line input. Note that we remove carriage returns and line feeds before checking the body length.
				body = body.replaceAll("(\\n|\\r)", "");
				
				String validationMessage = "";
		
				if (APIConstants.captureBodyMinLength > body.length()) {
					validationMessage = resources.getString(R.string.capture_text_photo_body_text_too_short);
				} else if (APIConstants.captureBodyMaxLength < body.length()) {
					validationMessage = resources.getString(R.string.capture_text_photo_body_text_too_long);
				}
				
				if (validationMessage.length() > 0) {
					Toast.makeText(this, validationMessage, Toast.LENGTH_LONG).show();
				} else {
					
					// Start the 'CaptureLocation' activity.
					Intent pickLocationIntent = new Intent(this, CaptureLocation.class);
					pickLocationIntent.putExtra("body", body);
					pickLocationIntent.putExtra("longitude", longitude);
					pickLocationIntent.putExtra("latitude", latitude);
					pickLocationIntent.putExtra("fixAccuracyMeters", fixAccuracyMeters);
					pickLocationIntent.putExtra("captureUniqueID", _captureUniqueId);
					if (null != _photoFile) {
						pickLocationIntent.putExtra("photoFile", _photoFile);
					}
					startActivity(pickLocationIntent);
				}
			}
		}

		showState();
	}

	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.capture_text_photo_button_capture:
			captureButtonClicked();
			break;
		case R.id.capture_text_photo_button_toggle_photo:
			togglePhoto();
			break;
		default:
			throw new UnknownClickableItemException(view.getId());
		}
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (_state) {
		case FAILED_ALLOW_ACK:
			// Something was wrong, go back to data-entry to let the user try again.
			_errorMessage = "";
			_state = State.DATA_ENTRY;
			showState();
			break;
		case FAILED_NO_GPS_SERVICE:
			// Next time the activity starts, don't assume there is still a problem with GPS.
			_errorMessage = "";
			_state = State.DATA_ENTRY;
			startActivity(new Intent(this, Home.class));
			break;
		case FAILED_ALREADY_CAPTURED:
		case DATA_ENTRY:
		case FAILED_INVALID_CREDENTIALS:
			throw new IllegalStateException();
		default:
			throw new UnexpectedEnumValueException(_state);
		}
	}

	// 'LocationListener' interface members.

	@Override
	public void onLocationChanged(Location location) {
		// A GPS fix has been obtained, store it.
		_location = location;
	}

	@Override
	public void onProviderDisabled(String provider) {
		_state = State.FAILED_NO_GPS_SERVICE;
		showState();
	}

	@Override
	public void onProviderEnabled(String provider) {
		// Nothing to do.
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// Nothing to do.
	}
	
	// Photo capture.

	private void togglePhoto() {
		
		if (null != _photoFile) {
			// Delete the photo file - note that this method does not throw IOException on failure.
			_photoFile.delete(); 
			_photoFile = null;
		} else {
			try {
				_photoFile = ExternalStorageUtilities.getTempFileOnExternalStorage(_jpegExtensionWithoutDot, getResources());
				_photoFile.deleteOnExit();
			    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			    cameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(_photoFile));
			    startActivityForResult(cameraIntent, _takePhotoIntentUId);
			} catch (IOException e) {
				_state = State.FAILED_ALLOW_ACK;
				_errorMessage = e.getLocalizedMessage();
			}
		}
		
		showState();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (_takePhotoIntentUId == requestCode) {
			
			// We are being called back for the photo task.
			if (RESULT_CANCELED == resultCode) {
				// The task was canceled or something went wrong.
				// Delete the photo file - note that this method does not throw IOException on failure.
				_photoFile.delete();
				_photoFile = null;
			}
			// Otherwise, the task succeeded (we can't be sure that some manufacturers won't use a custom return code.)
			
			// If the file exists, it will be displayed, and it will be uploaded when we perform the capture.
			
			showState();
		}
	}

}
