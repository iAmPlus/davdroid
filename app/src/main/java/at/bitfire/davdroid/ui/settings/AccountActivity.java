/*
 * Copyright © 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.davdroid.ui.settings;

import android.accounts.Account;
import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;
import android.view.MenuItem;

import aneeda.view.Window;
import at.bitfire.davdroid.R;

public class AccountActivity extends Activity {
	public static final String EXTRA_ACCOUNT = "account";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_ANEEDA_ACTION_BAR);
		requestWindowFeature(Window.FEATURE_ANEEDA_ACTION_PANEL_LAYOUT);

		super.onCreate(savedInstanceState);

		setContentView(R.layout.settings_account_activity);

		final FragmentManager fm = getFragmentManager();
		AccountFragment fragment = (AccountFragment)fm.findFragmentById(R.id.account_fragment);
		if (fragment == null) {
			fragment = new AccountFragment();
			final Bundle args = new Bundle(1);
			Account account = getIntent().getExtras().getParcelable(EXTRA_ACCOUNT);
			args.putParcelable(AccountFragment.ARG_ACCOUNT, account);
			fragment.setArguments(args);

			getFragmentManager().beginTransaction()
				.add(R.id.account_fragment, fragment, SettingsActivity.TAG_ACCOUNT_SETTINGS)
				.commit();
		}
        ActionBar actionBar = getActionBar();
        if(actionBar != null){
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

}
      @Override
      public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
            	finish();
            	return true;
            }
         return super.onOptionsItemSelected(item);
     }

}
