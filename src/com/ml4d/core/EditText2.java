package com.ml4d.core;

import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;

/**
 * An EditText control that forces the 'Done' action - there is a problem with 
 * multiline EditText controls (i.e. those for which text wrapping is enabled) that the 
 * 'return' action is always available. 
 */
public class EditText2 extends EditText {
	
	// See:
	// http://stackoverflow.com/questions/5014219/multiline-edittext-with-done-softinput-action-label-on-2-3
	
    public EditText2(Context context) {
        super(context);
    }

    public EditText2(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditText2(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
	
	@Override
	public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
	    InputConnection connection = super.onCreateInputConnection(outAttrs);
	    
	    int imeActions = outAttrs.imeOptions & EditorInfo.IME_MASK_ACTION;
	    
	    // We want to show 'Done' instead of the 'return' key.
	    if ((imeActions & EditorInfo.IME_ACTION_DONE) != 0) {
	    	
	        // Clear the existing action.
	        outAttrs.imeOptions ^= imeActions;
	        
	        // Set the DONE action.
	        outAttrs.imeOptions |= EditorInfo.IME_ACTION_DONE;
	    }
	    
	    
	    if ((outAttrs.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) {
	        outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NO_ENTER_ACTION;
	    }
	    
	    return connection;
	}


}
