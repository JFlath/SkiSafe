package com.flather.skisafe;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;

public class MapFragment extends Fragment {
	/**
	 * The fragment argument representing the section number for this
	 * fragment.
	 */
	public static final String ARG_SECTION_NUMBER = "section_number";
	
	private SupportMapFragment fragment;
	private GoogleMap mMap;

	public MapFragment() {
	}

	
	    @Override
	    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	        return inflater.inflate(R.layout.fragment_main_map, container, false);
	    }

	    @Override
	    public void onActivityCreated(Bundle savedInstanceState) {
	        super.onActivityCreated(savedInstanceState);
	        FragmentManager fm = getChildFragmentManager();
	        fragment = (SupportMapFragment) fm.findFragmentById(R.id.location_map);
	        if (fragment == null) {
	            fragment = SupportMapFragment.newInstance();
	            fm.beginTransaction().replace(R.id.location_map, fragment).commit();
	        }
	    }

	    @Override
	    public void onResume() {
	        super.onResume();
	        if (mMap == null) {
	            mMap = fragment.getMap();
	            mMap.setMyLocationEnabled(true);
	        }
	    }
}
	

