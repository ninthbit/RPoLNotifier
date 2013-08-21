package com.evilknights.rpolnotifier;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity {

    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {        
        super.onCreate(savedInstanceState);        
        addPreferencesFromResource( R.xml.activity_settings );
        
        //log preference changes.  Will be replaced when per/game settings are stored in DB
        Preference.OnPreferenceChangeListener listener = new 
        		Preference.OnPreferenceChangeListener()
        	{
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Log.d(Common.TAG, preference.getKey() 
						+ " as a " + newValue.getClass().getCanonicalName()
						+ " = " + newValue.toString() );
				return true;
			}
		};
		getPreferenceScreen().setOnPreferenceChangeListener(listener);
    }

    @Override
    public void onDestroy(){
    	super.onDestroy();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 0, 0, R.string.options_purgeports);
        menu.add(Menu.NONE, 1, 1, R.string.options_unregister);
        menu.add(Menu.NONE, 2, 2, R.string.options_exit);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                clearPortraits();
                return true;
            case 1:
            	clearRegistration();
            	finish();
                return true;
            case 2:
                finish();
                return true;
        }
        return false;
    }

    private void clearRegistration(){
    	final Context context = getApplicationContext();
    	//Clear the login information for RPoL
    	new AsyncTask<Void,Void,Void>(){
    		@Override
        	protected Void doInBackground(Void... params) {
    	    	ServerUtilities.unregister(context);
	            SharedPreferences.Editor prefEditor = PreferenceManager
	                    .getDefaultSharedPreferences(context).edit();
	            prefEditor.remove("feedhash")
		            .remove("username")
		            .remove("uid")
		            .commit();
	            return null;
    		}
            @Override
            protected void onPostExecute(Void wth) {
                Toast.makeText(context, R.string.unregistercomplete, Toast.LENGTH_LONG).show();
            }
        }.execute();
    }

    private void clearPortraits(){
    	final Context context = getApplicationContext();
        PortraitManager pm = new PortraitManager(context);
        pm.open();
        pm.purge();
        pm.close();
    }
    
}
