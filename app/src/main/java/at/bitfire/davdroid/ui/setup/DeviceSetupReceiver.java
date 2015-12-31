package at.bitfire.davdroid.ui.setup;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.BroadcastReceiver;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Toast;

import at.bitfire.davdroid.Constants;
import at.bitfire.davdroid.R;
import at.bitfire.davdroid.log.StringLogger;
import at.bitfire.davdroid.resource.LocalAddressBook;
import at.bitfire.davdroid.resource.LocalCalendar;
import at.bitfire.davdroid.resource.DavResourceFinder;
import at.bitfire.davdroid.resource.LocalTaskList;
import at.bitfire.davdroid.resource.ServerInfo;
import at.bitfire.davdroid.syncadapter.AccountSettings;
import at.bitfire.ical4android.CalendarStorageException;
import at.bitfire.ical4android.TaskProvider;
import at.bitfire.vcard4android.ContactsStorageException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.Properties;

import lombok.Cleanup;

public class DeviceSetupReceiver extends BroadcastReceiver {

	final static String TAG = "DeviceSetupReceiver";
	final static String status = "status";
	final static String message = "message";

	class UpdateAccount extends AsyncTask<String, Integer, String>{

		String accountName;
		String accountServer;
		PendingResult result;
		//Context mContext;
		Boolean hasAddressBook = false;
		Boolean hasCalendar = false;
		Boolean addressBookEnabled = true;
		Boolean calendarEnabled = true;
		String sync_type;

		public UpdateAccount(String accountName, String accountType, PendingResult result, /*Context context,*/ String sync_type, String enabled_services) {
			this.accountName = accountName;
			this.accountServer = accountType;
			this.result = result;
			//this.mContext = context;
			this.sync_type = sync_type;
			this.addressBookEnabled = enabled_services.equalsIgnoreCase("both") || enabled_services.equalsIgnoreCase("contacts");
			this.calendarEnabled = enabled_services.equalsIgnoreCase("both") || enabled_services.equalsIgnoreCase("calendar");
		}

