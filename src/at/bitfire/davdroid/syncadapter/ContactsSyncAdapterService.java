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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import lombok.Synchronized;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Service;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import at.bitfire.davdroid.Constants;
import at.bitfire.davdroid.resource.CardDavAddressBook;
import at.bitfire.davdroid.resource.LocalAddressBook;
import at.bitfire.davdroid.resource.LocalCollection;
import at.bitfire.davdroid.resource.RemoteCollection;

public class ContactsSyncAdapterService extends Service {
	private static ContactsSyncAdapter syncAdapter;

	@Override @Synchronized
	public void onCreate() {
		if (syncAdapter == null)
			syncAdapter = new ContactsSyncAdapter(getApplicationContext());
	}

	@Override
	public void onDestroy() {
		syncAdapter.close();
		syncAdapter = null;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return syncAdapter.getSyncAdapterBinder();
	}

	private static class ContactsSyncAdapter extends DavSyncAdapter {
		private final static String TAG = "davdroid.ContactsSyncAdapter";


		private ContactsSyncAdapter(Context context) {
			super(context);
		}

		@Override
		protected Map<LocalCollection<?>, RemoteCollection<?>> getSyncPairs(Account account, ContentProviderClient provider) {
			AccountSettings settings = new AccountSettings(getContext(), account);
			String addressBookPath = accountManager.getUserData(account, Constants.ACCOUNT_KEY_ADDRESSBOOK_PATH);
			if (addressBookPath == null)
				return null;

			try {
				String	userName = null,
						password = null;
				String accessToken = null;
				if(accountManager.getUserData(account, Constants.ACCOUNT_SERVER).equals("Google")) {
					AccountManagerFuture<Bundle> authBundle = accountManager.getAuthToken(account, Constants.ACCOUNT_KEY_ACCESS_TOKEN, null, null, null, null);
					accessToken = authBundle.getResult().getString(AccountManager.KEY_AUTHTOKEN);
					accessToken = "Bearer " + accessToken;
				} else {
					userName = settings.getUserName();
					password = settings.getPassword();
				}

				LocalCollection<?> database = new LocalAddressBook(account, provider, settings);

				URI uri = null;
				if(accountManager.getUserData(account, Constants.ACCOUNT_KEY_BASE_URL) != null)
					uri = new URI(accountManager.getUserData(account, Constants.ACCOUNT_KEY_BASE_URL)).resolve(addressBookPath);
				else if(accountManager.getUserData(account, Constants.ACCOUNT_KEY_CARDDAV_URL) != null)
					uri = new URI(accountManager.getUserData(account, Constants.ACCOUNT_KEY_CARDDAV_URL)).resolve(addressBookPath);
				else
					return null;

				RemoteCollection<?> dav = null;
				if(accountManager.getUserData(account, Constants.ACCOUNT_SERVER).equals("Google")) {
					dav = new CardDavAddressBook(httpClient, uri.toString(), accessToken);
				}
				if(accountManager.getUserData(account, Constants.ACCOUNT_SERVER).equals("Yahoo")) {
					dav = new CardDavAddressBook(httpClient, uri.toString(), userName, password, true);
				}

				Map<LocalCollection<?>, RemoteCollection<?>> map = new HashMap<LocalCollection<?>, RemoteCollection<?>>(2);
				map.put(database, dav);

				return map;
			} catch (URISyntaxException ex) {
				Log.e(TAG, "Couldn't build address book URI", ex);
			} catch (OperationCanceledException ex) {
				Log.e(TAG, "OAuth canceled", ex);
			} catch (AuthenticatorException ex) {
				Log.e(TAG, "OAuth authentication error", ex);
			} catch (IOException ex) {
				Log.e(TAG, "OAuth failed, network error", ex);
			}

			return null;
		}
	}
}
