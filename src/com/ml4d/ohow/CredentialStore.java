package com.ml4d.ohow;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * Stores credentials for the OHOW API. Single instance.
 * Lifetime is tied to the process. 
 */
public class CredentialStore {

	private static CredentialStore _instance;
	private boolean _haveVerifiedCredentials;
	private String _username;
	private String _password;

	/**
	 * Class is single instance, we do not allow direct instantiation.
	 * @param context
	 */
	private CredentialStore() {
		SharedPreferences preferences = App.Instance.getSharedPreferences("CredentialStore", Context.MODE_PRIVATE);
		_haveVerifiedCredentials = preferences.getBoolean("_haveVerifiedCredentials", false);

		if (_haveVerifiedCredentials) {
			_username = preferences.getString("_username", "");
			_password = CryptUtility.decrypt(preferences.getString("_password", ""));
		}
	}
	
	/**
	 * Gets the single instance of this class (creating one if necessary).
	 * @param activity 
	 * @return
	 */
	public synchronized static CredentialStore getInstance()
	{
		// We have ensured we are running on the UI thread, so there is no need for locking here. 
		if (null == _instance) {
			// Note that to prevent leaking a reference to an activity, we use the application context to manipulate the preferences.
			_instance = new CredentialStore();
		}
		return _instance;
	}
	
	private void saveState() {
		SharedPreferences preferences = App.Instance.getSharedPreferences("CredentialStore", Context.MODE_PRIVATE);
		Editor editor = preferences.edit();
		editor.putBoolean("_haveVerifiedCredentials", _haveVerifiedCredentials);
		editor.putInt("version", 1); // A version might be useful in the future.
		
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
	public synchronized void setKnownGoodDetails(String username, String password) {
		_haveVerifiedCredentials = true;
		_username = username;
		_password = password;
		saveState();
	}
	
	/**
	 * Clear all saved credentials. Should be called when the OHOW API indicates the credentials are invalid.
	 */
	public synchronized void clear() {
		this._haveVerifiedCredentials = false;
		this._username = "";
		this._password = "";
		saveState();
	}
	
	/**
	 * Are credentials stored by this class (there is an assumption that if credentials have been stored, they were verifed as being valid at some point).
	 * @return
	 */
	public synchronized boolean getHaveVerifiedCredentials() {
		return _haveVerifiedCredentials;
	}
	
	public synchronized String getUsername() {
		return _username;
	}
	
	public synchronized String getPassword() {
		return _password;
	}	
}
