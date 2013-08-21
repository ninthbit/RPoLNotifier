package com.evilknights.rpolnotifier;

import com.google.android.gcm.GCMRegistrar;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AppLoginService extends IntentService {
	public static final String USERNAME = "com.evilknights.rpolnotifier.AppLoginService.username";
	public static final String PASSWORD = "com.evilknights.rpolnotifier.AppLoginService.password";
	public static final String STATUS = "com.evilknights.rpolnotifier.AppLoginService.response";
	
	public AppLoginService(){
		super("AppLoginService");
	}
	
	@Override
    protected void onHandleIntent(Intent intent) {
        String username = intent.getStringExtra(USERNAME);
        String password = intent.getStringExtra(PASSWORD);
        final Context appContext = getApplicationContext();
        
		publishProgress(R.string.rpolloggingin);
		//Log into RPoL
    	if (RpolScraper.isLoggedIn( appContext ) == false){
    		if (!RpolScraper.login(appContext , username, password )){
    			publishProgress(R.string.rpolloginfailed);
    	    	return;
    		}
    	}
		publishProgress(R.string.rpolloggedin);


		//Register with GCM
		publishProgress(R.string.gcmloggingin);
		String gcmid = "";
		if (GCMRegistrar.isRegistered(appContext) == false){
        	GCMRegistrar.register( appContext, GCMIntentService.SENDER_ID );
        	//We'll give it ten seconds to respond (20 x 500ms)
        	for (int i = 0; i<20; i++){
            	try{
            		Log.d(Common.TAG, "Thread sleeping for 500ms before retry");
                	Thread.sleep(500);
            	} catch (InterruptedException ie) {
            		Log.d(Common.TAG, "Thread interupted.  Retries canceled");
        			publishProgress(R.string.gcmloginfailed);
                    return;
            	}
                gcmid = GCMRegistrar.getRegistrationId(appContext);
        		if ( !gcmid.equals("") ){
                	publishProgress(R.string.gcmloggedin);
        			break;
        		}
        	}
            if ( gcmid.equals("") ) {
            	publishProgress(R.string.gcmloginfailed);
            	return;
            }
        } else {
        	publishProgress(R.string.gcmloggedin);
        }

        
        //Register with notification server
		publishProgress(R.string.srvrloggingin);
        boolean registered =
        		GCMRegistrar.isRegisteredOnServer(appContext );
        if (!registered) {
        	if (!ServerUtilities.register(appContext, GCMRegistrar.getRegistrationId(appContext), username)){
            	publishProgress(R.string.srvrloginfailed);
            	return;
        	}
        }
    	publishProgress(R.string.srvrloggedin);

	} // end onHandleIntent
	
	private void publishProgress(int status){
		//send broadcast
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(STATUS);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		broadcastIntent.putExtra(STATUS, status);
		sendBroadcast(broadcastIntent);
	}
}
