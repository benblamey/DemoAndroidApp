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
public class SlideShow extends Activity implements OnClickListener {

	/**
	 * The zero-based index of the slide being shown.
	 */
	private int _slideNumber;
	private Intent _intentWhenFinished;
	
	/**
	 * The key that *MUST* be used when starting the intent to indicate which intent
	 * the user should be sent to when the slide show is finished.
	 */
	public static final String CALLBACK_INTENT_EXTRA_KEY = "intent_when_finished";
	
	private static final String SLIDE_NUMBER_INTENT_EXTRA_KEY = "slide_number";
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.slide_show);
		
		// Note that the image inside the ImageView gets persisted for us.
		
		Intent intent = getIntent();
		_slideNumber = intent.getIntExtra(SLIDE_NUMBER_INTENT_EXTRA_KEY, 0); // Show the first slide by default.
		_intentWhenFinished = (Intent)intent.getParcelableExtra(CALLBACK_INTENT_EXTRA_KEY);
		
		if (null == _intentWhenFinished) {
			throw new RuntimeException("When starting this activity, one must specify the activity to go to when finished. See: CALLBACK_INTENT_EXTRA_KEY");
		}
		
		findViewById(R.id.slide_show_root_view).setOnClickListener(this);
		ImageView imageView = (ImageView)findViewById(R.id.slide_show_image_view);
		Resources resources = getResources();
		
		int id = -1;
		try {
			id = R.drawable.class.getField("slideshow_" + Integer.toString(_slideNumber)).getInt(null);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			throw new ImprobableCheckedExceptionException(e);
		} catch (SecurityException e) {
			e.printStackTrace();
			throw new ImprobableCheckedExceptionException(e);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			throw new ImprobableCheckedExceptionException(e);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
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
		
		Intent intent;
		if (thereIsAnotherSlide) {
			intent = new Intent(this, SlideShow.class);
			intent.putExtra(CALLBACK_INTENT_EXTRA_KEY, _intentWhenFinished);
			intent.putExtra(SLIDE_NUMBER_INTENT_EXTRA_KEY, _slideNumber + 1); 
		} else {
			intent = _intentWhenFinished;
		}
		
		startActivity(intent);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(SLIDE_NUMBER_INTENT_EXTRA_KEY, _slideNumber);
		outState.putParcelable(CALLBACK_INTENT_EXTRA_KEY, _intentWhenFinished);
		// Note that the image inside the ImageView gets persisted for us.
	}
	
}