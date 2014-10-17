/*******************************************************************************
 * Copyright (c) 2014 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package at.bitfire.davdroid.syncadapter;

import android.accounts.AccountAuthenticatorActivity;
import android.content.Intent;
import android.os.Bundle;
import at.bitfire.davdroid.Constants;
import at.bitfire.davdroid.R;

public class AddAccountActivity extends AccountAuthenticatorActivity {

	public static final String ARG_ACCOUNT_TYPE = null;
	public static final String ARG_AUTH_TYPE = null;
	public static final String ARG_ACCOUNT_NAME = null;

 	// this boolean extra specifies whether in app menu needs to be displayed for this screen 
    	private static final String EXTRA_IS_DEVICE_SETUP = "isDeviceSetup";
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.add_account);
		
		boolean isDeviceSetup = false;
		if(getIntent() != null){
			if(getIntent().getExtras() != null)
			    isDeviceSetup = getIntent().getExtras().getBoolean(EXTRA_IS_DEVICE_SETUP);
		}
		
		if(isDeviceSetup) {
			/* Disable In-app menu */
			disableInAppMenu();
		}

		if(getIntent().hasExtra(Constants.ACCOUNT_KEY_ACCESS_TOKEN)) {
			getFragmentManager().beginTransaction()
			.add(R.id.fragment_container, new UserCredentialsFragment(), "user_details")
			.commit();
		}
		if (savedInstanceState == null) {	// first call
			getFragmentManager().beginTransaction()
				.add(R.id.fragment_container, new SelectServerFragment(), "select_server")
				.commit();
		}

	}

	protected void onActivityResult(int requestCode, int resultCode,
			Intent data) {

		super.onActivityResult(requestCode, resultCode, data);
	}

}
