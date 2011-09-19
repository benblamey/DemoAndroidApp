package com.ml4d.core;

import java.io.IOException;

import android.os.Parcel;

/**
 * Various utility methods for Parcel.
 */
public class Parcel2 {

	// We use more unlikely magic numbers to reduce the probability of misinterpreting data
	// that wasn't originally written by the 'writeBoolean()' helper method.
	private static final int TRUE_VALUE = 1234;
	private static final int FALSE_VALUE = -4321;
	
	public static void writeBoolean(boolean value, Parcel parcel) {
		parcel.writeInt(value ? TRUE_VALUE : FALSE_VALUE);
	}
	
	public static boolean readBoolean(Parcel parcel) throws IOException {
		
		int value = parcel.readInt();
		boolean result;
		
		if (TRUE_VALUE == value) {
			result = true;
		} else if (FALSE_VALUE == value) {
			result = false;
		} else {
			throw new IOException("A boolean was not stored in the class at this position.");
		}
		
		return result;
	}
	
	public void writeBoolean(Parcel parcel, boolean value) {	
		if (value) {
			parcel.writeInt(TRUE_VALUE);
		} else {
			parcel.writeInt(FALSE_VALUE);
		}
	}

}