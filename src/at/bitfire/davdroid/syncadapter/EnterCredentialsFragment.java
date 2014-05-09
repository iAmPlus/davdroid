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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.iamplus.aware.AwareSlidingLayout;
import at.bitfire.davdroid.Constants;
import at.bitfire.davdroid.R;

public class EnterCredentialsFragment extends Fragment {

	AwareSlidingLayout mSlidingLayer;
	ServerInfo serverInfo;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.enter_credentials, container, false);

		ListView listView = (ListView) v.findViewById(R.id.select_server);
		String[] values = getResources().getStringArray(R.array.server_names);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(v.getContext(),
				android.R.layout.simple_list_item_1, android.R.id.text1, values);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if(isOnline()) {
					String account_server = parent.getAdapter().getItem(position).toString();
					if(account_server.equals("Yahoo")) {
						UserCredentialsFragment uf = new UserCredentialsFragment();
						getFragmentManager().beginTransaction()
							.replace(R.id.fragment_container, uf)
							.addToBackStack(null)
							.commitAllowingStateLoss();
					} else {
						//serverInfo = (ServerInfo)getArguments().getSerializable(Constants.KEY_SERVER_INFO);
						//String accountName = editAccountName.getText().toString();
						serverInfo = new ServerInfo(account_server);
						//serverInfo.setAccountName(accountName);
						Intent intent = new Intent(getActivity(), AuthenticatorActivity.class);
						intent.putExtra(Constants.KEY_SERVER_INFO, serverInfo);
						startActivityForResult(intent, 0);
					}
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

		});
		mSlidingLayer = (AwareSlidingLayout) v.findViewById(R.id.slidinglayout);
		mSlidingLayer.setOnActionListener(new AwareSlidingLayout.OnActionListener(){
			
			@Override
			public void onAction(int type){
				if(type == AwareSlidingLayout.NEGATIVE) {
					getActivity().onBackPressed();
					if(mSlidingLayer != null){
						mSlidingLayer.reset();
					}
				}
			}
		});

		return v;
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

	public void onActivityResult(int requestCode, int resultCode,
			Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			/*serverInfo = (ServerInfo)getArguments().getSerializable(
				Constants.KEY_SERVER_INFO);*/
			Bundle arguments = new Bundle();
			serverInfo.setAccountName(data.getStringExtra("account_name"));
			//serverInfo.setCookie(data.getStringExtra("cookie"));
			arguments.putSerializable(Constants.KEY_SERVER_INFO, serverInfo);
			FragmentTransaction ft = getFragmentManager().beginTransaction();

			/*Bundle arguments = new Bundle();
			arguments.putSerializable(Constants.KEY_SERVER_INFO, serverInfo);*/

			DialogFragment dialog = new QueryServerDialogFragment();
			dialog.setArguments(arguments);
			dialog.show(ft, QueryServerDialogFragment.class.getName());
			//accountDetails.setArguments(arguments);

			/*getFragmentManager().beginTransaction()
				.replace(R.id.fragment_container, accountDetails)
				.addToBackStack(null)
				.commitAllowingStateLoss();*/
		}
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		if (mSlidingLayer != null) {
			mSlidingLayer.reset();
		}
	}

}
