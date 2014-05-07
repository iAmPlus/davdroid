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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
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

public class AccountDetailsFragment extends Fragment {

	@Override
	public void onResume() {
		super.onResume();
		if(mSlidingLayer != null)
			mSlidingLayer.reset();
	}

	ServerInfo serverInfo;

	EditText editAccountName;

	AwareSlidingLayout mSlidingLayer = null;

	//String account_server = null;

	@Override
	public View onCreateView(LayoutInflater inflater,
			ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.account_details, container, false);

		serverInfo = (ServerInfo)getArguments().getSerializable(
			Constants.KEY_SERVER_INFO);

		editAccountName = (EditText)v.findViewById(R.id.account_name);
		editAccountName.setImeOptions(EditorInfo.IME_ACTION_DONE);
		editAccountName.setOnEditorActionListener(
				new TextView.OnEditorActionListener () {
				@Override
				public boolean onEditorAction(TextView v,
						int actionId, KeyEvent event) {
					if(actionId == EditorInfo.IME_ACTION_DONE 
					|| actionId == EditorInfo.IME_NULL
					|| event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
						addAccount();
						return true;
					}
					return false;
				}
			});

		mSlidingLayer = (AwareSlidingLayout)v.findViewById(R.id.slidinglayout);
		mSlidingLayer.setOnActionListener(new AwareSlidingLayout.OnActionListener(){
			@Override
			public void onAction(int type){
				switch(type) {
				case AwareSlidingLayout.POSITIVE:
					addAccount();
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

	@SuppressLint("NewApi")
	void addAccount() {
		if(TextUtils.isEmpty(editAccountName.getText().toString())) {
			Toast.makeText(getActivity().getBaseContext(),R.string.error_empty_account_name,
						Toast.LENGTH_SHORT).show();
			return;
		}
		String account_name = editAccountName.getText().toString();
		AccountManager mAccountManager = AccountManager.get(getActivity().getApplicationContext());
		Account []accounts = mAccountManager.getAccountsByType(Constants.ACCOUNT_TYPE);
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
			return;
		}
	}

	void queryServer() {
		/*FragmentTransaction ft = getFragmentManager().beginTransaction();

		Bundle arguments = new Bundle();
		arguments.putSerializable(Constants.KEY_SERVER_INFO, serverInfo);

		DialogFragment dialog = new QueryServerDialogFragment();
		dialog.setArguments(arguments);
		dialog.show(ft, QueryServerDialogFragment.class.getName());*/
	}
}
