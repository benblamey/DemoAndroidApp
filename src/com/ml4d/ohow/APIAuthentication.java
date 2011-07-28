package com.ml4d.ohow;

import java.util.Date;

import com.ml4d.ohow.exceptions.CalledFromWrongThreadException;
import com.ml4d.ohow.exceptions.UnexpectedEnumValueException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

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
	private Context _context;
	
	private enum State {
		NO_CREDENTIALS, 
		HAVE_CREDENTIALS_AND_ASSUMED_VALID_SESSION_KEY,
		HAVE_CREDENTIALS_AND_KNOWN_EXPIRED_SESSION_KEY,
		HAVE_KNOWN_INVALID_CREDENTIALS,
	}
	
	public APIAuthentication(Context context) {
		_context = context;
		
		_preferences = context.getSharedPreferences("APIAuthentication", Context.MODE_PRIVATE);
		_state = State.valueOf(State.class, _preferences.getString("_state", State.NO_CREDENTIALS.name()));
		
		_username = _preferences.getString("_username", "");
		_password = _preferences.getString("_password", "");
		_sessionExpirationDate = DateTimeUtilities.getTimeFromUnixTimeMs(_preferences.getLong("_sessionExpirationDate", 0));					
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
		verifyOnMainUIThread();
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
	
	/**
	 * Query the OHOW Server API to refresh the session key using the currently held credentials.
	 */
	public void updateState() {
		verifyOnMainUIThread();
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Stores a recent session key from the server, along with the username and password which was used to retrieve it.
	 * @param username
	 * @param password
	 * @param sessionKey
	 * @param expirationTime
	 */
	public void setKnownGoodDetails(String username, String password, String sessionKey, Date expirationTime) {
		verifyOnMainUIThread();
		this._username = username;
		this._password = password;
		this._sessionKey = sessionKey;
		this._sessionExpirationDate = expirationTime;
		this._state = State.HAVE_CREDENTIALS_AND_ASSUMED_VALID_SESSION_KEY;
		
		saveState();
	}
	
	public void Clear() {
		verifyOnMainUIThread();
		this._username = "";
		this._password = "";
		this._sessionKey = "";
		this._sessionExpirationDate = DateTimeUtilities.getTimeFromUnixTime(0);
		this._state = State.NO_CREDENTIALS;
		saveState();
	}
	
	public String getSessionKey() {
		verifyOnMainUIThread();
		return _sessionKey;
	}
	
	public String getUsername() {
		verifyOnMainUIThread();
		return _username;
	}

	private void verifyOnMainUIThread() {
		if (Thread.currentThread() != _context.getMainLooper().getThread()) {
			throw new CalledFromWrongThreadException();
		}
	}
	
}
