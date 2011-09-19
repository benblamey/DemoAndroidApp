package com.ml4d.ohow;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

import com.ml4d.core.JSONHelper;

/**
 * A location (excluding latitude and longitude) for an OHOW capture.
 * 
 * Usually generated from the results of a request to the Google Places API, see:
 * http://code.google.com/apis/maps/documentation/places/index.html
 */
public class LocationForCapture implements Serializable {

	/**
	 * The version used when serializing/deserializing instances of this exception.
	 */
	private static final long serialVersionUID = 1L;
	
	private String _locationName;
    private String _googleLocationRetrievalRef;
    private String _googleLocationStableRef;
    private boolean _isListed;
    private static final LocationForCapture UNLISTED_LOCATION = new LocationForCapture();  
	
    /**
     * Creates an instance based on JSON received from the Google Places API.
     * @param json
     * @throws JSONException
     */
	public LocationForCapture(JSONObject json) throws JSONException {
		// There are some issues with JSONObject.GetString() - we use our helper instead.
		_locationName = JSONHelper.getStringOrNull(json, "name");
		_googleLocationRetrievalRef = JSONHelper.getStringOrNull(json, "reference");
		_googleLocationStableRef = JSONHelper.getStringOrNull(json, "id");
		_isListed = true;
	}
	
	// Ctor for an unlisted location.
	private LocationForCapture() {
		_isListed = false;
	}
	
	/**
	 * Gets a dummy instance that can be used as a placeholder to indicate that the current place
	 * is not amongst the location results returned from, for example, the Google Places API.
	 * @return
	 */
	public static LocationForCapture getUnlisted() {
		return UNLISTED_LOCATION;
	}
	
	public String getLocationName() {
		return _locationName;
	}
	
	/**
	 * Gets a string that can be used at any time in the future to retrieve details of this place
	 * from the Google places API. Note that this identifier may be different for a given place between
	 * successive requests to the Google Places API. See google docs for details.
	 */
	public String getGoogleLocationRetrievalRef() {
		return _googleLocationRetrievalRef;
	}

	/**
	 * Gets a stable identifier from this place used by Google. Cannot be used to retrieve details about this place.
	 */
	public String getGoogleLocationStableRef() {
		return _googleLocationStableRef;
	}
	
	/**
	 * Gets whether this instance represents a listed place (i.e. a place
	 * listed in the results of, for example, a Google Places request) or an unlisted 'dummy' place.
	 * @return
	 */
	public boolean getIsListed() {
		return _isListed;
	}
}
