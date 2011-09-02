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
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

/**
 * An ImageView that obtains its image asynchronously from the web.
 * @author ben
 *
 */
public class WebImageView extends ImageView {
	
	private String _url;

    public WebImageView(Context context) {
        super(context);
    }
    
    public WebImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public WebImageView(Context context, AttributeSet attrs, int defStyle) {
    	super(context, attrs, defStyle);
    }
    
    public void setUrl(String url) {
    	if (!String2.areEqual(_url, url)) {
    		
    		_url = url;
	    	// If a different image is being shown, remove it.
	    	setImageBitmap(null);
	    	
	    	if ((null != url) && ("" != url)) {
	    		GetImageTask photoTask = new GetImageTask(this, url);
	    		photoTask.execute((Void[])null);
	    	}
    	}
    }
    
	/**
	 * Asynchronously performs the get moment photo HTTP request.
	 */
	private class GetImageTask extends AsyncTask<Void, Void, Void> {
		
		private WeakReference<WebImageView> _parent;
		private HttpGet _httpGet;
		private Bitmap _bitmap;
		private String _url;

		public GetImageTask(WebImageView parent, String url) {
			// Use a weak-reference for the parent activity. This prevents a memory leak should the activity be destroyed.
			_parent = new WeakReference<WebImageView>(parent);
			_url = url;
			
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
				
				if (String2.areEqual(parent._url, _url)) {
					// 'parent' will be null if it has already been garbage collected.
					// Display the image.
					setImageBitmap(_bitmap);
				}
			}
		}
	}

}