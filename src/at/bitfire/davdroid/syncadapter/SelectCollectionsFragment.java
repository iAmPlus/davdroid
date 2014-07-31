/*******************************************************************************
 * Copyright (c) 2014 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package at.bitfire.davdroid.syncadapter;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import at.bitfire.davdroid.Constants;
import at.bitfire.davdroid.R;

public class SelectCollectionsFragment extends ListFragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
		Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.select_collections, container, false);
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
		Button cancel = (Button) view.findViewById(R.id.cancel);
		Button next = (Button) view.findViewById(R.id.next_action);
		cancel.setBackgroundColor(getActivity().getApplicationColor());
		next.setBackgroundColor(getActivity().getApplicationColor());

		cancel.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {
				final Activity activity = getActivity();
				activity.onBackPressed();
				activity.overridePendingTransition(
					android.R.anim.quick_exit_in,
					android.R.anim.quick_exit_out);
			}
		});

		next.setVisibility(View.GONE);
		next.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (getListView().getCheckedItemCount() > 0) {
				onDone();
				}
			}
		});

		final ListView listView = getListView();

		final ServerInfo serverInfo = (ServerInfo) getArguments()
			.getSerializable(Constants.KEY_SERVER_INFO);
		final SelectCollectionsAdapter adapter = new SelectCollectionsAdapter(
			view.getContext(), serverInfo);
		setListAdapter(adapter);

		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				int itemPosition = position - 1;	// one list header view at pos. 0
				if (adapter.getItemViewType(itemPosition) == SelectCollectionsAdapter.TYPE_ADDRESS_BOOKS_ROW) {

				// ((ServerInfo.ResourceInfo)adapter.getItem(itemPosition)).setEnabled(true);
				// unselect all other address books
				for (int pos = 1; pos <= adapter.getNAddressBooks(); pos++)
					if (pos != itemPosition) {
					listView.setItemChecked(pos + 1, false);
					}
				}
				boolean next_visible = (listView.getCheckedItemCount() > 0);
				Button next = (Button) getView().findViewById(R.id.next_action);
				next.setVisibility(next_visible?View.VISIBLE:View.GONE);
			}
		});
	}

	void onDone() {
		ServerInfo serverInfo = (ServerInfo) getArguments().getSerializable(Constants.KEY_SERVER_INFO);

		// synchronize only selected collections
		for (ServerInfo.ResourceInfo addressBook : serverInfo.getAddressBooks())
		addressBook.setEnabled(false);
		for (ServerInfo.ResourceInfo calendar : serverInfo.getCalendars())
		calendar.setEnabled(false);

		ListAdapter adapter = getListView().getAdapter();
		for (long id : getListView().getCheckedItemIds()) {
			int position = (int) id;	// +1 because header view is inserted at pos. 0
			ServerInfo.ResourceInfo info = (ServerInfo.ResourceInfo) adapter.getItem(position);
			info.setEnabled(true);
		}

		// pass to "account details" fragment
		AccountDetailsFragment accountDetails = new AccountDetailsFragment();
		Bundle arguments = new Bundle();
		arguments.putSerializable(Constants.KEY_SERVER_INFO, serverInfo);
		if (getArguments().getBundle(Constants.ACCOUNT_BUNDLE) != null) {
			arguments.putBundle(Constants.ACCOUNT_BUNDLE, getArguments().getBundle(Constants.ACCOUNT_BUNDLE));
		}
		accountDetails.setArguments(arguments);

		getFragmentManager().beginTransaction()
			.replace(R.id.fragment_container, accountDetails)
			.addToBackStack(null).commitAllowingStateLoss();
    }

}
