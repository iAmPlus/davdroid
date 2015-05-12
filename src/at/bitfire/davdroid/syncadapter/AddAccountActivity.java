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

import aneeda.app.ActivityHelper;

public class AddAccountActivity extends AccountAuthenticatorActivity {

	public static final String ARG_ACCOUNT_TYPE = null;
	public static final String ARG_AUTH_TYPE = null;
	public static final String ARG_ACCOUNT_NAME = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.add_account);
		
		boolean isDeviceSetup = false;

		if(getIntent() != null){

			isDeviceSetup = getIntent().getBooleanExtra(Constants.EXTRA_IS_DEVICE_SETUP, false);
			if(isDeviceSetup) {
				/* Disable In-app menu */
				ActivityHelper.disableInAppMenu(this);
			}

			if(getIntent().hasExtra(Constants.ACCOUNT_KEY_ACCESS_TOKEN)) {

				UserCredentialsFragment ucFrag = new UserCredentialsFragment();
				Bundle b = new Bundle();
				b.putBoolean(Constants.EXTRA_IS_DEVICE_SETUP, isDeviceSetup);
				ucFrag.setArguments(b);

				getFragmentManager().beginTransaction()
				.add(R.id.fragment_container, ucFrag, "user_details")
				.commit();
			}
		}
		
		if (savedInstanceState == null) {	// first call

			SelectServerFragment ssFrag = new SelectServerFragment();
			Bundle b = new Bundle();
			b.putBoolean(Constants.EXTRA_IS_DEVICE_SETUP, isDeviceSetup);
			ssFrag.setArguments(b);

			getFragmentManager().beginTransaction()
				.add(R.id.fragment_container, ssFrag, "select_server")
				.commit();
		}

	}

	protected void onActivityResult(int requestCode, int resultCode,
			Intent data) {

		super.onActivityResult(requestCode, resultCode, data);
	}

}
