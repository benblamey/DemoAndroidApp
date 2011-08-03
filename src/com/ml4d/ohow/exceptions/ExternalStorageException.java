package com.ml4d.ohow.exceptions;

import java.io.IOException;

/**
 * Some kind of problem with the external storage.
 */
public class ExternalStorageException extends IOException {

	/**
	 * The version used when serializing/deserializing instances of this exception.
	 */
	private static final long serialVersionUID = 1L;
	
	private String _description;
	
	public ExternalStorageException(String description) {
		super();
		_description = description;
	}
	
	@Override
	public String getLocalizedMessage() {
		return _description;
	}
}
