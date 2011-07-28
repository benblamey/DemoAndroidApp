package com.ml4d.ohow;

import com.ml4d.ohow.exceptions.CalledFromWrongThreadException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * Stores credentials for the OHOW API.
 */
public class CredentialStore {

	private Context _context;
	private SharedPreferences _preferences;
	private boolean _haveVerifiedCredentials;
	private String _username;
	private String _password;

	public CredentialStore(Context context) {
		
		_context = context;
		_preferences = context.getSharedPreferences("APIAuthentication", Context.MODE_PRIVATE);
		_haveVerifiedCredentials = _preferences.getBoolean("_haveVerifiedCredentials", false);

		if (_haveVerifiedCredentials) {
			_username = _preferences.getString("_username", "");
			_password = CryptUtility.decrypt(_preferences.getString("_password", ""));
		}
	}
	
	private void saveState() {
		Editor editor = _preferences.edit();
		editor.putBoolean("_haveVerifiedCredentials", _haveVerifiedCredentials);
		
		if (_haveVerifiedCredentials) {
			editor.putString("_username", _username);
			editor.putString("_password", CryptUtility.encrypt(_password));
		}
		editor.commit();
	}
	
	/**
	 * Stores a set of credentials that have been verified as being correct by the OHOW server.
	 * @param username
	 * @param password
	 */
	public void setKnownGoodDetails(String username, String password) {
		verifyOnMainUIThread();
		_haveVerifiedCredentials = true;
		_username = username;
		_password = password;
		saveState();
	}
	
	/**
	 * Clear all saved credentials. Should be called when the OHOW API indicates the credentials are invalid.
	 */
	public void Clear() {
		verifyOnMainUIThread();
		this._haveVerifiedCredentials = false;
		this._username = "";
		this._password = "";
		saveState();
	}
	
	/**
	 * Are credentials stored by this class (there is an assumption that if credentials have been stored, they were verifed as being valid at some point).
	 * @return
	 */
	public boolean getHaveVerifiedCredentials() {
		return _haveVerifiedCredentials;
	}
	
	public String getUsername() {
		verifyOnMainUIThread();
		return _username;
	}
	
	public String getPassword() {
		verifyOnMainUIThread();
		return _password;
	}

	private void verifyOnMainUIThread() {
		if (Thread.currentThread() != _context.getMainLooper().getThread()) {
			throw new CalledFromWrongThreadException();
		}
	}
	
}
