/**
 * 
 */
package com.ml4d.ohow;

import java.util.Calendar;
import java.util.Date;

import com.ml4d.ohow.exceptions.UnexpectedEnumValueException;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

/**
 * @author ben
 *
 */
public class APIAuthentication {

	private SharedPreferences _preferences;
	private State _state;
	private Date _sessionExpirationDate;
	private String _sessionKey;
	private String _username;
	private String _password;
	
	private enum State {
		NO_CREDENTIALS, 
		HAVE_CREDENTIALS_AND_ASSUMED_VALID_SESSION_KEY,
		HAVE_CREDENTIALS_AND_KNOWN_EXPIRED_SESSION_KEY,
		HAVE_KNOWN_INVALID_CREDENTIALS,
		//HAVE_UNKNOWN_CREDENTIALS, << I don't think we need this one. TBD.
	}
	
	public APIAuthentication(Context context) {
		
		_preferences = context.getSharedPreferences("APIAuthentication", Context.MODE_PRIVATE);
		_state = State.valueOf(State.class, _preferences.getString("_state", State.NO_CREDENTIALS.name()));
		
		_username = _preferences.getString("_username", "");
		_password = _preferences.getString("_password", "");
		 
		long expires = _preferences.getLong("_sessionExpirationDate", 0);
		final Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(expires);
		_sessionExpirationDate = cal.getTime();
					
		_sessionKey = _preferences.getString("_sessionKey", "");
	}
	
	private void saveState() {
		Editor editor = _preferences.edit();
		editor.putString("_state", _state.name());
		editor.putString("_sessionKey", _sessionKey);
		editor.putString("_username", _username);
		editor.putString("_password", _password);
		editor.putLong("_sessionExpirationDate", _sessionExpirationDate.getTime());
		editor.commit();
	}
	
	public boolean getWhetherCachedDataShouldBeDisplayed() {
		
		Log.d("OHOW", _state.toString());
		boolean shouldDisplay;
		switch (_state) {
		case NO_CREDENTIALS:
		case HAVE_KNOWN_INVALID_CREDENTIALS:
			shouldDisplay = false;
			break;
		case HAVE_CREDENTIALS_AND_ASSUMED_VALID_SESSION_KEY:
		case HAVE_CREDENTIALS_AND_KNOWN_EXPIRED_SESSION_KEY:
			shouldDisplay = true;
			break;
		default:
			throw new UnexpectedEnumValueException(_state);
		}
		return shouldDisplay;
	}
	
	public void updateState() {
		
		// go to server and refresh key.
		// if authentication fails, update state to known invalid credentials.
		
		// exceptions:
		// no credentials
		// no internet
		// api error
		// invalid credentials
	}
	
	public void setKnownGoodSessionKeyAndUsername(String username, String password, String sessionKey, Date expirationTime) {
		this._username = username;
		this._password = password;
		this._sessionKey = sessionKey;
		this._sessionExpirationDate = expirationTime;
		this._state = State.HAVE_CREDENTIALS_AND_ASSUMED_VALID_SESSION_KEY;
		
		saveState();
	}
	
	public String getSessionKey() {
		return _sessionKey;
		
		
//		switch (_state) {
//		case NO_CREDENTIALS:
//			// exception: no credentials
//			break;
//		case HAVE_CREDENTIALS_AND_KNOWN_EXPIRED_SESSION_KEY:
//			// known expired session key.
//			break;
//		case HAVE_KNOWN_INVALID_CREDENTIALS:
//			// exception: known invalid credentials.
//			break;
//		case HAVE_CREDENTIALS_AND_ASSUMED_VALID_SESSION_KEY:
//			return _sessionKey;
//		}
		
	}
	
	public String getUsername() {
		return _username;
	}
}
