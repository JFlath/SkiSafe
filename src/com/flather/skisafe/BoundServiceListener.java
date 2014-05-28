package com.flather.skisafe;

import android.hardware.SensorEvent;
import android.location.Location;

public interface BoundServiceListener {
	public void locUpdate(Location loc);
	public void accUpdate(SensorEvent event);
	public void accMaxUpdate(double x,double y,double z);
	public void speedMaxUpdate(float s);
}
