package com.ml4d.core;

/**
 * Various utility methods for strings.
 * @author ben
 *
 */
public class String2 {

	/**
	 * Compute whether two strings are equal. Comparison is case-sensitive,
	 * nulls are allowed.
	 */
	public static boolean areEqual(String s1, String s2) {
		
		boolean areEqual;
		if (null == s1) {
			areEqual = (null == s2); 
		} else {
			// S1 is non-null.
			if (null == s2) {
				areEqual = false;
			} else {
				areEqual = s1.equals(s2);
			}
		}
		
		return areEqual;
	}
}
