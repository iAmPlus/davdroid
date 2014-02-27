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

import java.net.MalformedURLException;
import java.net.URL;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import at.bitfire.davdroid.R;
import at.bitfire.davdroid.URIUtils;

public class EnterCredentialsFragment extends Fragment implements TextWatcher {
	/*String protocol;*/

	/*TextView textAuthUrl;
	EditText editBaseURL, editUserName, editToken;
	CheckBox checkboxPreemptive;*/
	String account_server;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.enter_credentials, container, false);

		// protocol selection spinner
		//textAuthUrl = (TextView) v.findViewById(R.id.http_url);

		Spinner spnrProtocol = (Spinner) v.findViewById(R.id.select_server);
		spnrProtocol.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				account_server = parent.getAdapter().getItem(position).toString();
				/*String[] urlArray = getResources().getStringArray(R.array.auth_urls);
				auth_url = urlArray[position].concat("&client_id=")
											 .concat(client_id)
											 .concat("&redirect_uri=")
											 .concat(redirect_uri);
				textAuthUrl.setAutoLinkMask(Linkify.WEB_URLS);
				textAuthUrl.setText(auth_url);
				textAuthUrl.setVisibility(View.VISIBLE);*/
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				//textAuthUrl.setVisibility(View.GONE);
			}
		});
		spnrProtocol.setSelection(0);	// HTTPS

		// other input fields
		//editBaseURL = (EditText) v.findViewById(R.id.baseURL);
		//editBaseURL.addTextChangedListener(this);

/*		editUserName = (EditText) v.findViewById(R.id.userName);
		editUserName.addTextChangedListener(this);

		editToken = (EditText) v.findViewById(R.id.token);
		editToken.addTextChangedListener(this);

		checkboxPreemptive = (CheckBox) v.findViewById(R.id.auth_preemptive);*/

		// hook into action bar
		setHasOptionsMenu(true);

		return v;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
	    inflater.inflate(R.menu.enter_credentials, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.next:
			AccountDetailsFragment accountDetails = new AccountDetailsFragment();
			Bundle arguments = new Bundle();
			ServerInfo serverInfo = new ServerInfo(
					account_server
				);
			arguments.putSerializable(SelectCollectionsFragment.KEY_SERVER_INFO, serverInfo);
			accountDetails.setArguments(arguments);
			
			getFragmentManager().beginTransaction()
				.replace(R.id.fragment_container, accountDetails)
				.addToBackStack(null)
				.commitAllowingStateLoss();
			break;
		default:
			return false;
		}
		return true;
	}

	// input validation

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		boolean ok = true;
			/*editUserName.getText().length() > 0 &&
			editToken.getText().length() > 0;*/

		// check host name
/*		try {
			URL url = new URL(URIUtils.sanitize(auth_url));
			if (url.getHost() == null || url.getHost().isEmpty())
				ok = false;
		} catch (MalformedURLException e) {
			ok = false;
		}*/

		MenuItem item = menu.findItem(R.id.next);
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
