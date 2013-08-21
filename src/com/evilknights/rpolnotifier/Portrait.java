package com.evilknights.rpolnotifier;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class Portrait {
	 String gameid;
	 String path;
	 String url;
	 
	 public Portrait(String id, String p, String u){
		 gameid = id;
		 path = p;
		 url = u;
	 }
	 
	 public Bitmap getBitmap(){
		 	Bitmap bmPortrait;

	    	if ( (path != null) && (!path.equals("")) ){
	    		bmPortrait = BitmapFactory.decodeFile(path);
	    	} else {
	    		bmPortrait = null;
	    	}
	    	return bmPortrait;
	 }
}