package com.ml4d.ohow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ml4d.ohow.exceptions.*;

import android.content.res.Resources;

/**
 * Things relating to the Google API.
 */
public class GooglePlacesAPI {
	
	// The types of places that are suitable for a capture. This field is populated at run-time.
	private static String _capturePlaceTypes = null;
	
	// Various response strings. 
	
	/**
	 * "indicates that no errors occurred; the place was successfully detected and at least one result was returned."
	 */
	private static final String GOOGLE_API_STATUS_OK = "OK";
	
	/**
	 * "indicates that the search was successful but returned no results. This may occur if the search was passed a latlng in a remote location."
	 */
	private static final String GOOGLE_API_STATUS_ZERO_RESULTS = "ZERO_RESULTS";
	
	/**
	 * "indicates that you are over your quota."
	 */
	@SuppressWarnings("unused")
	private static final String GOOGLE_API_STATUS_OVER_QUERY_LIMIT = "OVER_QUERY_LIMIT";
	
	/**
	 * "indicates that your request was denied, generally because of lack of a sensor parameter."
	 */
	@SuppressWarnings("unused")
	private static final String GOOGLE_API_STATUS_REQUEST_DENIED = "REQUEST_DENIED";
	
	/**
	 * "generally indicates that a required query parameter (location or radius) is missing."
	 */
	@SuppressWarnings("unused")
	private static final String GOOGLE_API_STATUS_INVALID_REQUEST = "INVALID_REQUEST";
	
	/**
	 * Process a JSON response from the google places API.
	 * @return a collection of 'LocationForCapture' objects.
	 * @throws NoResponseAPIException
	 * @throws ApiViaHttpException
	 */
	public static Collection<LocationForCapture> ProcessJSONResponse(HttpResponse response, Resources resources) throws NoResponseAPIException,
			ApiViaHttpException {
		
		if (null == response) {
			throw new NoResponseAPIException();
		} else {
			int statusCode = response.getStatusLine().getStatusCode();

			HttpEntity entity = response.getEntity();

			if (null == entity) {
				throw new NoResponseAPIException();
			}

			String content;
			try {
				content = EntityUtils.toString(entity);
			} catch (ParseException e) {
				throw new NoResponseAPIException();
			} catch (IOException e) {
				throw new NoResponseAPIException();
			}

			JSONObject responseJson;
			 
			String status = "";
			ArrayList<LocationForCapture> locations = new ArrayList<LocationForCapture>();
			
			try {
				
				JSONArray result;
				
				// These accessors throw a JSONException if the particular JSON property doesn't exist.
				responseJson = new JSONObject(content);
				status = responseJson.getString("status");
				result = responseJson.getJSONArray("results"); 
				
				if (GOOGLE_API_STATUS_ZERO_RESULTS.equals(status)) {
					// Return an empty set.
					return locations;
				}
				
				for (int i=0; i<result.length(); i++) {
					// Throws a JSONException if the object in the array is not a JSON object.
					locations.add(new LocationForCapture(result.getJSONObject(i)));
				}
								
			} catch (JSONException e) {
				String friendlyErrorMessage = response.getStatusLine().getReasonPhrase();
				throw new ApiViaHttpException(friendlyErrorMessage, statusCode, e);
			}

			if ((200 != statusCode) || !status.equals(GOOGLE_API_STATUS_OK)) {
				// Something went wrong with the request.

				// Due to the nature of the request, getting an error from the Google API is unlikely - 
				// we do not provide localized strings for errors.
				String exceptionMessage;

				// If we don't have a friendly message for the error, use the
				// one returned by the API.
				if (0 < status.length()) {
					// Use a prefix to make the text seem a bit more friendly.
					exceptionMessage = resources.getString(R.string.unfriendly_error_prefix) + " " + status;
				} else { 
					// If there wasn't a message returned by the API, it means the
					// response wasn't in JSON etc.
					// Fall back to the reason phrase in the header.
					exceptionMessage = resources.getString(R.string.unfriendly_error_prefix) + " " + response.getStatusLine().getReasonPhrase();
				}

				throw new ApiViaHttpException(exceptionMessage, statusCode);
			} else {				
				return locations;
			}
		}
	}
	
