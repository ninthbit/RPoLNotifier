package com.evilknights.rpolnotifier;

import com.google.android.gcm.GCMRegistrar;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;



/**
 * Helper class that handles interactions with Applications Server
 */
public final class ServerUtilities {

    private static final int MAX_ATTEMPTS = 5;
    private static final int BACKOFF_MILLI_SECONDS = 2000;
    private static final Random random = new Random();

    /**
     * Register this account/device pair within the server.
     *
     * @return whether the registration succeeded or not.
     */
    static boolean register(final Context ctx, final String regId, final String username) {
    	final Context context = ctx.getApplicationContext();
    	String serverUrl = getSiteURL(context);
        Map<String, String> postdata = new HashMap<String, String>();
        
        //load variables for post to registration service
        String feedhash = RpolScraper.getFeedHash(context);
        postdata.put("feedhash", feedhash);
        postdata.put("gcmid", regId);
        postdata.put("username", username);
        try {
			postdata.put("appversion", context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0).versionName );
		} catch (NameNotFoundException e) {
			postdata.put("appversion", "NameNotFound");
		}

        Log.d(Common.TAG, "registering to srvr: (url = \"" + serverUrl + "\"), "
        		+ "(userid = \"" + username + "\"), " 
        		+ "(feedhash = \"" + feedhash + "\"), " 
        		+ "(gcmid = \"" + regId + "\")" );
 
        // postit.
        long backoff = BACKOFF_MILLI_SECONDS;
        int retcode = 0;
        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
        	Log.d(Common.TAG, "Attempt #" + i + " of " + MAX_ATTEMPTS + "attempts to register");
        	retcode = post(serverUrl, postdata);
        	if (retcode == 200){
            	Log.d(Common.TAG, "Registration succeeded" );
            	GCMRegistrar.setRegisteredOnServer( context, true);
            	//long lifespan = 13 * 7 * 24 * 60 * 60 * 1000;  // about 3 months in milliseconds
            	long lifespan = 7862400000l;
            	GCMRegistrar.setRegisterOnServerLifespan( context, lifespan);
            	return true;
        	} else {
            	Log.d(Common.TAG, "Attempt to register failed: " + Integer.toString(retcode) );
        	}

        	try{
        		Log.d(Common.TAG, "Tread sleeping for " + Long.toString(backoff) + "ms before try");
            	Thread.sleep(backoff);
        	} catch (InterruptedException ie) {
        		Log.d(Common.TAG, "Tread interupted.  Retries canceled");
                return false;
        	}
            backoff = BACKOFF_MILLI_SECONDS + random.nextInt(1000);
        }
        return false;
    }

    /**
     * Unregister this account/device pair within the server.
     */
    static boolean unregister(final Context ctx) {
    	Context context = ctx.getApplicationContext();
    	SharedPreferences sharedPrefs =
                PreferenceManager.getDefaultSharedPreferences(context); //Get Pref

    	String username = sharedPrefs.getString("username", "");
        String feedhash = RpolScraper.getFeedHash(context);

        Log.d(Common.TAG, "Unregistering: (username = " + username + ")");
    	
    	String serverUrl = getSiteURL(context);
        Map<String, String> postdata = new HashMap<String, String>();
        
        //load variables for post to registration service
        postdata.put("feedhash", feedhash);
        postdata.put("gcmid", "");
        postdata.put("username", username);
        try {
			postdata.put("appversion", context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0).versionName );
		} catch (NameNotFoundException e) {
			postdata.put("appversion", "NameNotFound");
		}
 
        Log.d(Common.TAG, "unregistering to srvr: (url = \"" + serverUrl + "\"), "
        		+ "(userid = \"" + username + "\"), " 
        		+ "(feedhash = \"" + feedhash + "\"), " 
        		+ "(gcmid = \"\")" );
 
        // postit.
        long backoff = BACKOFF_MILLI_SECONDS;
        int retcode = 0;
        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
        	Log.d(Common.TAG, "Attempt #" + i + " of " + MAX_ATTEMPTS + "attempts to register");
        	retcode = post(serverUrl, postdata);
        	if (retcode == 200){
            	Log.d(Common.TAG, "Unregistration succeeded" );
            	GCMRegistrar.setRegisteredOnServer(context, false);
            	return true;
        	} else {
            	Log.d(Common.TAG, "Attempt to Unregister failed: " + Integer.toString(retcode) );
        	}

        	try{
        		Log.d(Common.TAG, "Tread sleeping for " + Long.toString(backoff) + "ms before try");
            	Thread.sleep(backoff);
        	} catch (InterruptedException ie) {
        		Log.d(Common.TAG, "Tread interupted.  Retries canceled");
                return false;
        	}
            backoff = BACKOFF_MILLI_SECONDS + random.nextInt(1000);
        }
        return false;

    }

    /**
     * Issue a POST request to the server.
     *
     * @param endpoint POST address.
     * @param postdata request post parameters.
     *
     */
    private static int post(String endpoint, Map<String, String> postdata){
    	Response res = null;
    	Map<String, String> cookies = null;

        // http://jsoup.org/apidocs/org/jsoup/Connection.Response.html
    	try{
    		res = Jsoup
              .connect(endpoint)
              .data(postdata)
              .method(Connection.Method.POST)
              .execute();
            cookies = res.cookies();
          }
    	catch(HttpStatusException hse){
    		//if we have an actual code, return it
    		return hse.getStatusCode();
    	}
    	catch(IOException ioe){
    		//generic connection failure, not bothering with catching everything
    		return 666;
    	}

    	//we didn't io fail, so return the result
    	if ( (res.statusCode() == 200) && (cookies.get("success").equals("true")) ){
            return res.statusCode();
    	} else {
    		//we'll use this for internal DB failure until the php script can return correctly
    		return 500;
    	}

    }

    /**
     * @param context
     * @return			Returns either production or beta URL
     */
    public static String getSiteURL(Context context){
    	boolean isDebuggable = 
    			( 0 != ( context.getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE ) );
    	if (isDebuggable){
    		return "http://www.evilknights.com/rpol/Register-beta.php";
    	} else {
    		return "http://www.evilknights.com/rpol/Register.php";
    	}
    }

}