		@Override
		protected String doInBackground(String... arg0) {
			AccountManager accountManager = AccountManager.get(mContext);
			AccountDetailsReader reader = new AccountDetailsReader(mContext);
			Properties properties =
				reader.getProperties(accountServer);
			Account account = null;
			String errorMessage = "";

			Intent resultIntent = new Intent();
			resultIntent.setAction("at.bitfire.davdroid.ADD_ACCOUNT_RESPONSE");
			resultIntent.addCategory(Intent.CATEGORY_DEFAULT);

			for (Account acc : accountManager.getAccountsByType(Constants.ACCOUNT_TYPE)) {
				if(acc.name.equals(accountName)) {
					account = acc;
					break;
				}
			}

			if(account == null) {
				resultIntent.putExtra(status, "Failed");
				errorMessage.concat(mContext.getString(
						R.string.exception_account));
				return errorMessage;
			}
            accountManager.setUserData(account, Constants.ACCOUNT_SERVER, accountServer);

			ServerInfo serverInfo = null;

			String userName = accountName;
            switch (accountServer) {

				case "Yahoo":
					if(userName.contains("@")) {
						userName = userName.substring(0,userName.indexOf("@"));
					}
				case "iCloud":
					serverInfo = new ServerInfo(URI.create("mailto:" + accountName), userName, accountManager.getPassword(account), true);
					break;
				case "Google":
					serverInfo = new ServerInfo(URI.create("mailto:" + accountName), userName, "", true);
					accountManager.setUserData(account, "client_id", properties.getProperty("client_id_value"));
					accountManager.setUserData(account, properties.getProperty("client_secret_name"), properties.getProperty("client_secret_value"));
					accountManager.setUserData(account, "token_url", properties.getProperty("token_url"));
					try {
						String token = accountManager.blockingGetAuthToken(account, Constants.ACCOUNT_KEY_ACCESS_TOKEN, false);
						if(token == null) {
							return "Require OAuth";
						}
						serverInfo.setAccessToken(token);
					} catch (OperationCanceledException | IOException | AuthenticatorException e) {
						resultIntent.putExtra(status, "Failed " + e.getLocalizedMessage());
						return e.getLocalizedMessage();
					}
                    break;
				default:
					resultIntent.putExtra(status, "Failed");
					errorMessage.concat(mContext.getString(
							R.string.exception_account));
					return errorMessage;
            }
            serverInfo.setCaldavURI(properties.getProperty("caldav_url"));
            serverInfo.setCarddavURI(properties.getProperty("carddav_url"));

			StringLogger logger = new StringLogger("DavResourceFinder", true);
			DavResourceFinder finder = new DavResourceFinder(logger, mContext, serverInfo);
			finder.findResources();

			// duplicate logs to ADB
			String logs = logger.toString();
			try {
				@Cleanup BufferedReader logStream = new BufferedReader(new StringReader(logs));
				Constants.log.info("Successful resource detection:");
				String line;
				while ((line = logStream.readLine()) != null)
					Constants.log.debug(line);
			} catch (IOException e) {
				Constants.log.error("Couldn't read resource detection logs", e);
			}

            for (ServerInfo.ResourceInfo addressBook : serverInfo.getAddressBooks())
                addressBook.setEnabled(true);
            for (ServerInfo.ResourceInfo calendar : serverInfo.getCalendars())
                calendar.setEnabled(true);
            for (ServerInfo.ResourceInfo todoList : serverInfo.getTaskLists())
                todoList.setEnabled(false);

			addSync(account, ContactsContract.AUTHORITY, serverInfo.getAddressBooks(), new AddSyncCallback() {
                @Override
                public void createLocalCollection(Account account, ServerInfo.ResourceInfo resource) throws ContactsStorageException {
                    @Cleanup("release") ContentProviderClient provider = mContext.getContentResolver().acquireContentProviderClient(ContactsContract.AUTHORITY);
                    if (provider != null) {
                        LocalAddressBook addressBook = new LocalAddressBook(account, provider);

                        // set URL
                        addressBook.setURL(resource.getUrl());

                        // set Settings
                        ContentValues settings = new ContentValues(2);
                        settings.put(ContactsContract.Settings.SHOULD_SYNC, 1);
                        settings.put(ContactsContract.Settings.UNGROUPED_VISIBLE, 1);
                        addressBook.updateSettings(settings);
                    } else
                        Constants.log.error("Couldn't access Contacts Provider");
                }
            });
            ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, addressBookEnabled);

			addSync(account, CalendarContract.AUTHORITY, serverInfo.getCalendars(), new AddSyncCallback() {
                @Override
                public void createLocalCollection(Account account, ServerInfo.ResourceInfo calendar) {
                    try {
                        LocalCalendar.create(account, mContext.getContentResolver(), calendar);
                    } catch (CalendarStorageException e) {
                        Constants.log.error("Couldn't create local calendar", e);
                    }
                }
            });
            ContentResolver.setSyncAutomatically(account, CalendarContract.AUTHORITY, calendarEnabled);

			addSync(account, TaskProvider.ProviderName.OpenTasks.authority, serverInfo.getTaskLists(), new AddSyncCallback() {
                @Override
                public void createLocalCollection(Account account, ServerInfo.ResourceInfo todoList) {
                    try {
                        LocalTaskList.create(account, mContext.getContentResolver(), todoList);
                    } catch (CalendarStorageException e) {
                        Constants.log.error("Couldn't create local task list", e);
                    }
                }
            });
            ContentResolver.setSyncAutomatically(account, TaskProvider.ProviderName.OpenTasks.authority, false);

            Bundle userData = AccountSettings.createBundle(serverInfo);
            accountManager.setUserData(account, Constants.ACCOUNT_KEY_SETTINGS_VERSION, userData.getString(Constants.ACCOUNT_KEY_SETTINGS_VERSION));
            accountManager.setUserData(account, Constants.ACCOUNT_KEY_USERNAME, userData.getString(Constants.ACCOUNT_KEY_USERNAME));
            accountManager.setUserData(account, Constants.ACCOUNT_KEY_AUTH_PREEMPTIVE, userData.getString(Constants.ACCOUNT_KEY_AUTH_PREEMPTIVE));

