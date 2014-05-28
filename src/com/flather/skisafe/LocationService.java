package com.flather.skisafe;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

public class LocationService extends Service {
	private static final int OUT_OF_DATE = 1000 * 60 * 2;
	
	private final IBinder mBinder = new LocationBinder();
	private BoundServiceListener mListener;
		
	private LocationManager locationManager;
	private LocationListener locationListenerGPS;
	private LocationListener locationListenerNet;
	private SensorManager accelManager;
	private SensorEventListener accelListener;
	private Sensor accel;

	private NotificationManager mNotificationManager;
	
	private SharedPreferences settings;
	private String contactNum;
	
	private Location locStat = null;
	private Boolean message = false;
	private Boolean inDanger = false;

	private double danger = 30; //default value of 30
	private double xMax= 0;
	private double yMax= 0;
	private double zMax= 0;
	
	private FileOutputStream fos;
	private Float speed = 0f;
	private Float maxSpeed = 0f;
	
	public LocationService() {
	}

	/**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocationBinder extends Binder {
        LocationService getService() {
            // Return this instance of LocalService so clients can call public methods
            return LocationService.this;
        }
        
        public void setListener(BoundServiceListener listener) {
            mListener = listener;
        }
    }
	
    public int onStartCommand(Intent intent, int flags, int startId) {
    	
    	locationManager=(LocationManager) getSystemService(Context.LOCATION_SERVICE);  
		
		//Set listener for GPS
		locationListenerGPS= new LocationListener(){

			@Override
			public void onLocationChanged(Location loc) {
				if(betterLocation(loc, locStat)) {
					locWrite(loc);
					locStat = loc; // update best current location
					mListener.locUpdate(loc);
				}
			}
	
			@Override
			public void onProviderDisabled(String provider) {
				// TODO Auto-generated method stub
				
			}
	
			@Override
			public void onProviderEnabled(String provider) {
				// TODO Auto-generated method stub
				
			}
	
			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
				// TODO Auto-generated method stub
			}
    	   
    	   
       };
       
       //Identical listener for network, set service at requestLocationUpdates
       	locationListenerNet= new LocationListener(){

			@Override
			public void onLocationChanged(Location loc) {
				if(betterLocation(loc, locStat)) {
					locWrite(loc);
					locStat = loc; // update best current location
					mListener.locUpdate(loc);
				}
			}
	
			@Override
			public void onProviderDisabled(String provider) {
				// TODO Auto-generated method stub
				
			}
	
			@Override
			public void onProviderEnabled(String provider) {
				// TODO Auto-generated method stub
				
			}
	
			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
				// TODO Auto-generated method stub
			}
   	   
   	   
      };
		
      	accelManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
      	
      	accel = accelManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
      	
      	accelListener = new SensorEventListener() {
            @Override
            public void onAccuracyChanged(Sensor sensor, int acc) {
            
            }
            @Override
            public void onSensorChanged(SensorEvent event) {
            	if(event.values[0] > xMax){
        			xMax = event.values[0];
        			mListener.accMaxUpdate(xMax,yMax,zMax);
        		}
        		if(event.values[1] > yMax){
        			yMax = event.values[1];
        			mListener.accMaxUpdate(xMax,yMax,zMax);
        		}
        		if(event.values[2] > zMax){
        			zMax = event.values[2];
        			mListener.accMaxUpdate(xMax,yMax,zMax);
        		}
       			double mag = (Math.sqrt((Math.pow(event.values[0], 2))+(Math.pow(event.values[1], 2))+(Math.pow(event.values[2], 2))));
        			
        		if(mag >= danger && !inDanger){
        			inDanger = true;//Set DANGER flag
        			displayDangerDialog();			
        		}
        	
            	mListener.accUpdate(event);
            }
     
        };
      	
      	

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }
    
	private void locWrite(Location loc) {
		String line;
		if(loc.getSpeed() != 0.0){
			speed = loc.getSpeed();
		}else{
			if(locStat != null){
				speed = loc.distanceTo(locStat)/((loc.getTime() - locStat.getTime())/1000);
			}
		}
		if(speed > maxSpeed){
			maxSpeed = speed;
			mListener.speedMaxUpdate(maxSpeed);
		}
			
		line =  loc.getTime()/1000 + "," 
				+ Double.toString(loc.getLongitude()) + "," 
				+ Double.toString(loc.getLatitude()).toString() + "," 
				+ Float.toString(speed) + "\r\n";
		
		try {
			fos.write(line.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public IBinder onBind(Intent intent) {
		Toast.makeText(getApplicationContext(), "Service Bound", Toast.LENGTH_SHORT).show();

		return mBinder;
	}
	
	private void displayDangerDialog() {
		Intent mIntent=new Intent(getApplicationContext(), DialogActivity.class);
		mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		mIntent.putExtra("com.flather.skisafe.locStat", locStat);
		mIntent.putExtra("com.flather.skisafe.contactNum", contactNum);
		mIntent.putExtra("com.flather.skisafe.message", message);
		startActivity(mIntent);


	}

	public void setTracking(boolean b) {
		String s;
		if(b){
			s = "Tracking On";
			//Request updates from both services simultaneously
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListenerGPS);			
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, locationListenerNet);
			
			accelManager.registerListener(accelListener, accel, SensorManager.SENSOR_DELAY_NORMAL);
			
			xMax = 0;
			yMax = 0;
			zMax = 0;
			mListener.accMaxUpdate(xMax,yMax,zMax);
			inDanger = false;
			
			settings = getSharedPreferences("mPrefs", 0);
		    contactNum = settings.getString("contactNumber", "");
		    if(settings.getString("dangerVal", "") != ""){
		    	danger = Double.parseDouble(settings.getString("dangerVal", ""));
		    }
		    
		    try {
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(System.currentTimeMillis());
				String name = cal.get(Calendar.YEAR) + ":" + cal.get(Calendar.MONTH) + ":" + cal.get(Calendar.DAY_OF_MONTH)+ " " + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND)+ " " + "Location.csv";
				fos = openFileOutput(name, Context.MODE_APPEND);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    
	
		}else{
			s = "Tracking Off";
			//Cancel updates from both services
			locationManager.removeUpdates(locationListenerGPS);	
			locationManager.removeUpdates(locationListenerNet);	
			//Cancel updates from sensor
			accelManager.unregisterListener(accelListener);
			inDanger = false;
			try {
				fos.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//notifyTracking(b);
		Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
	}
	
	public void setMessage(boolean m){
		String s;
		if(m){
			s = "Messaging On";
		}else
			s = "Messaging Off";
		message = m;
		Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
	}
	
	private void notifyTracking(Boolean b){
		
		if(b){
			NotificationCompat.Builder mBuilder =
			        new NotificationCompat.Builder(this)
			        .setSmallIcon(R.drawable.ic_launcher)
			        .setContentTitle(getResources().getString(R.string.app_name))
			        .setContentText(getResources().getString(R.string.tracking_in_progress));			

			//TODO Intent to return here
			// mId allows you to update the notification later on.
			mNotificationManager.notify(1, mBuilder.build());
		}else{
			mNotificationManager.cancel(1);
		}
	}
	
	private boolean betterLocation(Location loc, Location currentLoc) {
		/**
		 * TODO: code from Google. Needs redesigning to suit app purposes
		 */
		
