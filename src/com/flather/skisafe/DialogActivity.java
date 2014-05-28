package com.flather.skisafe;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class DialogActivity extends Activity implements OnClickListener {
	
	Location locStat;
	String contactNum;
	boolean message;
	private SmsManager mSmsManager;
	CountDownTimer timer;
	
	TextView count;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_dialog);
		
		Bundle b = getIntent().getExtras();
		locStat = b.getParcelable("com.flather.skisafe.locStat");
		contactNum = b.getString("com.flather.skisafe.contactNum");
		message = b.getBoolean("com.flather.skisafe.message");
		
		
		Button dialogCancel = (Button) findViewById(R.id.buttonDialogCancel);
		Button dialogOk = (Button) findViewById(R.id.buttonDialogOk);
		dialogCancel.setOnClickListener(this);  
        dialogOk.setOnClickListener(this);
        
        count = (TextView) findViewById(R.id.textViewCountDown);
        mSmsManager = SmsManager.getDefault();
        
        notifyAlert();
        
        timer = new CountDownTimer(31000, 1000) {

            public void onTick(long millisUntilFinished) {
                count.setText(String.valueOf(millisUntilFinished / 1000));
            }

            public void onFinish() {
               send();
               finish();
            }
         }.start();

	}
	private void send(){
		if(message){
			if(locStat != null){
				String alertText = "Accident at Lat: "+ locStat.getLatitude() + " Long: " + locStat.getLongitude();
				mSmsManager.sendTextMessage(contactNum, null, alertText, null, null);	
				Toast.makeText(getApplicationContext(), "Alert SMS sent to " + contactNum, Toast.LENGTH_SHORT).show();
			}else{
				Toast.makeText(getApplicationContext(), "No Location to Send", Toast.LENGTH_SHORT).show();//TODO Set flag to end once location acquired
			}
		}
	}
	

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.buttonDialogCancel:
			finish();
			break;
		case R.id.buttonDialogOk:  
			send();
			timer.cancel();
			finish();
			break;	
		}
	}
	
	private void notifyAlert(){
		long[] pattern = {500,500,500,500,500,500,500,500,500};
		NotificationCompat.Builder mBuilder =
		        new NotificationCompat.Builder(this)
				.setLights(Color.RED, 500, 500)
				.setVibrate(pattern)
				.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
		        .setSmallIcon(R.drawable.ic_action_warning)
		        .setContentTitle(getResources().getString(R.string.crash_detected));
		/*
		// Creates an explicit intent for an Activity in your app
		Intent resultIntent = new Intent(this, ResultActivity.class);

		// The stack builder object will contain an artificial back stack for the
		// started Activity.
		// This ensures that navigating backward from the Activity leads out of
		// your application to the Home screen.
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(ResultActivity.class);
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent =
		        stackBuilder.getPendingIntent(
		            0,
		            PendingIntent.FLAG_UPDATE_CURRENT
		        );
		mBuilder.setContentIntent(resultPendingIntent);
		*/
		NotificationManager mNotificationManager =
		    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.
		mNotificationManager.notify(1, mBuilder.build());
		
	}


}
