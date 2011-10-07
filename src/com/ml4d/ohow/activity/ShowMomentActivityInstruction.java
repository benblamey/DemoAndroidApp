package com.ml4d.ohow.activity;

import java.io.IOException;
import java.util.Date;
import android.os.Parcel;
import android.os.Parcelable;
import com.ml4d.core.Parcel2;
import com.ml4d.core.exceptions.ImprobableCheckedExceptionException;
import com.ml4d.ohow.Moment;
import com.ml4d.ohow.activity.ShowMomentActivityInstruction;
import com.ml4d.ohow.activity.ShowMomentInstanceActivityInstruction;
import com.ml4d.ohow.activity.ShowNewerMomentActivityInstruction;
import com.ml4d.ohow.activity.ShowOlderMomentActivityInstruction;


/**
 * The base class of one of a number of 'instruction' objects that are passed to the 'ShowMomentActivity' to instruct it
 * on how to load the moment it should display.
 * @author ben
 */
abstract class ShowMomentActivityInstruction implements Parcelable {
	
	private static final int SHOW_MOMENT_INSTANCE_ACTIVITY_INSTRUCTION_ID = 1;
	private static final int SHOW_OLDER_MOMENT_ACTIVITY_INSTRUCTION_ID = 2;
	private static final int SHOW_NEWER_MOMENT_ACTIVITY_INSTRUCTION_ID = 3;
	
	private double _latitude;
	private double _longitude;
	private int _radiusMetres;

	protected ShowMomentActivityInstruction(double latitude, double longitude, int radiusMetres) {
		_latitude = latitude;
		_longitude = longitude;
		_radiusMetres = radiusMetres;
	}
	
	protected ShowMomentActivityInstruction(Parcel in) {
		_latitude = in.readDouble();
		_longitude = in.readDouble();
		_radiusMetres = in.readInt();
	}
	
	public double getLatitude() {
		return _latitude;
	}
	
	public double getLongitude() {
		return _longitude;
	}
	
	public int getRadiusMetres() {
		return _radiusMetres;
	}
	
	// Parcelable implementation.
	
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {

		int id ;
		if (this instanceof ShowMomentInstanceActivityInstruction) {
			id = SHOW_MOMENT_INSTANCE_ACTIVITY_INSTRUCTION_ID;
		} else if (this instanceof ShowOlderMomentActivityInstruction) {
			id = SHOW_OLDER_MOMENT_ACTIVITY_INSTRUCTION_ID;
		} else if (this instanceof ShowNewerMomentActivityInstruction) {
			id = SHOW_NEWER_MOMENT_ACTIVITY_INSTRUCTION_ID;
		} else {
			throw new RuntimeException("unknown derived class");
		}
		dest.writeInt(id);
		
		dest.writeDouble(_latitude);
		dest.writeDouble(_longitude);
		dest.writeInt(_radiusMetres);
		
		writeToParcel(dest);
	}
	
	protected abstract void writeToParcel(Parcel dest);
	
    public static final Parcelable.Creator<ShowMomentActivityInstruction> CREATOR
    	= new Parcelable.Creator<ShowMomentActivityInstruction>() {
			public ShowMomentActivityInstruction createFromParcel(Parcel in) {
				
				int typeId = in.readInt();
				ShowMomentActivityInstruction result;
				
				switch (typeId) {
				case SHOW_MOMENT_INSTANCE_ACTIVITY_INSTRUCTION_ID:
					result = new ShowMomentInstanceActivityInstruction(in);
					break;
				case SHOW_OLDER_MOMENT_ACTIVITY_INSTRUCTION_ID:
					result = new ShowOlderMomentActivityInstruction(in);
					break;
				case SHOW_NEWER_MOMENT_ACTIVITY_INSTRUCTION_ID:
					result = new ShowNewerMomentActivityInstruction(in);
					break;
				default:
					throw new RuntimeException("Unknown ID for derived type.");
				}
				
				return result;
			}
			
			public ShowMomentActivityInstruction[] newArray(int size) {
				throw new RuntimeException("Not supported");
			}
    	};

}

class ShowMomentInstanceActivityInstruction extends ShowMomentActivityInstruction {
	
	private Moment _moment;
	private boolean _haveNewer;
	private boolean _haveOlder;

	public ShowMomentInstanceActivityInstruction(double latitude, double longitude, int radiusMetres, Moment moment, boolean haveNewer, boolean haveOlder) {
		super(latitude, longitude, radiusMetres);
		_moment = moment;
		_haveNewer = haveNewer;
		_haveOlder= haveOlder;
	}

	public ShowMomentInstanceActivityInstruction(Parcel in) {
		super(in);
		_moment = (Moment)in.readSerializable();
		try {
			_haveNewer = Parcel2.readBoolean(in);
			_haveOlder = Parcel2.readBoolean(in);
		} catch (IOException e) {
			throw new ImprobableCheckedExceptionException(e);
		}
	}
	
	public Moment getMoment() {
		return _moment;
	}
	
	public boolean getHaveNewer() {
		return _haveNewer;
	}
	
	public boolean getHaveOlder() {
		return _haveOlder;
	}

	@Override
	protected void writeToParcel(Parcel dest) {
		dest.writeSerializable(_moment);
		Parcel2.writeBoolean(_haveNewer, dest);
		Parcel2.writeBoolean(_haveOlder, dest);
	}
	
}

class ShowOlderMomentActivityInstruction extends ShowMomentActivityInstruction {
	
	private int _currentMomentId;
	private Date _currentDateCreatedUtc;

	public ShowOlderMomentActivityInstruction(double latitude, double longitude, int radiusMetres, int currentMomentId, Date currentDateCreatedUtc) {
		super(latitude, longitude, radiusMetres);
		_currentMomentId = currentMomentId;
		_currentDateCreatedUtc = currentDateCreatedUtc;
	}

	public ShowOlderMomentActivityInstruction(Parcel in) {
		super(in);
		_currentMomentId = in.readInt();
		_currentDateCreatedUtc = (Date)in.readSerializable();
	}
	
	public int getCurrentMomentId() {
		return _currentMomentId;
	}
	
	public Date getCurrentDateCreatedUtc() {
		return _currentDateCreatedUtc;
	}

	@Override
	protected void writeToParcel(Parcel dest) {
		dest.writeInt(_currentMomentId);
		dest.writeSerializable(_currentDateCreatedUtc);
	}

}

class ShowNewerMomentActivityInstruction extends ShowMomentActivityInstruction {

	private int _currentMomentId;
	private Date _currentDateCreatedUtc;

	public ShowNewerMomentActivityInstruction(double latitude, double longitude, int radiusMetres, int currentMomentId, Date currentDateCreatedUtc) {
		super(latitude, longitude, radiusMetres);
		_currentMomentId = currentMomentId;
		_currentDateCreatedUtc = currentDateCreatedUtc;
	}

	public ShowNewerMomentActivityInstruction(Parcel in) {
		super(in);
		_currentMomentId = in.readInt();
		_currentDateCreatedUtc = (Date)in.readSerializable();
	}
	
	public int getCurrentMomentId() {
		return _currentMomentId;
	}
	
	public Date getCurrentDateCreatedUtc() {
		return _currentDateCreatedUtc;
	}

	@Override
	protected void writeToParcel(Parcel dest) {
		dest.writeInt(_currentMomentId);
		dest.writeSerializable(_currentDateCreatedUtc);
	}
}

