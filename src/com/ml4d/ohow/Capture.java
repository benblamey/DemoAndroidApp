package com.ml4d.ohow;

import java.io.*;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;

import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpProtocolParams;
import com.ml4d.ohow.exceptions.*;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/*
 * Interactive logic for the 'capture' activity.
 */
public class Capture extends Activity implements OnClickListener, DialogInterface.OnClickListener, LocationListener {

	/**
	 * With the activity lifecycle an an asynchronous HTTP request to handle,
	 * this class is designed as a state machine at its core.
	 * http://en.wikipedia.org/wiki/State_machine
	 */
	private enum State {
		DATA_ENTRY, WAITING, SUCCESS, FAILED, FAILED_INVALID_CREDENTIALS, FAILED_NO_GPS_SERVICE
	}

	/**
	 * The message used only with the State.FAILED state.
	 */
	private String _errorMessage;
	private CaptureTask _captureTask;
	private State _state;
	private DialogInterface _dialog;
	private Location _location;
	private boolean _gettingLocationUpdates;
	private File _photoFile;
	
	private static final String _jpegMime = "image/jpeg";
	private static final String _jpegExtensionWithoutDot = "jpg";
	
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
		
		// Update the photo and the photo button.
		String togglePhotoButtonText = resources.getString(R.string.capture_photo_button_toggle_add);
		if ((null != _photoFile) && (_photoFile.exists())) {
			Options bitmapOptions = new Options();
			bitmapOptions.inSampleSize = 4; // Open the bitmap as 1/4 its original size to save memory.
			Bitmap photoBitmap = BitmapFactory.decodeFile(_photoFile.getAbsolutePath(), bitmapOptions);
			((ImageView)findViewById(R.id.imageview_photo)).setImageBitmap(photoBitmap);
			togglePhotoButtonText = resources.getString(R.string.capture_photo_button_toggle_remove);
		} else { 
			((ImageView)findViewById(R.id.imageview_photo)).setImageBitmap(null);
		}
		((android.widget.Button)findViewById(R.id.capture_button_toggle_photo)).setText(togglePhotoButtonText); 

		switch (_state) {
		case DATA_ENTRY:
			// Nothing to do.
			break;
		case WAITING:
			// Show a 'waiting' dialog.
			_dialog = ProgressDialog.show(this, resources.getString(R.string.capture_waiting_dialog_title),
					resources.getString(R.string.capture_waiting_dialog_body), true, // Indeterminate.
					false); // Not cancellable.
			break;
		case SUCCESS:
			// Clear the field so that it isn't here if the user navigates back in history.
			((TextView) findViewById(R.id.capture_edittext_body)).setText("");
			
			// Start the 'home' activity.
			// Credentials/session key has already been stored.
			startActivity(new Intent(this, Home.class));
			
			// Show the user some toast to inform them of the success.
			Toast.makeText(this, resources.getString(R.string.capture_success), Toast.LENGTH_LONG).show();
			
			_state = State.DATA_ENTRY;
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
			noGpsfailedDialog.setMessage(resources.getString(R.string.dialog_error_gps_no_gps));
			noGpsfailedDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", this);
			noGpsfailedDialog.setCancelable(false); // Prevent the user from cancelling the dialog with the back key.
			noGpsfailedDialog.show();
			_dialog = noGpsfailedDialog;
			break;
		default:
			throw new UnexpectedEnumValueException(_state);
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.capture);
		
		if (!CredentialStore.getInstance(this).getHaveVerifiedCredentials()) {
			// Start the sign in activity.
			startActivity(new Intent(this, SignIn.class));
		}

		findViewById(R.id.capture_button_capture).setOnClickListener(this);
		findViewById(R.id.capture_button_toggle_photo).setOnClickListener(this);

		if (savedInstanceState != null) {
			_state = Enum.valueOf(State.class, savedInstanceState.getString("_state"));
			_errorMessage = savedInstanceState.getString("_errorMessage");
			_photoFile = (File)savedInstanceState.getSerializable("_photoFile");
			
			// If we have previously successfully logged in, go back to the data-entry state.
			// Otherwise we will redirect immediately back to the home activity.
			if (State.SUCCESS == _state) {
				_state = State.DATA_ENTRY;
				_errorMessage = "";
			} else if (State.FAILED_INVALID_CREDENTIALS == _state) {
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
			restoreTextViewInstanceState(savedInstanceState, R.id.capture_edittext_body);

			// Restore the focused view.
			View focusTarget = findViewById(savedInstanceState.getInt("focused_view"));
			if (null != focusTarget) {
				focusTarget.requestFocus();
			}
			
			ensureGettingGPSUpdates();

		} else {
			_state = State.DATA_ENTRY;
		}

		showState();
	}

