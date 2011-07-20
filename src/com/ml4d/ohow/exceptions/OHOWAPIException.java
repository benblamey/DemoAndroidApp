package com.ml4d.ohow.exceptions;

/*
 * Represents any kind of error that occurs when a response is received from the OHOW API.
 * If a response is not received for any reason, do not use this exception. Consider using the NoResponseAPIException.
 */
public class OHOWAPIException extends Exception {

	private String _description;
	private int _httpCode = -1;
	private int _exceptionCode = -1;

	/**
	 * The version used when serializing/deserializing instances of this exception.
	 */
	private static final long serialVersionUID = 1L;

	public OHOWAPIException(String description, int httpCode) {
		super();
		_description = description;
		_httpCode = httpCode;
	}

	public OHOWAPIException(String description, int httpCode, int exceptionCode) {
		super();
		_description = description;
		_httpCode = httpCode;
		_exceptionCode = exceptionCode;
	}
	
	public OHOWAPIException(String description, int httpCode, Exception innerException) {
		super(innerException);
		_description = description;
		_httpCode = httpCode;
	}

	public OHOWAPIException(String description, int httpCode, int exceptionCode, Exception innerException) {
		super(innerException);
		_description = description;
		_httpCode = httpCode;
		_exceptionCode = exceptionCode;
	}

	public String getLocalizedMessage() {
		return _description;
	}

	public int getHttpCode() {
		return _httpCode;
	}

	public int getExceptionCode() {
		return _exceptionCode;
	}

}
