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

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import at.bitfire.davdroid.Constants;
import at.bitfire.davdroid.R;
import at.bitfire.davdroid.URIUtils;

public class EnterCredentialsFragment extends Fragment {
	String protocol;

	ServerInfo serverInfo;
	TextView textHttpWarning;
	EditText editBaseURL, editUserName, editPassword;
	CheckBox checkboxPreemptive;
	Button btnNext;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.enter_credentials, container, false);

		// protocol selection spinner
		/*textHttpWarning = (TextView) v.findViewById(R.id.http_warning);

		Spinner spnrProtocol = (Spinner) v.findViewById(R.id.select_protocol);
		spnrProtocol.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				protocol = parent.getAdapter().getItem(position).toString();
				textHttpWarning.setVisibility(protocol.equals("https://") ? View.GONE : View.VISIBLE);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				protocol = null;
			}
		});
		spnrProtocol.setSelection(1);	// HTTPS

		// other input fields
		editBaseURL = (EditText) v.findViewById(R.id.baseURL);
		editUserName = (EditText) v.findViewById(R.id.userName);
		editPassword = (EditText) v.findViewById(R.id.password);
		
		checkboxPreemptive = (CheckBox) v.findViewById(R.id.auth_preemptive);*/

		return v;
	}

	void queryServer() {
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		
		Bundle args = new Bundle();
		
		String host_path = editBaseURL.getText().toString();
		args.putString(Constants.ACCOUNT_KEY_BASE_URL, URIUtils.sanitize(protocol + host_path));
		args.putString(Constants.ACCOUNT_KEY_USERNAME, editUserName.getText().toString());
		args.putString(Constants.ACCOUNT_KEY_PASSWORD, editPassword.getText().toString());
		args.putBoolean(Constants.ACCOUNT_KEY_AUTH_PREEMPTIVE, checkboxPreemptive.isChecked());
		
		DialogFragment dialog = new QueryServerDialogFragment();
		dialog.setArguments(args);
		dialog.show(ft, QueryServerDialogFragment.class.getName());
	}

}
