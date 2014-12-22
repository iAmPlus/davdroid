package at.bitfire.davdroid.syncadapter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import ch.boye.httpclientandroidlib.util.TextUtils;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.util.Log;
import at.bitfire.davdroid.Constants;
import at.bitfire.davdroid.R;
import at.bitfire.davdroid.resource.LocalCalendar;
import at.bitfire.davdroid.webdav.DavException;
import ch.boye.httpclientandroidlib.HttpException;
import at.bitfire.davdroid.resource.LocalStorageException;
import at.bitfire.davdroid.resource.DavResourceFinder;
import at.bitfire.davdroid.resource.ServerInfo;

public class DeviceSetupReceiver extends BroadcastReceiver {

	final static String TAG = "DeviceSetupReceiver";
	final static String status = "status";
	final static String message = "message";
	
	class UpdateAccount extends AsyncTask<String, Integer, String>{
		
		String accountName;
		String accountServer;
		PendingResult result;
		Context mContext;
		Boolean hasAddressBook = false;
		Boolean hasCalendar = false;
		String sync_type;
		
		public UpdateAccount(String accountName, String accountType, PendingResult result, Context context, String sync_type) {
			this.accountName = accountName;
			this.accountServer = accountType;
			this.result = result;
			this.mContext = context;
			this.sync_type = sync_type;
		}

		@Override
		protected String doInBackground(String... arg0) {
			ServerInfo serverInfo = new ServerInfo(accountServer);
			AccountDeatilsReader reader = new AccountDeatilsReader(mContext);
			AccountManager accountManager = AccountManager.get(mContext);
			Properties properties =
				reader.getProperties(serverInfo.getAccountServer());
			Account account = null;
			String errorMessage = "";
			Boolean valid_credentials = true;
			
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

			if(serverInfo.getAccountServer().equals("Google")) {
				accountManager.setUserData(account, "client_id", properties.getProperty("client_id_value"));
				accountManager.setUserData(account, properties.getProperty("client_secret_name"), properties.getProperty("client_secret_value"));
				accountManager.setUserData(account, "token_url", properties.getProperty("token_url"));
				accountManager.setUserData(account, Constants.ACCOUNT_SERVER, properties.getProperty("type"));
				
				AccountManagerFuture<Bundle> tokenBundle = accountManager.getAuthToken(account, Constants.ACCOUNT_KEY_ACCESS_TOKEN, null, false, null, null);
				try {
					String token = tokenBundle.getResult().getString(AccountManager.KEY_AUTHTOKEN);
					if(token == null) {
		            			return "Require OAuth";
					}
					accountManager.invalidateAuthToken(Constants.ACCOUNT_TYPE, token);
					tokenBundle = accountManager.getAuthToken(account, Constants.ACCOUNT_KEY_ACCESS_TOKEN, null, false, null, null);
					token = tokenBundle.getResult().getString(AccountManager.KEY_AUTHTOKEN);
					if(token == null) {
		            			return "Refresh token failed";
					}
					String authBearer = "Bearer " + token;
					serverInfo.setAccessToken(authBearer);
				} catch (OperationCanceledException e) {
					valid_credentials = false;
					errorMessage.concat(e.getMessage());
				} catch (AuthenticatorException e) {
					valid_credentials = false;
					errorMessage.concat(e.getMessage());
				} catch (IOException e) {
					valid_credentials = false;
					errorMessage.concat(e.getMessage());
				}
			} else {
				//serverInfo.setUserName(accountManager.getUserData(account, Constants.ACCOUNT_KEY_USERNAME));

				String userName = accountName;
				if(serverInfo.getAccountServer().equals("Yahoo") && userName.indexOf("@") != -1) {
					userName = userName.substring(0, userName.indexOf("@"));
				}
				serverInfo.setUserName(userName);
				serverInfo.setPassword(accountManager.getPassword(account));
			}
			
			if(errorMessage != "") {
				resultIntent.putExtra(status, "Failed");
				errorMessage.concat(mContext.getString(
						R.string.exception_token));
				return errorMessage;
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
			
			
			try {
				DavResourceFinder.findResources(mContext, serverInfo, sync_type);
			} catch (URISyntaxException e) {
				valid_credentials = false;
				serverInfo.setErrorMessage(mContext.getString(R.string.exception_uri_syntax, e.getMessage()));
			}  catch (IOException e) {
				valid_credentials = false;
				serverInfo.setErrorMessage(mContext.getString(R.string.exception_io, e.getLocalizedMessage()));
			} catch (HttpException e) {
				valid_credentials = false;
				Log.e(TAG, "HTTP error while querying server info", e);
				serverInfo.setErrorMessage(mContext.getString(R.string.exception_http, e.getLocalizedMessage()));
			} catch (DavException e) {
				valid_credentials = false;
				Log.e(TAG, "DAV error while querying server info", e);
				serverInfo.setErrorMessage(mContext.getString(R.string.exception_incapable_resource, e.getLocalizedMessage()));
			}

			if(!valid_credentials) {
				resultIntent.putExtra(status, "Failed");
				accountManager.removeAccount(account, null, null);
				return errorMessage;
			}

			if((!serverInfo.getCalendars().isEmpty()) || (!serverInfo.getAddressBooks().isEmpty()) ) {

				boolean syncContacts = false;
				if(sync_type.equalsIgnoreCase("both") || sync_type.equalsIgnoreCase("contacts")) {
					for (ServerInfo.ResourceInfo addressBook : serverInfo.getAddressBooks()) {
						addressBook.setEnabled(true);
						ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 1);
						syncContacts = true;
						hasAddressBook = true;
						continue;
					}
				}

				Bundle accountData = AccountSettings.createBundle(serverInfo);
				accountManager.setUserData(account, "version", accountData.getString("version"));
				accountManager.setUserData(account, Constants.ACCOUNT_KEY_USERNAME,
						accountData.getString(Constants.ACCOUNT_KEY_USERNAME));
				accountManager.setUserData(account, Constants.ACCOUNT_KEY_AUTH_PREEMPTIVE,
						accountData.getString(Constants.ACCOUNT_KEY_AUTH_PREEMPTIVE));
				if(accountData.containsKey(Constants.ACCOUNT_SERVER))
					accountManager.setUserData(account, Constants.ACCOUNT_SERVER, serverInfo.getAccountServer());
				if(accountData.containsKey(Constants.ACCOUNT_KEY_BASE_URL))
					accountManager.setUserData(account, Constants.ACCOUNT_KEY_BASE_URL, serverInfo.getBaseURL());
				if(accountData.containsKey(Constants.ACCOUNT_KEY_CARDDAV_URL))
					accountManager.setUserData(account, Constants.ACCOUNT_KEY_CARDDAV_URL, serverInfo.getCarddavURL());
				if(accountData.containsKey(Constants.ACCOUNT_KEY_CALDAV_URL))
					accountManager.setUserData(account, Constants.ACCOUNT_KEY_CALDAV_URL, serverInfo.getCaldavURL());
				if(accountData.containsKey(Constants.ACCOUNT_KEY_ADDRESSBOOK_PATH))
					accountManager.setUserData(account, Constants.ACCOUNT_KEY_ADDRESSBOOK_PATH,
						accountData.getString(Constants.ACCOUNT_KEY_ADDRESSBOOK_PATH));
				
				if (syncContacts) {
					ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 1);
					ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true);
				} else
					ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 0);
		
