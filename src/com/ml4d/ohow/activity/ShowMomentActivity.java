package com.ml4d.ohow.activity;

import java.io.IOException;
import java.text.DateFormat;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONException;

import com.ml4d.core.String2;
import com.ml4d.core.WebImageView;
import com.ml4d.core.exceptions.UnexpectedEnumValueException;
import com.ml4d.core.exceptions.UnknownClickableItemException;

import com.ml4d.ohow.CredentialStore;
import com.ml4d.ohow.ITaskFinished;
import com.ml4d.ohow.Moment;
import com.ml4d.ohow.OHOWAPIResponseHandler;
import com.ml4d.ohow.R;
import com.ml4d.ohow.exceptions.ApiViaHttpException;
import com.ml4d.ohow.exceptions.NoResponseAPIException;
import com.ml4d.ohow.tasks.MomentLocationRecentSearchTask;
import com.ml4d.ohow.tasks.ShowMomentTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Interactive logic for the sign in activity.
 */
public class ShowMomentActivity extends Activity implements DialogInterface.OnClickListener, ITaskFinished, View.OnClickListener {

	// These fields are not persisted.
	private AsyncTask<Void, Void, Void> _getMomentTask;
	private Dialog _dialog;
	
	// These fields are persisted.
	private State _entryState;
	private String _ohowAPIError; // If the state is 'API_ERROR_RESPONSE', details of the error.
	private Moment _moment;

	public static String EXTRA_MODE_KEY = "mode";
	
	public static String EXTRA_MODE_VALUE_MOMENT_ID = "show moment with id";
	public static String EXTRA_MODE_VALUE_PREVIOUS = "show previous moment";
	public static String EXTRA_MODE_VALUE_NEXT = "show next moment";
	
	public static String EXTRA_MOMENT_ID_KEY = "moment_id";
	public static String EXTRA_MOMENT_LATITUDE_KEY = "latitude";
	public static String EXTRA_MOMENT_LONGITUDE_KEY = "longitude";
	public static String EXTRA_MOMENT_CREATED_TIME_UTC_KEY = "created time";
	public static String EXTRA_MOMENT_SEARCH_RADIUS_METRES_KEY = "radius";
	public static String EXTRA_NEXT_OR_PREVIOUS_INTENT = "intent";
	
	/**
	 * This an intent with this key is set, the 'previous' button gets disabled.
	 */
	public static String EXTRA_NO_PREVIOUS_MOMENT_KEY = "no_previous_moment";

