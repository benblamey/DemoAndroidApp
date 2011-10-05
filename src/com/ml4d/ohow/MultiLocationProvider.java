package com.ml4d.ohow;

import java.util.ArrayList;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

/**
 * Combines location updates from the network positioning service and GPS.
 */
public class MultiLocationProvider implements LocationListener {

	/**
	 * The single instance of this class.
	 */
	private static MultiLocationProvider _instance;
	
	/** 
	 * A hint for the GPS location update interval, in milliseconds.
	 */
	private static final int GPS_SUGGESTED_UPDATE_INTERVAL_MS = 10000;

	/**
	 * The minimum distance interval for update, in metres.
	 */
	private static final int GPS_SUGGESTED_UPDATE_INTERVAL_METRES = 1;
	
	/**
	 * The name of this provider (used in some of the callback functions).
	 */
	public static String PROVIDER_NAME = "MULTI_PROVIDER";
	
	private static final int TWO_MINUTES_IN_SECONDS = 1000 * 60 * 2;
	
	private ArrayList<LocationListener> _listeners = new ArrayList<LocationListener>();
	private Location _location;
	private Context _context;
	private boolean _isProviderEnabled;
	
	public synchronized static MultiLocationProvider getInstance() {
		if (null == _instance) {
			_instance = new MultiLocationProvider();
		}
		return _instance;
	}

	private MultiLocationProvider() {
		_context = App.Instance.getApplicationContext();
	}
	
	/**
	 * Adds a listener to this location provider.
	 * If this method is called more than once with the same parameter, calls other than
	 * the first will be silently ignored.
	 * @param listener
	 */
	public synchronized void addListener(LocationListener listener) {
		if (null == listener) {
			throw new IllegalArgumentException("'listener' cannot be null.");
		} else if (this == listener) {
			throw new IllegalArgumentException("The provider cannot listen to itself.");
		} else if (!_listeners.contains(listener)) {

			_listeners.add(listener);
			
			if (_listeners.size() == 1) {
				// Start listening to the underlying location providers.			
				
				// Get the most recent GPS fix (this might be null or out of date).
				LocationManager locationManager = (LocationManager)_context.getSystemService(Context.LOCATION_SERVICE);
		
				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,  
						GPS_SUGGESTED_UPDATE_INTERVAL_MS, 
						GPS_SUGGESTED_UPDATE_INTERVAL_METRES, this, App.Instance.getMainLooper());
				locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,  
						GPS_SUGGESTED_UPDATE_INTERVAL_MS, 
						GPS_SUGGESTED_UPDATE_INTERVAL_METRES, this, App.Instance.getMainLooper());
				
				updateLocation(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
				updateLocation(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
				
				notifyIsProviderEnabled();
			}

		}
		
	}
	
	/**
	 * Remove a listener from this location provider.
	 * An exception is not thrown if the item is not already subscribed.
	 * @param listener
	 */
	public synchronized void removeListener(LocationListener listener) {
		if (null == listener) {
			throw new IllegalArgumentException("'listener' cannot be null.");
		}
		
		// An exception is not thrown if the item is not in the list.
		_listeners.remove(listener);
		
		if (_listeners.isEmpty()) {
			LocationManager locationManager = (LocationManager)_context.getSystemService(Context.LOCATION_SERVICE);
			if (null != locationManager) {
				locationManager.removeUpdates(this);
			}
			
			notifyIsProviderEnabled();
		}
	}
	
	/**
	 * Gets whether this provider is enabled - 
	 * i.e. whether at least one of the underlying providers is enabled.
	 * @return
	 */
	public synchronized boolean getIsEnabled() {
		return _isProviderEnabled;
	}
	
	private void notifyIsProviderEnabled() {
		boolean oldEnabled = _isProviderEnabled;

		// Get the most recent GPS fix (this might be null or out of date).
		LocationManager locationManager = (LocationManager)_context.getSystemService(Context.LOCATION_SERVICE);

		// We consider ourselves enabled if at least one of the underlying providers are enabled.	
		_isProviderEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) || 
			locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		
		if (oldEnabled != _isProviderEnabled) {
			if (_isProviderEnabled) {
			    for (LocationListener listener : _listeners) {
			    	listener.onProviderEnabled(PROVIDER_NAME);
			    }
			} else {
			    for (LocationListener listener : _listeners) {
			    	listener.onProviderDisabled(PROVIDER_NAME);
			    }
			}
		}
	}
	
	@Override
	public synchronized void onLocationChanged(android.location.Location location) {
		updateLocation(location);
	}
	
	private void updateLocation(Location newLocation) {
		if (isBetterLocation(newLocation, _location)) {
			_location = newLocation;
		    for (LocationListener listener : _listeners) {
		    	listener.onLocationChanged(_location);
		    }
		}
	}
	
	public synchronized Location getLocation() {
		return _location;
	}
	
	/** Determines whether one Location reading is better than the current Location fix.
	  * @param location  The new Location that you want to evaluate
	  * @param currentBestLocation  The current Location fix, to which you want to compare the new one
	  */
	private static boolean isBetterLocation(Location location, Location currentBestLocation) {
		
		if (location == null) {
	        return false;
	    }
		else if (currentBestLocation == null) {
			// We use the new value if it is not significantly old. 
			// Both times in mS since 1/1/1970 UTC.
			return (System.currentTimeMillis() - location.getTime()) < TWO_MINUTES_IN_SECONDS;
	    }

	    // Check whether the new location fix is newer or older.
	    long timeDelta = location.getTime() - currentBestLocation.getTime();
	    boolean isSignificantlyNewer = timeDelta > TWO_MINUTES_IN_SECONDS;
	    boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES_IN_SECONDS;
	    boolean isNewer = timeDelta > 0;

	    // If it's been more than two minutes since the current location, use the new location
	    // because the user has likely moved.
	    if (isSignificantlyNewer) {
	        return true;
	    // If the new location is more than two minutes older, it must be worse.
	    } else if (isSignificantlyOlder) {
	        return false;
	    } else {

		    // Check whether the new location fix is more or less accurate.
		    int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
		    boolean isLessAccurate = accuracyDelta > 0;
		    boolean isMoreAccurate = accuracyDelta < 0;
		    boolean isSignificantlyLessAccurate = accuracyDelta > 200;
	
		    // Check if the old and new location are from the same provider.
		    boolean isFromSameProvider = isSameProvider(location.getProvider(),
		            currentBestLocation.getProvider());
	
		    // Determine location quality using a combination of timeliness and accuracy.
		    if (isMoreAccurate) {
		        return true;
		    } else if (isNewer && !isLessAccurate) {
		        return true;
		    } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
		        return true;
		    } else {
		    	return false;
		    }
	    }
	}

	/** 
	 * Checks whether two providers are the same.
	 * */
	private static boolean isSameProvider(String provider1, String provider2) {
	    if (provider1 == null) {
	      return provider2 == null;
	    }
	    return provider1.equals(provider2);
	}
	
	@Override
	public synchronized void onStatusChanged(String provider, int status, Bundle extras) {
		// Nothing to do.
	}

	@Override
	public synchronized void onProviderEnabled(String provider) {
		notifyIsProviderEnabled();
	}

	@Override
	public synchronized void onProviderDisabled(String provider) {
		notifyIsProviderEnabled();
	}
	
}
