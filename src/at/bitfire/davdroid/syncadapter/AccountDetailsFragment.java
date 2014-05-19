/*******************************************************************************
 * Copyright (c) 2014 Richard Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *	 Richard Hirner (bitfire web engineering) - initial API and implementation
 ******************************************************************************/
package at.bitfire.davdroid.syncadapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Fragment;
import android.content.ContentResolver;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.text.TextUtils;
import android.util.Log;
import com.iamplus.aware.AwareSlidingLayout;
import at.bitfire.davdroid.Constants;
import at.bitfire.davdroid.R;
import at.bitfire.davdroid.resource.LocalCalendar;

public class AccountDetailsFragment extends Fragment {
	public static final String KEY_SERVER_INFO = "server_info";

	@Override
	public void onResume() {
		super.onResume();
		if(mSlidingLayer != null)
			mSlidingLayer.reset();
	}

	ServerInfo serverInfo;

	EditText editAccountName;

	AwareSlidingLayout mSlidingLayer = null;

	@Override
	public View onCreateView(LayoutInflater inflater,
			ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.account_details, container, false);

		serverInfo = (ServerInfo)getArguments().getSerializable(
			Constants.KEY_SERVER_INFO);
		if(TextUtils.isEmpty(serverInfo.getAccountName())) {

			editAccountName = (EditText)v.findViewById(R.id.account_name);
	
			editAccountName.setImeOptions(EditorInfo.IME_ACTION_DONE);
			editAccountName.setOnEditorActionListener(
					new TextView.OnEditorActionListener () {
					@Override
					public boolean onEditorAction(TextView v,
							int actionId, KeyEvent event) {
						if((actionId == EditorInfo.IME_ACTION_DONE 
						|| actionId == EditorInfo.IME_NULL
						|| event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
						&& !TextUtils.isEmpty(editAccountName.getText().toString())) {
							addAccount(editAccountName.getText().toString());
							return true;
						}
						return false;
					}
				});
		} else
			addAccount(serverInfo.getAccountName());

		mSlidingLayer = (AwareSlidingLayout)v.findViewById(R.id.slidinglayout);
		mSlidingLayer.setOnActionListener(new AwareSlidingLayout.OnActionListener(){
			@Override
			public void onAction(int type){
				switch(type) {
				case AwareSlidingLayout.POSITIVE:
					if(!TextUtils.isEmpty(editAccountName.getText().toString())) {
							addAccount(editAccountName.getText().toString());
					}
					if(mSlidingLayer != null){
						mSlidingLayer.reset();
					}
					break;

				case AwareSlidingLayout.NEGATIVE:
					getActivity().onBackPressed();
					if(mSlidingLayer != null){
						mSlidingLayer.reset();
					}
					break;
				}
			}
		});

		return v;
	}


	// actions

	void addAccount(String account_name) {
		try {

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
					if (calendar.isEnabled()) {
						LocalCalendar.create(account, getActivity().getContentResolver(), calendar);
						syncCalendars = true;
					}
				if (syncCalendars) {
					ContentResolver.setIsSyncable(account, CalendarContract.AUTHORITY, 1);
					ContentResolver.setSyncAutomatically(account, CalendarContract.AUTHORITY, true);
				} else
					ContentResolver.setIsSyncable(account, CalendarContract.AUTHORITY, 0);
				
				getActivity().finish();
			} else
				Toast.makeText(getActivity(), "Couldn't create account (account with this name already existing?)", Toast.LENGTH_LONG).show();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
}
