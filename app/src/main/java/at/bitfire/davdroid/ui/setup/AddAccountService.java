package at.bitfire.davdroid.ui.setup;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.IntentService;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.Properties;

import at.bitfire.davdroid.Constants;
import at.bitfire.davdroid.R;
import at.bitfire.davdroid.log.StringLogger;
import at.bitfire.davdroid.resource.DavResourceFinder;
import at.bitfire.davdroid.resource.LocalAddressBook;
import at.bitfire.davdroid.resource.LocalCalendar;
import at.bitfire.davdroid.resource.LocalTaskList;
import at.bitfire.davdroid.resource.ServerInfo;
import at.bitfire.davdroid.syncadapter.AccountSettings;
import at.bitfire.ical4android.CalendarStorageException;
import at.bitfire.ical4android.TaskProvider;
import at.bitfire.vcard4android.ContactsStorageException;

import lombok.Cleanup;

/**
 * Created by sree on 4/1/16.
 */
public class AddAccountService extends IntentService {

	final static String STATUS = "status";
	final static String MESSAGE = "message";
	static Context mContext = null;

	/**
	 * Creates an IntentService.  Invoked by your subclass's constructor.
	 */
	public AddAccountService() {
		super("AddAccountService");
	}

	private void broadCastResult(Boolean success, String accountName, String message) {
		Intent resultIntent = new Intent();
		resultIntent.setAction("at.bitfire.davdroid.ADD_ACCOUNT_RESPONSE");
		resultIntent.addCategory(Intent.CATEGORY_DEFAULT);
		resultIntent.putExtra("account_name", accountName);
		if(success)
			resultIntent.putExtra(STATUS, "Success");
		else
			resultIntent.putExtra(STATUS, "Failed");

		if(message != null)
			resultIntent.putExtra(MESSAGE, message);

		mContext.sendBroadcast(resultIntent);
	}

	@Override
	protected void onHandleIntent(Intent addAccount) {

		mContext = this.getApplicationContext();

		//Needed for ServiceLoader
		Thread.currentThread().setContextClassLoader(mContext.getClassLoader());

		String accountName = addAccount.getStringExtra(Constants.ACCOUNT_KEY_ACCOUNT_NAME);
		String accountServer = addAccount.getStringExtra(Constants.ACCOUNT_KEY_ACCOUNT_TYPE);
		Constants.log.info("Received Intent. Configuring contacts and calendar");

		if(accountName == null) {
			Constants.log.error("No account name present in Intent");
			broadCastResult(false, accountName, mContext.getString(
					R.string.exception_account));
			return;
		}
		if(accountServer == null) {
			Constants.log.error("No account type present in Intent");
			broadCastResult(false, accountName, mContext.getString(
					R.string.unknown_account_type));
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
		Constants.log.info("Account name:" + accountName + " Type:" + accountServer + " sync_type:" + sync_type + " enabled_services:" + enabled_services);

		Boolean addressBookEnabled = enabled_services.equalsIgnoreCase("both") || enabled_services.equalsIgnoreCase("contacts");
		Boolean calendarEnabled = enabled_services.equalsIgnoreCase("both") || enabled_services.equalsIgnoreCase("calendar");

		if(accountServer.equalsIgnoreCase("google")) {
			accountServer = "Google";
		} else if(accountServer.equalsIgnoreCase("yahoo")) {
			accountServer = "Yahoo";
		} else if(accountServer.equalsIgnoreCase("icloud")) {
			accountServer = "iCloud";
		} else {
			broadCastResult(false, accountName, mContext.getString(
					R.string.unknown_account_type));
			return;
		}

		AccountManager accountManager = AccountManager.get(mContext);
		AccountDetailsReader reader = new AccountDetailsReader(mContext);
		Properties properties =
				reader.getProperties(accountServer);
		Account account = null;

		for (Account acc : accountManager.getAccountsByType(Constants.ACCOUNT_TYPE)) {
			if(acc.name.equals(accountName)) {
				account = acc;
				break;
			}
		}

		if(account == null) {
			broadCastResult(false, accountName, mContext.getString(
					R.string.exception_account));
			return;
		}
		accountManager.setUserData(account, Constants.ACCOUNT_SERVER, accountServer);

		ServerInfo serverInfo;

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
						broadCastResult(false, accountName, mContext.getString(R.string.oauth_error));
						return;
					}
					serverInfo.setAccessToken(token);
				} catch (OperationCanceledException | IOException | AuthenticatorException e) {
					broadCastResult(false, accountName, e.getLocalizedMessage());
					return;
				}
				break;
			default:
				broadCastResult(false, accountName, mContext.getString(R.string.unknown_account_type));
				return;
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

		broadCastResult(true, accountName, null);
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
						Constants.log.error("Couldn't add sync collection", e);
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
