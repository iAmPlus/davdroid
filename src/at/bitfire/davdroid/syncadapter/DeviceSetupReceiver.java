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
import at.bitfire.davdroid.Constants;
import at.bitfire.davdroid.R;
import at.bitfire.davdroid.resource.LocalCalendar;
import at.bitfire.davdroid.webdav.DavException;
import at.bitfire.davdroid.webdav.DavHttpClient;
import at.bitfire.davdroid.webdav.DavIncapableException;
import at.bitfire.davdroid.webdav.PermanentlyMovedException;
import at.bitfire.davdroid.webdav.WebDavResource;
import at.bitfire.davdroid.webdav.HttpPropfind.Mode;

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
			WebDavResource base = null;
			String principalPath = null;
			
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
				AccountManagerFuture<Bundle> token = accountManager.getAuthToken(account, Constants.ACCOUNT_KEY_ACCESS_TOKEN, null, null, null, null);
				try {
					authBearer = "Bearer " + token.getResult().getString(AccountManager.KEY_AUTHTOKEN);
				} catch (OperationCanceledException e) {
					errorMessage.concat(e.getMessage());
				} catch (AuthenticatorException e) {
					errorMessage.concat(e.getMessage());
				} catch (IOException e) {
					errorMessage.concat(e.getMessage());
				}
			} else {
				serverInfo.setUserName(accountManager.getUserData(account, Constants.ACCOUNT_KEY_USERNAME));
				serverInfo.setUserName(accountManager.getPassword(account));
				userName = serverInfo.getUserName();
				password = serverInfo.getPassword();
			}
			
			if(errorMessage != "") {
				resultIntent.putExtra(status, "Failed");
				errorMessage.concat(mContext.getString(
						R.string.exception_token));
				return errorMessage;
			}

			CloseableHttpClient httpClient = DavHttpClient.create();
			try {

				if(TextUtils.isEmpty(serverInfo.getBaseURL())
						&& (properties.getProperty(Constants.ACCOUNT_KEY_BASE_URL) != null) )
				{
					serverInfo.setBaseURL(
						properties.getProperty(Constants.ACCOUNT_KEY_BASE_URL));
				}
				
				if(authBearer != null)
					base = new WebDavResource(httpClient,
							new URI(serverInfo.getBaseURL()), authBearer);
				else
					base = new WebDavResource(httpClient,
							new URI(serverInfo.getBaseURL()), userName, password, true, true);
				try {
					if(base != null) {

						base.options();
						base.propfind(Mode.EMPTY_PROPFIND);
						// (1/5) detect capabilities
						serverInfo.setCardDAV(base.supportsDAV("addressbook"));
						serverInfo.setCardDAV(
								base.supportsDAV("calendar-access"));
					} 
					if (base == null || !serverInfo.isCardDAV()){
						serverInfo.setCarddavURL(properties.getProperty(
							Constants.ACCOUNT_KEY_CARDDAV_URL));

						if(authBearer != null)
							base= new WebDavResource(httpClient, new URI(
									serverInfo.getCarddavURL()), authBearer);
						else
							base = new WebDavResource(httpClient, new URI(serverInfo.getCarddavURL()), userName,
									password, true, true);

						base.propfind(Mode.EMPTY_PROPFIND);
						// (1/5) detect capabilities
						base.options();
						serverInfo.setCardDAV(base.supportsDAV("addressbook"));
					} else {
						throw new DavIncapableException(
							mContext.getString(
								R.string.neither_caldav_nor_carddav));
					}

					// (2/5) get principal URL
					base.options();
					base.propfind(Mode.CURRENT_USER_PRINCIPAL);
					serverInfo.setCardDAV(base.supportsDAV("addressbook"));
					principalPath = base.getCurrentUserPrincipal();

					/*Removed since google doesn't support 
					 *!base.supportsMethod("REPORT") on base
					 *but only on principal url ||*/
					if (!base.supportsMethod("PROPFIND")
							|| !serverInfo.isCardDAV() ) {

						throw new DavIncapableException(
							mContext.getString(
								R.string.neither_caldav_nor_carddav));
					}
				} catch(PermanentlyMovedException e) {
					//cardDavRedirect = true;
					e.printStackTrace();
					serverInfo.setCardDAV(true);
					// Special case for google.
					// Google gives a 301 permenantly moved to addressbook URL.
					// Parsing and taking the principal URL instead.
					principalPath = base.getRedirectionURL();
					URI carddavBase = null;
					if(serverInfo.getCarddavURL() != null)
						carddavBase = new URI(
							serverInfo.getCarddavURL()).resolve(principalPath);
					else
						carddavBase = new URI(
							serverInfo.getBaseURL()).resolve(principalPath);
					serverInfo.setCarddavURL(carddavBase.toString());
					int end = principalPath.indexOf((int)'@');
					end = principalPath.indexOf((int)'/', end);
					principalPath = principalPath.substring(0, end+1);
				}

				if (principalPath != null) {
					Log.i(TAG, "Found principal path: " + principalPath);
				} else
					throw new DavIncapableException(
						mContext.getString(R.string.error_principal_path));

				WebDavResource principal = new WebDavResource(base, principalPath);
				principal.propfind(Mode.ADDRESS_BOOK_HOME_SETS);

				String pathAddressBooks = null;
				if (serverInfo.isCardDAV()) {
					pathAddressBooks = principal.getAddressbookHomeSet();
					if (pathAddressBooks != null) {
						Log.i(TAG, "Found address book home set: "
								+ pathAddressBooks);
					} else
						throw new DavIncapableException(
							mContext.getString(
								R.string.error_home_set_address_books));
				}


				// (4/5) get address books
				if (serverInfo.isCardDAV()) {
					List<ServerInfo.ResourceInfo> addressBooks =
							new LinkedList<ServerInfo.ResourceInfo>();

					WebDavResource homeSetAddressBooks =
							new WebDavResource(principal,
								pathAddressBooks, true);
					homeSetAddressBooks.propfind(
							Mode.ADDRESS_BOOK_MEMBERS_COLLECTIONS);

					if (homeSetAddressBooks.getMembers() != null)
						for (WebDavResource resource :
								homeSetAddressBooks.getMembers())
							if (resource.isAddressBook()) {
								Log.i(TAG, "Found address book: "
										+ resource.getLocation().getRawPath());
								ServerInfo.ResourceInfo info =
										new ServerInfo.ResourceInfo(
									ServerInfo.ResourceInfo.Type.ADDRESS_BOOK,
									resource.isReadOnly(),
									resource.getLocation().getRawPath(),
									resource.getDisplayName(),
									resource.getDescription(),
									resource.getColor()
								);
								info.setEnabled(true);
								hasAddressBook = true;
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
							R.string.exception_dav_addressbook, e.getMessage()));
			} catch (at.bitfire.davdroid.webdav.HttpException e) {
				errorMessage.concat(
						mContext.getString(
							R.string.exception_dav_addressbook, e.getMessage()));
			} catch (PermanentlyMovedException e) {
				errorMessage.concat(
						mContext.getString(
							R.string.exception_dav_addressbook, e.getMessage()));
			} catch (IOException e) {
				errorMessage.concat(
						mContext.getString(
							R.string.exception_dav_addressbook, e.getMessage()));
			} catch (DavException e) {
				errorMessage.concat(
						mContext.getString(
							R.string.exception_dav_addressbook, e.getMessage()));
			}

			if(errorMessage != "") {
				errorMessage.concat("\n");
			}

			try {

				if(base == null || !serverInfo.isCalDAV()) {
					if(properties.getProperty(Constants.ACCOUNT_KEY_CALDAV_URL)
								!= null) {
						serverInfo.setCaldavURL(properties.getProperty(
								Constants.ACCOUNT_KEY_CALDAV_URL));
						if(authBearer != null)
							base= new WebDavResource(httpClient, new URI(
								serverInfo.getCaldavURL()), authBearer);
						else
							base = new WebDavResource(httpClient, new URI(serverInfo.getCaldavURL()), userName,
									password, true, true);
					} else {
						throw new DavIncapableException(
							mContext.getString(
								R.string.neither_caldav_nor_carddav));
					}

					base.options();
					serverInfo.setCalDAV(base.supportsDAV("calendar-access"));

					/*Removed since google doesn't 
					 *support !base.supportsMethod("REPORT") ||*/
					if (!base.supportsMethod("PROPFIND")
							|| !serverInfo.isCalDAV())
						throw new DavIncapableException(
							mContext.getString(
								R.string.neither_caldav_nor_carddav));

					// (2/5) get principal URL
					base.propfind(Mode.CURRENT_USER_PRINCIPAL);

					principalPath = base.getCurrentUserPrincipal();
					if (principalPath != null)
						Log.i(TAG, "Found principal path: " + principalPath);
					else
						throw new DavIncapableException(
							mContext.getString(
								R.string.error_principal_path));
				}

				WebDavResource principal = new WebDavResource(base, principalPath);

				principal.propfind(Mode.CALENDAR_HOME_SETS);

				String pathCalendars = null;
				if (serverInfo.isCalDAV()) {
					pathCalendars = principal.getCalendarHomeSet();
					if (pathCalendars != null)
						Log.i(TAG, "Found calendar home set: " + pathCalendars);
					else
						throw new DavIncapableException(
							mContext.getString(
								R.string.error_home_set_calendars));
				}


				// (5/5) get calendars
				if (serverInfo.isCalDAV()) {
					WebDavResource homeSetCalendars =
							new WebDavResource(principal, pathCalendars, true);
					homeSetCalendars.propfind(
							Mode.CALENDAR_MEMBERS_COLLECTIONS);

					List<ServerInfo.ResourceInfo> calendars =
							new LinkedList<ServerInfo.ResourceInfo>();
					if (homeSetCalendars.getMembers() != null)
						for (WebDavResource resource :
										homeSetCalendars.getMembers())
							if (resource.isCalendar()) {
								Log.i(TAG, "Found calendar: "
										+ resource.getLocation().getRawPath());
								if (resource.getSupportedComponents()
														!= null) {
									// CALDAV:supported-calendar-component-set
									// available
									boolean supportsEvents = false;
									for (String supportedComponent :
											resource.getSupportedComponents())
										if (supportedComponent.equalsIgnoreCase(
																	"VEVENT"))
											supportsEvents = true;
									// ignore collections without VEVENT support
									if (!supportsEvents)
										continue;
								}
								ServerInfo.ResourceInfo info =
										new ServerInfo.ResourceInfo(
									ServerInfo.ResourceInfo.Type.CALENDAR,
									resource.isReadOnly(),
									resource.getLocation().getRawPath(),
									resource.getDisplayName(),
									resource.getDescription(),
									resource.getColor()
								);
								info.setEnabled(true);
								info.setTimezone(resource.getTimezone());
								hasCalendar = true;
								calendars.add(info);
							}

					serverInfo.setCalendars(calendars);
				}

			} catch (URISyntaxException e) {
				errorMessage.concat(
					mContext.getString(
						R.string.exception_dav_calendar, e.getMessage()));
			} catch (DavIncapableException e) {
				errorMessage.concat(
						mContext.getString(
							R.string.exception_dav_calendar, e.getMessage()));
				e.printStackTrace();
			} catch (at.bitfire.davdroid.webdav.HttpException e) {
				errorMessage.concat(
						mContext.getString(
							R.string.exception_dav_calendar, e.getMessage()));
				e.printStackTrace();
			} catch (PermanentlyMovedException e) {
				errorMessage.concat(
						mContext.getString(
							R.string.exception_dav_calendar, e.getMessage()));
				e.printStackTrace();
			} catch (IOException e) {
				errorMessage.concat(
						mContext.getString(
							R.string.exception_dav_calendar, e.getMessage()));
				e.printStackTrace();
			} catch (DavException e) {
				errorMessage.concat(
						mContext.getString(
							R.string.exception_dav_calendar, e.getMessage()));
				e.printStackTrace();
			}

			if (hasAddressBook || hasCalendar) {
						
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
						} catch (RemoteException e) {
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
			Intent resultIntent = new Intent();
			if (hasAddressBook || hasCalendar) {
				resultIntent.putExtra(status, "success");
			} else
				resultIntent.putExtra(status, "failed");
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
		UpdateAccount account_task = new UpdateAccount(
				addAccount.getStringExtra(Constants.ACCOUNT_KEY_ACCOUNT_NAME), 
				addAccount.getStringExtra(Constants.ACCOUNT_KEY_ACCOUNT_TYPE),
				result, context);
		account_task.execute();
	}

}
