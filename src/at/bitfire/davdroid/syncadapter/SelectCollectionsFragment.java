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
import android.widget.SlidingFrameLayout;
import android.widget.SlidingLayer;
import at.bitfire.davdroid.Constants;
import at.bitfire.davdroid.R;
import at.bitfire.davdroid.resource.LocalCalendar;

public class SelectCollectionsFragment extends Fragment {

	SlidingLayer mSlidingLayer;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.select_collections, container, false);
		mSlidingLayer = (SlidingLayer)v.findViewById(R.id.slidinglayout);
		mSlidingLayer.setPositiveText(this.getString(R.string.next_action));
		mSlidingLayer.setNegativeText("");
		mSlidingLayer.setNegativeButtonVisibility(View.INVISIBLE);
		mSlidingLayer.setOnInteractListener(new SlidingFrameLayout.OnInteractListener(){
			@Override
			public void onPositiveAction(){

				getActivity().finish();

				if(mSlidingLayer != null){
					mSlidingLayer.resetAction();
				}
			}
			@Override
			public void onNegativeAction(){
			}
			@Override
			public boolean onPositiveActionStart() {

				return false;
			}

			@Override
			public boolean onNegativeActionStart() {
				return false;
			}

		});
		return v;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);


		ServerInfo serverInfo = (ServerInfo)getArguments().getSerializable(Constants.KEY_SERVER_INFO);

		// synchronize only selected collections
		for (ServerInfo.ResourceInfo addressBook : serverInfo.getAddressBooks())
			addressBook.setEnabled(true);
		for (ServerInfo.ResourceInfo calendar : serverInfo.getCalendars())
			calendar.setEnabled(true);

		try {
			AccountManager accountManager = AccountManager.get(getActivity().getApplicationContext());
			Account account = new Account(serverInfo.getAccountName(), Constants.ACCOUNT_TYPE);
			if(serverInfo.getBaseURL() != null)
				accountManager.setUserData(account, Constants.ACCOUNT_KEY_BASE_URL, serverInfo.getBaseURL());
			else {
				if(serverInfo.getCaldavURL() != null)
					accountManager.setUserData(account, Constants.ACCOUNT_KEY_CALDAV_URL, serverInfo.getCaldavURL());
				if(serverInfo.getCarddavURL() != null)
					accountManager.setUserData(account, Constants.ACCOUNT_KEY_CARDDAV_URL, serverInfo.getCarddavURL());
			}

			boolean syncContacts = false;
			for (ServerInfo.ResourceInfo addressBook : serverInfo.getAddressBooks())
				if (addressBook.isEnabled()) {
					accountManager.setUserData(account, Constants.ACCOUNT_KEY_ADDRESSBOOK_PATH, addressBook.getPath());
					ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 1);
					syncContacts = true;
					continue;
				}
			if (syncContacts) {
				ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 1);
				ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true);
			} else
				ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 0);

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


		} catch (RemoteException e) {
		}


	}

}
