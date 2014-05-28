package com.flather.skisafe;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.SensorEvent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import com.flather.skisafe.ConfirmMessagingDialogFragment.ConfirmMessagingDialogListener;
import com.flather.skisafe.LocationService.LocationBinder;


public class TrackingFragment extends Fragment implements OnClickListener, ConfirmMessagingDialogListener {
	  
	/**
	 * The fragment argument representing the section number for this
	 * fragment.
	 */
	public static final String ARG_SECTION_NUMBER = "section_number";
	
	//Default values
	private double caution = 20;
	private double danger = 30;
		
	private boolean tracking = false;
	private boolean message = false;
	private boolean inDanger = false;
	
	private TextView locTimeField;
	private TextView latitudeField;
	private TextView longitudeField;
	private TextView providerField;
	
	private TextView xCoordField;
	private TextView yCoordField;
	private TextView zCoordField;
	
	private TextView xMaxCoordField;
	private TextView yMaxCoordField;
	private TextView zMaxCoordField;
	
	private TextView speedAvgField;
	private TextView speedMaxField;
	
	CompoundButton toggleTracking;
	CompoundButton toggleMessage;
	
	private SharedPreferences settings;
	
	private TextView alertField;
		
    private LocationService mService;

	private boolean mBound;


	public TrackingFragment() {
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
 	}
	
	
	@Override
	public void onStart() {
		super.onStart();
		//Link to views to output data
		
		locTimeField = (TextView) getView().findViewById(R.id.textLocationTime);
		latitudeField = (TextView) getView().findViewById(R.id.textLatValue);
		longitudeField = (TextView) getView().findViewById(R.id.textLongValue);
		providerField = (TextView) getView().findViewById(R.id.textProviderValue);
		
		xCoordField = (TextView) getView().findViewById(R.id.textXCoordValue);
		yCoordField = (TextView) getView().findViewById(R.id.textYCoordValue);
		zCoordField = (TextView) getView().findViewById(R.id.textZCoordValue);
		
		xMaxCoordField = (TextView) getView().findViewById(R.id.textXCoordValueMax);
		yMaxCoordField = (TextView) getView().findViewById(R.id.textYCoordValueMax);
		zMaxCoordField = (TextView) getView().findViewById(R.id.textZCoordValueMax);
		
		speedAvgField = (TextView) getView().findViewById(R.id.textSpeedAverageValue);
		speedMaxField = (TextView) getView().findViewById(R.id.textSpeedMaxValue);
		
		alertField = (TextView) getView().findViewById(R.id.textAlert);
		
		Intent intent = new Intent(this.getActivity(), LocationService.class);
		
		if(!serviceRunning()){
			//Only start service if not already running
			getActivity().startService(intent);
		}
		
	}
	
	@Override
	public void onResume() {
		super.onResume();
		//Bind to service
        Intent intent = new Intent(this.getActivity(), LocationService.class);
        getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        mBound = true;
	}
	
