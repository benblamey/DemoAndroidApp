package com.ml4d.ohow.exceptions;

/*
 * Occurs when the OHOW API returns an object successfully, but the result object is not as expected (e.g. it is missing properties, or is the wrong type etc.)
 */
public class UnexpectedOHOWAPIResponseException extends Exception {

	private String _description;

	/**
	 * The version used when serializing/deserializing instances of this exception.
	 */
	private static final long serialVersionUID = 1L;

	public UnexpectedOHOWAPIResponseException(String description) {
		super();
		_description = description;
	}
	
	public UnexpectedOHOWAPIResponseException(String description, Exception inner) {
		super(inner);
		_description = description;
	}

	public String getLocalizedMessage() {
		return _description;
	}

}