	private enum State {
		WAITING_FOR_API,
		HAVE_MOMENT,
		NO_MOMENT,
		NO_API_RESPONSE, 
		API_ERROR_RESPONSE, 
		API_GARBAGE_RESPONSE };
		
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (!CredentialStore.getInstance().getHaveVerifiedCredentials()) {
			SignInActivity.signInAgain(this);
		} else {
			setContentView(R.layout.show_moment_activity);
			
			// The activity is being started.
			Intent startingIntent = getIntent();
	
			Button nextButton = (Button)findViewById(R.id.show_moment_activity_button_next);
			nextButton.setOnClickListener(this);
			
			Button prevButton = (Button)findViewById(R.id.show_moment_activity_button_previous);
			if (!startingIntent.hasExtra(EXTRA_NO_PREVIOUS_MOMENT_KEY)) {			
				prevButton.setOnClickListener(this);
			} else {
				prevButton.setVisibility(View.VISIBLE);
			}
			
			boolean getMoment = true;
			if (null != savedInstanceState) {
				// The activity is being restored from serialised state.
				_moment = (Moment)savedInstanceState.getSerializable("_moment");
				_entryState = Enum.valueOf(State.class, savedInstanceState.getString("_entryState"));
				_ohowAPIError = savedInstanceState.getString("_ohowAPIError");
				
				if (State.WAITING_FOR_API == _entryState) {
					getMoment = true;
				}
			} 
			
			if (getMoment) {

				
				String mode = startingIntent.getStringExtra(EXTRA_MODE_KEY);
				if (String2.areEqual(mode, EXTRA_MODE_VALUE_MOMENT_ID)) {
					
					int momentId = startingIntent.getIntExtra(EXTRA_MOMENT_ID_KEY, -1); // Moments always have positive IDs.
					if (-1 == momentId) {
						throw new RuntimeException("This activity should only be started by the with the intent extra set specifying the moment ID.");
					}
					
					// We don't need these yet, but if we need to fetch a next or previous entry, we will need them.
					double latitude = startingIntent.getDoubleExtra(EXTRA_MOMENT_LATITUDE_KEY, 999);
					if (999 == latitude) {
						throw new RuntimeException("latitude is mandatory for this mode");
					}
					
					double longitude = startingIntent.getDoubleExtra(EXTRA_MOMENT_LONGITUDE_KEY, 999);
					if (999 == longitude) {
						throw new RuntimeException("latitude is mandatory for this mode");
					}
					
					int radiusMetres = startingIntent.getIntExtra(EXTRA_MOMENT_SEARCH_RADIUS_METRES_KEY, -1);
					if (-1 == radiusMetres) {
						throw new RuntimeException("radius is mandatory for this mode.");
					}
	 				
					_getMomentTask = new ShowMomentTask(this, momentId);
					_getMomentTask.execute((Void[])null);
	
					// Fetch the photo - there might not be one, but it is faster to try immediately and risk the wasted effort than
					// wait until we have fetched the moment.
					String url = OHOWAPIResponseHandler.getBaseUrlIncludingTrailingSlash(false) + "photo.php"
						+ "?" 
						+ "id=" + Integer.toString(momentId)
						+ "&photo_size=medium"; // Get the full-sized image.
					((WebImageView)findViewById(R.id.show_moment_activity_image_view_photo)).setUrl(url);
	
				} else if (String2.areEqual(mode, EXTRA_MODE_VALUE_PREVIOUS)) {
					
					int momentID = startingIntent.getIntExtra(EXTRA_MOMENT_ID_KEY, -1); // Moments always have positive IDs.
					if (-1 == momentID) {
						throw new RuntimeException("This activity should only be started by the with the intent extra set specifying the moment ID.");
					}
					
					double latitude = startingIntent.getDoubleExtra(EXTRA_MOMENT_LATITUDE_KEY, 999);
					if (999 == latitude) {
						throw new RuntimeException("latitude is mandatory for this mode");
					}
					
					double longitude = startingIntent.getDoubleExtra(EXTRA_MOMENT_LONGITUDE_KEY, 999);
					if (999 == longitude) {
						throw new RuntimeException("latitude is mandatory for this mode");
					}
					
					int radiusMetres = startingIntent.getIntExtra(EXTRA_MOMENT_SEARCH_RADIUS_METRES_KEY, -1);
					if (-1 == radiusMetres) {
						throw new RuntimeException("radius is mandatory for this mode.");
					}
					
					Date dateCreatedUtc = (Date)startingIntent.getSerializableExtra(EXTRA_MOMENT_CREATED_TIME_UTC_KEY);
					
					// public MomentLocationRecentSearchTask(ITaskFinished parent, double latitude, double longitude, int maxResults, 
					// int radiusMeters, Date dateCreatedUTCMax, int maxID) {
					_getMomentTask = new MomentLocationRecentSearchTask(this, latitude, longitude, 1, radiusMetres, dateCreatedUtc, momentID);
					_getMomentTask.execute((Void[])null);
				} else if (String2.areEqual(mode, EXTRA_MODE_VALUE_NEXT)) {
					
					int momentID = startingIntent.getIntExtra(EXTRA_MOMENT_ID_KEY, -1); // Moments always have positive IDs.
					if (-1 == momentID) {
						throw new RuntimeException("This activity should only be started by the with the intent extra set specifying the moment ID.");
					}
					
					double latitude = startingIntent.getDoubleExtra(EXTRA_MOMENT_LATITUDE_KEY, 999);
					if (999 == latitude) {
						throw new RuntimeException("latitude is mandatory for this mode");
					}
			
					double longitude = startingIntent.getDoubleExtra(EXTRA_MOMENT_LONGITUDE_KEY, 999);
					if (999 == longitude) {
						throw new RuntimeException("latitude is mandatory for this mode");
					}
					
					int radiusMetres = startingIntent.getIntExtra(EXTRA_MOMENT_SEARCH_RADIUS_METRES_KEY, -1);
					if (-1 == radiusMetres) {
						throw new RuntimeException("radius is mandatory for this mode.");
					}
					
					Date dateCreatedUtc = (Date)startingIntent.getSerializableExtra(EXTRA_MOMENT_CREATED_TIME_UTC_KEY);
					
					_getMomentTask = new MomentLocationRecentSearchTask(this, latitude, longitude, 1, radiusMetres, momentID, dateCreatedUtc);
					_getMomentTask.execute((Void[])null);
				}
				
				_entryState = State.WAITING_FOR_API;
				_ohowAPIError = "";
			}
			
			
			showState();
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		// The activity is about to become visible.
		if (!CredentialStore.getInstance().getHaveVerifiedCredentials()) {
			SignInActivity.signInAgain(this);
		} else {
			showState();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		// The activity has become visible (it is now "resumed").
		if (!CredentialStore.getInstance().getHaveVerifiedCredentials()) {
			SignInActivity.signInAgain(this);
		} else {
			showState();
		}
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
		
		outState.putSerializable("_moment", _moment);
		outState.putString("_entryState", _entryState.name());
		outState.putString("_ohowAPIError", _ohowAPIError);
	}
	
	/**
	 * Ensures the asynchronous HTTP task is stopped.
	 */
	private void ensureTaskIsStopped() {
		if (State.WAITING_FOR_API == _entryState) {
			if (_getMomentTask != null) {
				_getMomentTask.cancel(false); // Don't interrupt the operation if
												// it has started. The results
												// are difficult to predict.
				_getMomentTask = null;
			}
			
			_ohowAPIError = getResources().getString(R.string.dialog_error_task_canceled);
			_entryState = State.API_ERROR_RESPONSE;
		}
	}
	
	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (State.NO_MOMENT == this._entryState) {
			if (null != _dialog) {
				_dialog = null;
			}

			Intent i = getIntent().getParcelableExtra(EXTRA_NEXT_OR_PREVIOUS_INTENT);
			startActivity(i);
			finish();
		} else {
			throw new RuntimeException("This state doesn't show a dialog.");
		}
	}
	

