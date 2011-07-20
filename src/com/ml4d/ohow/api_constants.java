package com.ml4d.ohow;

/**
 * Constants defined by the OHOW API.
 *
 * This file should contain appropriate constants defined in the PHP file 'core.php'.
 * For simplicity and security, only include what is necessary for the app to work. Remember that anything here may be readable by an attacker.
 * @author ben@ml4d.com
 */
public class api_constants
{
	public static final int sha1_hash_length = 40;
	public static final int session_key_length = sha1_hash_length;

	// These values should match those in the database.
	public static final int username_max_length = 255;
	public static final int first_name_max_length = 255;
	public static final int last_name_max_length = 255;
	public static final int email_max_length = 255;

	// We require a minimal string length for the user's details, as a deterrent against junk data.
	public static final int username_min_length = 5;
	public static final int first_name_min_length = 1;
	public static final int last_name_min_length = 1;
	public static final int email_min_length = 5;
	
	// The Regex used to validate an email address, the same one as used by the API.
	public static final String email_regex = "{^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$}";

	// Passwords.
	public static final int password_max_length = 255;
	public static final int password_min_length = 5;

	// Entries.
	public static final int entry_location_name_max_length = 1024;
	public static final int entry_text_max_length = 9999;

	// Photos.
	public static final int photo_max_size_bytes = 5 * 1024 * 1024;
}
