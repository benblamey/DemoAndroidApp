package com.ml4d.ohow.activity;

import java.io.IOException;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.ml4d.core.exceptions.UnexpectedEnumValueException;
import com.ml4d.ohow.CredentialStore;
import com.ml4d.ohow.ITaskFinished;
import com.ml4d.ohow.Moment;
import com.ml4d.ohow.MomentArrayAdapter;
import com.ml4d.ohow.R;
import com.ml4d.ohow.exceptions.ApiViaHttpException;
import com.ml4d.ohow.exceptions.NoResponseAPIException;
import com.ml4d.ohow.tasks.MomentLocationRecentSearchTask;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

/*
 * Interactive logic for the 'LocalTimeline' activity.
 */
public class LocalTimelineActivity extends ListActivity implements ITaskFinished, AdapterView.OnItemClickListener {

	// These fields are persisted.
	private State _state;
	private String _ohowAPIError; // If the state is 'API_ERROR_RESPONSE', details of the error.
	private ArrayList<Moment> _moments; 
	
	// These fields are not persisted.
	private MomentLocationRecentSearchTask _getMomentTask;
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

			// Search for a maximum of 30 results in a radius of 1000 metres.
			_getMomentTask = new MomentLocationRecentSearchTask(this, latitude, longitude, 30, 1000);
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
		outState.putString("_ohowAPIError", _ohowAPIError);
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
	
	@Override
	public void CallMeBack(Object sender) {
		if (sender == _getMomentTask) {
			State state;
			ArrayList<Moment> moments = null;
			String ohowAPIError = "";
			
			try {
				JSONArray resultArray = _getMomentTask.getResult();
				
				if (resultArray.length() > 0) {
					state = State.HAVE_MOMENT;
					moments = new ArrayList<Moment>();
					for (int i = 0; i < resultArray.length(); i++) {
						Object resultItem = resultArray.get(i);
						if (resultItem instanceof JSONObject) {
							JSONObject resultItemObject = (JSONObject)resultItem;
							moments.add(new Moment(resultItemObject));
						} else {
							Log.d("OHOW", "Result array item not an object..");
							state = State.API_GARBAGE_RESPONSE;
							break;
						}
					}
				} else {
					Log.d("OHOW", "Result array has zero moments.");
					state = State.API_HAS_NO_MOMENTS;
				}

			} catch (NoResponseAPIException e) {
				state = State.NO_API_RESPONSE;
			} catch (ApiViaHttpException e) {
				state  = State.API_ERROR_RESPONSE;
				ohowAPIError = e.getLocalizedMessage();
			} catch (JSONException e) {
				state = State.API_GARBAGE_RESPONSE;
			} catch (IOException e) {
				state = State.NO_API_RESPONSE;
			}

			_state = state;
			if (State.HAVE_MOMENT == state) {
				_moments = moments;
			}
			_ohowAPIError = ohowAPIError;
			_getMomentTask = null;
			showState();
		}
	}
		
}
