package com.ml4d.ohow.exceptions;

/**
 * When a switch statement is used to handle a 'OnClickListener.onClick' method, and the ID of the button is unknown.
 * For situations where the programmer should have handled all the cases of the buttons in the layout, i.e. a programmer error.
 */
public class UnknownClickableItemException extends RuntimeException {

	/**
	 * The version used when serializing/deserializing instances of this exception.
	 */
	private static final long serialVersionUID = 1L;
	private int _itemId;
	
	public UnknownClickableItemException(int itemId) {
		_itemId = itemId;
	}
	
	public int getItemId() {
		return _itemId;
	}
}
