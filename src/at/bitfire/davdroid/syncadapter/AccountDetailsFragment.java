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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.account_details, container, false);

		serverInfo = (ServerInfo)getArguments().getSerializable(Constants.KEY_SERVER_INFO);

		editAccountName = (EditText)v.findViewById(R.id.account_name);

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
		if(isOnline()) {
			serverInfo = (ServerInfo)getArguments().getSerializable(Constants.KEY_SERVER_INFO);
			String accountName = editAccountName.getText().toString();
			serverInfo.setAccountName(accountName);
			Intent intent = new Intent(getActivity(), AccountAuthenticatorActivity.class);
			intent.putExtra(Constants.KEY_SERVER_INFO, serverInfo);
			startActivityForResult(intent, 0);
		} else {
			AlertDialog dialog;
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(getActivity().getResources().getString(R.string.no_network))
				.setMessage(getActivity().getResources().getString(R.string.connect_to_network))
				.setCancelable(false)
				.setNegativeButton(R.string.network_dialog_back,new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,int id) {
						// if this button is clicked, just close
						// the dialog box and do nothing
						dialog.cancel();
					}
				});
			dialog = builder.create();
			dialog.show();
		}
	}

	public void onActivityResult(int requestCode, int resultCode,
			Intent data) {
		if (resultCode == Activity.RESULT_OK) {
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

	private boolean isOnline() {
		ConnectivityManager cm =
			(ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		}
		return false;
	}
}