				boolean syncCalendars = false;
				if(sync_type.equalsIgnoreCase("both") || sync_type.equalsIgnoreCase("calendar")) {
					for (ServerInfo.ResourceInfo calendar : serverInfo.getCalendars()) {
						try {
							LocalCalendar.create(account, mContext.getContentResolver(), calendar);
							syncCalendars = true;
							hasCalendar = true;
						} catch (LocalStorageException e) {
							e.printStackTrace();
						}
					}
				}
				if (syncCalendars) {
					ContentResolver.setIsSyncable(account, CalendarContract.AUTHORITY, 1);
					ContentResolver.setSyncAutomatically(account, CalendarContract.AUTHORITY, true);
				} else
					ContentResolver.setIsSyncable(account, CalendarContract.AUTHORITY, 0);

				return "";
			}
			return errorMessage;
		}
		
		@Override
		protected void onPostExecute(String error) {
			Intent resultIntent = new Intent();
			resultIntent.setAction("at.bitfire.davdroid.ADD_ACCOUNT_RESPONSE");
			resultIntent.putExtra("account_name", accountName);
			//resultIntent.addCategory(Intent.CATEGORY_DEFAULT);
			if (hasAddressBook || hasCalendar) {
				resultIntent.putExtra(status, "success");
			} else {
				resultIntent.putExtra(status, "failed");
			}
			if (error != "") {
				resultIntent.putExtra(message, error);
			}
			mContext.sendBroadcast(resultIntent);
			result.finish();
		}
	}
	
	@Override
	public void onReceive(Context context, Intent addAccount) {
		PendingResult result = goAsync();
		String sync_type = "both";
		if(addAccount.hasExtra("sync_type")) {
			sync_type = addAccount.getStringExtra("sync_type");
		}
		UpdateAccount account_task = new UpdateAccount(
				addAccount.getStringExtra(Constants.ACCOUNT_KEY_ACCOUNT_NAME),
				addAccount.getStringExtra(Constants.ACCOUNT_KEY_ACCOUNT_TYPE),
				result, context, sync_type);
		account_task.execute();
	}

}
