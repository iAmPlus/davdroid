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
import android.os.RemoteException;
import android.util.Log;
import at.bitfire.davdroid.Constants;
import at.bitfire.davdroid.resource.CalDavCalendar;
import at.bitfire.davdroid.resource.LocalCalendar;
import at.bitfire.davdroid.resource.LocalCollection;
import at.bitfire.davdroid.resource.RemoteCollection;

public class CalendarsSyncAdapterService extends Service {
	private static SyncAdapter syncAdapter;

	@Override @Synchronized
	public void onCreate() {
		if (syncAdapter == null)
			syncAdapter = new SyncAdapter(getApplicationContext());
	}

	@Override
	public IBinder onBind(Intent intent) {
		return syncAdapter.getSyncAdapterBinder(); 
	}

	private static class SyncAdapter extends DavSyncAdapter {
		private final static String TAG = "davdroid.CalendarsSyncAdapter";

		public SyncAdapter(Context context) {
			super(context);
		}

		@Override
		protected Map<LocalCollection<?>, RemoteCollection<?>> getSyncPairs(Account account, ContentProviderClient provider) {
			try {
				Map<LocalCollection<?>, RemoteCollection<?>> map = new HashMap<LocalCollection<?>, RemoteCollection<?>>();

				for (LocalCalendar calendar : LocalCalendar.findAll(account, provider)) {

					URI uri = null;
					if(accountManager.getUserData(account, Constants.ACCOUNT_KEY_BASE_URL) != null)
						uri = new URI(accountManager.getUserData(account, Constants.ACCOUNT_KEY_BASE_URL)).resolve(calendar.getPath());
					else if(accountManager.getUserData(account, Constants.ACCOUNT_KEY_CALDAV_URL) != null)
						uri = new URI(accountManager.getUserData(account, Constants.ACCOUNT_KEY_CALDAV_URL)).resolve(calendar.getPath());
					else
						return null;
					accountManager.invalidateAuthToken(Constants.ACCOUNT_TYPE, Constants.ACCOUNT_KEY_ACCESS_TOKEN);
					AccountManagerFuture<Bundle> authBundle = accountManager.getAuthToken(account, Constants.ACCOUNT_KEY_ACCESS_TOKEN, null, null, null, null);
					String accessToken = authBundle.getResult().getString(AccountManager.KEY_AUTHTOKEN);

					RemoteCollection<?> dav = new CalDavCalendar(uri.toString(), accessToken);
					map.put(calendar, dav);
				}
				return map;
			} catch (RemoteException ex) {
				Log.e(TAG, "Couldn't find local calendars", ex);
			} catch (URISyntaxException ex) {
				Log.e(TAG, "Couldn't build calendar URI", ex);
			} catch (OperationCanceledException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (AuthenticatorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return null;
		}
	}
}
