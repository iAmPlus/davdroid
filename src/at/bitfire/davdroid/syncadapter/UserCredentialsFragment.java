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

import ch.boye.httpclientandroidlib.util.TextUtils;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import com.iamplus.aware.AwareSlidingLayout;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import at.bitfire.davdroid.Constants;
import at.bitfire.davdroid.R;

public class UserCredentialsFragment extends Fragment implements TextWatcher {
	
	@Override
	public void onResume() {
		super.onResume();
		if(mSlidingLayer != null)
			mSlidingLayer.reset();
	}

	AwareSlidingLayout mSlidingLayer = null;
	
	EditText editUserName, editPassword;
	ServerInfo serverInfo;
	

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.user_details, container, false);
		
		editUserName = (EditText) v.findViewById(R.id.user_name);
		editUserName.addTextChangedListener(this);
		
		editPassword = (EditText) v.findViewById(R.id.password);
		editPassword.addTextChangedListener(this);
		editPassword.setImeOptions(EditorInfo.IME_ACTION_DONE);
		editPassword.setOnEditorActionListener(
				new TextView.OnEditorActionListener () {
				@Override
				public boolean onEditorAction(TextView v,
						int actionId, KeyEvent event) {
					if(actionId == EditorInfo.IME_ACTION_DONE 
					|| actionId == EditorInfo.IME_NULL
					|| event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
						if(!TextUtils.isEmpty(editUserName.getText().toString()) 
								&& !TextUtils.isEmpty(editPassword.getText().toString()))
							queryServer();
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
					if(!TextUtils.isEmpty(editUserName.getText().toString()) 
								&& !TextUtils.isEmpty(editPassword.getText().toString()))
							queryServer();
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

	void queryServer() {
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		
		Bundle args = new Bundle();
		
		serverInfo = new ServerInfo("Yahoo");
		serverInfo.setAccountName(editUserName.getText().toString());
		
		AccountManager mAccountManager = AccountManager.get(getActivity().getApplicationContext());
		Account account = new Account(editUserName.getText().toString(), Constants.ACCOUNT_TYPE);
		Bundle userData = AccountSettings.createBundle(serverInfo);
		args.putSerializable(Constants.KEY_SERVER_INFO, serverInfo);

		if (mAccountManager.addAccountExplicitly(account, editPassword.getText().toString(), userData)) {
			mAccountManager.setUserData(account, "user_name", editUserName.getText().toString());
			mAccountManager.setUserData(account, Constants.ACCOUNT_SERVER, "Yahoo");
			// account created, now create calendars

			DialogFragment dialog = new QueryServerDialogFragment();
			dialog.setArguments(args);
		    dialog.show(ft, QueryServerDialogFragment.class.getName());
		} else
			Toast.makeText(getActivity(), "Couldn't create account (account with this name already existing?)", Toast.LENGTH_LONG).show();
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
