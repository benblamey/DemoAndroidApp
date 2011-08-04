package com.ml4d.core;

import java.util.Calendar;
import java.util.Date;

/**
 * Various utility methods for the manipulation of dates and times.
 */
public class DateTimeUtilities {

	/**
	 * Gets a 'java.util.Date' from a UNIX timestamp (number of seconds since midnight 1/1/1970).
	 * @param unixTimeStamp
	 * @return
	 */
	public static Date getTimeFromUnixTime(int unixTimeStamp) {
		final Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(unixTimeStamp * 1000L);
		return cal.getTime();
	}

	/**
	 * Gets a 'java.util.Date' from a UNIX mS timestamp (number of milli-seconds since midnight 1/1/1970).
	 * @param unixTimeStamp
	 * @return
	 */
	public static Date getTimeFromUnixTimeMs(long unixTimeStampMs) {
		final Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(unixTimeStampMs);
		return cal.getTime();
	}

}
