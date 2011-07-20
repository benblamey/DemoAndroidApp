package com.ml4d.ohow.exceptions;

/*
 * Used in circumstances where the developer does not wish to really catch a checked exception, but doesn't want
 * to annotate the method either.
 */
public class ImprobableCheckedExceptionException extends RuntimeException {

	/**
	 * The version used when serializing/deserializing instances of this exception.
	 */
	private static final long serialVersionUID = 1L;
	
	public ImprobableCheckedExceptionException(Exception ex)
	{
		super(ex);
	}

}
