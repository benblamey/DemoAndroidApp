package com.ml4d.ohow;

import java.nio.charset.Charset;

/**
 * Various utility methods relating to character sets.
 */
public class Charset2 {

	private static final Charset _utf8 = Charset.forName("UTF-8");
	
	/**
	 * Get a java.nio.charset.Charset object for the UTF-8 character set.
	 * @return
	 */
	public static Charset getUtf8() {
		return _utf8;
	}
}
