package com.ml4d.ohow.activity;

import com.ml4d.core.exceptions.ImprobableCheckedExceptionException;
import com.ml4d.ohow.R;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

/**
 * Interactive logic for the slide show activity.
 */
public class SlideShowActivity extends Activity implements OnClickListener {

	/**
	 * The zero-based index of the slide being shown.
	 */
	private int _slideNumber;
	
	private static final String SLIDE_NUMBER_INTENT_EXTRA_KEY = "slide_number";
	
	/**
	 * The unique ID that identifies the intents that show the slides.
	 */
	private static final int showNextSlideRequestCode = 1234;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.slide_show);
		
		// Note that the image inside the ImageView gets persisted for us.
		
		Intent intent = getIntent();
		_slideNumber = intent.getIntExtra(SLIDE_NUMBER_INTENT_EXTRA_KEY, 0); // Show the first slide by default.

		findViewById(R.id.slide_show_root_view).setOnClickListener(this);
		ImageView imageView = (ImageView)findViewById(R.id.slide_show_image_view);
		Resources resources = getResources();
		
		int id = -1;
		try {
			id = R.drawable.class.getField("slideshow_" + Integer.toString(_slideNumber)).getInt(null);
		} catch (IllegalArgumentException e) {
			throw new ImprobableCheckedExceptionException(e);
		} catch (SecurityException e) {
			throw new ImprobableCheckedExceptionException(e);
		} catch (IllegalAccessException e) {
			throw new ImprobableCheckedExceptionException(e);
		} catch (NoSuchFieldException e) {
			throw new ImprobableCheckedExceptionException(e);
		}
		
		Drawable slideDrawable = (resources.getDrawable(id));
		imageView.setImageDrawable(slideDrawable);
	}
	
	@Override
	public void onClick(View v) {
		// Whatever has been clicked, we want to go to the next slide.
		
		boolean thereIsAnotherSlide;
		try {
			R.drawable.class.getField("slideshow_" + Integer.toString(_slideNumber + 1));
			thereIsAnotherSlide = true;
		} catch (NoSuchFieldException e) {
			thereIsAnotherSlide = false;
		}
		
		if (thereIsAnotherSlide) {
			Intent intent = new Intent(this, SlideShowActivity.class);
			intent.putExtra(SLIDE_NUMBER_INTENT_EXTRA_KEY, _slideNumber + 1);
			startActivityForResult(intent, showNextSlideRequestCode);
		} else {
			setResult(Activity.RESULT_OK);
			finish();
		}
	}
	
	protected void onActivityResult (int requestCode, int resultCode, Intent data) {
		if (requestCode == showNextSlideRequestCode) {
			if (RESULT_CANCELED == resultCode) {
				// Do nothing.
			} else if (RESULT_OK == resultCode) {
				setResult(RESULT_OK);
				finish();				
			} else {
				throw new RuntimeException("unexpected result code.");
			}
		}
	}
	
	
}