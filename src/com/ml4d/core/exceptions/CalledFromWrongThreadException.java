package com.ml4d.core.exceptions;

/**
 * When a method is called from an inappropriate thread. Commonly thrown by objects that 
 * are not thread-safe, when they are called on a thread other than the thread that created them.
 *
 */
public class CalledFromWrongThreadException extends RuntimeException {

	/**
	 * The version used when serializing/deserializing instances of this exception.
	 */
	private static final long serialVersionUID = 1L;
	
	public CalledFromWrongThreadException() {
	}
}
