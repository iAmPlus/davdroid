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

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
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

public class SelectServerFragment extends Fragment {

	AwareSlidingLayout mSlidingLayer;
	ServerInfo serverInfo;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.select_server, container, false);

		ListView listView = (ListView) v.findViewById(R.id.select_server_list);
		String[] values = getResources().getStringArray(R.array.server_names);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(v.getContext(),
				android.R.layout.simple_list_item_1, android.R.id.text1, values);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if(isOnline()) {
					String account_server = parent.getAdapter().getItem(position).toString();

					serverInfo = new ServerInfo(account_server);
					Bundle args = new Bundle();
					args.putSerializable(Constants.KEY_SERVER_INFO, serverInfo);
					UserCredentialsFragment ucf = new UserCredentialsFragment();
					ucf.setArguments(args);
					getFragmentManager().beginTransaction()
						.replace(R.id.fragment_container, ucf)
						.addToBackStack(null)
						.commitAllowingStateLoss();
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

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		if (mSlidingLayer != null) {
			mSlidingLayer.reset();
		}
	}

}
