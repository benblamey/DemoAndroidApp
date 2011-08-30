package com.ml4d.ohow;

import java.text.DateFormat;
import java.util.Locale;
import java.util.TimeZone;

import com.ml4d.core.WebImageView;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * An ArrayAdapter<T> specifically for 'Moment' objects for display the 'location_item_view'.
 */
public class MomentArrayAdapter extends ArrayAdapter<Moment> {
	
	// Field names are as in parent class - and otherwise I've kept the code as similar as possible.
	
	/**
     * The resource indicating what views to inflate to display the content of this
     * array adapter.
     */
    private int mResource;
    
    private LayoutInflater mInflater;
    
    private Context mContext;
	
    /**
     * Constructor
     *
     * @param context The current context.
     * @param resource The resource ID for a layout file containing a layout to use when
     *                 instantiating views.
     * @param layoutResourceId The id of the TextView within the layout resource to be populated
     */
    public MomentArrayAdapter(Context context, int layoutResourceId,  Moment[] objects) {
    	super(context, layoutResourceId, objects);
        init(context, layoutResourceId, 0);
	}

    // This method is retained to maintain similarity with the parent class.
    private void init(Context context, int resource, int textViewResourceId) {
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mResource = resource;
        mContext = context;
    }
    
    /**
     * {@inheritDoc}
     */
	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createViewFromResource(position, convertView, parent, mResource);
    }

    private View createViewFromResource(int position, View convertView, ViewGroup parent, int resource) {
        View view;

        if (convertView == null) {
            view = mInflater.inflate(resource, parent, false);
        } else {
            view = convertView;
        }

        Moment moment = getItem(position);
        
		String location = moment.getLocationName();
		if ((null == location) || (0 == location.length())) {
			location = Double.toString(moment.getLongitude()) + ", " + 
				Double.toString(moment.getLatitude());
		}

		Resources resources = mContext.getResources();
		
		// Not that the 'default' locale means the 'local culture'.
		String body = String.format(Locale.getDefault(), resources.getString(R.string.moment_body_format), moment.getBody()); 
		
		// The 'default' locale (used by getDateTimeInstance()) is suitable for the local culture, and should not be used for persistence, etc.
		DateFormat localDateFormat = DateFormat.getDateTimeInstance(
				DateFormat.SHORT, // Date.
				DateFormat.MEDIUM); // Time.
		localDateFormat.setTimeZone(TimeZone.getDefault());
		String details = String.format(Locale.getDefault(), resources.getString(R.string.moment_detail_format), moment.getUsername(), localDateFormat.format(moment.getDateCreatedUTC()));
		
		TextView bodyTextView = (TextView)view.findViewById(R.id.local_timeline_text_view_body);
		TextView locationTextView = (TextView)view.findViewById(R.id.local_timeline_text_view_capture_location);
		TextView detailsTextView = (TextView)view.findViewById(R.id.local_timeline_text_view_details);
		
		bodyTextView.setText(body);
		locationTextView.setText(location);
		detailsTextView.setText(details);
		
		if (moment.getHasPhoto()) {
			String url = OHOWAPIResponseHandler.getBaseUrlIncludingTrailingSlash(false) + "photo.php"
				+ "?" 
				+ "id=" + Double.toString(moment.getId())
				+ "&thumbnail=true";
			((WebImageView)view.findViewById(R.id.local_timeline_item_web_image_view)).setUrl(url);
		}
		
        return view;
    }
}
