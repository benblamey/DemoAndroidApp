package com.ml4d.core;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpProtocolParams;
import com.ml4d.core.exceptions.ImprobableCheckedExceptionException;
import com.ml4d.ohow.App;
import com.ml4d.ohow.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

/**
 * An ImageView that obtains its image asynchronously from the web.
 * Shows a placeholder image if no URL is set, or if the image could not be downloaded from the URL.
 */
public class WebImageView extends ImageView {
	
	private String _url;
	private GetImageTask _task;

    public WebImageView(Context context) {
        super(context);
        setImageResource(R.drawable.photo_placeholder);
    }
    
    public WebImageView(Context context, AttributeSet attrs) {
    	super(context, attrs);
    	setImageResource(R.drawable.photo_placeholder);
    }
    
    public WebImageView(Context context, AttributeSet attrs, int defStyle) {
    	super(context, attrs, defStyle);
    	setImageResource(R.drawable.photo_placeholder);
    }
    
    public void setUrl(String url) {
    	if (!String2.areEqual(_url, url)) {
    		_url = url;
    		
			// Cancel any task that is already running.
    		if (_task != null) {
    			_task.cancel(true);
    			_task = null;
    		}

	    	// If a different image is being shown, remove it.
	    	setImageBitmap(null);
	    	
	    	if ((null != url) && ("" != url)) {
	    		_task = new GetImageTask(this, url);
	    		_task.execute((Void[])null);
	    	} else {
	    		setImageResource(R.drawable.photo_placeholder);
	    	}
    	}
    }
    
    protected Parcelable onSaveInstanceState() {
    	WebImageViewState state = new WebImageViewState();
    	state.url = _url;
    	state.parentState = super.onSaveInstanceState();
    	state.isWaiting = (null != this._task);
    	
    	if (_task != null) {
	    	_task.cancel(true);
	    	_task = null;
    	}
    	return state;
    }
    
    protected void onRestoreInstanceState(Parcelable inState) {
    	WebImageViewState state = (WebImageViewState)inState;
    	_url = state.url;
    	super.onRestoreInstanceState(state.parentState);
    	
    	if (state.isWaiting) {
			// A task to fetch a new image was in progress when we
			// previously persisted our state.
	    	// If a different image is being shown, remove it.
	    	setImageBitmap(null);
	    	_task = new GetImageTask(this, _url);
	    	_task.execute((Void[])null);
    	}
    }

	/**
	 * Asynchronously performs the get moment photo HTTP request.
	 */
	private class GetImageTask extends AsyncTask<Void, Void, Void> {
		
		private WeakReference<WebImageView> _parent;
		private HttpGet _httpGet;
		private Bitmap _bitmap;

		public GetImageTask(WebImageView parent, String url) {
			// Use a weak-reference for the parent activity. This prevents a memory leak should the activity be destroyed.
			_parent = new WeakReference<WebImageView>(parent);
			
			_httpGet = new HttpGet(url);
			// We need to accept any kind of image, or JSON - so for simplicity just accept anything.
			_httpGet.setHeader("Accept", "*/*");
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			// This is executed on a background thread.
			HttpClient client = new DefaultHttpClient();
			HttpProtocolParams.setUserAgent(client.getParams(), App.Instance.getUserAgent());
			
			HttpResponse response;
			try {
				response = client.execute(_httpGet);
			} catch (ClientProtocolException e) {
				response = null;
			} catch (IOException e) {
				response = null;
			}
			
			Bitmap bitmap = null;
			
			if (null == response) {
				Log.e("OHOW", "No response when getting remote image.");
			} else {
				int statusCode = response.getStatusLine().getStatusCode();

				if (200 == statusCode) {
					Log.d("OHOW", Integer.toString(statusCode));
					HttpEntity entity = response.getEntity();

					if (null == entity) {
						Log.e("OHOW", "No HTTP response body getting remote image.");
					} else {

						try {
							InputStream inputStream = entity.getContent(); 
							bitmap = BitmapFactory.decodeStream(inputStream);
							
						} catch (IllegalStateException e) {
							throw new ImprobableCheckedExceptionException(e);
						} catch (IOException e) {
							throw new ImprobableCheckedExceptionException(e);
						}
					}
				}
			}
			
			_bitmap = bitmap;
			return null;
		}
		
		protected void onPostExecute(Void unused) {
			// On the main thread.
			WebImageView parent = _parent.get();
			if (null != parent) {
				
				if (parent._task == this) {
					
					if (null == _bitmap) {						
						setImageResource(R.drawable.photo_placeholder);
					} else {
						 // 'parent' will be null if it has already been garbage collected.
						 // Display the image.
						setImageBitmap(_bitmap);
					}
					
					parent._task = null;
				}
			}
		}
	}
	
	private Context _context;

}

/**
 * State for the WebImageView.
 */
class WebImageViewState implements Parcelable {

	public Parcelable parentState;
	public String url;
	public boolean isWaiting;
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(parentState, 0);
		dest.writeString(url);
		Parcel2.writeBoolean(isWaiting, dest);
	}
	
    public static final Parcelable.Creator<WebImageViewState> CREATOR
    	= new Parcelable.Creator<WebImageViewState>() {
			public WebImageViewState createFromParcel(Parcel in) {
			    return new WebImageViewState(in);
			}
			
			public WebImageViewState[] newArray(int size) {
			    return new WebImageViewState[size];
			}
		};

	private WebImageViewState(Parcel in) {
		parentState = in.readParcelable(null); // Use the default ClassLoader
		url = in.readString();
		try {
			isWaiting = Parcel2.readBoolean(in);
		} catch (IOException e) {
			throw new ImprobableCheckedExceptionException(e);
		}
	}
	
	public WebImageViewState() {
	}	
}
