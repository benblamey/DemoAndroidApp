package com.ml4d.core;

import com.ml4d.ohow.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Button;

/**
 * A custom button that allows an external font to be specified.
 * @author ben
 *
 */
public class Button2 extends Button {

    public Button2(Context context) {
    	super(context);
    }

    public Button2(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public Button2(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }
	
    private void init(Context context, AttributeSet attrs, int defStyle) {
        TypedArray ta = context.obtainStyledAttributes( attrs, R.styleable.Button2, defStyle, 0 );
		String text = ta.getString( R.styleable.Button2_font_asset_name);
		
		if( text != null )
		{
	        Log.i("Button2", text);
	        
	        Typeface font = Typeface.createFromAsset(context.getAssets(), text);
	        super.setTypeface(font);
		}
		
		// We need to re-cycle the typed array.
		ta.recycle();
    }

}
