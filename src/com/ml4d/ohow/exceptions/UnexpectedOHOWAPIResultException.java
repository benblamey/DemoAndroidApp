package com.ml4d.ohow.exceptions;

/*
 * Occurs when the OHOW API returns an object successfully, but the result object is not as expected (e.g. it is missing properties, or is the wrong type etc.)
 */
public class UnexpectedOHOWAPIResultException extends Exception {

	private String _description;

	/**
	 * The version used when serializing/deserializing instances of this exception.
	 */
	private static final long serialVersionUID = 1L;

	public UnexpectedOHOWAPIResultException(String description) {
		super();
		_description = description;
	}
	
	public UnexpectedOHOWAPIResultException(String description, Exception inner) {
		super(inner);
		_description = description;
	}

	public String getLocalizedMessage() {
		return _description;
	}

}
