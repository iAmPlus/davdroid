package at.bitfire.davdroid.syncadapter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import ch.boye.httpclientandroidlib.impl.client.CloseableHttpClient;
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
import android.os.RemoteException;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.util.Log;
import android.os.Bundle;
import at.bitfire.davdroid.Constants;
import at.bitfire.davdroid.R;
import at.bitfire.davdroid.resource.LocalCalendar;
import at.bitfire.davdroid.webdav.DavException;
import at.bitfire.davdroid.webdav.DavHttpClient;
import at.bitfire.davdroid.webdav.DavIncapableException;
import ch.boye.httpclientandroidlib.HttpException;
import at.bitfire.davdroid.webdav.WebDavResource;
import at.bitfire.davdroid.webdav.HttpPropfind.Mode;
import at.bitfire.davdroid.resource.LocalStorageException;

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
		
		public UpdateAccount(String accountName, String accountType, PendingResult result, Context context) {
			this.accountName = accountName;
			this.accountServer = accountType;
			this.result = result;
			this.mContext = context;
		}

		@Override
		protected String doInBackground(String... arg0) {
			String authBearer = null;
			String userName = null;
			String password = null;
			ServerInfo serverInfo = new ServerInfo(accountServer);
			AccountDeatilsReader reader = new AccountDeatilsReader(mContext);
			AccountManager accountManager = AccountManager.get(mContext);
			Properties properties =
				reader.getProperties(serverInfo.getAccountServer());
			Account account = null;
			String errorMessage = "";
			
			Intent resultIntent = new Intent();
			resultIntent.setAction("at.bitfire.davdroid.ADD_ACCOUNT_RESPONSE");
			resultIntent.addCategory(Intent.CATEGORY_DEFAULT);
			
			for (Account acc : accountManager.getAccounts()) {
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
					authBearer = "Bearer " + token;
				} catch (OperationCanceledException e) {
					errorMessage.concat(e.getMessage());
				} catch (AuthenticatorException e) {
					errorMessage.concat(e.getMessage());
				} catch (IOException e) {
					errorMessage.concat(e.getMessage());
				}
			} else {
				//serverInfo.setUserName(accountManager.getUserData(account, Constants.ACCOUNT_KEY_USERNAME));

				android.util.Log.v("SK", accountServer);
				android.util.Log.v("SK", accountName);
				android.util.Log.v("SK", accountManager.getPassword(account));
				userName = accountName;
				if(userName.indexOf("@") != -1) {
					userName = userName.substring(0, userName.indexOf("@"));
				}
				serverInfo.setUserName(userName);
				serverInfo.setPassword(accountManager.getPassword(account));
				password = serverInfo.getPassword();
			}
			
			if(errorMessage != "") {
				resultIntent.putExtra(status, "Failed");
				errorMessage.concat(mContext.getString(
						R.string.exception_token));
				return errorMessage;
			}

			CloseableHttpClient httpClient = DavHttpClient.create(true, true);
			try {
				WebDavResource base = null;
				if (!TextUtils.isEmpty(properties.getProperty(
						Constants.ACCOUNT_KEY_CARDDAV_URL))) {
					serverInfo.setCarddavURL(properties.getProperty(
						Constants.ACCOUNT_KEY_CARDDAV_URL));

					if(authBearer != null)
						base= new WebDavResource(httpClient, new URI(
								serverInfo.getCarddavURL()), authBearer);
					else
						base = new WebDavResource(httpClient, new URI(serverInfo.getCarddavURL()), userName,
								password, true);
				} else {
					if(!TextUtils.isEmpty(properties.getProperty(Constants.ACCOUNT_KEY_BASE_URL)) )
					{
						serverInfo.setBaseURL(
							properties.getProperty(Constants.ACCOUNT_KEY_BASE_URL));
						if(authBearer != null)
							base = new WebDavResource(httpClient,
									new URI(serverInfo.getBaseURL()), authBearer);
						else
							base = new WebDavResource(httpClient,
									new URI(serverInfo.getBaseURL()), userName, password, true);
					} else 
						throw new DavIncapableException(
							mContext.getString(
								R.string.no_carddav));
				}
				
				// CardDAV
				WebDavResource principal = getCurrentUserPrincipal(base, "carddav", "addressbook");
				if (principal != null) {
					serverInfo.setCardDAV(true);
					serverInfo.setCarddavURL(principal.getLocation().toString());
				
					principal.propfind(Mode.ADDRESS_BOOK_HOME_SETS);
					String pathAddressBooks = principal.getAddressbookHomeSet();
					if (pathAddressBooks != null)
						Log.i(TAG, "Found address book home set: " + pathAddressBooks);
					else
						throw new DavIncapableException(mContext.getString(R.string.error_home_set_address_books));
					
					WebDavResource homeSetAddressBooks = new WebDavResource(principal, pathAddressBooks);
					homeSetAddressBooks.propfind(Mode.MEMBERS_COLLECTIONS);
					
					List<ServerInfo.ResourceInfo> addressBooks = new LinkedList<ServerInfo.ResourceInfo>();
					if (homeSetAddressBooks.getMembers() != null)
						for (WebDavResource resource : homeSetAddressBooks.getMembers())
							if (resource.isAddressBook()) {
								Log.i(TAG, "Found address book: " + resource.getLocation().getRawPath());
								ServerInfo.ResourceInfo info = new ServerInfo.ResourceInfo(
									ServerInfo.ResourceInfo.Type.ADDRESS_BOOK,
									resource.isReadOnly(),
									resource.getLocation().toASCIIString(),
									resource.getDisplayName(),
									resource.getDescription(), resource.getColor()
								);
								addressBooks.add(info);
							}
					serverInfo.setAddressBooks(addressBooks);
				}
			} catch (URISyntaxException e) {
				errorMessage.concat(
					mContext.getString(
						R.string.exception_dav_addressbook, e.getMessage()));
			} catch (DavIncapableException e) {
				errorMessage.concat(
					mContext.getString(
						R.string.exception_incapable_resource,
						e.getLocalizedMessage()));
			} catch (DavException e) {
				errorMessage.concat(
					mContext.getString(
						R.string.exception_io, e.getLocalizedMessage()));
			} catch (HttpException e) {
				errorMessage.concat(
					mContext.getString(
						R.string.exception_http, e.getLocalizedMessage()));
			} catch (IOException e) {
				errorMessage.concat(
					mContext.getString(
						R.string.exception_http, e.getLocalizedMessage()));
			}

			if(errorMessage != "") {
				errorMessage.concat("\n");
			}

			try {
				WebDavResource base = null;
				if (!TextUtils.isEmpty(properties.getProperty(
						Constants.ACCOUNT_KEY_CALDAV_URL))) {
					serverInfo.setCaldavURL(properties.getProperty(
						Constants.ACCOUNT_KEY_CALDAV_URL));

					if(authBearer != null)
						base= new WebDavResource(httpClient, new URI(
								serverInfo.getCaldavURL()), authBearer);
					else
						base = new WebDavResource(httpClient, new URI(serverInfo.getCaldavURL()), userName,
								password, true);
				} else {
					if(!TextUtils.isEmpty(properties.getProperty(Constants.ACCOUNT_KEY_BASE_URL)) )
					{
						serverInfo.setBaseURL(
							properties.getProperty(Constants.ACCOUNT_KEY_BASE_URL));
						if(authBearer != null)
							base = new WebDavResource(httpClient,
									new URI(serverInfo.getBaseURL()), authBearer);
						else
							base = new WebDavResource(httpClient,
									new URI(serverInfo.getBaseURL()), userName, password, true);
					} else 
						throw new DavIncapableException(
							mContext.getString(
								R.string.no_caldav));
				}

				// CalDAV
				WebDavResource principal = getCurrentUserPrincipal(base, "caldav", "calendar-access");
				if (principal != null) {
					serverInfo.setCalDAV(true);
					serverInfo.setCaldavURL(principal.getLocation().toString());

					principal.propfind(Mode.CALENDAR_HOME_SETS);
					String pathCalendars = principal.getCalendarHomeSet();
					if (pathCalendars != null)
						Log.i(TAG, "Found calendar home set: " + pathCalendars);
					else
						throw new DavIncapableException(mContext.getString(R.string.error_home_set_calendars));
					
					WebDavResource homeSetCalendars = new WebDavResource(principal, pathCalendars);
					homeSetCalendars.propfind(Mode.MEMBERS_COLLECTIONS);
					
					List<ServerInfo.ResourceInfo> calendars = new LinkedList<ServerInfo.ResourceInfo>();
					if (homeSetCalendars.getMembers() != null)
						for (WebDavResource resource : homeSetCalendars.getMembers())
							if (resource.isCalendar()) {
								Log.i(TAG, "Found calendar: " + resource.getLocation().getRawPath());
								if (resource.getSupportedComponents() != null) {
									// CALDAV:supported-calendar-component-set available
									boolean supportsEvents = false;
									for (String supportedComponent : resource.getSupportedComponents())
										if (supportedComponent.equalsIgnoreCase("VEVENT"))
											supportsEvents = true;
									if (!supportsEvents)	// ignore collections without VEVENT support
										continue;
								}
								ServerInfo.ResourceInfo info = new ServerInfo.ResourceInfo(
									ServerInfo.ResourceInfo.Type.CALENDAR,
									resource.isReadOnly(),
									resource.getLocation().toASCIIString(),
									resource.getDisplayName(),
									resource.getDescription(), resource.getColor()
								);
								info.setTimezone(resource.getTimezone());
								calendars.add(info);						}
					serverInfo.setCalendars(calendars);
				}

			} catch (URISyntaxException e) {
				errorMessage.concat(
					mContext.getString(
						R.string.exception_uri_syntax, e.getMessage()));
			}  catch (IOException e) {
				errorMessage.concat(
					mContext.getString(
						R.string.exception_io, e.getLocalizedMessage()));
			} catch (DavIncapableException e) {
				errorMessage.concat(
					mContext.getString(
						R.string.exception_incapable_resource,
						e.getLocalizedMessage()));
			} catch (HttpException e) {
				errorMessage.concat(
					mContext.getString(
						R.string.exception_http, e.getLocalizedMessage()));
			} catch (DavException e) {
				errorMessage.concat(
					mContext.getString(
						R.string.exception_http, e.getLocalizedMessage()));
			}

			if((!serverInfo.getCalendars().isEmpty()) || (!serverInfo.getAddressBooks().isEmpty()) ) {
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
						
				boolean syncContacts = false;
				for (ServerInfo.ResourceInfo addressBook : serverInfo.getAddressBooks())
					if (addressBook.isEnabled()) {
						ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 1);
						syncContacts = true;
						continue;
					}
				if (syncContacts) {
					ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 1);
					ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true);
				} else
					ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 0);
		
				boolean syncCalendars = false;
				for (ServerInfo.ResourceInfo calendar : serverInfo.getCalendars())
					if (calendar.isEnabled()) {
						try {
							LocalCalendar.create(account, mContext.getContentResolver(), calendar);
						} catch (LocalStorageException e) {
							e.printStackTrace();
						}
						syncCalendars = true;
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
			android.util.Log.v("SK", "onPostExecute");
			Intent resultIntent = new Intent();
			resultIntent.setAction("at.bitfire.davdroid.ADD_ACCOUNT_RESPONSE");
			resultIntent.putExtra("account_name", accountName);
			//resultIntent.addCategory(Intent.CATEGORY_DEFAULT);
			if (hasAddressBook || hasCalendar) {
				android.util.Log.v("SK", "onPostExecute: success");
				resultIntent.putExtra(status, "success");
			} else {
				android.util.Log.v("SK", "onPostExecute: failed");
				resultIntent.putExtra(status, "failed");
			}
			if (error != "") {
				android.util.Log.v("SK", "onPostExecute: error");
				resultIntent.putExtra(message, error);
			}
			android.util.Log.v("SK", "onPostExecute: sendBroadcast");
			mContext.sendBroadcast(resultIntent);
			result.finish();
		}
	}
	/**
		 * Detects the current-user-principal for a given WebDavResource. At first, /.well-known/ is tried. Only
		 * if no current-user-principal can be detected for the .well-known location, the given location of the resource
		 * is tried.
		 * When a current-user-principal is found, it is queried for davCapability via OPTIONS.  
		 * @param resource 		Location that will be queried
		 * @param serviceName	Well-known service name ("carddav", "caldav")
		 * @param davCapability	DAV capability to check for ("addressbook", "calendar-access")
		 * @return	WebDavResource of current-user-principal for the given service, or null if it can't be found or it is incapable
		 */
		private WebDavResource getCurrentUserPrincipal(WebDavResource resource, String serviceName, String davCapability) throws IOException {
			// look for well-known service (RFC 5785)
			try {
				WebDavResource wellKnown = new WebDavResource(resource, "/.well-known/" + serviceName);
				wellKnown.propfind(Mode.CURRENT_USER_PRINCIPAL);
				if (wellKnown.getCurrentUserPrincipal() != null) {
					WebDavResource principal = new WebDavResource(wellKnown, wellKnown.getCurrentUserPrincipal());
					if (checkCapabilities(principal, davCapability))
						return principal;
					else
						Log.w(TAG, "Current-user-principal " + resource.getLocation() + " found via well-known service, but it doesn't support required DAV facilities");
				}
			} catch (HttpException e) {
				Log.d(TAG, "Well-known service detection failed with HTTP error", e);
			} catch (DavException e) {
				Log.d(TAG, "Well-known service detection failed at DAV level", e);
			}

			try {
				// fall back to user-given initial context path 
				resource.propfind(Mode.CURRENT_USER_PRINCIPAL);
				if (resource.getCurrentUserPrincipal() != null) {
					WebDavResource principal = new WebDavResource(resource, resource.getCurrentUserPrincipal());
					if (checkCapabilities(principal, davCapability))
						return principal;
					else
						Log.w(TAG, "Current-user-principal " + resource.getLocation() + " found at user-given location, but it doesn't support required DAV facilities");
				}
			} catch (HttpException e) {
				Log.d(TAG, "Service detection failed with HTTP error", e);
			} catch (DavException e) {
				Log.d(TAG, "Service detection failed at DAV level", e);
			}
			return null;
		}
		
		private boolean checkCapabilities(WebDavResource resource, String davCapability) throws IOException {
			// check for necessary capabilities
			// TODO: Some properties are available only on specific
			// sub URLs and not on principal URL. Need to fix this.
			try {
				resource.options();
				if (resource.supportsDAV(davCapability) &&
					resource.supportsMethod("PROPFIND") &&
					resource.supportsMethod("REPORT") )
				//	resource.supportsMethod("GET") &&
				//	resource.supportsMethod("PUT") &&
				//	resource.supportsMethod("DELETE"))
					return true;
			} catch(HttpException e) {
				// for instance, 405 Method not allowed
			}
			return false;
		}
	
	@Override
	public void onReceive(Context context, Intent addAccount) {
		android.util.Log.v("SK", "onReceive");
		PendingResult result = goAsync();
		UpdateAccount account_task = new UpdateAccount(
				addAccount.getStringExtra(Constants.ACCOUNT_KEY_ACCOUNT_NAME), 
				addAccount.getStringExtra(Constants.ACCOUNT_KEY_ACCOUNT_TYPE),
				result, context);
		account_task.execute();
	}

}
