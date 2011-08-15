package com.ml4d.ohow;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;

import com.ml4d.core.exceptions.CalledFromWrongThreadException;
import com.ml4d.core.exceptions.ImprobableCheckedExceptionException;
import com.ml4d.core.google_import.Base64;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * Stores a list of IDs of recently captured moments.
 * This allows the UI to prevent the user from re-capturing a previously captured moment by navigating back in history.
 */
public class CapturedMoments {

	private static CapturedMoments _instance;
	private Context _context;
	private SharedPreferences _preferences;
	private ArrayList<String> _capturedMomentIds;
	private static final int _maximumCapturedMomentIdsToKeep = 50;
	private static final int _version = 1;
	
	/**
	 * Gets the single instance of this class (creating one if necessary).
	 * @param activity 
	 * @return
	 */
	public static CapturedMoments getInstance(Activity activity)
	{
		// This method may only be called from the main looper thread.
		if (Thread.currentThread() != activity.getMainLooper().getThread()) {
			throw new CalledFromWrongThreadException();
		}
	
		// We have ensured we are running on the UI thread, so there is no need for locking here. 
		if (null == _instance) {
			// Note that to prevent leaking a reference to an activity, we use the application context to manipulate the preferences.
			_instance = new CapturedMoments(activity.getApplicationContext());
		}
		return _instance;
	}
	
	/**
	 * Record that this particular moment has been captured.
	 * @param captureUniqueId
	 */
	public void momentHasBeenCaptured(String captureUniqueId) {
		verifyOnMainUIThread();
		// Adds the ID to the end of the array.
		_capturedMomentIds.add(captureUniqueId);
		saveState();
	}
	
	/**
	 * Get whether the moment with the given unique ID has been captured.
	 * @param captureUniqueId
	 * @return
	 */
	public boolean hasMomentBeenCapturedRecently(String captureUniqueId) {
		verifyOnMainUIThread();
		for (String id : _capturedMomentIds) {
			if (id.equals(captureUniqueId)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Class is single instance, we do not allow direct instantiation.
	 * @param context
	 */
	@SuppressWarnings("unchecked")
	private CapturedMoments(Context context) {
		_context = context;
		_preferences = context.getSharedPreferences("CredentialStore", Context.MODE_PRIVATE);
		
		String capturedMomentIdsSerialized = _preferences.getString("_capturedMomentIds", "");
		
		if ((null == capturedMomentIdsSerialized) || (0 == capturedMomentIdsSerialized.length())) {
			_capturedMomentIds = new ArrayList<String>();
		} else {
			byte[] data = Base64.decode(capturedMomentIdsSerialized, Base64.DEFAULT);
			ByteArrayInputStream s = new ByteArrayInputStream(data);
			try {
				ObjectInputStream foo = new ObjectInputStream(s);
				_capturedMomentIds = (ArrayList<String>)foo.readObject();
			} catch (StreamCorruptedException e) {
				throw new ImprobableCheckedExceptionException(e);
			} catch (IOException e) {
				throw new ImprobableCheckedExceptionException(e);
			} catch (ClassNotFoundException e) {
				throw new ImprobableCheckedExceptionException(e);
			}
		}
	}
	
	private void saveState() {
		// Remove the older moment IDs from the start of the sting.
		while (_capturedMomentIds.size() > _maximumCapturedMomentIdsToKeep) {
			_capturedMomentIds.remove(0);
		}
		
		byte[] capturedMomentData = null;
        ByteArrayOutputStream capturedMomentOutputStream = new ByteArrayOutputStream();
        try {
        	ObjectOutputStream oos = new ObjectOutputStream(capturedMomentOutputStream);
        	oos.writeObject(_capturedMomentIds);
        	// We don't bother to close() the ObjectOutputStream - we just close the underlying stream.
            capturedMomentData = capturedMomentOutputStream.toByteArray();
        } catch (IOException e) {
        	throw new ImprobableCheckedExceptionException(e);
		} finally {
            try {
				capturedMomentOutputStream.close();
			} catch (IOException e) {
				throw new ImprobableCheckedExceptionException(e);
			}
        }
		
		Editor editor = _preferences.edit();
		editor.putString("_capturedMomentIds", Base64.encodeToString(capturedMomentData, Base64.DEFAULT));
		editor.putInt("version", _version);
		editor.commit();
	}
	
	private void verifyOnMainUIThread() {
		if (Thread.currentThread() != _context.getMainLooper().getThread()) {
			throw new CalledFromWrongThreadException();
		}
	}
	
}
