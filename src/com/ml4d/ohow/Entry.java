/**
 * 
 */
package com.ml4d.ohow;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;

import com.ml4d.core.JSONHelper;

/**
 * An OHOW Entry, as received from the OHOW API.
 */
public class Entry implements Serializable {

// Example JSON:
//    {
//        "id":157,
//        "username":"benb",
//        "latitude":51.453956604,
//        "longitude":-2.59948801994,
//        "location_name":"Start The Bus",
//        "body":"fffjdd ",
//        "date_created_utc":"2011-08-09 15:17:25",
//        "google_location_retrieval_ref":"CnRkAAAA1UOwHJEWG0PuZT7hTTpJKNa0lI2FsuvackvzyFJSfyRNRj0zH8jSAMGBN_cp2dJyDkogdWpgqxMkCfybCnCt6I9gEoJiicZoYNomWFoIEqTAj6DU6Sx_JaPSnBm1VtAArTU1DwXoOTHcNY58Ag9BGRIQvo2uW7A1BdOxmLaz-JwWnhoUc4oRxqRS5GP-eVveQfDaj5Fxods",
//        "google_location_stable_ref":"1cad10ae13131df22bf54d493e2538461793549c",
//        "has_photo":0
//     }	
	
	/**
	 * The version used when serializing/deserializing instances of this exception.
	 */
	private static final long serialVersionUID = 1L;
	
	private int _id;
	private String _username;
	private double _latitude;
	private double _longitude;
	private String _locationName;
	private String _body;
	private Date _dateCreatedUTC;
    private String _googleLocationRetrievalRef;
    private String _googleLocationStableRef;
    private boolean _hasPhoto;
    private static final SimpleDateFormat _dateFormatterUTC;
    
	static {
		_dateFormatterUTC = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		_dateFormatterUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
    
    /**
     * Creates an instance based on JSON received from the Google Places API.
     * @param json
     * @throws JSONException
     */
	public Entry(JSONObject json) throws JSONException {

		// There are some issues with JSONObject.GetString() - we use our helper instead.
		_id = json.getInt("id");
		_username =  JSONHelper.getStringOrNull(json, "username");
		_latitude = json.getDouble("latitude");
		_longitude = json.getDouble("longitude");
		_locationName = JSONHelper.getStringOrNull(json, "location_name"); 
		_body = JSONHelper.getStringOrNull(json, "body");

		String dateCreatedUTCString = JSONHelper.getStringOrNull(json, "date_created_utc");
	    try {
			_dateCreatedUTC = (Date)_dateFormatterUTC.parse(dateCreatedUTCString);
		} catch (ParseException e) {
			throw new JSONException("Could not parse date: " + dateCreatedUTCString);
		}
		
		_googleLocationRetrievalRef = JSONHelper.getStringOrNull(json, "google_location_retrieval_ref");
		_googleLocationStableRef = JSONHelper.getStringOrNull(json, "google_location_stable_ref");
	}
	
	public int getId() {
		return _id;
	}
	
	public String getUsername() {
		return _username;
	}
	
	public double getLatitude() {
		return _latitude;
	}
	
	public double getLongitude() {
		return _longitude;
	}
	
	public String getLocationName() {
		return _locationName;
	}
	
	public String getBody() {
		return _body;
	}
	
	public Date getDateCreatedUTC() {
		return _dateCreatedUTC;
	}

	public String getGoogleLocationRetrievalRef() {
		return _googleLocationRetrievalRef;
	}

	public String getGoogleLocationStableRef() {
		return _googleLocationStableRef;
	}
	
	public boolean getHasPhoto() {
		return _hasPhoto;
	}
	
}
