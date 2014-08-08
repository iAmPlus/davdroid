/*******************************************************************************
 * Copyright (c) 2014 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package at.bitfire.davdroid.syncadapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Fragment;
import android.app.Activity;
import android.content.ContentResolver;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.text.TextUtils;
import at.bitfire.davdroid.Constants;
import at.bitfire.davdroid.R;
import at.bitfire.davdroid.resource.LocalCalendar;
import at.bitfire.davdroid.resource.LocalStorageException;
import at.bitfire.davdroid.resource.ServerInfo;

public class AccountDetailsFragment extends Fragment {
	public static final String KEY_SERVER_INFO = "server_info";

	ServerInfo serverInfo;

	@Override
	public View onCreateView(LayoutInflater inflater,
			ViewGroup container, Bundle savedInstanceState) {

		serverInfo = (ServerInfo)getArguments().getSerializable(
			Constants.KEY_SERVER_INFO);

		addAccount(serverInfo.getAccountName());
		return null;
	}


	// actions
	
	void addAccount(String account_name) {

		getActivity().setResult(Activity.RESULT_CANCELED);

		if (TextUtils.isEmpty(account_name)) {
			Toast.makeText(getActivity(), R.string.account_name_empty, Toast.LENGTH_LONG).show();
			getActivity().finish();
			return;
		}

		AccountManager accountManager = AccountManager.get(getActivity());
		Account []accounts = accountManager.getAccountsByType(Constants.ACCOUNT_TYPE);
		Boolean already_exist = false;
		for (Account acc : accounts) {
			if(acc.name.equals(account_name)) {
				already_exist = true;
				break;
			}
		}
		if(already_exist) {
			Toast.makeText(getActivity().getBaseContext(),R.string.error_account_name_exist,
					Toast.LENGTH_SHORT).show();
			if(TextUtils.isEmpty(serverInfo.getAccountName())) {
				return;
			} else {
				getActivity().finish();
			}
		}
		Account account = new Account(account_name, Constants.ACCOUNT_TYPE);
		Bundle userData = AccountSettings.createBundle(serverInfo);
		
		Bundle accountData = getArguments().getBundle(Constants.ACCOUNT_BUNDLE);
		if(accountData != null) {
			userData.putAll(accountData);
		}
		boolean syncContacts = false;
		for (ServerInfo.ResourceInfo addressBook : serverInfo.getAddressBooks())
			if (addressBook.isEnabled()) {
				ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 1);
				syncContacts = true;
				continue;
			}
		if (syncContacts) {
			ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 1);
			ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true);
		} else
			ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 0);
		
		if (accountManager.addAccountExplicitly(account, serverInfo.getPassword(), userData)) {
			// account created, now create calendars
			if(userData.containsKey(Constants.ACCOUNT_KEY_ACCESS_TOKEN)) {
				accountManager.setAuthToken(account, Constants.ACCOUNT_KEY_ACCESS_TOKEN,
						userData.getString(Constants.ACCOUNT_KEY_ACCESS_TOKEN));
			}
			if(userData.containsKey(Constants.ACCOUNT_KEY_REFRESH_TOKEN)) {
				accountManager.setAuthToken(account, Constants.ACCOUNT_KEY_REFRESH_TOKEN,
						userData.getString(Constants.ACCOUNT_KEY_REFRESH_TOKEN));
			}
			boolean syncCalendars = false;
			for (ServerInfo.ResourceInfo calendar : serverInfo.getCalendars())
				if (calendar.isEnabled())
					try {
						LocalCalendar.create(account, getActivity().getContentResolver(), calendar);
						syncCalendars = true;
					} catch (LocalStorageException e) {
						Toast.makeText(getActivity(), "Couldn't create calendar(s): " + e.getMessage(), Toast.LENGTH_LONG).show();
					}
			if (syncCalendars) {
				ContentResolver.setIsSyncable(account, CalendarContract.AUTHORITY, 1);
				ContentResolver.setSyncAutomatically(account, CalendarContract.AUTHORITY, true);
			} else
				ContentResolver.setIsSyncable(account, CalendarContract.AUTHORITY, 0);
			
			getActivity().setResult(Activity.RESULT_OK);
			getActivity().finish();
		} else
			Toast.makeText(getActivity(), R.string.account_already_exists, Toast.LENGTH_LONG).show();
	}
}