		if (currentLoc == null) {
	        // A new location is always better than no location
	        return true;
	    }

	    // Check whether the new location fix is newer or older
	    long timeDelta = loc.getTime() - currentLoc.getTime();
	    boolean isSignificantlyNewer = timeDelta > OUT_OF_DATE;
	    boolean isSignificantlyOlder = timeDelta < -OUT_OF_DATE;
	    boolean isNewer = timeDelta > 0;

	    // If it's been more than two minutes since the current location, use the new location
	    // because the user has likely moved
	    if (isSignificantlyNewer) {
	        return true;
	    // If the new location is more than two minutes older, it must be worse
	    } else if (isSignificantlyOlder) {
	        return false;
	    }

	    // Check whether the new location fix is more or less accurate
	    int accuracyDelta = (int) (loc.getAccuracy() - currentLoc.getAccuracy());
	    boolean isLessAccurate = accuracyDelta > 0;
	    boolean isMoreAccurate = accuracyDelta < 0;
	    boolean isSignificantlyLessAccurate = accuracyDelta > 200;

	    // Check if the old and new location are from the same provider
	    boolean isFromSameProvider = isSameProvider(loc.getProvider(),
	            currentLoc.getProvider());

	    // Determine location quality using a combination of timeliness and accuracy
	    if (isMoreAccurate) {
	        return true;
	    } else if (isNewer && !isLessAccurate) {
	        return true;
	    } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
	        return true;
	    }
	    return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
	    if (provider1 == null) {
	      return provider2 == null;
	    }
	    return provider1.equals(provider2);
	}
	

}

