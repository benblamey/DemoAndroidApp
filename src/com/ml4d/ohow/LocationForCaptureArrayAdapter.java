/**
 * 
 */
package com.ml4d.ohow;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * An ArrayAdapter<T> specifically for 'LocationForCapture' objects.
 */
public class LocationForCaptureArrayAdapter extends ArrayAdapter<LocationForCapture> {
	
	// Field names are as in parent class - and otherwise I've kept the code as similar as possible.
	
	/**
     * The resource indicating what views to inflate to display the content of this
     * array adapter.
     */
    private int mResource;
    
    /**
     * If the inflated resource is not a TextView, {@link #mFieldId} is used to find
     * a TextView inside the inflated views hierarchy. This field must contain the
     * identifier that matches the one defined in the resource file.
     */
    private int mFieldId = 0;
    
    private LayoutInflater mInflater;
    
    private String mUnlistedPlaceName;
	
    /**
     * Constructor
     *
     * @param context The current context.
     * @param resource The resource ID for a layout file containing a layout to use when
     *                 instantiating views.
     * @param textViewResourceId The id of the TextView within the layout resource to be populated
     */
    public LocationForCaptureArrayAdapter(Context context, int textViewResourceId,  LocationForCapture[] objects, String unlistedPlaceName) {
    	super(context, textViewResourceId, objects);
        init(context, textViewResourceId, 0);
        mUnlistedPlaceName = unlistedPlaceName;
	}

    // This method is retained to maintain similarity with the parent class.
    private void init(Context context, int resource, int textViewResourceId) {
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mResource = resource;
        mFieldId = textViewResourceId;
    }
    
    /**
     * {@inheritDoc}
     */
	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createViewFromResource(position, convertView, parent, mResource);
    }

    private View createViewFromResource(int position, View convertView, ViewGroup parent,
            int resource) {
        View view;
        TextView text;

        if (convertView == null) {
            view = mInflater.inflate(resource, parent, false);
        } else {
            view = convertView;
        }

        try {
            if (mFieldId == 0) {
                //  If no custom field is assigned, assume the whole resource is a TextView
                text = (TextView) view;
            } else {
                //  Otherwise, find the TextView field within the layout
                text = (TextView) view.findViewById(mFieldId);
            }
        } catch (ClassCastException e) {
            Log.e("ArrayAdapter", "You must supply a resource ID for a TextView");
            throw new IllegalStateException(
                    "ArrayAdapter requires the resource ID to be a TextView", e);
        }

        LocationForCapture item = getItem(position);
        
        if (item.getIsListed()) {
        	text.setText(item.getLocationName());
        } else {
        	text.setText(mUnlistedPlaceName);
        }

        return view;
    }

}