	@Override
	public void onStop() {
		super.onDestroy();
		if (mBound) {
			//Does not require updates when stopped
            getActivity().unbindService(mConnection);
            mBound = false;
        }
		if(!tracking){
			//If not tracking, stop the service to save battery
			Intent intent = new Intent(this.getActivity(), LocationService.class);
			getActivity().stopService(intent);
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_main_tracking,
				container, false);
		//Set onClickListener for toggle tracking switch
		toggleTracking = (CompoundButton) rootView.findViewById(R.id.trackingSwitch);
        toggleTracking.setOnClickListener(this);
        //Set onClickListener for toggle messaging switch
        toggleMessage = (CompoundButton) rootView.findViewById(R.id.messageSwitch);
        toggleMessage.setOnClickListener(this);
		
        return rootView;
	}
	

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.trackingSwitch:        	
        	tracking = ((CompoundButton) v).isChecked();
			setTracking(tracking);
			break;
		case R.id.messageSwitch:        	
        	message = ((CompoundButton) v).isChecked();
			//No effect on fragment so directly toggle on service
        	mService.setMessage(message);
			break;
        }
		
	}
	
	private void setTracking(boolean b) {
		if(b){
			if(!message){
				//Display dialog if needed
				 DialogFragment dialog = new ConfirmMessagingDialogFragment();
				 dialog.setTargetFragment(this, 1);
			     dialog.show(getActivity().getSupportFragmentManager(), "ConfirmMessagingDialogFragment");
			}
			
			mService.setTracking(true);
			
			//Update threshold values
			//N.B. Affects changing of UI only
			settings = getActivity().getSharedPreferences("mPrefs", 0);
			if(settings.getString("cautionVal", "") != ""){
		    	caution = Double.parseDouble(settings.getString("cautionVal", ""));
		    }
			if(settings.getString("dangerVal", "") != ""){
		    	danger = Double.parseDouble(settings.getString("dangerVal", ""));
		    }
			
			locTimeField.setText(getResources().getString(R.string.current));

			
		}else{
			
			mService.setTracking(false);
			
			//Override UI alert state
			alertField.setText("Not Tracking");
			alertField.setTextColor(Color.BLACK);
			inDanger = false;
			
			locTimeField.setText(getResources().getString(R.string.last));
		}
	}
	
	private void locDisplayUpdater(Location loc) {	
		latitudeField.setText(Location.convert(loc.getLatitude(), Location.FORMAT_SECONDS));
		longitudeField.setText(Location.convert(loc.getLongitude(), Location.FORMAT_SECONDS));
		providerField.setText(loc.getProvider());	
	}
	
	private void accDisplayUpdater(SensorEvent event) {	
		//Extract force data to rounded values
		xCoordField.setText(Double.toString(Math.round(event.values[0])));
		yCoordField.setText(Double.toString(Math.round(event.values[1])));
		zCoordField.setText(Double.toString(Math.round(event.values[2])));
		
		if(!inDanger){//Do not switch back if DANGER state has previously been reached
			switch(dangerZone(event)){
			case 0:
				alertField.setText(getResources().getString(R.string.safe));
				alertField.setTextColor(Color.GREEN);
				break;
			case 1:
				alertField.setText(getResources().getString(R.string.caution));
				alertField.setTextColor(0xffffa500);
				break;
			case 2:
				alertField.setText(getResources().getString(R.string.danger));
				alertField.setTextColor(Color.RED);
				break;
			}
		}
	}
	
	private int dangerZone(SensorEvent event){
		//Total acceleration 
		double mag = (Math.sqrt((Math.pow(event.values[0], 2))+(Math.pow(event.values[1], 2))+(Math.pow(event.values[2], 2))));
		
		if(mag >= danger){
			inDanger = true;//Set DANGER flag
			return 2;
		}else if (mag >= caution)
			return 1;
		else{
			return 0;
		}
	}
	private void accMaxDisplayUpdater(double x, double y, double z){
		//Extract force data to rounded values	
		xMaxCoordField.setText(Double.toString(Math.round(x)));
		yMaxCoordField.setText(Double.toString(Math.round(y)));
		zMaxCoordField.setText(Double.toString(Math.round(z)));
	}
	private void speedMaxDisplayUpdater(float s) {
		//speedMaxField.setText(Float.toString(s*3.6f));
	}

	
	private ServiceConnection mConnection = new ServiceConnection() {


		@Override
	    public void onServiceDisconnected(ComponentName arg0) {
	        mBound = false;
	    }

	    @Override
	    public void onServiceConnected(ComponentName name, IBinder service) {
	       //Cast the IBinder and get LocalService instance
	       LocationBinder binder = (LocationBinder) service;
	       mService = binder.getService();
	       //Set custom listener
	       binder.setListener(new BoundServiceListener() {

				@Override
				public void locUpdate(Location loc) {
					locDisplayUpdater(loc);
				}

				@Override
				public void accUpdate(SensorEvent event) {
					accDisplayUpdater(event);
				}

				@Override
				public void accMaxUpdate(double x, double y, double z) {
					accMaxDisplayUpdater(x,y,z);
				}

				@Override
				public void speedMaxUpdate(float s) {
					speedMaxDisplayUpdater(s);	
				}

	        });
	        mBound = true;

	    }
	};


	@Override
	public void onDialogPositiveClick() {
		message = true;
		//Toggle messaging directly on service
		mService.setMessage(message);
		toggleMessage.setChecked(true);
	}

	@Override
	public void onDialogNegativeClick() {
		//No action required
	}

	private boolean serviceRunning(){
		ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        //Check against all running services to see if LocationService is running
	    	if (LocationService.class.getName().equals(service.service.getClassName())) {
	        	return true;
	        }
        }
		return false;
	}



	
}

