package com.evilknights.rpolnotifier;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.text.format.Time;
import android.util.Log;
import android.net.Uri;

import com.google.android.gcm.GCMBaseIntentService;
import java.util.Arrays;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.preference.PreferenceManager;

/**
 * IntentService responsible for handling GCM messages.
 */
public class GCMIntentService extends GCMBaseIntentService {

//    @SuppressWarnings("hiding")
 
	private static final String TAG = Common.TAG;
    public static final String SENDER_ID = "578438244602";

    public GCMIntentService() {
        super(SENDER_ID);
    	Log.d(TAG, "Sending SENDER_ID: "+SENDER_ID);
    }

    @Override
    protected void onRegistered(Context context, String registrationId) {
        Log.d(TAG, "Device registered: regId = " + registrationId);
        gcmStatus(context, "REGISTERED");
    }

    @Override
    protected void onUnregistered(Context context, String registrationId) {
    	Log.d(TAG, "Device unregistered");
    }

    @Override
    protected void onMessage(Context context, Intent intent) {
        Log.d(TAG, "Received message for game: " 
        		+ intent.getExtras().getString("game") );
        generateNotification(context, intent);
    }

    @Override
    protected void onDeletedMessages(Context context, int total) {
        Log.d(TAG, "Received deletions: "
        		+ Integer.toString(total));
    }

    @Override
    public void onError(Context context, String errorId) {
        Log.d(TAG, "Received error: " + errorId);
        gcmStatus(context, "ERROR_FAIL");
    }

    @Override
    protected boolean onRecoverableError(Context context, String errorId) {
        Log.d(TAG, "Recoverable error: " + errorId);
        gcmStatus(context, "ERROR_RECOVER");
        return super.onRecoverableError(context, errorId);
    }
    
    private static void gcmStatus(Context context, String status){
    	SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    	Log.d(TAG, "Saving gcmstatus as " + status);
    	sharedPrefs.edit().putString("gcmstatus", status).commit();
    }

