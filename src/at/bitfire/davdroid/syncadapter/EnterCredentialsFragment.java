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

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SlidingFrameLayout;
import android.widget.SlidingLayer;
import at.bitfire.davdroid.Constants;
import at.bitfire.davdroid.R;

public class EnterCredentialsFragment extends Fragment {

	SlidingLayer mSlidingLayer;

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
				String account_server = parent.getAdapter().getItem(position).toString();
				AccountDetailsFragment accountDetails = new AccountDetailsFragment();
				Bundle arguments = new Bundle();
				ServerInfo serverInfo = new ServerInfo(account_server);
				arguments.putSerializable(Constants.KEY_SERVER_INFO, serverInfo);
				accountDetails.setArguments(arguments);

				getFragmentManager().beginTransaction()
					.replace(R.id.fragment_container, accountDetails)
					.addToBackStack(null)
					.commitAllowingStateLoss();
			}

		});
		mSlidingLayer = (SlidingLayer) /*(SlidingLayer)getActivity()*/v.findViewById(R.id.slidinglayout);
		mSlidingLayer.setPositiveText("");
		mSlidingLayer.setPositiveButtonVisibility(View.INVISIBLE);
		mSlidingLayer.setNegativeText(this.getString(R.string.message_cancel_text));
		mSlidingLayer.setOnInteractListener(new SlidingFrameLayout.OnInteractListener(){
			@Override
			public void onPositiveAction(){
			}
			@Override
			public void onNegativeAction(){
				getActivity().onBackPressed();
				if(mSlidingLayer != null){
					mSlidingLayer.resetAction();
				}
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
	public void onViewCreated(View view, Bundle savedInstanceState) {
		if (mSlidingLayer != null) {
			mSlidingLayer.resetAction();
		}
	}

}
