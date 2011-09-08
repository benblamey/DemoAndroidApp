package com.ml4d.ohow;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.lang3.time.FastDateFormat;

import com.ml4d.core.WebImageView;
import com.ml4d.core.exceptions.UnexpectedEnumValueException;

import android.content.Context;
import android.content.res.Resources;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

/**
 * An ArrayAdapter<T> specifically for 'Moment' objects for display the 'location_item_view'.
 */
public class MomentArrayAdapter implements ListAdapter {

	// As suggested in one of the SDK samples, for best performance of the 'getView' method,
	// we set the tag of the item view to be an object holding references to the controls within it.
	// This saves doing a view.findById(..) call.
	class ViewHolder {
		public TextView _bodyTextView;
		public TextView _locationTextView;
		public TextView _detailsTextView;
		public WebImageView _imageView;
	}
	
	private final DataSetObservable _dataSetObservable = new DataSetObservable();
    private LayoutInflater _inflater;
    private ArrayList<Moment> _moments;
    private EndState _endState = EndState.INITIAL;
    private String _momentBodyFormat;
    private String _momentDetailFormat;
        
	// The state of the end of the list.
    public enum EndState 
    {
    	INITIAL,
    	NO_MOMENTS,
    	THERE_ARE_MORE_MOMENTS,
    	ARE_NO_MORE_MOMENTS,
    	WAITING,
    }
    
	// The various types of views for items in the list.
    private enum viewType
    {
    	MOMENT,
    	NO_MOMENTS_LABEL,
    	ARE_NO_MORE_MOMENTS_LABEL,
    	WAITING_LABEL,    	
    }

    private static final int viewTypeCount = viewType.values().length;
    
    public MomentArrayAdapter(Context context, ArrayList<Moment> moments, EndState endState) {
    	
    	if (EndState.INITIAL == endState) {
    		throw new IllegalArgumentException("'endState' cannot be 'INITIAL'");
    	}
        _inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        _moments = moments;
        _endState = endState;

        // The 'getView' method needs to execute very quickly, to save a few mS, get the resource strings now.
		Resources resources = context.getResources();
		_momentBodyFormat = resources.getString(R.string.moment_body_format);
		_momentDetailFormat = resources.getString(R.string.moment_detail_format);
	}

    public void setEndState(EndState state) {
    	if (_endState != state) {
	    	_endState = state;
	    	_dataSetObservable.notifyChanged();
    	}
    }
    
	@Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view;

        if (position == _moments.size()) {
	        if (convertView == null) {
	        	int resource;
	        	
	        	switch (_endState) {
	        	case ARE_NO_MORE_MOMENTS:
	        		resource = R.layout.local_timeline_end_item;
	        		break;
	        	case THERE_ARE_MORE_MOMENTS:
	        		throw new RuntimeException("there is no view for this");
	        	case INITIAL:
					throw new RuntimeException("We should have left this state by now - programmer error.");
	        	case NO_MOMENTS:
	        		resource = R.layout.local_timeline_empty_item;
	        		break;
	        	case WAITING:
	        		resource = R.layout.local_timeline_waiting_item;
	        		break;
	        	default:
	        		throw new UnexpectedEnumValueException(_endState);
	        	}
	        	
	            view = _inflater.inflate(resource, parent, false);
	        } else {
	            view = convertView;
	        }
	        
        } else {
	        
            TextView bodyTextView;
			TextView locationTextView;
			TextView detailsTextView;
			WebImageView photoImageView;
	
	        if (convertView == null) {
	            view = _inflater.inflate(R.layout.local_timeline_moment_item, parent, false);
	            bodyTextView = (TextView)view.findViewById(R.id.local_timeline_text_view_body);
				locationTextView = (TextView)view.findViewById(R.id.local_timeline_text_view_capture_location);
				detailsTextView = (TextView)view.findViewById(R.id.local_timeline_text_view_details);
				photoImageView = (WebImageView)view.findViewById(R.id.local_timeline_item_web_image_view);
	            
	            ViewHolder holder = new ViewHolder();
	            holder._bodyTextView = bodyTextView;
	            holder._locationTextView = locationTextView;
	            holder._detailsTextView = detailsTextView;
	            holder._imageView = photoImageView;
	            
	            view.setTag(holder);
	        } else {
	            view = convertView;
	            
	            ViewHolder holder = (ViewHolder)convertView.getTag();
	            bodyTextView = holder._bodyTextView;
				locationTextView = holder._locationTextView;
				detailsTextView = holder._detailsTextView;
				photoImageView = holder._imageView;
	        }
	
	        Moment moment = (Moment)getItem(position);
	        
			String location = moment.getLocationName();
			if ((null == location) || (0 == location.length())) {
				location = Double.toString(moment.getLongitude()) + ", " + 
					Double.toString(moment.getLatitude());
			}
	
			
			String body = String.format(Locale.getDefault(), _momentBodyFormat, moment.getBody()); 
			
			// We use 'FastDateFormat' from the external library, because the 'java.text.DateFormat' class has really bad performance.
			FastDateFormat localDateFormat = FastDateFormat.getDateTimeInstance(
					DateFormat.SHORT, // Date.
					DateFormat.MEDIUM, // Time.
					TimeZone.getDefault(), // Convert times into the local timezone.
					Locale.getDefault()); // Format the string according to the local culture.
			
			String details = String.format(Locale.getDefault(), _momentDetailFormat, moment.getUsername(), localDateFormat.format(moment.getDateCreatedUTC()));
			bodyTextView.setText(body);
			locationTextView.setText(location);
			detailsTextView.setText(details);
			
			// Note - views get re-used. We need to explicitly clear the image if there isn't one for this moment. 
			String url;
			if (moment.getHasPhoto()) {
				url = OHOWAPIResponseHandler.getBaseUrlIncludingTrailingSlash(false) + "photo.php"
					+ "?" 
					+ "id=" + Integer.toString(moment.getId())
					+ "&photo_size=thumbnail";
			} else {
				url = null;
			}
			photoImageView.setUrl(url);
        }
		
