/*******************************************************************************
 * Copyright (c) 2014 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package at.bitfire.davdroid.syncadapter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

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
import android.os.RemoteException;
import android.util.Log;
import at.bitfire.davdroid.Constants;
import at.bitfire.davdroid.resource.CalDavCalendar;
import at.bitfire.davdroid.resource.LocalCalendar;
import at.bitfire.davdroid.resource.LocalCollection;
import at.bitfire.davdroid.resource.RemoteCollection;

public class CalendarsSyncAdapterService extends Service {
	private static SyncAdapter syncAdapter;
    // Object to use as a thread-safe lock
    private static final Object sSyncAdapterLock = new Object();
	
	
	@Override
	public void onCreate() {
		synchronized (sSyncAdapterLock) {
			if (syncAdapter == null)
				syncAdapter = new SyncAdapter(getApplicationContext());
		}
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
	

	private static class SyncAdapter extends DavSyncAdapter {
		private final static String TAG = "davdroid.CalendarsSyncAdapter";

		
		private SyncAdapter(Context context) {
			super(context);
		}
		
		@Override
		protected Map<LocalCollection<?>, RemoteCollection<?>> getSyncPairs(Account account, ContentProviderClient provider) {
			AccountSettings settings = new AccountSettings(getContext(), account);
			boolean preemptive = settings.getPreemptiveAuth();

			try {
				Map<LocalCollection<?>, RemoteCollection<?>> map = new HashMap<LocalCollection<?>, RemoteCollection<?>>();
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

				for (LocalCalendar calendar : LocalCalendar.findAll(account, provider)) {

					URI uri = null;
					if(accountManager.getUserData(account, Constants.ACCOUNT_KEY_BASE_URL) != null)
						uri = new URI(accountManager.getUserData(account, Constants.ACCOUNT_KEY_BASE_URL)).resolve(calendar.getUrl());
					else if(accountManager.getUserData(account, Constants.ACCOUNT_KEY_CALDAV_URL) != null)
						uri = new URI(accountManager.getUserData(account, Constants.ACCOUNT_KEY_CALDAV_URL)).resolve(calendar.getUrl());
					else
						return null;
					RemoteCollection<?> dav = null;
					if(accountManager.getUserData(account, Constants.ACCOUNT_SERVER).equals("Google")) {
						dav = new CalDavCalendar(httpClient, uri.toString(), accessToken);
					}
					if(accountManager.getUserData(account, Constants.ACCOUNT_SERVER).equals("Yahoo")) {
						dav = new CalDavCalendar(httpClient, uri.toString(), userName, password, true);
					}
					map.put(calendar, dav);
				}
				return map;
			} catch (RemoteException ex) {
				Log.e(TAG, "Couldn't find local calendars", ex);
			} catch (URISyntaxException ex) {
				Log.e(TAG, "Couldn't build calendar URI", ex);
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
