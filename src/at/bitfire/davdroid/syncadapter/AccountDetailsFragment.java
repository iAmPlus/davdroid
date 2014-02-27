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
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import at.bitfire.davdroid.Constants;
import at.bitfire.davdroid.R;
import at.bitfire.davdroid.resource.LocalCalendar;

public class AccountDetailsFragment extends Fragment implements TextWatcher {
	public static final String KEY_SERVER_INFO = "server_info";
	
	ServerInfo serverInfo;
	
	EditText editAccountName;
	
	//String account_server = null;
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.account_details, container, false);
		
		serverInfo = (ServerInfo)getArguments().getSerializable(KEY_SERVER_INFO);
		
		editAccountName = (EditText)v.findViewById(R.id.account_name);
		editAccountName.addTextChangedListener(this);
		
		/*TextView textAccountNameInfo = (TextView)v.findViewById(R.id.account_name_info);
		if (!serverInfo.hasEnabledCalendars())
			textAccountNameInfo.setVisibility(View.GONE);*/
	
		setHasOptionsMenu(true);
		return v;
	}
	
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
	    inflater.inflate(R.menu.account_details, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.add_account:
			addAccount();
			break;
		default:
			return false;
		}
		return true;
	}


	// actions
	
	@SuppressLint("NewApi")
	void addAccount() {
		ServerInfo serverInfo = (ServerInfo)getArguments().getSerializable(KEY_SERVER_INFO);
		//try {
		String accountName = editAccountName.getText().toString();
		serverInfo.setAccountName(accountName);
			/*Bundle bnd = new Bundle();
			bnd.putString(Constants.ACCOUNT_KEY_ACCOUNT_NAME, accountName);
			bnd.putString(Constants.ACCOUNT_KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);*/
		Intent intent = new Intent(getActivity(), AccountAuthenticatorActivity.class);
		intent.putExtra(Constants.ACCOUNT_KEY_ACCOUNT_NAME, accountName);
		intent.putExtra(Constants.ACCOUNT_KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
		startActivityForResult(intent, 0);
			//startActivity(intent);
			
			/*AccountManager accountManager = AccountManager.get(getActivity().getApplicationContext());
			Account account = new Account(accountName, Constants.ACCOUNT_TYPE);
			Bundle userData = new Bundle();
			userData.putString(Constants.ACCOUNT_KEY_BASE_URL, serverInfo.getBaseURL());
			
			boolean syncContacts = false;
			for (ServerInfo.ResourceInfo addressBook : serverInfo.getAddressBooks())
				if (addressBook.isEnabled()) {
					userData.putString(Constants.ACCOUNT_KEY_ADDRESSBOOK_PATH, addressBook.getPath());
					ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 1);
					syncContacts = true;
					continue;
				}
			if (syncContacts) {
				ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 1);
				ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true);
			} else
				ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 0);
			
			if (accountManager.addAccountExplicitly(account, "", userData)) {
				// account created, now create calendars
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
				
				//getActivity().finish();
				account_server = serverInfo.getBaseURL();
				account_server.concat("?access_token=" + accountManager.getAuthToken(account, "access_token", null, getActivity(), null, null));
				
			} else
				Toast.makeText(getActivity(), "Couldn't create account (account with this name already existing?)", Toast.LENGTH_LONG).show();*/

		/*} catch (RemoteException e) {
		}*/
	}
	
	public void onActivityResult(int requestCode, int resultCode,
            Intent data) {
		Log.v("sk", "On activity result2");
        if (resultCode == Activity.RESULT_OK) {
        	Log.v("sk", "Result OK");
            // A contact was picked.  Here we will just display it
            // to the user.
        	queryServer();
        }
    }
	
	void queryServer() {
		FragmentTransaction ft = getFragmentManager().beginTransaction();

		Bundle arguments = new Bundle();
		arguments.putSerializable(SelectCollectionsFragment.KEY_SERVER_INFO, serverInfo);
		//selectCollections.setArguments(arguments);

		/*String host_path = auth_url;
		args.putString(QueryServerDialogFragment.EXTRA_BASE_URL, URIUtils.sanitize(host_path));
		args.putString(QueryServerDialogFragment.EXTRA_USER_NAME, editUserName.getText().toString());
		args.putString(QueryServerDialogFragment.EXTRA_TOKEN, editToken.getText().toString());
		args.putBoolean(QueryServerDialogFragment.EXTRA_AUTH_PREEMPTIVE, checkboxPreemptive.isChecked());*/
		//args.putString(QueryServerDialogFragment.EXTRA_ACCOUNT_SERVER, account_server);

		DialogFragment dialog = new QueryServerDialogFragment();
		dialog.setArguments(arguments);
	    dialog.show(ft, QueryServerDialogFragment.class.getName());
	}

	
	// input validation
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		boolean ok = false;
		ok = editAccountName.getText().length() > 0;
		MenuItem item = menu.findItem(R.id.add_account);
		item.setEnabled(ok);
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		getActivity().invalidateOptionsMenu();
	}

	@Override
	public void afterTextChanged(Editable s) {
	}
}
