/*******************************************************************************
 * Copyright (c) 2014 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *	 Richard Hirner (bitfire web engineering) - initial API and implementation
 ******************************************************************************/
package at.bitfire.davdroid.syncadapter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import android.app.DialogFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.Activity;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.Button;
import at.bitfire.davdroid.Constants;
import at.bitfire.davdroid.R;
import at.bitfire.davdroid.resource.DavResourceFinder;
import at.bitfire.davdroid.resource.ServerInfo;
import at.bitfire.davdroid.webdav.DavException;
import ch.boye.httpclientandroidlib.HttpException;
import ch.boye.httpclientandroidlib.util.TextUtils;

public class QueryServerDialogFragment extends DialogFragment
		implements LoaderCallbacks<ServerInfo> {
	private static final String TAG = "davdroid.QueryServerDialogFragment";
	static ServerInfo serverInfo = null;
	static Context mContext;
	static Bundle userData = null;
	Loader<ServerInfo> loader = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Holo);
		setCancelable(false);
	}

	public void createLoader() {

		loader =
				getLoaderManager().initLoader(0, getArguments(), this);
		loader.forceLoad();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.progress_dialog, container, false);

		mContext = getActivity();
		serverInfo = (ServerInfo)
				getArguments().getSerializable(Constants.KEY_SERVER_INFO);
		userData = getArguments().getBundle(Constants.ACCOUNT_BUNDLE);
		Button cancel = (Button) v.findViewById(R.id.cancel);
        cancel.setBackgroundColor(getActivity().getApplicationColor());
        cancel.setVisibility(View.VISIBLE);

        cancel.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View v) {
                final Activity activity = getActivity();
                loader.cancelLoad();
                getLoaderManager().destroyLoader(0);
                getDialog().dismiss();
                getFragmentManager().popBackStackImmediate();
                //((FragmentAlertDialog)activity).doNegativeClick();
                activity.overridePendingTransition(
                        android.R.anim.quick_exit_in,
                        android.R.anim.quick_exit_out);
            }
        });
		createLoader();

		return v;
	}

	@Override
	public Loader<ServerInfo> onCreateLoader(int id, Bundle args) {
		Log.i(TAG, "onCreateLoader");
		return new ServerInfoLoader(getActivity(), args);
	}

	@Override
	public void onLoadFinished(Loader<ServerInfo> loader,
			ServerInfo serverInfo) {
		if (serverInfo.getErrorMessage() != null && !(serverInfo.getErrorMessage().isEmpty()))
			Toast.makeText(getActivity(),
				serverInfo.getErrorMessage(), Toast.LENGTH_LONG).show();
		if ((!serverInfo.getAddressBooks().isEmpty()) || (!serverInfo.getCalendars().isEmpty())) {
			SelectCollectionsFragment selectCollections =
				new SelectCollectionsFragment();
			Bundle arguments = new Bundle();
			arguments.putSerializable(Constants.KEY_SERVER_INFO, serverInfo);
			if(userData != null) {
				arguments.putBundle(Constants.ACCOUNT_BUNDLE, userData);
			}
			selectCollections.setArguments(arguments);

			getFragmentManager().beginTransaction()
				.replace(R.id.fragment_container, selectCollections)
				.addToBackStack(null)
				.commitAllowingStateLoss();
		}
		
		getDialog().dismiss();
	}

	@Override
	public void onLoaderReset(Loader<ServerInfo> arg0) {
	}

	static class ServerInfoLoader extends AsyncTaskLoader<ServerInfo> {
		private static final String TAG = "davdroid.ServerInfoLoader";
		final Bundle args;
		final Context context;
		
		public ServerInfoLoader(Context context, Bundle args) {
			super(context);
			this.context = context;
			this.args = args;
		}

		@Override
		public ServerInfo loadInBackground() {
			AccountDeatilsReader reader = new AccountDeatilsReader(context);
			Properties properties =
				reader.getProperties(serverInfo.getAccountServer());
			serverInfo.setErrorMessage("");

			if(serverInfo.getAccountServer().equals("Google")) {
				userData.putString("client_id", properties.getProperty("client_id_value"));
				userData.putString(properties.getProperty("client_secret_name"), properties.getProperty("client_secret_value"));
				userData.putString("token_url", properties.getProperty("token_url"));
				userData.putString(Constants.ACCOUNT_SERVER, properties.getProperty("type"));
				String authBearer = "Bearer " + userData.getString(Constants.ACCOUNT_KEY_ACCESS_TOKEN);
				serverInfo.setAccessToken(authBearer);
			}

			if (!TextUtils.isEmpty(properties.getProperty(Constants.ACCOUNT_KEY_CARDDAV_URL))) {
				serverInfo.setCarddavURL(properties.getProperty(Constants.ACCOUNT_KEY_CARDDAV_URL));
			}
			if(!TextUtils.isEmpty(properties.getProperty(Constants.ACCOUNT_KEY_BASE_URL)) ) {
				serverInfo.setBaseURL(properties.getProperty(Constants.ACCOUNT_KEY_BASE_URL));
			}
			if (!TextUtils.isEmpty(properties.getProperty(Constants.ACCOUNT_KEY_CALDAV_URL))) {
				serverInfo.setCaldavURL(properties.getProperty(Constants.ACCOUNT_KEY_CALDAV_URL));
			}

			serverInfo.setErrorMessage("");
			try {
				DavResourceFinder.findResources(context, serverInfo);
			} catch (URISyntaxException e) {
				serverInfo.setErrorMessage(getContext().getString(R.string.exception_uri_syntax, e.getMessage()));
			}  catch (IOException e) {
				serverInfo.setErrorMessage(getContext().getString(R.string.exception_io, e.getLocalizedMessage()));
			} catch (HttpException e) {
				Log.e(TAG, "HTTP error while querying server info", e);
				serverInfo.setErrorMessage(getContext().getString(R.string.exception_http, e.getLocalizedMessage()));
			} catch (DavException e) {
				Log.e(TAG, "DAV error while querying server info", e);
				serverInfo.setErrorMessage(getContext().getString(R.string.exception_incapable_resource, e.getLocalizedMessage()));
			}
			
			return serverInfo;
		}
	}

}
