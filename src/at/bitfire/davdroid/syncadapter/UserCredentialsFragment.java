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
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.CheckBox;

import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import at.bitfire.davdroid.Constants;
import at.bitfire.davdroid.R;
import at.bitfire.davdroid.URIUtils;

public class UserCredentialsFragment extends Fragment {
	String protocol;
	
	EditText editBaseURL, editUserName, editPassword;
	CheckBox checkboxPreemptive;

	//EditText editUserName, editPassword;
	ServerInfo serverInfo;
	Account reauth_account = null;

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.user_details, container, false);

        final AccountManager mgr = AccountManager.get(getActivity()
                .getApplicationContext());

        if (getActivity().getIntent().hasExtra(
                Constants.ACCOUNT_KEY_ACCESS_TOKEN)) {
            reauth_account = getActivity().getIntent().getParcelableExtra(
                    Constants.ACCOUNT_PARCEL);
            serverInfo = new ServerInfo(mgr.getUserData(reauth_account,
                    Constants.ACCOUNT_SERVER));
        } else
            serverInfo = (ServerInfo) getArguments().getSerializable(
                    Constants.KEY_SERVER_INFO);

        Spinner spnrProtocol = (Spinner) v.findViewById(R.id.select_protocol);
        editBaseURL = (EditText) v.findViewById(R.id.baseURL);
        checkboxPreemptive = (CheckBox) v.findViewById(R.id.auth_preemptive);
        if (!serverInfo.getAccountServer().equals("Other")) {
            editBaseURL.setVisibility(View.GONE);
            checkboxPreemptive.setVisibility(View.GONE);
            spnrProtocol.setVisibility(View.GONE);
        } else {
            spnrProtocol
                    .setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent,
                                View view, int position, long id) {
                            protocol = parent.getAdapter().getItem(position)
                                    .toString();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                            protocol = null;
                        }
                    });
            spnrProtocol.setSelection(1); // HTTPS
            if (reauth_account != null) {
                editBaseURL.setText(mgr.getUserData(reauth_account,
                        Constants.ACCOUNT_KEY_BASE_URL));
                editBaseURL.setInputType(InputType.TYPE_NULL);
            }
        }

        editUserName = (EditText) v.findViewById(R.id.user_name);
        if (reauth_account != null) {
            editUserName.setText(reauth_account.name);
            editUserName.setInputType(InputType.TYPE_NULL);
        }

        editPassword = (EditText) v.findViewById(R.id.password);
        editPassword.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editPassword
                .setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId,
                            KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE
                                || actionId == EditorInfo.IME_NULL
                                || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                            if (!TextUtils.isEmpty(editUserName.getText()
                                    .toString())
                                    && !TextUtils.isEmpty(editPassword
                                            .getText().toString()))
                                if (reauth_account != null) {
                                    mgr.setPassword(reauth_account,
                                            editPassword.getText().toString());
                                    getActivity().finish();
                                } else
                                    queryServer();
                            return true;
                        }
                        return false;
                    }
                });

        if (serverInfo.getAccountServer().equals("Google")) {
            editUserName.setVisibility(View.GONE);
            editPassword.setVisibility(View.GONE);
            Intent intent = new Intent(getActivity(),
                    AuthenticatorActivity.class);
            intent.putExtra(Constants.KEY_SERVER_INFO, serverInfo);
            if (reauth_account != null)
                intent.putExtra(Constants.ACCOUNT_PARCEL, reauth_account);
            startActivityForResult(intent, 0);
            return v;
        }

        Button cancel = (Button) v.findViewById(R.id.cancel);
        Button next = (Button) v.findViewById(R.id.next_action);
        cancel.setBackgroundColor(getActivity().getApplicationColor());
        next.setBackgroundColor(getActivity().getApplicationColor());
        cancel.setVisibility(View.VISIBLE);
        next.setVisibility(View.VISIBLE);

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

        next.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(editUserName.getText().toString())
                        && !TextUtils
                                .isEmpty(editPassword.getText().toString())) {
                    queryServer();
                }
            }
        });

        return v;
    }

	void queryServer() {
			Bundle args = new Bundle();
			String host_path = editBaseURL.getText().toString();
			args.putString(Constants.ACCOUNT_KEY_BASE_URL, URIUtils.sanitize(protocol + host_path));

            //Fix For Yahoo Not Syncing Contacts
	        String userName = editUserName.getText().toString();
	        int atIndex = userName.indexOf('@');
	        userName = atIndex != -1 ? userName.substring(0, atIndex) : userName;

			serverInfo.setAccountName(editUserName.getText().toString());
			serverInfo.setUserName(userName);
			serverInfo.setPassword(editPassword.getText().toString());
			args.putSerializable(Constants.KEY_SERVER_INFO, serverInfo);

			nextTransaction(args);
	}

	public void onActivityResult(int requestCode, int resultCode,
			Intent data) {
		if(reauth_account != null) {
			AccountManager accountManager = AccountManager.get(getActivity());
            if (data == null) {
                getActivity().onBackPressed();
                Log.d("IamPlusSyncAdapter:UserCredentialsFrag", "onActivityResult : intent null");
                return;
            }
			Bundle reauth_bundle = data.getBundleExtra(Constants.ACCOUNT_BUNDLE);
			if(reauth_bundle.containsKey(Constants.ACCOUNT_KEY_ACCESS_TOKEN)) {
				accountManager.setAuthToken(reauth_account, Constants.ACCOUNT_KEY_ACCESS_TOKEN,
						reauth_bundle.getString(Constants.ACCOUNT_KEY_ACCESS_TOKEN));
			}
			if(reauth_bundle.containsKey(Constants.ACCOUNT_KEY_REFRESH_TOKEN)) {
				accountManager.setAuthToken(reauth_account, Constants.ACCOUNT_KEY_REFRESH_TOKEN,
						reauth_bundle.getString(Constants.ACCOUNT_KEY_REFRESH_TOKEN));
			}
			if(reauth_bundle.containsKey("oauth_expires_in")) {
				accountManager.setUserData(reauth_account, "oauth_expires_in",
						reauth_bundle.getString("oauth_expires_in"));
			}
			getActivity().finish();
		}
        if (resultCode == Activity.RESULT_OK) {
            Bundle arguments = new Bundle();
            if (data == null) {
                getActivity().onBackPressed();
                Log.d("IamPlusSyncAdapter:UserCredentialsFrag", "onActivityResult : intent null");
                return;
            }
            serverInfo.setAccountName(data.getStringExtra("account_name"));
            arguments.putSerializable(Constants.KEY_SERVER_INFO, serverInfo);
            arguments.putBundle(Constants.ACCOUNT_BUNDLE, data.getBundleExtra(Constants.ACCOUNT_BUNDLE));
            nextTransaction(arguments);
        } else {
            getActivity().onBackPressed();
        }
	}
	
	void nextTransaction(Bundle arguments) {
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		DialogFragment dialog = new QueryServerDialogFragment();
		dialog.setArguments(arguments);
		dialog.show(ft, QueryServerDialogFragment.class.getName());
	}
}
