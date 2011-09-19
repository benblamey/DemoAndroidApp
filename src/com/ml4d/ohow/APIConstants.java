package com.ml4d.ohow;

/**
 * Various constants defined by the OHOW API.
 *
 * This file should contain appropriate constants defined in the PHP file 'core.php' (and any others that are necessary).
 * Remember that anything here may be readable by an attacker, so don't include access keys/tokens etc. unless necessary.
 */
public class APIConstants
{
	public static final int SHA1_HASH_LENGTH = 40;
	public static final int SESSION_KEY_LENGTH = SHA1_HASH_LENGTH;

	// Unfortunately, Java is lacking a verbatim string operators (like '@' in C#), so these strings are in a sense double-escaped - edit with caution!
	public static final String USERNAME_REGEX = "^[a-zA-Z0-9-_\\.]{3,20}$";
	public static final String EMAIL_REGEX = "^[a-zA-Z0-9!#$%&\'*+/=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&\'*+/=?^_`{|}~-]+)*@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?$";
	
	// The maximum and minimum lengths of some of the fields used in registration.
	public static final int FIRST_NAME_MAX_LENGTH = 255;
	public static final int LAST_NAME_MAX_LENGTH = 255;
	public static final int EMAIL_MAX_LENGTH = 255;

	public static final int FIRST_NAME_MIN_LENGTH = 1;
	public static final int LAST_NAME_MIN_LENGTH = 1;

	public static final int PASSWORD_MAX_LENGTH = 255;
	public static final int PASSWORD_MIN_LENGTH = 6;
	
	// Constants relating to the body of a capture.
	// A body text should fail validation if it *MATCHES* this regex (the others pass validation if they match).
	public static final String CAPTURE_BODY_FAIL_REGEX = "[\\n|\\r]";
	public static final int CAPTURE_BODY_MIN_LENGTH = 3;
	public static final int CAPTURE_BODY_MAX_LENGTH = 9999;
	
	// Photos.
	public static final int PHOTO_SIZE_MAX_BYTES = 5 * 1024 * 1024;
}
