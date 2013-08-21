package com.evilknights.rpolnotifier;

import com.google.android.gcm.GCMRegistrar;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private LoginResponseReciever receiver;
    
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Context context = this.getApplicationContext();

    	// Make sure the device has the proper dependencies.
        GCMRegistrar.checkDevice( context );
        // Make sure the manifest was properly set
        GCMRegistrar.checkManifest( context );

        IntentFilter filter = new IntentFilter(AppLoginService.STATUS);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        receiver = new LoginResponseReciever();

        //Check Login Status
        if (checkRPOL(context) && checkGCM(context) && checkSRVR(context) ){
        	//launch Settings
        	startActivity(new Intent(this, SettingsActivity.class));
        	finish();  //kill login activity
        } else {
        	//If not setup, Display screen
        	setContentView(R.layout.activity_main);
            registerReceiver(receiver, filter);
        	
        }
    }
    
	
    @Override
    protected void onDestroy() {
    	try{
        	unregisterReceiver(receiver);
    	} catch (IllegalArgumentException e){
    		//do nothing
    	}
    	
        GCMRegistrar.onDestroy(this.getApplicationContext() );
        super.onDestroy();
    }

    public void onClickedCheckboxShowPass(View v){
    	CheckBox chkShowPass = (CheckBox) findViewById(R.id.checkbox_showpass);
    	EditText txtPassword = (EditText) findViewById(R.id.password);

    	if (chkShowPass.isChecked() == false) {
    		txtPassword.setInputType(
    			InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    	} else {
    		txtPassword.setInputType(
   				InputType.TYPE_CLASS_TEXT);
    	}
    }
    
    // BROADCAST RECEIVER
    public class LoginResponseReciever extends BroadcastReceiver {

    	@Override
    	public void onReceive(Context context, Intent intent) {
//    		String status = intent.getStringExtra("DEFINE_ME");
    		int status = intent.getIntExtra(AppLoginService.STATUS, R.string.loginfailed);
    		
     		TextView txtStatus = (TextView) findViewById(R.id.loginstatus);
        	txtStatus.setText(status);
        	Log.d(Common.TAG, "Progress update: " + context.getString(status) );

        	//Set checkboxes or display dialog
        	switch( status ) {
           		case R.string.rpolloggingin:{
           			CheckBox chkRPOL = (CheckBox) findViewById(R.id.checkbox_rpol);
           			chkRPOL.setEnabled(true);
           			break;
                }
                case R.string.rpolloggedin:{
                	CheckBox chkRPOL = (CheckBox) findViewById(R.id.checkbox_rpol);
                	chkRPOL.setChecked(true);
                	break;
                }
                case R.string.rpolloginfailed:{
                	regError(context, R.string.rpolloginfailed, R.string.rpolloginfailedmessage );
                	break;
                }
           		case R.string.gcmloggingin:{
           			CheckBox chkGCM = (CheckBox) findViewById(R.id.checkbox_gcm);
           			chkGCM.setEnabled(true);
           			break;
                }
                case R.string.gcmloggedin:{
            		CheckBox chkGCM = (CheckBox) findViewById(R.id.checkbox_gcm);
            		chkGCM.setChecked(true);
            		break;
	            }
	            case R.string.gcmloginfailed:{
                	regError(context, R.string.gcmloginfailed, R.string.gcmloginfailedmessage );
                	break;
	            }
	            case R.string.srvrloggingin:{
	            	CheckBox chkSRVR = (CheckBox) findViewById(R.id.checkbox_srvr);
	            	chkSRVR.setEnabled(true);
	            	break;
		        }
	            case R.string.srvrloggedin:{
	            	CheckBox chkSRVR = (CheckBox) findViewById(R.id.checkbox_srvr);
	                chkSRVR.setChecked(true);
	                Toast.makeText(context, R.string.registrationcomplete, Toast.LENGTH_SHORT).show();
	                Log.d(Common.TAG, "Launching Notification Settings");
	                startActivity(new Intent(context, SettingsActivity.class));
	            	finish();  //kill login activity
	                break;
	            }
	            case R.string.srvrloginfailed:{
	            	regError(context, R.string.srvrloginfailed, R.string.srvrloginfailedmessage );
	            	break;
	            }
	            case R.string.loginfailed:{
	            	regError(context, R.string.loginfailed, R.string.loginfailedmessage );
	            	break;
	            }

        	}
    	}
    }
    
    public void onClickedButtonLogin(View v){
//    	final Context context = v.getContext().getApplicationContext();
    	EditText username = (EditText) findViewById(R.id.username);
    	EditText password = (EditText) findViewById(R.id.password);
    	
    	preExecLogin();
    	
    	Intent loginIntent = new Intent(this, AppLoginService.class);
    	loginIntent.putExtra(AppLoginService.USERNAME, username.getText().toString() );
    	loginIntent.putExtra(AppLoginService.PASSWORD, password.getText().toString() );
    	startService(loginIntent);
    	
    }
    
	private void preExecLogin(){
		// Kill the button until task completes
		Button button = (Button) findViewById(R.id.login);
		button.setEnabled(false);
		// Clear the boxes
			CheckBox chkRPOL = (CheckBox) findViewById(R.id.checkbox_rpol);
    	chkRPOL.setChecked(false);
			chkRPOL.setEnabled(false);
			CheckBox chkGCM = (CheckBox) findViewById(R.id.checkbox_gcm);
		chkGCM.setChecked(false);
			chkGCM.setEnabled(false);
    	CheckBox chkSRVR = (CheckBox) findViewById(R.id.checkbox_srvr);
        chkSRVR.setChecked(false);
    	chkSRVR.setEnabled(false);
	}

    private void regError(Context context, int title, int message){
    	Log.d("RPoLregE", "Reg Error: " + message + context.getString(message) );
    	// re-enable the login button
    	Button button = (Button) findViewById(R.id.login);
		button.setEnabled(true);
		//notify the user
    	AlertDialog.Builder dlgAlert = new AlertDialog.Builder(context);
    	dlgAlert.setTitle(getString(title));
    	dlgAlert.setMessage(getString(message));
    	dlgAlert.setPositiveButton("Ok",
    		    new DialogInterface.OnClickListener() {
    		        public void onClick(DialogInterface dialog, int which) {
    		          //dismiss the dialog  
    		        }
    		    });
    	dlgAlert.setCancelable(true);
    	dlgAlert.create().show();
    }
    
    private boolean checkRPOL(Context context){
    	if (RpolScraper.isLoggedIn(context)){
    		return true;
    	} else {
    		return false;
    	}
    }
    
    private boolean checkGCM(Context context){
    	//return true if GCM registered
        final String regId = GCMRegistrar.getRegistrationId( context );
        Log.d(Common.TAG, "GCM ID: " + regId);
        if (regId.equals("")) {
        	return false;
        } else {
        	return true;
        }
    }
    
    private boolean checkSRVR(Context context){
    	//return true if server is registered
        if (GCMRegistrar.isRegisteredOnServer( context )) {
        	Log.d(Common.TAG, "Is registered on server");
        	return true;
        	
        } else {
        	Log.d(Common.TAG, "Is NOT registered on server");
        	return false;
        }
    }

} // MainActivity
