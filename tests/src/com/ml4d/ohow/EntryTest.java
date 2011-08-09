package com.ml4d.ohow;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;

import junit.framework.TestCase;

public class EntryTest extends TestCase {

	private static final String _exampleJson = 
		  "      {"
		+ "         \"id\":157,"
		+ "         \"username\":\"benb\","
		+ "         \"latitude\":51.453956604,"
		+ "         \"longitude\":-2.59948801994,"
		+ "         \"location_name\":\"Start The Bus\","
		+ "         \"body\":\"fffjdd\","
		+ "         \"date_created_utc\":\"2011-08-09 15:17:25\","
		+ "         \"google_location_retrieval_ref\":\"CnRkAAAA1UOwHJEWG0PuZT7hTTpJKNa0lI2FsuvackvzyFJSfyRNRj0zH8jSAMGBN_cp2dJyDkogdWpgqxMkCfybCnCt6I9gEoJiicZoYNomWFoIEqTAj6DU6Sx_JaPSnBm1VtAArTU1DwXoOTHcNY58Ag9BGRIQvo2uW7A1BdOxmLaz-JwWnhoUc4oRxqRS5GP-eVveQfDaj5Fxods\","
		+ "         \"google_location_stable_ref\":\"1cad10ae13131df22bf54d493e2538461793549c\","
		+ "         \"has_photo\":true"
		+ "      }";
	
	
	public void test1() throws JSONException {
		
		JSONObject entryJson = new JSONObject(_exampleJson);

		Entry entry = new Entry(entryJson);
		
		assertEquals(157, entry.getId());
		assertEquals("benb", entry.getUsername());
		assertEquals(0, Double.compare(51.453956604, entry.getLatitude()));
		assertEquals(0, Double.compare(-2.59948801994, entry.getLongitude()));
		assertEquals("Start The Bus", entry.getLocationName());
		assertEquals("fffjdd", entry.getBody());
		
		// Compare the entry creation time. Note that everything is in UTC - not local time.
		Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC")); 
		cal.set(2011, Calendar.AUGUST, 9, 15, 17, 25); // Note that month and hour are zero-based.
		cal.set(Calendar.MILLISECOND, 0);
		assertEquals(0, cal.getTime().compareTo(entry.getDateCreatedUTC()));


		assertEquals("CnRkAAAA1UOwHJEWG0PuZT7hTTpJKNa0lI2FsuvackvzyFJSfyRNRj0zH8jSAMGBN_cp2dJyDkogdWpgqxMkCfybCnCt6I9gEoJiicZoYNomWFoIEqTAj6DU6Sx_JaPSnBm1VtAArTU1DwXoOTHcNY58Ag9BGRIQvo2uW7A1BdOxmLaz-JwWnhoUc4oRxqRS5GP-eVveQfDaj5Fxods",
				entry.getGoogleLocationRetrievalRef());
		
		assertEquals("1cad10ae13131df22bf54d493e2538461793549c",
				entry.getGoogleLocationStableRef());
		
		assertEquals(true, entryJson.getBoolean("has_photo"));
	}
	
}
