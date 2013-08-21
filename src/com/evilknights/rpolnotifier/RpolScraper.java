package com.evilknights.rpolnotifier;

//import com.evilknights.rpolnotifier.DataHandler;

import android.content.Context;
import android.util.Log;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.preference.PreferenceManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.Connection;
import org.jsoup.Connection.Response;


/**
 * Helper class that handles interactions with RolePlay Online
 */

public final class RpolScraper {

	final private static String TAG = Common.TAG;
	
    /**
     * @param context
     * @return			Empty string returned if not logged in
     */
    public static String getUID(Context ctx){
    	final Context context = ctx.getApplicationContext();
    	//check for uid in quick prefs
        SharedPreferences sharedPrefs =
                PreferenceManager.getDefaultSharedPreferences(context);
        String uid = sharedPrefs.getString("uid", "");
        Log.d(TAG, "UID fetched: " + uid);
        return uid;
    }

    /**
     * @param context
     * @return			Debug = beta-uid else uid
     */
    public static String getUIDName(Context context){
    	boolean isDebuggable = 
    			( 0 != ( context.getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE ) );
    	if (isDebuggable){
    		return "beta-uid";
    	} else {
    		return "uid";
    	}
    }
    
    /**
     * @param context
     * @return			Returns either mobile or beta URL
     */
    public static String getSiteURL(Context context){
    	boolean isDebuggable = 
    			( 0 != ( context.getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE ) );
    	if (isDebuggable){
    		return "http://beta.rpol.net";
    	} else {
    		return "http://m.rpol.net";
    	}
    }
    
    /**
     * @param context
     * @param username
     * @param password
     */
    public static boolean login(Context context, String username, String password){
    	Log.d(TAG, "Logging in with User:\"" + username + "\" and Pass:\"" + password);
    	Map<String, String> cookies = null;
        Response res = null;
        String strURL = getSiteURL(context) + "/login.cgi";
        Log.d(TAG, "URL: "+ strURL);
        try{
          res = Jsoup
            .connect(strURL)
            .data("username", username, "password", password, "perm", "1", 
            		"redir", "1", "specialaction", "Login")
            .method(Connection.Method.POST)
            .execute();
          cookies = res.cookies();
        } 
        catch(IOException ioe){
        	cookies = new HashMap<String, String>();
        }

        //  If we connect AND get a valid session, save the session
        SharedPreferences sharedPrefs =
                PreferenceManager.getDefaultSharedPreferences(context); //Get Pref
        SharedPreferences.Editor prefEditor = sharedPrefs.edit();       //get editor

        //check if uid or beta-uid  (debug uses beta site)
        String uidName = getUIDName(context);
        Log.d(TAG, "UID name: " + uidName);
        Boolean loggedin = false;
        if (cookies.containsKey( uidName ) ){
            String uid = cookies.get(uidName);
            Log.d(TAG, "UID: Value:"+uid);

        	if (!uid.equals("")){
        		prefEditor.putString("uid", cookies.get(uidName) );
        		prefEditor.putString("username", username );
        		Log.d(TAG, "UID saved");
        		loggedin =  true;
        	} else {
        		Log.d(TAG, "UID field is empty");
        		loggedin = false;
        	}
        } else {
        	Log.d(TAG, "UID not present in cookies");
        	prefEditor.putString("uid", "");
        	loggedin = false;
        }
        prefEditor.commit();
        return loggedin;
    }

    /**
     * @param context
     * @return			Returns NO_LOGIN, NO_LINK, or IO_ERROR on failure 
     */
    public static String getFeedHash(Context context) {

    	SharedPreferences sharedPrefs =
                PreferenceManager.getDefaultSharedPreferences(context); //Get Pref

    	if (sharedPrefs.contains("feedhash")){
    		return sharedPrefs.getString("feedhash", "");
    	}

    	
    	Log.d(TAG, "Fetching feed hash from RPoL");
    	String siteURL = getSiteURL(context);
    	String url = siteURL + "/usermodules/profile.cgi?action=feeds";
        String linkurl = "";
        Log.d(TAG, "Feeds URL: " +url);
        try{
          Document doc = Jsoup
            .connect(url)
            .cookie(getUIDName(context), getUID(context))
            .get();

          // parse for the link  (apparently the homepage has this tag in the header
          // may replace with a more accurate snatch from there later.
          Elements links = doc.select("a[href]");
          for (Element link : links) {
             linkurl = link.attr("abs:href");
             // If the link is the Atom Feed (starts with a 3), then return it
             if (linkurl.startsWith( siteURL + "/feeds.cgi?q=3" )){
            	 String hash = linkurl.split("=")[1];
            	 Log.d(TAG, "Hash found: "+ hash);
    			 if ( hash.equals("3CEMCCmJ0dgpxGwAsehU") ){
    				 return "NO_LOGIN";}
    			 else {
    				 sharedPrefs.edit().putString("feedhash", hash);
    				 return hash; }
             }
          }
          Log.d(TAG, "No link found on page");
          return "NO_LINK"; // no link was found, default return is NO LINK
        } catch(IOException ioe){
        	Log.d(TAG, "IO error, page failed to fetch");
        	return "IO_ERROR";}  // failed to pull a page
    }

    /**
     * @param context
     * @param gameID
     * @return 			Returns NO_LINK, or IO_ERROR on failure
     */
    public static String getPortraitURL(Context context, String gameID){
    	String url = getSiteURL(context) + "/usermodules/profile.cgi?gi=" + gameID;

    	try{
          Document doc = Jsoup
            .connect(url)
            .cookie(getUIDName(context), getUID(context) )
            .get();

/*          //starts with "http:​/​/​rpol"  & ends with "aaa000.jpg"
          Elements imgs = doc.select("img[src^=http:​/​/​rpol]").select(
        		  "img[src~=[a-z][a-z][a-z][0-9][0-9][0-9].(jpe?g|png|gif)$]");
*/
          Elements imgs = doc.select("img[src^=http://rpol]");

          Log.d(TAG, "Selected " + Integer.toString(imgs.size() ) );

          for (Element img : imgs) {
              Log.d(TAG, "Img: " + img.attr("src"));
          }
          
          if (imgs.isEmpty() ){
        	  Log.d(TAG, "Portrait scraped at: NO_LINK");
        	  return "NO_LINK";
          } else {
        	  Log.d(TAG, "Portrait scraped at: "+ imgs.first().attr("src"));
        	  return imgs.first().attr("src");  //should only be one at this point
          }

        } catch(IOException ioe){
        	Log.d(Common.TAG, "Portrait scraped at: IO_ERROR");
        	return "IO_ERROR";}  // failed to pull a page

    }
    
    /**
     * @param context
     * @return true if logged in, else false
     */
    public static boolean isLoggedIn(Context context){
    	//return true if logged in
    	//UID is the cookie from a valid web session
    	String uid = getUID(context);
        if ( uid.equals("") ){
        	return false;
        	//call sub to check the box
        } else {
        	return true;
        }

    }

    
}