	protected void onSaveInstanceState(Bundle outState) {
		tearEverythingDown();

		// Because we have different layouts for portrait and landscape views,
		// we need to manually save and restore the state of the TextViews.
		saveTextViewInstanceState(outState, R.id.capture_edittext_body);

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
		// The activity is about to become visible.
		ensureGettingGPSUpdates();
		showState();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// The activity has become visible (it is now "resumed").
		ensureGettingGPSUpdates();
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
	 * Ensure we are getting GPS location updates.
	 */
	private void ensureGettingGPSUpdates() {
		// Obtaining a GPS can take about 30 seconds, so we start the GPS provider as soon as we start the activity,
		// this way, a fix is hopefully available by the time the user is ready to capture. 
		
		if (!_gettingLocationUpdates) {
		
			// Get the most recent GPS fix (this might be null or out of date).
			LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
			
			// If GPS is not available, fail outright immediately (unless we're busy, showing another error, etc.).
			if ((State.DATA_ENTRY == _state) && (!locationManager.isProviderEnabled("gps"))) {
				_state = State.FAILED_NO_GPS_SERVICE;
			}
			
			_location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			
			// Begin listening for further GPS location updates.
			locationManager.requestLocationUpdates("gps", _gpsSuggestedUpdateIntervalMS, _gpsSuggestedUpdateDistanceMetres, this, getMainLooper());
			_gettingLocationUpdates = true;
		}
	}
	
	/**
	 * Ensures the asynchronous HTTP task is stopped and that we are no longer watching GPS location updates.
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
		
		// Ensure the Async HTTP task is cleaned away.
		
		// Don't interrupt the operation if it has started. The results are difficult to predict.
		if (_captureTask != null) {
			_captureTask.cancel(false); 
			_captureTask = null;
		}
		
		if (State.WAITING == _state) {
			_errorMessage = getResources().getString(R.string.dialog_error_task_canceled);
			_state = State.FAILED;
		}
	}
	
	private void captureButtonClicked() {
		Resources resources = getResources();

		CredentialStore store = CredentialStore.getInstance(this);
		if (!store.getHaveVerifiedCredentials()) {
			_errorMessage = "";
			_state = State.FAILED_INVALID_CREDENTIALS;
		} else {
			if (null == _location) {
				_errorMessage = resources.getString(R.string.dialog_error_gps_no_fix);
				_state = State.FAILED;
			} else {
				double longitude = _location.getLongitude();
				double latitude = _location.getLatitude();
				long unixTimestampMs = _location.getTime();
				
				if ((System.currentTimeMillis() - unixTimestampMs) > _maximumGpsFixAgeMs) {
					// The most recent GPS fix is too old.
					_errorMessage = resources.getString(R.string.dialog_error_gps_no_fix);
					_state = State.FAILED;
				} else {
					// Validate the user data the same as it will be validated by the OHOW API.
					String body = ((TextView) findViewById(R.id.capture_edittext_body)).getText().toString();
					String validationMessage = "";
			
					if (APIConstants.captureBodyMinLength > body.length()) {
						validationMessage = resources.getString(R.string.capture_body_text_too_short);
					} else if (APIConstants.captureBodyMaxLength < body.length()) {
						validationMessage = resources.getString(R.string.capture_body_text_too_long);
					}
					
					if (validationMessage.length() > 0) {
						Toast.makeText(this, validationMessage, Toast.LENGTH_LONG).show();
					} else {
	
						// The HttpClient will verify the certificate is signed by a trusted
						// source.
						HttpPost post = new HttpPost("https://cpanel02.lhc.uk.networkeq.net/~soberfun/1/capture.php");
						post.setHeader("Accept", "application/json");
						post.setHeader("X_OHOW_DEBUG_KEY", "sj30fj5X9whE93Bf0tjfhSh3jkfs2w03udj92");
		
						// PHP doesn't seem to accept the post if we specify a character set in the 'MultipartEntity' constructor. 
						MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
						final String textMimeType = "text/plain";
						final Charset utf8 = Charset2.getUtf8();
						 
						try {
							entity.addPart(new FormBodyPart("username", new StringBody(store.getUsername(), textMimeType, utf8))); 
							entity.addPart(new FormBodyPart("password", new StringBody(store.getPassword(), textMimeType, utf8)));
							entity.addPart(new FormBodyPart("body", new StringBody(body, textMimeType, utf8)));
							entity.addPart(new FormBodyPart("longitude", new StringBody(Double.toString(longitude), textMimeType, utf8)));
							entity.addPart(new FormBodyPart("latitude", new StringBody(Double.toString(latitude), textMimeType, utf8)));
						} catch (UnsupportedEncodingException e) {
							throw new ImprobableCheckedExceptionException(e);
						}
						
						if (null != _photoFile) {
							FileBody photoFilePart= new FileBody(_photoFile, _photoFile.getAbsolutePath(), _jpegMime, Charset2.getUtf8().name());
							entity.addPart("photo", photoFilePart);
						}
						post.setEntity(entity);
						
						_state = State.WAITING;
						_captureTask = new CaptureTask(this);
						_captureTask.execute(post);
					}
				}
			}
		}

		showState();
	}

	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.capture_button_capture:
			captureButtonClicked();
			break;
		case R.id.capture_button_toggle_photo:
			togglePhoto();
			break;
		default:
			throw new UnknownClickableItemException(view.getId());
		}
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (_state) {
		case FAILED:
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
		case SUCCESS:
		case DATA_ENTRY:
		case WAITING:
		case FAILED_INVALID_CREDENTIALS:
			throw new IllegalStateException();
		default:
			throw new UnexpectedEnumValueException(_state);
		}
	}

	/**
	 * Asynchronously performs a HTTP request.
	 */
	private class CaptureTask extends AsyncTask<HttpPost, Void, HttpResponse> {
		private WeakReference<Capture> _parent;
		private String _userAgent;		 

		public CaptureTask(Capture parent) {
			// Use a weak-reference for the parent activity. This prevents a memory leak should the activity be destroyed.
			_parent = new WeakReference<Capture>(parent);

			// Whilst we are on the UI thread, build a user-agent string from
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
			
			Capture parent = _parent.get();
			
			// 'parent' will be null if it has already been garbage collected.
			if (parent._captureTask == this) {

				CredentialStore auth = CredentialStore.getInstance(parent);
				
				try {
					// ProcessJSONResponse() appropriately handles a null result.
					
					// We don't actually care about the response, we just need to ensure there are no errors.
					APIResponseHandler.ProcessJSONResponse(response, getResources());

					// To complete without error is a success.
					parent._state = State.SUCCESS;
					
				} catch (OHOWAPIException e) {
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
				_state = State.FAILED;
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
