package com.ml4d.ohow.activity;

import java.io.IOException;
import java.text.DateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import org.json.JSONException;
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
import android.app.Dialog;
import android.app.ProgressDialog;
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
public class ShowMomentActivity extends Activity implements ITaskFinished, View.OnClickListener { //DialogInterface.OnClickListener, 

	// These fields are not persisted.
	private AsyncTask<Void, Void, Void> _getMomentTask;
	private Dialog _dialog;
	
	// These fields are persisted.
	private State _entryState;
	private String _ohowAPIError; // If the state is 'API_ERROR_RESPONSE', details of the error.
	private Moment _moment;
	private boolean _canGoOlder;
	private boolean _canGoNewer;

	public static String EXTRA_INSTRUCTION_KEY = "instructon";
	
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
		
		if (!CredentialStore.getInstance().getHaveVerifiedCredentials()) {
			SignInActivity.signInAgain(this);
		} else {
			setContentView(R.layout.show_moment_activity);
			
			Button nextButton = (Button)findViewById(R.id.show_moment_activity_button_next);
			nextButton.setOnClickListener(this);
			
			Button prevButton = (Button)findViewById(R.id.show_moment_activity_button_previous);
			prevButton.setOnClickListener(this);
			
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
				
				// The activity is being started.
				Intent startingIntent = getIntent();
				
				ShowMomentActivityInstruction rawInstruction = startingIntent.getParcelableExtra(EXTRA_INSTRUCTION_KEY);
				
				if (rawInstruction instanceof ShowMomentInstanceActivityInstruction) {
					ShowMomentInstanceActivityInstruction instruction = (ShowMomentInstanceActivityInstruction)rawInstruction;

					 _canGoNewer = instruction.getHaveNewer();
					 _canGoOlder = instruction.getHaveOlder();
					 
					_moment = instruction.getMoment();
						
					// 'showState()' will fetch the photo (if there is one).
					
					_ohowAPIError = "";
					_entryState = State.HAVE_MOMENT;
					
				} else if (rawInstruction instanceof ShowNewerMomentActivityInstruction) {
					ShowNewerMomentActivityInstruction instruction = (ShowNewerMomentActivityInstruction)rawInstruction;
					
					// We assume that we have come from an older moment, so that moment must exist.
					_canGoOlder = true;
					_canGoNewer = false; // We won't know until we complete the search API request whether there is another moment.
					
					// Note: the ordering of the last 2 parameters indicates we are searching for newer items.
					_getMomentTask = new MomentLocationRecentSearchTask(this, 
							instruction.getLatitude(), 
							instruction.getLongitude(), 
							2, 
							instruction.getRadiusMetres(),
							instruction.getCurrentMomentId(),
							instruction.getCurrentDateCreatedUtc());
					_getMomentTask.execute((Void[])null);
					
					_entryState = State.WAITING_FOR_API;
					_ohowAPIError = "";
					
				} else if (rawInstruction instanceof ShowOlderMomentActivityInstruction) {
					ShowOlderMomentActivityInstruction instruction = (ShowOlderMomentActivityInstruction)rawInstruction;

					// We assume that we have come from an newer moment, so that moment must exist.
					_canGoNewer = true;
					_canGoOlder = false; // We won't know until we complete the search API request whether there is another moment.
					
					// Note: the ordering of the last 2 parameters indicates we are searching for older items.
					_getMomentTask = new MomentLocationRecentSearchTask(this, 
							instruction.getLatitude(), 
							instruction.getLongitude(), 
							2, 
							instruction.getRadiusMetres(),
							instruction.getCurrentDateCreatedUtc(),
							instruction.getCurrentMomentId());
					_getMomentTask.execute((Void[])null);
					
					_entryState = State.WAITING_FOR_API;
					_ohowAPIError = "";
					
				} else {
					throw new RuntimeException("No instruction was specified.");
				}
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
	
	private void showState() {
		
		if (null != _dialog) {
			_dialog.dismiss();
			_dialog = null;
		}
		
		Resources resources = getResources();
		
		Button nextButton = (Button)findViewById(R.id.show_moment_activity_button_next);
		Button prevButton = (Button)findViewById(R.id.show_moment_activity_button_previous);
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
			
			nextButton.setEnabled(this._canGoNewer);
			prevButton.setEnabled(this._canGoOlder);

		} else if (State.WAITING_FOR_API == _entryState) {
			// Show a 'waiting' dialog.
			_dialog = ProgressDialog.show(this, resources.getString(R.string.local_timeline_activity_label),
					resources.getString(R.string.general_waiting), true, // Indeterminate.
					false); // Not cancellable.
			location = "";
			body = "";
			details = "";
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
				case HAVE_MOMENT:
					throw new RuntimeException("This state was handled above?!");
				default:
					throw new UnexpectedEnumValueException(_entryState);
			}
			
			location = "";
			details = "";
			((WebImageView)findViewById(R.id.show_moment_activity_image_view_photo)).setUrl(null);
			
			nextButton.setEnabled(false);
			prevButton.setEnabled(false);
		}
		
		TextView textViewLocation = (TextView)findViewById(R.id.show_moment_activity_text_view_capture_location);
		TextView textViewBody = (TextView)findViewById(R.id.show_moment_activity_activity_text_view_body);
		TextView textViewDetails = (TextView)findViewById(R.id.show_moment_activity_text_view_details);
		
		textViewLocation.setText(location);
		textViewBody.setText(body);
		textViewDetails.setText(details);
	}

	@Override
	public void callMeBack(Object sender) {
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
						
						ShowMomentActivityInstruction startingInstruction = getIntent().getParcelableExtra(EXTRA_INSTRUCTION_KEY);
						
						if (startingInstruction instanceof ShowNewerMomentActivityInstruction) {
							_canGoNewer = moments.size() > 1;
						} else if (startingInstruction instanceof ShowOlderMomentActivityInstruction) {
							_canGoOlder = moments.size() > 1;
						} else {
							throw new RuntimeException("Unexpected starting intent");
						}
						
					} else {
						Log.d("OHOW", "Moment was expected - but was not returned.");
						error = State.API_ERROR_RESPONSE;
						apiErrorMessage = "Moment was expected - but was not returned.";
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
						ShowMomentActivityInstruction startingInstruction = getIntent().getParcelableExtra(EXTRA_INSTRUCTION_KEY);
						
						ShowNewerMomentActivityInstruction instruction = new ShowNewerMomentActivityInstruction(
							_moment.getLatitude(),
							_moment.getLongitude(),
							startingInstruction.getRadiusMetres(),
							_moment.getId(),
							_moment.getDateCreatedUTC());
						
						Intent i = new Intent(this, ShowMomentActivity.class);
						i.putExtra(EXTRA_INSTRUCTION_KEY, instruction);
						startActivity(i);
						break;
					}
				case R.id.show_moment_activity_button_previous:
					{
						ShowMomentActivityInstruction startingInstruction = getIntent().getParcelableExtra(EXTRA_INSTRUCTION_KEY);
						
						ShowOlderMomentActivityInstruction instruction = new ShowOlderMomentActivityInstruction(
							_moment.getLatitude(),
							_moment.getLongitude(),
							startingInstruction.getRadiusMetres(),
							_moment.getId(),
							_moment.getDateCreatedUTC());
						
						Intent i = new Intent(this, ShowMomentActivity.class);
						i.putExtra(EXTRA_INSTRUCTION_KEY, instruction);
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