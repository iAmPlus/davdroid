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
import android.app.ListFragment;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import at.bitfire.davdroid.Constants;
import at.bitfire.davdroid.R;
import at.bitfire.davdroid.resource.LocalCalendar;

public class SelectCollectionsFragment extends ListFragment {
	
	Button button;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = super.onCreateView(inflater, container, savedInstanceState);
		setHasOptionsMenu(true);
		return v;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		setListAdapter(null);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		final ListView listView = getListView();
		listView.setPadding(20, 30, 20, 30);
		
		View header = getActivity().getLayoutInflater().inflate(R.layout.select_collections_header, null);
		View footer = getActivity().getLayoutInflater().inflate(R.layout.select_collections_footer, null);
		listView.addHeaderView(header);
		listView.addFooterView(footer);
		
		final ServerInfo serverInfo = (ServerInfo)getArguments().getSerializable(Constants.KEY_SERVER_INFO);
		final SelectCollectionsAdapter adapter = new SelectCollectionsAdapter(serverInfo);
		setListAdapter(adapter);
		
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		button = (Button) footer.findViewById(R.id.finish_button);
		listView.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				int itemPosition = position - 1;	// one list header view at pos. 0
				if (adapter.getItemViewType(itemPosition) == SelectCollectionsAdapter.TYPE_ADDRESS_BOOKS_ROW) {
					// unselect all other address books
					for (int pos = 1; pos <= adapter.getNAddressBooks(); pos++)
						if (pos != itemPosition)
							listView.setItemChecked(pos + 1, false);
				}
				
				button.setEnabled(true);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				button.setEnabled(false);
				
			}
		});

		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				ServerInfo serverInfo = (ServerInfo)getArguments().getSerializable(Constants.KEY_SERVER_INFO);
				
				// synchronize only selected collections
				for (ServerInfo.ResourceInfo addressBook : serverInfo.getAddressBooks())
					addressBook.setEnabled(false);
				for (ServerInfo.ResourceInfo calendar : serverInfo.getCalendars())
					calendar.setEnabled(false);
				
				ListAdapter adapter = getListView().getAdapter();
				for (long id : getListView().getCheckedItemIds()) {
					int position = (int)id + 1;		// +1 because header view is inserted at pos. 0 
					ServerInfo.ResourceInfo info = (ServerInfo.ResourceInfo)adapter.getItem(position);
					info.setEnabled(true);
				}
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
				
				// pass to "account details" fragment
				Intent intent = new Intent(Settings.ACTION_ADD_ACCOUNT);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);

			}
			
		});
	}

}
