package com.ml4d.ohow;

//import java.util.Date;

import com.ml4d.ohow.exceptions.CalledFromWrongThreadException;
//import com.ml4d.ohow.exceptions.UnexpectedEnumValueException;

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
	private boolean _haveVerifiedCredentials;
	//private State _state;
	//private Date _sessionExpirationDate;
	//private String _sessionKey;
	private String _username;
	private String _password;
	private Context _context;
	//private CryptUtility _crpyto = new CryptUtility();
	
//	private enum State {
//		NO_CREDENTIALS, 
//		HAVE_CREDENTIALS_AND_ASSUMED_VALID_SESSION_KEY,
//		HAVE_CREDENTIALS_AND_KNOWN_EXPIRED_SESSION_KEY,
//		HAVE_KNOWN_INVALID_CREDENTIALS,
//	}
	
	public APIAuthentication(Context context) {
		
		_context = context;
		
		_preferences = context.getSharedPreferences("APIAuthentication", Context.MODE_PRIVATE);
		_haveVerifiedCredentials = _preferences.getBoolean("_haveVerifiedCredentials", false);
		//_state = State.valueOf(State.class, _preferences.getString("_state", State.NO_CREDENTIALS.name()));
		
		if (_haveVerifiedCredentials) {
		
		_username = _preferences.getString("_username", "");
		
//	    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());        
//	    byte[] input = " www.java2s.com ".getBytes();
//	    byte[] keyBytes = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
//	        0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17 };
//
//	    SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
//	    Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding", "BC");
//	    System.out.println("input text : " + new String(input));

	    // encryption pass

	    //byte[] cipherText = new byte[input.length];
//	    cipher.init(Cipher.ENCRYPT_MODE, key);
//	    int ctLength = cipher.update(input, 0, input.length, cipherText, 0);
//	    ctLength += cipher.doFinal(cipherText, ctLength);
//	    System.out.println("cipher text: " + new String(cipherText) + " bytes: " + ctLength);
		
		String rawPassword = _preferences.getString("_password", "");
		Log.d("OHOW", rawPassword);
		
		_password = CryptUtility.decrypt(rawPassword);
		
		
		
		}
//		_sessionExpirationDate = DateTimeUtilities.getTimeFromUnixTimeMs(_preferences.getLong("_sessionExpirationDate", 0));					
//		_sessionKey = _preferences.getString("_sessionKey", "");
	}
	
	private void saveState() {
		Editor editor = _preferences.edit();
//		editor.putString("_state", _state.name());
//		editor.putString("_sessionKey", _sessionKey);
		editor.putBoolean("_haveVerifiedCredentials", _haveVerifiedCredentials);
		
		if (_haveVerifiedCredentials) {
			editor.putString("_username", _username);
			editor.putString("_password", CryptUtility.encrypt(_password));
	//		editor.putLong("_sessionExpirationDate", _sessionExpirationDate.getTime());
		}
		editor.commit();
	}
	
//	public boolean getWhetherCachedDataShouldBeDisplayed() {
//		verifyOnMainUIThread();
//		boolean shouldDisplay;
//		switch (_state) {
//		case NO_CREDENTIALS:
//		case HAVE_KNOWN_INVALID_CREDENTIALS:
//			shouldDisplay = false;
//			break;
//		case HAVE_CREDENTIALS_AND_ASSUMED_VALID_SESSION_KEY:
//		case HAVE_CREDENTIALS_AND_KNOWN_EXPIRED_SESSION_KEY:
//			shouldDisplay = true;
//			break;
//		default:
//			throw new UnexpectedEnumValueException(_state);
//		}
//		return shouldDisplay;
//	}
	
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
	public void setKnownGoodDetails(String username, String password) {
		verifyOnMainUIThread();
		_haveVerifiedCredentials = true;
		_username = username;
		_password = password;
//		this._sessionKey = sessionKey;
//		this._sessionExpirationDate = expirationTime;
//		this._state = State.HAVE_CREDENTIALS_AND_ASSUMED_VALID_SESSION_KEY;
		
		
		saveState();
	}
	
	public void Clear() {
		verifyOnMainUIThread();
		this._haveVerifiedCredentials = false;
		this._username = "";
		this._password = "";
//		this._sessionKey = "";
//		this._sessionExpirationDate = DateTimeUtilities.getTimeFromUnixTime(0);
//		this._state = State.NO_CREDENTIALS;
		saveState();
	}
	
//	public String getSessionKey() {
//		verifyOnMainUIThread();
//		return _sessionKey;
//	}
	
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
