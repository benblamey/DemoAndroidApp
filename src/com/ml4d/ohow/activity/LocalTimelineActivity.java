package com.ml4d.ohow.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONException;
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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.ListView;

/*
 * Interactive logic for the 'LocalTimeline' activity.
 */
public class LocalTimelineActivity extends ListActivity implements ITaskFinished, AdapterView.OnItemClickListener, OnScrollListener, DialogInterface.OnDismissListener {

	private static final int NUMBER_OF_MOMENTS_TO_GET_AT_A_TIME = 10;
	private static final int SEARCH_RADIUS_METRES = 1000;
	
	/**
	 * The required intent (double) extra for the latitude.
	 */
	public static String EXTRA_LATITUDE = "latitude";
	
	/**
	 * The required intent (double) extra for the longitude.
	 */
	public static String EXTRA_LONGITUDE = "longitude";
	
	// These fields are persisted.
	private State _state = State.INITIAL_STATE;
	private String _ohowAPIError; // If the state is 'API_ERROR_RESPONSE', details of the error.
	private ArrayList<Moment> _moments;
	private double _latitude;
	private double _longitude;
	
	// These fields are not persisted.
	private MomentLocationRecentSearchTask _getMomentTask;
	private Dialog _dialog;

	private enum State {
		INITIAL_STATE,
		WAITING_FOR_API,
    	HAVE_MOMENTS_THERE_ARE_NO_MORE_PREVIOUS,
    	HAVE_MOMENTS_THERE_ARE_MORE,
		API_HAS_NO_MOMENTS,
		NO_API_RESPONSE, 
		API_ERROR_RESPONSE, 
		API_GARBAGE_RESPONSE,
		FAILED_ROTATE};

	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (!CredentialStore.getInstance().getHaveVerifiedCredentials()) {
			SignInActivity.signInAgain(this);
		} else {
			ListView listView = getListView();
			listView.setBackgroundResource(R.drawable.activity_background);
			listView.setTextFilterEnabled(false); // We don't support text-filtering for moments.
			listView.setOnItemClickListener(this);
			getListView().setOnScrollListener(this);
			
			Intent startingIntent = getIntent();		
			_latitude = startingIntent.getDoubleExtra(EXTRA_LATITUDE, -1);
			_longitude = startingIntent.getDoubleExtra(EXTRA_LONGITUDE, -1);
	
			if (null != savedInstanceState) {
				_moments = (ArrayList<Moment>)savedInstanceState.getSerializable("_moments");
				_state = Enum.valueOf(State.class, savedInstanceState.getString("_state"));
				_ohowAPIError = savedInstanceState.getString("_ohowAPIError");
				_latitude = savedInstanceState.getDouble("_latitude");
				_longitude = savedInstanceState.getDouble("_longitude");
				
				if (State.WAITING_FOR_API == _state) {
					_state = ((null != _moments) && (!_moments.isEmpty())) ?
							State.HAVE_MOMENTS_THERE_ARE_MORE : State.INITIAL_STATE;
					getSomeMoments();
				}
			} else {
				getSomeMoments();
			}
	
			showState();
		}
	}

	private void getSomeMoments() {
		if (State.INITIAL_STATE == _state)
		{
			assert _getMomentTask == null;
			// Search for a maximum of 30 results in a radius of 1000 metres.
			_getMomentTask = new MomentLocationRecentSearchTask(this, _latitude, _longitude, NUMBER_OF_MOMENTS_TO_GET_AT_A_TIME + 1, SEARCH_RADIUS_METRES);
			_getMomentTask.execute((Void[])null);
			_state = State.WAITING_FOR_API;
		} else if (State.HAVE_MOMENTS_THERE_ARE_MORE == _state) {
			assert _moments != null;
			assert !_moments.isEmpty();
			assert _getMomentTask == null;
			
			// The last item in the list is the "oldest" moment.
			Moment oldestMoment = _moments.get(_moments.size() - 1);
			
			Date dateCreatedUTCMax = oldestMoment.getDateCreatedUTC();
			int maxID = oldestMoment.getId();
			
			// Search for a maximum of 30 results in a radius of 1000 metres.
			_getMomentTask = new MomentLocationRecentSearchTask(this, _latitude, _longitude, NUMBER_OF_MOMENTS_TO_GET_AT_A_TIME + 1, SEARCH_RADIUS_METRES, dateCreatedUTCMax, maxID);
			_getMomentTask.execute((Void[])null);
			_state = State.WAITING_FOR_API;
			
		} else {
			// Do nothing.
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
		String message = null;
		switch (_state) {
			case INITIAL_STATE:
				throw new RuntimeException("Should have left this state by now!");
			case API_ERROR_RESPONSE:
				message = _ohowAPIError;
				break;
			case API_GARBAGE_RESPONSE:
				message = resources.getString(R.string.error_ohow_garbage_response);
				break;
			case NO_API_RESPONSE:
				message = resources.getString(R.string.comms_error);
				break;
			case API_HAS_NO_MOMENTS:
				message = resources.getString(R.string.home_no_history_here);
				break;
			case FAILED_ROTATE:
				message = resources.getString(R.string.dialog_error_rotate_when_busy);
				break;
			case WAITING_FOR_API:
				if (null == _moments) { 
					// Show a 'waiting' dialog.
					_dialog = ProgressDialog.show(this, resources.getString(R.string.local_timeline_activity_label),
							resources.getString(R.string.general_waiting), true, // Indeterminate.
							false); // Not cancellable.
					break;
				}
				// Fall through...
			case HAVE_MOMENTS_THERE_ARE_NO_MORE_PREVIOUS:
			case HAVE_MOMENTS_THERE_ARE_MORE:
				assert null != _moments;
				assert false;
				
				MomentArrayAdapter listAdapter = (MomentArrayAdapter)this.getListAdapter();
				MomentArrayAdapter.EndState endState;
				switch (_state) {
					case HAVE_MOMENTS_THERE_ARE_NO_MORE_PREVIOUS:
						endState = MomentArrayAdapter.EndState.ARE_NO_MORE_MOMENTS; 
						break;
					case HAVE_MOMENTS_THERE_ARE_MORE:
						endState = MomentArrayAdapter.EndState.THERE_ARE_MORE_MOMENTS;
						break;
					case WAITING_FOR_API:
						endState = MomentArrayAdapter.EndState.WAITING;
						break;
					default:
						throw new RuntimeException("Shouldn't be possible"); 
				}
				
				if (null != listAdapter) {
					listAdapter.setEndState(endState);
				} else {
					listAdapter = new MomentArrayAdapter(this, _moments, endState);
					setListAdapter(listAdapter);
				}
				break;
			default:
				throw new UnexpectedEnumValueException(_state);
		}
		
		if (null != message) {
			// Show a 'failed' dialog.
			AlertDialog failedDialog = new AlertDialog.Builder(this).create();
			failedDialog.setTitle(resources.getString(R.string.local_timeline_activity_label));
			failedDialog.setMessage(message);
			failedDialog.setOnDismissListener(this);
			failedDialog.show();
			_dialog = failedDialog;
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// Show the particular moment.
		Moment moment = _moments.get(position);
		
		// Compute whether to enable the 'newer' and 'older' buttons should be enabled when showing the moment.
		boolean haveNewer;
		boolean haveOlder;
		if (position + 1 == this._moments.size()) {
			haveNewer = this._moments.size() > 1;
			haveOlder = (State.HAVE_MOMENTS_THERE_ARE_MORE == this._state);
		} else if (0 == position) {
			haveNewer = false; // Assume no moments have been added since we obtained the local timeline results.
			haveOlder = true;
		} else {
			haveNewer = true;
			haveOlder = true;
		}
		
		ShowMomentInstanceActivityInstruction instruction = new ShowMomentInstanceActivityInstruction(
			_latitude,
			_longitude,
			SEARCH_RADIUS_METRES,
			moment,
			haveNewer,
			haveOlder);
		
		Intent i = new Intent(this, ShowMomentActivity.class);
		i.putExtra(ShowMomentActivity.EXTRA_INSTRUCTION_KEY, instruction);
		startActivity(i);
	}
	
	@Override
	public void callMeBack(Object sender) {
		if (sender == _getMomentTask) {
			assert _state == State.WAITING_FOR_API;
			State state;
			List<Moment> fetchedMoments = null;
			String ohowAPIError = "";
			
			try {
				fetchedMoments = _getMomentTask.getResult();
				
				if (fetchedMoments.size() > 0) {

					if (NUMBER_OF_MOMENTS_TO_GET_AT_A_TIME + 1 == fetchedMoments.size()) {
						state = State.HAVE_MOMENTS_THERE_ARE_MORE;
						fetchedMoments.remove(NUMBER_OF_MOMENTS_TO_GET_AT_A_TIME); // Discard the last moment.
					} else {
						state = State.HAVE_MOMENTS_THERE_ARE_NO_MORE_PREVIOUS;
					}
				} else {
					Log.d("OHOW", "Result array has zero moments.");
					if (_moments != null) {
						// We were trying to fetch more moments - there are no more moments.
						state = State.HAVE_MOMENTS_THERE_ARE_NO_MORE_PREVIOUS;
					} else {
						// We were fetching the initial set of moments.
						state = State.API_HAS_NO_MOMENTS;
					}
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

			if (null == this._moments) {
				_moments = new ArrayList<Moment>();
			}

			if (null != fetchedMoments) {
				// Insert the new moments at the end of the list.
				_moments.addAll(_moments.size(), fetchedMoments);				
			}
			_ohowAPIError = ohowAPIError;
			_getMomentTask = null;
			_state = state;
			showState();
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// Nothing to do.
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		if (State.HAVE_MOMENTS_THERE_ARE_MORE == _state) { 
	        boolean getMoreMoments = (firstVisibleItem + 2 * visibleItemCount) >= totalItemCount;

	        if (getMoreMoments) {
	        	getSomeMoments();
	        	showState();
	        }
		}
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		// The user has dismissed the failed dialog.
		// Go back.
		this.finish();
	}

}