	private void showState() {
		Resources resources = getResources();
			
		String location;
		String body;
		String details;
		if (null != _moment) {
			// Otherwise, if we have a moment, we show it. We do this even if we failed to get a new moment.

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
			
			// Get the photo associated with the moment.			
			String photoUrl;
			if (_moment.getHasPhoto()) {
				photoUrl = OHOWAPIResponseHandler.getBaseUrlIncludingTrailingSlash(false) + "photo.php"
					+ "?" 
					+ "id=" + Integer.toString(_moment.getId())
					+ "&photo_size=medium"; // Get the full-sized image.
				
			} else {
				// Clear any existing image.
				photoUrl = null;
			}
			((WebImageView)findViewById(R.id.show_moment_activity_image_view_photo)).setUrl(photoUrl);
			
			findViewById(R.id.show_moment_activity_button_next).setEnabled(true);
			findViewById(R.id.show_moment_activity_button_previous).setEnabled(true);

		} else {
			
			switch (_entryState) {
				case NO_MOMENT:
					// Show a 'failed' dialog.
					AlertDialog failedDialog = new AlertDialog.Builder(this).create();
					failedDialog.setTitle(resources.getString(R.string.error_dialog_title));
					failedDialog.setMessage("No moment!");
					failedDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", this);
					failedDialog.setCancelable(false); // Prevent the user from cancelling the dialog with the back key.
					failedDialog.show();
					_dialog = failedDialog;

					location = "";
					body = "";
					details = "";
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
			((WebImageView)findViewById(R.id.show_moment_activity_image_view_photo)).setUrl(null);

			findViewById(R.id.show_moment_activity_button_next).setEnabled(false);
			findViewById(R.id.show_moment_activity_button_previous).setEnabled(false);
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
			
			if (sender instanceof ShowMomentTask) {
				
				ShowMomentTask getMomentTask = (ShowMomentTask)sender;
				State error;
				String apiErrorMessage = "";
				Moment moment = null;
				
				try {
					moment = getMomentTask.getResult();
					error = State.HAVE_MOMENT;
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
				} catch (IOException e) {
					Log.d("OHOW", e.toString());
					error = State.NO_API_RESPONSE;
				}
	
				_entryState = error;
				_ohowAPIError = apiErrorMessage;
				_moment = moment;
				_getMomentTask = null;
				showState();
			} else if (sender instanceof MomentLocationRecentSearchTask) {
			
				MomentLocationRecentSearchTask getMomentTask = (MomentLocationRecentSearchTask)sender;
				State error;
				String apiErrorMessage = "";
				Moment moment = null;
				
				try {
					List<Moment> moments = getMomentTask.getResult();
					if (moments.size() > 0) {
						moment = moments.get(0);
						error = State.HAVE_MOMENT;
					} else {
						error = State.NO_MOMENT;
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
				} catch (IOException e) {
					Log.d("OHOW", e.toString());
					error = State.NO_API_RESPONSE;
				}
	
				_entryState = error;
				_ohowAPIError = apiErrorMessage;
				_moment = moment;
				_getMomentTask = null;
				showState();				
				
			} else {
				throw new RuntimeException("Type of task is not expected. Programmer mistake!");
			}
		}
	}

	@Override
	public void onClick(View v) {
		
		if (_moment != null) {
		
			switch (v.getId()) {
				case R.id.show_moment_activity_button_next:
					{
						Intent i = new Intent(this, ShowMomentActivity.class);
						// We want to show a moment with a particular ID.
						i.putExtra(ShowMomentActivity.EXTRA_MODE_KEY, ShowMomentActivity.EXTRA_MODE_VALUE_NEXT);
						
						// These all need to be the same as before to ensure we are paginating through the same set of results.
						i.putExtra(EXTRA_MOMENT_LATITUDE_KEY, getIntent().getDoubleExtra(EXTRA_MOMENT_LATITUDE_KEY, -1));
						i.putExtra(EXTRA_MOMENT_LONGITUDE_KEY, getIntent().getDoubleExtra(EXTRA_MOMENT_LONGITUDE_KEY, -1));
						i.putExtra(EXTRA_MOMENT_SEARCH_RADIUS_METRES_KEY, getIntent().getIntExtra(EXTRA_MOMENT_SEARCH_RADIUS_METRES_KEY, -1));
						
						i.putExtra(EXTRA_MOMENT_ID_KEY, _moment.getId());
						i.putExtra(EXTRA_MOMENT_CREATED_TIME_UTC_KEY, _moment.getDateCreatedUTC());

						// This activity is marked as 'NoHistory', because we don't
						// want to end up with a history of 100s of states of this activity.
						// However, if the user reaches the end of the list, they will
						// will want to go back - we implement this by simply starting the previous intent.
						i.putExtra(EXTRA_NEXT_OR_PREVIOUS_INTENT, this.getIntent());
						
						startActivity(i);
						break;
					}
				case R.id.show_moment_activity_button_previous:
					{
						Intent i = new Intent(this, ShowMomentActivity.class);
						// We want to show a moment with a particular ID.
						i.putExtra(ShowMomentActivity.EXTRA_MODE_KEY, ShowMomentActivity.EXTRA_MODE_VALUE_PREVIOUS);
						
						// These all need to be the same as before to ensure we are paginating through the same set of results.
						i.putExtra(EXTRA_MOMENT_LATITUDE_KEY, getIntent().getDoubleExtra(EXTRA_MOMENT_LATITUDE_KEY, -1));
						i.putExtra(EXTRA_MOMENT_LONGITUDE_KEY, getIntent().getDoubleExtra(EXTRA_MOMENT_LONGITUDE_KEY, -1));
						i.putExtra(EXTRA_MOMENT_SEARCH_RADIUS_METRES_KEY, getIntent().getIntExtra(EXTRA_MOMENT_SEARCH_RADIUS_METRES_KEY, -1));
						
						i.putExtra(EXTRA_MOMENT_ID_KEY, _moment.getId());
						i.putExtra(EXTRA_MOMENT_CREATED_TIME_UTC_KEY, _moment.getDateCreatedUTC());

						// This activity is marked as 'NoHistory', because we don't
						// want to end up with a history of 100s of states of this activity.
						// However, if the user reaches the end of the list, they will
						// will want to go back - we implement this by simply starting the previous intent.
						i.putExtra(EXTRA_NEXT_OR_PREVIOUS_INTENT, this.getIntent());

						startActivity(i);
						break;
					}
				default:
					throw new UnknownClickableItemException(v.getId());
			}
		} else  {
			throw new RuntimeException("These buttons should be disabled when there is no moment.");
		}
		
		
	}
		
}