    /**
     * Issues a notification to inform the user that server has sent a message.
     */
	private static void generateNotification(Context context, Intent intent) {
     // load the preferences
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (sharedPrefs.getBoolean("notify_disabled", false)){
          Log.d(TAG, "Notification disabled");
          return; 
          } // notify turned off, exit out

     // extract the GCM data from the original intent
        String strGameID  = intent.getExtras().getString("game");
        int intGameID = Integer.parseInt(strGameID);
        String strTitle   = intent.getExtras().getString("title");
        String strSummary = intent.getExtras().getString("summary");
        boolean isPrivate = strSummary.contains("private");

     // Check private ignore list to see if note is from a blocked game
        String stringignorelist;
        String[] ignorelist;
        if (isPrivate == false) {
          stringignorelist = sharedPrefs.getString("adv_ignorelist", "");
          ignorelist = stringignorelist.split(",");
          if ( Arrays.asList(ignorelist).contains(strGameID) ){
            Log.d(TAG, "Public notification ignored for gameID: " + strGameID);
            return; } //no notifications for this game.
        }        

        stringignorelist = sharedPrefs.getString("priv_ignorelist", "");
        ignorelist = stringignorelist.split(",");
        if ( Arrays.asList(ignorelist).contains(strGameID) ){
          Log.d(TAG, "Notification ignored for gameID: " + strGameID);
          return; } //no notifications for this game.


     //Notification notification = new Notification(icon, strTitle, when);
   		NotificationCompat.Builder noteBuilder = new  NotificationCompat.Builder(context);
   		int noteDefaults = 0;

     // Check for quiet hours 
    	boolean quiethours = false;
        if (sharedPrefs.getBoolean("quiethours_enabled", false) ){
        	Log.d(TAG, "Quiet hours enabled, checking time");
        	Time cTime = new Time();
        	cTime.setToNow();
        	int qHourStart = Integer.parseInt(sharedPrefs.getString("quiethours_start", "22:00").split(":")[0] );
        	int qMinStart =  Integer.parseInt(sharedPrefs.getString("quiethours_start", "22:00").split(":")[1] );
        	int qHourStop =  Integer.parseInt(sharedPrefs.getString("quiethours_stop",  "06:00").split(":")[0] );
        	int qMinStop =   Integer.parseInt(sharedPrefs.getString("quiethours_stop",  "06:00").split(":")[1] );
        	int cTiM = (cTime.hour *60) + cTime.minute;   //Current Time in Minutes
        	int qTiMstart = (qHourStart *60) + qMinStart; //Quite time in minutes start
        	int qTiMstop  = (qHourStop *60)  + qMinStop;  //Quite time in minutes stop

        	if ( qTiMstart < qTiMstop ){   //check if time is between values
        		if ( (cTiM >= qTiMstart) && (cTiM <= qTiMstop)){
        			Log.d(TAG, "Notification set to quiet");
        			quiethours = true;
        		}
        	} else {   //Check if to make sure time is not outside the quite period
        		if ( (cTiM >= qTiMstart) != (cTiM <= qTiMstop) ){
        			Log.d(TAG, "Notification set to quiet");
        			quiethours = true;
        		}
        	}

        }
        //   The following IF encompasses sound, vibrate and led
		if (!quiethours){
			Log.d(TAG, "Notification not quieted");

     // set the sound to play
                        if(isPrivate){
			  noteBuilder.setSound(Uri.parse(sharedPrefs.getString("privatenotify_tone",
			  		"content://settings/system/notification_sound")) );
			} else {
			  noteBuilder.setSound(Uri.parse(sharedPrefs.getString("notify_tone",
			  		"content://settings/system/notification_sound")) );
			}
     // set the vibration settings
        if (sharedPrefs.getBoolean("notify_vib", true)){
          String strVibDataIn = sharedPrefs.getString("notify_vibpattern", "");  
          if (strVibDataIn == "") {
             noteDefaults |= Notification.DEFAULT_VIBRATE;
          } else {
             String[] strVibData = strVibDataIn.split(":");  
             long[] vibData = new long[strVibData.length]; 
             for (int i = 0; i < strVibData.length; i++) {     
                vibData[i] = Long.parseLong(strVibData[i]);
             }
             noteBuilder.setVibrate(vibData);
          }
        }

     // set the LED settings
        if (sharedPrefs.getBoolean("notify_led", true)){
          String strLedDataIn = sharedPrefs.getString("notify_ledpattern", "");  
          if (strLedDataIn.equals("") ) {
             noteDefaults |= Notification.DEFAULT_LIGHTS;
          } else {
           // set pattern and color
             String[] strLedData = strLedDataIn.split(":");
             int noteColor = 0xff000000; //set the alpha
             String strColor = sharedPrefs.getString("notify_ledcolor", "ffffff");
             try{
               	 noteColor |= Integer.parseInt(strColor, 16);
            	 Log.d(TAG, "Color hex to int passed: " + Integer.toHexString(noteColor) );
             } catch (NumberFormatException nfe){
            	 Log.d(TAG, "Color hex to int failed: "+strColor + " setting to Color.WHITE");
            	 noteColor = Color.WHITE;
             }
             noteBuilder.setLights( noteColor
            		 , Integer.parseInt(strLedData[1])
            		 , Integer.parseInt(strLedData[0]) );
          }
        }
		}// End quiet hours IF statement

     // set the activation action to open the browser to the game
	String strURL;
        if (isPrivate){
        	strURL = RpolScraper.getSiteURL(context) + "/private.cgi?gi=" + strGameID;
	} else {
        	strURL = RpolScraper.getSiteURL(context) + "/game.cgi?gi=" + strGameID;
 	}
        Uri uri = Uri.parse(strURL);
        Intent notificationIntent = new Intent(Intent.ACTION_VIEW, uri);
        //force browser choice
        Intent fIntent;
        if (sharedPrefs.getBoolean("forcebrowserchoice",  false) ){
        	fIntent = Intent.createChooser(notificationIntent,"Choose Broswer for: " + strGameID);
        } else {
        	fIntent = notificationIntent;
        }
        PendingIntent pintent =
                PendingIntent.getActivity(context, intGameID,  fIntent, 0);

     //Get large icon for newer devices
        PortraitManager pm = new PortraitManager(context);
        pm.open();
        Portrait iconPort =  pm.getPortrait(strGameID);
        Bitmap bigIcon = iconPort.getBitmap();
        if (bigIcon == null){
        	bigIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher);
        }
        pm.close();
     //Build the rest of the notification
        noteBuilder.setDefaults(noteDefaults)
	        .setContentTitle(strTitle)
	        .setContentText(strSummary)
	        .setContentIntent(pintent)
	        .setAutoCancel(true)
	        .setLargeIcon(bigIcon )
	        .setOnlyAlertOnce(false)
	        .setSmallIcon(R.drawable.ic_stat_notify)
	        .setOngoing(false)
	        .setWhen( System.currentTimeMillis() );
        
     // Send the notification
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(intGameID, noteBuilder.build() );
    }

}
