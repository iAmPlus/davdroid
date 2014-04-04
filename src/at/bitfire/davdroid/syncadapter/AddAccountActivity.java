/*******************************************************************************
 * Copyright (c) 2014 Richard Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Richard Hirner (bitfire web engineering) - initial API and implementation
 ******************************************************************************/
package at.bitfire.davdroid.syncadapter;

import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import at.bitfire.davdroid.R;

public class AddAccountActivity extends Activity {

	public static final String ARG_ACCOUNT_TYPE = null;
	public static final String ARG_AUTH_TYPE = null;
	public static final String ARG_ACCOUNT_NAME = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.add_account);

		if (savedInstanceState == null) {	// first call
			getFragmentManager().beginTransaction()
				.add(R.id.fragment_container, new EnterCredentialsFragment(), "enter_credentials")
				.commit();
		}

	}

	protected void onActivityResult(int requestCode, int resultCode,
			Intent data) {

		super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	public void finish() {
		AccountAuthenticatorResponse response = getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
		if (response != null) {
			response.onError(AccountManager.ERROR_CODE_CANCELED, "canceled");
		}
		super.finish();
	}

}
