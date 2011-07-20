package com.ml4d.ohow.exceptions;

/*
 * Used when a 'switch' statement (or equivalently a series of if/else if/.. statements does not handle a case
 * for a particular enum value. A switch statement should always account for all enum values, this exception indicates
 * programmer error and should not be caught under normal circumstances.
 */
public class UnexpectedEnumValueException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	@SuppressWarnings("rawtypes")
	private Enum _unexpectedValue;

	@SuppressWarnings("rawtypes")
	public UnexpectedEnumValueException(Enum unexpectedValue) {
		super(); // call superclass constructor
		_unexpectedValue = unexpectedValue;
	}
	
	public String getMessage()
	{
		return super.getMessage() + " Unexpected enum value: " + _unexpectedValue.toString();
	}

}