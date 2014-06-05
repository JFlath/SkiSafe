package com.flather.skisafe;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

public class UtilitiesFragment extends Fragment implements OnClickListener, SensorEventListener {
	
	private static final int OUT_OF_DATE = 1000 * 60 * 2;
	private static final double CAUTION = 20;
	
	private boolean logging = false;
	private boolean thresholded = false;
	private boolean thresholdExceeded = false;
	
	private SensorManager accelManager;
	private Sensor accel;
	private NotificationManager mNotificationManager;
	private FileOutputStream fos;
	
	CompoundButton toggleLogging;   
    CompoundButton toggleThreshold;
	Button setCautionButton;
	Button setDangerButton;
	
	SharedPreferences settings;
	SharedPreferences.Editor editor;
	String cautionVal;
	String dangerVal;
	EditText cautionValField;
	EditText dangerValField;
	
	

	
	/**
	 * The fragment argument representing the section number for this
	 * fragment.
	 */
	public static final String ARG_SECTION_NUMBER = "section_number";

	public UtilitiesFragment() {
	}
	
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
      	accelManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
      	
      	accel = accelManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
 	
      	mNotificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		//Link to views to output data
		settings = getActivity().getSharedPreferences("mPrefs", 0);
	    cautionVal = settings.getString("cautionVal", "");
		cautionValField = (EditText) getView().findViewById(R.id.editTextCautionVal);
		if(cautionVal != null){
			cautionValField.setText(cautionVal);
			cautionValField.setSelection(cautionVal.length());
		}
		dangerVal = settings.getString("dangerVal", "");
		dangerValField = (EditText) getView().findViewById(R.id.editTextDangerVal);
		if(dangerVal != null){
			dangerValField.setText(dangerVal);
			dangerValField.setSelection(dangerVal.length());
		}

	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_main_utilities,
				container, false);
		
		toggleLogging = (CompoundButton) rootView.findViewById(R.id.loggingSwitch);
        toggleLogging.setOnClickListener(this);
        
        toggleThreshold = (CompoundButton) rootView.findViewById(R.id.thresholdSwitch);
        toggleThreshold.setOnClickListener(this);
        
        setCautionButton = (Button) rootView.findViewById(R.id.buttonSetCautionVal);
        setCautionButton.setOnClickListener(this);
        
        setDangerButton = (Button) rootView.findViewById(R.id.buttonSetDangerVal);
        setDangerButton.setOnClickListener(this);
		
        return rootView;
	}
	

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.loggingSwitch:        	
        	logging = ((CompoundButton) v).isChecked();
			setLogging(logging);
			break;
		case R.id.thresholdSwitch:        	
	        	thresholded = ((CompoundButton) v).isChecked();
				break;
		case R.id.buttonSetCautionVal:        	
        	cautionVal = cautionValField.getText().toString();
        	settings = getActivity().getSharedPreferences("mPrefs",0);
            editor = settings.edit();
            editor.putString("cautionVal", cautionVal);
            editor.commit();
            Toast.makeText(getActivity(), "Caution threshold value updated to " + cautionVal, Toast.LENGTH_SHORT).show();
			break;
		case R.id.buttonSetDangerVal:      	
	    	dangerVal = dangerValField.getText().toString();
        	settings = getActivity().getSharedPreferences("mPrefs",0);
            editor = settings.edit();
            editor.putString("dangerVal", dangerVal);
            editor.commit();
            
            Toast.makeText(getActivity(), "Danger threshold value updated to " + dangerVal, Toast.LENGTH_SHORT).show();
			break;
	    }
		
	}
	
	private void setLogging(boolean log) {
		if(log){
			accelManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL);
			try {
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(System.currentTimeMillis());
				String name = cal.get(Calendar.YEAR) + ":" + cal.get(Calendar.MONTH) + ":" + cal.get(Calendar.DAY_OF_MONTH)+ " " + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND)+ " " + "Accel.csv";
				fos = getActivity().openFileOutput(name, Context.MODE_APPEND);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			toggleThreshold.setEnabled(false);
			notifyLogging(true);
		}else{
			//Cancel updates from sensor
			accelManager.unregisterListener(this);
			try {
				fos.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			toggleThreshold.setEnabled(true);
			notifyLogging(false);
		}
	}
	
	

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
	    if (provider1 == null) {
	      return provider2 == null;
	    }
	    return provider1.equals(provider2);
	}


	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onSensorChanged(SensorEvent event) {
		try {
			if(thresholded){//Check against threshold
				double mag = (Math.sqrt((Math.pow(event.values[0], 2))+(Math.pow(event.values[1], 2))+(Math.pow(event.values[2], 2))));
				if(mag >= CAUTION){
					String line;
					if(!thresholdExceeded){//If just crossed threshold under->over
						line =  (event.timestamp)-1 + ",0,0,0\r\n";//Zero values for easier plotting
						fos.write(line.getBytes());
					}
					line =  event.timestamp + "," + Double.toString(event.values[0]).toString() + "," + Double.toString(event.values[1]).toString() + "," + Double.toString(event.values[2]).toString() + "\r\n";
					fos.write(line.getBytes());
					thresholdExceeded = true;
				}else{
					if(thresholdExceeded){//If just crossed threshold over->under
						String line;
						line =  (event.timestamp)+1 + ",0,0,0\r\n";//Zero values for easier plotting
						fos.write(line.getBytes());
						thresholdExceeded = false;
					}
				}
			}else{//Log all data
				String line;
				line = event.timestamp + "," + Double.toString(event.values[0]).toString() + "," + Double.toString(event.values[1]).toString() + "," + Double.toString(event.values[2]).toString() + "\r\n";
				fos.write(line.getBytes());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void notifyLogging(Boolean b){
		
		if(b){
			NotificationCompat.Builder mBuilder =
			        new NotificationCompat.Builder(this.getActivity())
			        .setSmallIcon(R.drawable.ic_launcher)
			        .setContentTitle("Ski Safe")
			        .setContentText("Data logging in progress.");			
			
			//TODO Intent to return here
			// mId allows you to update the notification later on.
			mNotificationManager.notify(2, mBuilder.build());
		}else{
			mNotificationManager.cancel(2);
		}
	}
	
}