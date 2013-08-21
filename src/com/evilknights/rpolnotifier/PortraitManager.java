package com.evilknights.rpolnotifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.Connection.Response;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class PortraitManager {
	public final String TAG = Common.TAG;
	private SQLiteDatabase db;
	private mSQLhelper dbHelper;
	private static final String TABLE = "portraits";
	private Context cntx;
	
	public PortraitManager(Context context){
		cntx = context;
		dbHelper = new mSQLhelper(context);
	}
	
	public void open() throws SQLException{
		db = dbHelper.getWritableDatabase();
	}
	
	public void close(){
		dbHelper.close();
	}

        public void purge(){
                db.delete(TABLE, null, null);
        }

    public Portrait getPortrait(String gameid) {
    	Log.d(TAG, "Searching DB for " + gameid);
	    Cursor cursor = db.query(TABLE, new String[] { "gameid", "path", 
	    		"url" }, "gameid=?", new String[] { gameid },
	    		null, null, null, null);
	    Log.d(TAG, "Query ran");
	    Portrait result = null;
	    if ((cursor != null) && (cursor.getCount()>0) ) {
	    	Log.d(TAG, "Cursor appears valid");
	    	cursor.moveToFirst();
	    	result = new Portrait(cursor.getString(0), cursor.getString(1), 
	    			cursor.getString(2));
	    } else {
	    	Log.d(TAG, "Cursor is bad, Fetching image");
	    	result = fetchImage(gameid);
	    	setPortrait(result);
	    }
	    cursor.close();
	    return result;
    }
    
    private Portrait fetchImage(String gameID){
    	String url = RpolScraper.getPortraitURL(cntx, gameID);
    	Log.d(TAG, "Scraper returned: " + url);
    	boolean returnfail = true;
    	String fileName = "";
    	
    	// check for a valid URL
    	if ( !url.equals("NO_LINK") && !url.equals("IO_ERROR") ){
    		fileName = cntx.getFilesDir() +"/"+ url.substring( 
    				url.lastIndexOf('/')+1, url.length() );
    		Response resultImageResponse;  //create jsoup response
    	    
    		try {
    			resultImageResponse = Jsoup
    					.connect( url )
    					.ignoreContentType(true)
    					.execute();
    			Log.d(TAG, "Jsoup fetched, saving to "+fileName);
    			try {
    		    	FileOutputStream out = new FileOutputStream(
    		    			new File(fileName ));
    		        out.write(resultImageResponse.bodyAsBytes());
    		        out.close();
    		        returnfail = false;
    		        Log.d(TAG, "File saved");
    			} catch (FileNotFoundException e) {
    					Log.d(TAG, "Failed to save file: " + fileName);
    			}
    		} catch (IOException e1) {
    			Log.d(TAG, "Failed to download file: " + url);
    		}
        }
    	
    	Portrait newPortrait;
    	if (returnfail == true){
			newPortrait =  new Portrait(gameID, "", url);
		} else {
			newPortrait =  new Portrait(gameID, fileName, url);
		}
    	Log.d(TAG, "Returning newPortrait to getPortrait");
		return newPortrait;
    }
     
    // Add new portrait
    public long setPortrait(Portrait portIn) {
        ContentValues values = new ContentValues();
        values.put("gameid", portIn.gameid);
        values.put("path", portIn.path);
        values.put("url", portIn.url);
        Log.d(TAG, "setPortrait trying to update table");
        long count = db.update(TABLE, values, "gameid="+portIn.gameid, null);
        if (count < 1){
        	Log.d(TAG, "Bad count " + Long.toString(count) + ", insterting");
        	count = db.insert(TABLE, null, values);
        }
        return count;
    }

    // ############      Included Subclass     ###############
    private class mSQLhelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "dataHandler";
        private static final int DATABASE_VERSION = 1;
       
        public mSQLhelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);            }

        @Override
        public void onCreate(SQLiteDatabase db) {
        	String sql = "CREATE TABLE " + TABLE + "(_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            		+ "gameid TEXT UNIQUE, " + "path TEXT, " + "url TEXT )";
        	Log.d(Common.TAG, "onCreate " + sql);
            db.execSQL(sql);
        	Log.d(Common.TAG, "onCreate finished");
        }
     
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        	Log.d(Common.TAG, "onUpgrade");
        	switch( oldVersion ){
	        	case 1:{  Log.d(Common.TAG, "Updating to 1");       }
	        	case 2:{  Log.d(Common.TAG, "Updating to 2");       }
        	}
        	if (db.getVersion() < newVersion ){
        		 Log.d(Common.TAG, "ReVersioning failed, dumping all data and recreating");
            	db.execSQL("DROP TABLE IF EXISTS " + TABLE);
            	onCreate(db);
        	}
        }
    }// end of subclass
   
}//end of class
