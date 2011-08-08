package com.ml4d.ohow.exceptions;

/*
 * Represents any kind of error that occurs when a response is received from a remote API that is accessed via HTTP.
 * If a response is not received for any reason, do not use this exception. Consider using the NoResponseAPIException.
 */
public class ApiViaHttpException extends Exception {

	private String _description;
	private int _httpCode = -1;
	private int _exceptionCode = -1; // This is a detailed error code. OHOW provides this.

	/**
	 * The version used when serializing/deserializing instances of this exception.
	 */
	private static final long serialVersionUID = 1L;

	public ApiViaHttpException(String description, int httpCode) {
		super();
		_description = description;
		_httpCode = httpCode;
	}

	public ApiViaHttpException(String description, int httpCode, int exceptionCode) {
		super();
		_description = description;
		_httpCode = httpCode;
		_exceptionCode = exceptionCode;
	}
	
	public ApiViaHttpException(String description, int httpCode, Exception innerException) {
		super(innerException);
		_description = description;
		_httpCode = httpCode;
	}

	public ApiViaHttpException(String description, int httpCode, int exceptionCode, Exception innerException) {
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
