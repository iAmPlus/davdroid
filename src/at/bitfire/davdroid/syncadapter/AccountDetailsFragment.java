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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import at.bitfire.davdroid.Constants;
import at.bitfire.davdroid.R;

public class AccountDetailsFragment extends Fragment implements TextWatcher {
	
	ServerInfo serverInfo;
	
	EditText editAccountName;
	
	Button button;
	
	//String account_server = null;
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.account_details, container, false);
		
		serverInfo = (ServerInfo)getArguments().getSerializable(Constants.KEY_SERVER_INFO);
		
		editAccountName = (EditText)v.findViewById(R.id.account_name);
		editAccountName.addTextChangedListener(this);
		
		button = (Button) v.findViewById(R.id.add_account_next);
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				addAccount();
			}
			
		});
	
		setHasOptionsMenu(true);
		return v;
	}


	// actions
	
	@SuppressLint("NewApi")
	void addAccount() {
		serverInfo = (ServerInfo)getArguments().getSerializable(Constants.KEY_SERVER_INFO);
		String accountName = editAccountName.getText().toString();
		serverInfo.setAccountName(accountName);
		Intent intent = new Intent(getActivity(), AccountAuthenticatorActivity.class);
		intent.putExtra(Constants.KEY_SERVER_INFO, serverInfo);
		startActivityForResult(intent, 0);
	}
	
	public void onActivityResult(int requestCode, int resultCode,
            Intent data) {
		Log.v("sk", "On activity result2");
        if (resultCode == Activity.RESULT_OK) {
        	Log.v("sk", "Result OK");
        	queryServer();
        }
    }
	
	void queryServer() {
		FragmentTransaction ft = getFragmentManager().beginTransaction();

		Bundle arguments = new Bundle();
		arguments.putSerializable(Constants.KEY_SERVER_INFO, serverInfo);

		DialogFragment dialog = new QueryServerDialogFragment();
		dialog.setArguments(arguments);
	    dialog.show(ft, QueryServerDialogFragment.class.getName());
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
