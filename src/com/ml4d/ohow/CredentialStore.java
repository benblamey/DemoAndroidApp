package com.ml4d.ohow;

import com.ml4d.ohow.exceptions.CalledFromWrongThreadException;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * Stores credentials for the OHOW API. Single instance.
 */
public class CredentialStore {

	private static CredentialStore _instance;
	private Context _context;
	private SharedPreferences _preferences;
	private boolean _haveVerifiedCredentials;
	private String _username;
	private String _password;

	private CredentialStore(Context context) {
		
		_context = context;
		_preferences = context.getSharedPreferences("APIAuthentication", Context.MODE_PRIVATE);
		_haveVerifiedCredentials = _preferences.getBoolean("_haveVerifiedCredentials", false);

		if (_haveVerifiedCredentials) {
			_username = _preferences.getString("_username", "");
			_password = CryptUtility.decrypt(_preferences.getString("_password", ""));
		}
	}
	
	/**
	 * Gets the single instance of this class.
	 * @param activity 
	 * @return
	 */
	public static CredentialStore getInstance(Activity activity)
	{
		// This method may only be called from the main looper thread.
		if (Thread.currentThread() != activity.getMainLooper().getThread()) {
			throw new CalledFromWrongThreadException();
		}
	
		// We have ensured we are running on the UI thread, so there is no need for locking here. 
		if (null == _instance) {
			_instance = new CredentialStore(activity.getApplicationContext());
		}
		return _instance;
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
	public void clear() {
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