        return view;
    }
    
	@Override
	public int getCount() {
		
		int extraItems;
		switch (_endState) {
			case ARE_NO_MORE_MOMENTS:
				extraItems = 1;
				break;
			case THERE_ARE_MORE_MOMENTS:
				extraItems = 0;
				break;
			case INITIAL:
				throw new RuntimeException("We should have left this state by now - programmer error.");
			case NO_MOMENTS:
				extraItems = 1;
				break;
			case WAITING:
				extraItems = 1;
				break;
			default:
				throw new UnexpectedEnumValueException(_endState);
		}
		
		return extraItems + _moments.size();
	}

	@Override
	public Object getItem(int position) {
		
		Object item;
		if (position < _moments.size()) {
			item = _moments.get(position);
		} else {
			switch (_endState) {
			case ARE_NO_MORE_MOMENTS:
				item = viewType.ARE_NO_MORE_MOMENTS_LABEL;
				break;
			case THERE_ARE_MORE_MOMENTS:
				throw new RuntimeException("There is no view for this endstate.");
			case INITIAL:
				throw new RuntimeException("We should have left this state by now - programmer error.");
			case NO_MOMENTS:
				item = viewType.NO_MOMENTS_LABEL;
				break;
			case WAITING:
				item = viewType.WAITING_LABEL;
				break;
			default:
				throw new UnexpectedEnumValueException(_endState);
			}
		}
		return item;
	}

	@Override
	public long getItemId(int position) {
		// Moments have positive IDs,
		// end labels have negative IDs.
		long id;
		if (position < _moments.size()) {
			id = _moments.get(position).getId();
		} else {
			id = -_endState.ordinal();
		}
		return id;
	}

	@Override
    public void registerDataSetObserver(DataSetObserver observer) {
        _dataSetObservable.registerObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        _dataSetObservable.unregisterObserver(observer);
    }

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public int getItemViewType(int position) {
		int id;
		
		if (position < _moments.size()) {
			id = viewType.MOMENT.ordinal();
		} else {
			switch (_endState) {
			case ARE_NO_MORE_MOMENTS:
				id = viewType.ARE_NO_MORE_MOMENTS_LABEL.ordinal();
				break;
			case THERE_ARE_MORE_MOMENTS:
				throw new RuntimeException("There is no view for this endstate.");
			case INITIAL:
				throw new RuntimeException("We should have left this state by now - programmer error.");
			case NO_MOMENTS:
				id = viewType.NO_MOMENTS_LABEL.ordinal();
				break;
			case WAITING:
				id = viewType.WAITING_LABEL.ordinal();
				break;
			default:
				throw new UnexpectedEnumValueException(_endState);
			}
		}

		return id;
	}

	@Override
	public int getViewTypeCount() {
		return viewTypeCount;
	}

	@Override
	public boolean isEmpty() {
		return 0 == getCount();
	}

	@Override
	public boolean areAllItemsEnabled() {
		// The special markers are not enabled. 
		return false;
	}

	@Override
	public boolean isEnabled(int position) {
		// Only moments can be clicked.
		return (position < _moments.size());
	}
}
