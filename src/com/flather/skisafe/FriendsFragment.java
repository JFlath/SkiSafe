package com.flather.skisafe;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class FriendsFragment extends Fragment implements OnClickListener {
	/**
	 * The fragment argument representing the section number for this
	 * fragment.
	 */
	public static final String ARG_SECTION_NUMBER = "section_number";
	
	static final int PICK_CONTACT_REQUEST = 1;  // The request code

	private static final int RESULT_OK = 0;
	
	SharedPreferences settings;
	String contactNum;
	EditText contactNumField;

	public FriendsFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		
	}
	@Override
	public void onStart() {
		super.onStart();
		//Link to views to output data
		settings = getActivity().getSharedPreferences("mPrefs", 0);
	    contactNum = settings.getString("contactNumber", "");
		contactNumField = (EditText) getView().findViewById(R.id.editTextContactNum);
		if(contactNum != null){
			contactNumField.setText(contactNum);
			contactNumField.setSelection(contactNum.length());
		}

	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_main_friends,
				container, false);
		
		Button setContactNum = (Button) rootView.findViewById(R.id.buttonSetContactNum);
        setContactNum.setOnClickListener(this);
        
 		return rootView;
	}
	
	@Override
	public void onCreateOptionsMenu(
	      Menu menu, MenuInflater inflater) {
	   inflater.inflate(R.menu.friends, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
	    	case R.id.action_new:
	            openNew();
	            return true;
	        case R.id.action_settings:
	         //   openSettings();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	private void openNew() {
	    Intent pickContactIntent = new Intent(Intent.ACTION_PICK, Uri.parse("content://contacts"));
	    pickContactIntent.setType(Phone.CONTENT_TYPE); // Show user only contacts w/ phone numbers
	    startActivityForResult(pickContactIntent, PICK_CONTACT_REQUEST);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	    // Check which request we're responding to
	    if (requestCode == PICK_CONTACT_REQUEST) {
	        // Make sure the request was successful
	        if (resultCode == RESULT_OK) {
	            // The user picked a contact.
	            // The Intent's data Uri identifies which contact was selected.

	            // Do something with the contact here (bigger example below)
	        }
	    }
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.buttonSetContactNum:        	
        	contactNum = contactNumField.getText().toString();
        	settings = getActivity().getSharedPreferences("mPrefs",0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("contactNumber", contactNum);
            editor.commit();
            Toast.makeText(getActivity(), "Contact number updated", Toast.LENGTH_SHORT).show();
			break;
        }
		
	}
	
}