	/**
	 * Gets a string of the form "type1|type2|type3" for all the types that are suitable for use in a capture.
	 */
	public static String getCapturePlaceTypeS() {
		// We don't assign the string until the end, this makes the method thread-safe.
		if (null == _capturePlaceTypes) {
			String capturePlaceTypes = "";
			for (int i = 0; i < _capturePlaceTypesArray.length; i++) {
				capturePlaceTypes = capturePlaceTypes + "|" + _capturePlaceTypesArray[i]; 
			}
			// Drop the leading '|'.
			_capturePlaceTypes = capturePlaceTypes.substring(1);
		}
		return _capturePlaceTypes;
	}
	
	// Normally, fields are placed at the top of a class. This one is at the bottom 
	// to aid the readability of the rest of the class.
	/** 
	 * The following list is the supported types for Place Searches and Place adds.
	 * http://code.google.com/apis/maps/documentation/places/supported_types.html
	 * Note that some unsuitable types have been commented out: 
	 */ 
	private static final String[] _capturePlaceTypesArray = new String[] {
		"accounting",
		"airport",
		"amusement_park",
		"aquarium",
		"art_gallery",
		"atm",
		"bakery",
		"bank",
		"bar",
		"beauty_salon",
		"bicycle_store",
		"book_store",
		"bowling_alley",
		"bus_station",
		"cafe",
		"campground",
		"car_dealer",
		"car_rental",
		"car_repair",
		"car_wash",
		"casino",
		"cemetery",
		"church",
		"city_hall",
		"clothing_store",
		"convenience_store",
		"courthouse",
		"dentist",
		"department_store",
		"doctor",
		"electrician",
		"electronics_store",
		"embassy",
		"establishment",
		"finance",
		"fire_station",
		"florist",
		"food",
		"funeral_home",
		"furniture_store",
		"gas_station",
		"general_contractor",
		"geocode",
		"grocery_or_supermarket",
		"gym",
		"hair_care",
		"hardware_store",
		"health",
		"hindu_temple",
		"home_goods_store",
		"hospital",
		"insurance_agency",
		"jewelry_store",
		"laundry",
		"lawyer",
		"library",
		"liquor_store",
		"local_government_office",
		"locksmith",
		"lodging",
		"meal_delivery",
		"meal_takeaway",
		"mosque",
		"movie_rental",
		"movie_theater",
		"moving_company",
		"museum",
		"night_club",
		"painter",
		"park",
		"parking",
		"pet_store",
		"pharmacy",
		"physiotherapist",
		"place_of_worship",
		"plumber",
		"police",
		"post_office",
		"real_estate_agency",
		"restaurant",
		"roofing_contractor",
		"rv_park",
		"school",
		"shoe_store",
		"shopping_mall",
		"spa",
		"stadium",
		"storage",
		"store",
		"subway_station",
		"synagogue",
		"taxi_stand",
		"train_station",
		"travel_agency",
		"university",
		"veterinary_care",
		"zoo",

		// The following table lists types supported by the Places API when sending Place Search requests. These types cannot be used when adding a new Place.
		"administrative_area_level_1",
		"administrative_area_level_2",
		"administrative_area_level_3",
		"colloquial_area",
		"country",
		"floor",
		"intersection",
		//"locality" // (This type generally covers to large a geographical area to be useful in OHOW).
		"natural_feature",
		"neighborhood",
		//"political",  // (This type generally covers to large a geographical area to be useful in OHOW).
		"point_of_interest",
		"post_box",
		"postal_code",
		"postal_code_prefix",
		"postal_town",
		"premise",
		"room",
		"route",
		"street_address",
		"street_number",
		// "sublocality", 
		"sublocality_level_4",
		"sublocality_level_5",
		"sublocality_level_3",
		//"sublocality_level_2", // (This type generally covers to large a geographical area to be useful in OHOW).
		//"sublocality_level_1", // (This type generally covers to large a geographical area to be useful in OHOW).
		"subpremise",
		"transit_station"
	};

}
