package com.ml4d.ohow;

import java.util.Calendar;
import java.util.Date;

public class DateTimeUtilities {
	
	 public static Date getTimeFromUnixTime(int unixTimeStamp) {
			final Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(unixTimeStamp * 1000L); 
			return cal.getTime();
	 }
	 
	 public static Date getTimeFromUnixTimeMs(long unixTimeStampMs) {
			final Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(unixTimeStampMs); 
			return cal.getTime();
	 }

}