			return errorMessage;
		}

		@Override
		protected void onPostExecute(String error) {
			Intent resultIntent = new Intent();
			resultIntent.setAction("at.bitfire.davdroid.ADD_ACCOUNT_RESPONSE");
			resultIntent.putExtra("account_name", accountName);
			if (hasAddressBook || hasCalendar) {
				resultIntent.putExtra(status, "success");
			} else {
				resultIntent.putExtra(status, "failed");
			}
			if (!error.equals("")) {
				resultIntent.putExtra(message, error);
			}
			mContext.sendBroadcast(resultIntent);
			result.finish();
		}
	}

	static Context mContext = null;

	@Override
	public void onReceive(Context context, Intent addAccount) {

		Intent resultIntent = new Intent();
		resultIntent.setAction("at.bitfire.davdroid.ADD_ACCOUNT_RESPONSE");
		resultIntent.putExtra(status, "failed");
		mContext = context.getApplicationContext();
		Thread.currentThread().setContextClassLoader(mContext.getClassLoader());
		String account_name = addAccount.getStringExtra(Constants.ACCOUNT_KEY_ACCOUNT_NAME);
		String account_type = addAccount.getStringExtra(Constants.ACCOUNT_KEY_ACCOUNT_TYPE);
		if(account_name == null) {
			resultIntent.putExtra(message, "Account name not present");
			context.sendBroadcast(resultIntent);
			return;
		}
		if(account_type == null) {
			resultIntent.putExtra("account_name", account_name);
			resultIntent.putExtra(message, "Unknown account type");
			context.sendBroadcast(resultIntent);
			return;
		}
		String sync_type = addAccount.getStringExtra("sync_type");
		String enabled_services = addAccount.getStringExtra("enabled_services");
		if(sync_type == null) {
			sync_type = "both";
		}
		if(enabled_services == null) {
			enabled_services = "both";
		}

		if(account_type.equalsIgnoreCase("google")) {
			account_type = "Google";
		} else if(account_type.equalsIgnoreCase("yahoo")) {
			account_type = "Yahoo";
		} else if(account_type.equalsIgnoreCase("icloud")) {
			account_type = "iCloud";
		} else {
			resultIntent.putExtra("account_name", account_name);
			resultIntent.putExtra(message, "Unknown account type");
			context.sendBroadcast(resultIntent);
			return;
		}

		PendingResult result = goAsync();
		Log.v("davdroid", "onReceive: " + " name=" + account_name + " type=" + account_type + " sync=" + sync_type + " enabled=" + enabled_services);
		UpdateAccount account_task = new UpdateAccount(
				account_name, account_type,
				result, sync_type, enabled_services);
		account_task.execute();
	}

	protected interface AddSyncCallback {
		void createLocalCollection(Account account, ServerInfo.ResourceInfo resource) throws ContactsStorageException;
	}

	protected void addSync(Account account, String authority, ServerInfo.ResourceInfo[] resourceList, AddSyncCallback callback) {
		boolean sync = false;
		for (ServerInfo.ResourceInfo resource : resourceList) {
            if (resource.isEnabled()) {
                sync = true;
                if (callback != null)
                    try {
                        callback.createLocalCollection(account, resource);
                    } catch (ContactsStorageException e) {
                        Log.e(TAG, "Couldn't add sync collection", e);
                        Toast.makeText(mContext, "Couldn't set up synchronization for " + authority, Toast.LENGTH_LONG).show();
                    }
            }
        }
		if (sync) {
			ContentResolver.setIsSyncable(account, authority, 1);
			ContentResolver.setSyncAutomatically(account, authority, true);
		} else
			ContentResolver.setIsSyncable(account, authority, 0);
	}

}
