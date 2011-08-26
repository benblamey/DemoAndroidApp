package com.ml4d.ohow;

/**
 * Various constants defined by the OHOW API.
 *
 * This file should contain appropriate constants defined in the PHP file 'core.php' (and any others that are necessary).
 * Remember that anything here may be readable by an attacker, so don't include access keys/tokens etc. unless necessary.
 */
public class APIConstants
{
	public static final int sha1HashLength = 40;
	public static final int sessionKeyLength = sha1HashLength;

	// Unfortunately, Java is lacking a verbatim string operators (like '@' in C#), so these strings are in a sense double-escaped - edit with caution!
	public static final String usernameRegex = "^[a-zA-Z0-9-_\\.]{3,20}$";
	public static final String emailRegex = "^[a-zA-Z0-9!#$%&\'*+/=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&\'*+/=?^_`{|}~-]+)*@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?$";
	
	// The maximum and minimum lengths of some of the fields used in registration.
	public static final int firstNameMaxLength = 255;
	public static final int lastNameMaxLength = 255;
	public static final int emailMaxLength = 255;

	public static final int firstNameMinLength = 1;
	public static final int lastNameMinLength = 1;

	public static final int passwordMaxLength = 255;
	public static final int passwordMinLength = 6;
	
	// Constants relating to the body of a capture.
	// A body text should fail validation if it *MATCHES* this regex (the others pass validation if they match).
	public static final String captureBodyFailRegex = "[\\n|\\r]";
	public static final int captureBodyMinLength = 3;
	public static final int captureBodyMaxLength = 9999;
	
	// Photos.
	public static final int photoSizeMaxBytes = 5 * 1024 * 1024;
}
