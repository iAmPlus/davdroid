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
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.http.HttpException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import at.bitfire.davdroid.Constants;
import at.bitfire.davdroid.R;
import at.bitfire.davdroid.webdav.HttpPropfind.Mode;
import at.bitfire.davdroid.webdav.DavIncapableException;
import at.bitfire.davdroid.webdav.PermanentlyMovedException;
import at.bitfire.davdroid.webdav.WebDavResource;

public class QueryServerDialogFragment extends DialogFragment implements LoaderCallbacks<ServerInfo> {
	private static final String TAG = "davdroid.QueryServerDialogFragment";
	//public static final String EXTRA_ACCOUNT_SERVER = "account_server";
	static ServerInfo serverInfo = null;
	static Context mContext;
	public static String authCode = null;
	boolean hasAddressBook = true;
	boolean hasCalendar = true;

	ProgressBar progressBar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Holo_Light_Dialog);
		setCancelable(false);

		mContext = getActivity();
		serverInfo = (ServerInfo)getArguments().getSerializable(Constants.KEY_SERVER_INFO);
		AccountManager accountManager = AccountManager.get(mContext.getApplicationContext());
		Account account = new Account(serverInfo.getAccountName(), Constants.ACCOUNT_TYPE);
		Log.v("sk", "Calling getAuthToken  " + serverInfo.getAccountName() + "  " + Constants.ACCOUNT_TYPE);
		accountManager.getAuthToken(account, Constants.ACCOUNT_KEY_ACCESS_TOKEN, null, (Activity) mContext, new AccountManagerCallback<Bundle>(){

			@Override
			public void run(AccountManagerFuture<Bundle> authBundle) {
				// TODO Auto-generated method stub
				try {
					Log.v("sk", "Calling getToken " + " accounts");
					Bundle bnd = (Bundle) authBundle.getResult();
					authCode = bnd.getString(AccountManager.KEY_AUTHTOKEN);
					Log.d("sk", "GetToken Bundle is " + bnd);
					Log.d("sk", "access_token " + authCode);
					createLoader();

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

			}

		}, null);
	}

	public void createLoader() {
		Loader<ServerInfo> loader = getLoaderManager().initLoader(0, getArguments(), this);
		loader.forceLoad();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.query_server, container, false);
		return v;
	}

	@Override
	public Loader<ServerInfo> onCreateLoader(int id, Bundle args) {
		Log.i(TAG, "onCreateLoader");
		return new ServerInfoLoader(getActivity(), args);
	}

	@Override
	public void onLoadFinished(Loader<ServerInfo> loader, ServerInfo serverInfo) {
		if (serverInfo.getErrorMessage() != null)
			Toast.makeText(getActivity(), serverInfo.getErrorMessage(), Toast.LENGTH_LONG).show();
		if (hasAddressBook || hasCalendar) {
			SelectCollectionsFragment selectCollections = new SelectCollectionsFragment();
			Bundle arguments = new Bundle();
			arguments.putSerializable(Constants.KEY_SERVER_INFO, serverInfo);
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
		Bundle args;

		public ServerInfoLoader(Context context, Bundle args) {
			super(context);
			this.args = args;
		}

		@Override
		public ServerInfo loadInBackground() {

			AccountDeatilsReader reader = new AccountDeatilsReader(mContext);
			Properties properties = reader.getProperties(serverInfo.getAccountServer());
			WebDavResource base = null;
			WebDavResource principal = null;
			String errorMessage = "";

			try {

				//WebDavResource cardDavPrincipal = null;
				//boolean cardDavRedirect = false;

				if(properties.getProperty(Constants.ACCOUNT_KEY_BASE_URL) != null) {
					Log.v("sk", "Has base URL " + properties.getProperty(Constants.ACCOUNT_KEY_BASE_URL));
					serverInfo.setBaseURL(properties.getProperty(Constants.ACCOUNT_KEY_BASE_URL));
					base = new WebDavResource(new URI(serverInfo.getBaseURL()), true, authCode);
				}
				String principalPath;
				try {
					if(base != null) {
						Log.v("sk", "EMPTY_PROPFIND on " + properties.getProperty(Constants.ACCOUNT_KEY_BASE_URL));
						base.propfind(Mode.EMPTY_PROPFIND);
						// (1/5) detect capabilities
						Log.v("sk", "options on " + properties.getProperty(Constants.ACCOUNT_KEY_BASE_URL));
						base.options();
						serverInfo.setCardDAV(base.supportsDAV("addressbook"));
						serverInfo.setCalDAV(base.supportsDAV("calendar-access"));
					} 
					if (base == null || !serverInfo.isCardDAV()){
						Log.v("sk", "Has carddav URL " + properties.getProperty(Constants.ACCOUNT_KEY_CARDDAV_URL));
						serverInfo.setCarddavURL(properties.getProperty(Constants.ACCOUNT_KEY_CARDDAV_URL));
						base= new WebDavResource(new URI(serverInfo.getCarddavURL()), true, authCode);

						Log.v("sk", "EMPTY_PROPFIND on " + properties.getProperty(Constants.ACCOUNT_KEY_CARDDAV_URL));
						base.propfind(Mode.EMPTY_PROPFIND);
						// (1/5) detect capabilities
						Log.v("sk", "options on " + properties.getProperty(Constants.ACCOUNT_KEY_CARDDAV_URL));
						base.options();
						serverInfo.setCardDAV(base.supportsDAV("addressbook"));
					} else {
						throw new DavIncapableException(getContext().getString(R.string.neither_caldav_nor_carddav));
					}

					/*Removed since google doesn't support !base.supportsMethod("REPORT") on base but only on principal url ||*/
					if (!base.supportsMethod("PROPFIND") || !serverInfo.isCardDAV() ) {
						if (serverInfo.getCarddavURL() == null) {

							base.propfind(Mode.EMPTY_PROPFIND);
							// (1/5) detect capabilities
							base.options();
							serverInfo.setCardDAV(base.supportsDAV("addressbook"));
						}
					
							throw new DavIncapableException(getContext().getString(R.string.neither_caldav_nor_carddav));
					}

					// (2/5) get principal URL
					base.propfind(Mode.CURRENT_USER_PRINCIPAL);
					principalPath = base.getCurrentUserPrincipal();
				} catch(PermanentlyMovedException e) {
					//cardDavRedirect = true;
					Log.v("sk", "Caught permanently moved");
					e.printStackTrace();
					serverInfo.setCardDAV(true);
					// Special case for google. Google gives a 301 permenantly moved to addressbook URL.
					// Parsing and taking the principal URL instead.
					principalPath = base.getRedirectionURL();
					int end = principalPath.indexOf((int)'@');
					end = principalPath.indexOf((int)'/', end);
					principalPath = principalPath.substring(0, end+1);
					Log.v("sk", "Principal path " + principalPath);
				}

				if (principalPath != null)
					Log.i(TAG, "Found principal path: " + principalPath);
				else
					throw new DavIncapableException(getContext().getString(R.string.error_principal_path));

				principal = new WebDavResource(base, principalPath);
				principal.propfind(Mode.ADDRESS_BOOK_HOME_SETS);
				
				String pathAddressBooks = null;
				if (serverInfo.isCardDAV() /*&& !cardDavRedirect*/) {
					pathAddressBooks = principal.getAddressbookHomeSet();
					if (pathAddressBooks != null)
						Log.i(TAG, "Found address book home set: " + pathAddressBooks);
					else
						throw new DavIncapableException(getContext().getString(R.string.error_home_set_address_books));
				}


				// (4/5) get address books
				if (serverInfo.isCardDAV()) {
					List<ServerInfo.ResourceInfo> addressBooks = new LinkedList<ServerInfo.ResourceInfo>();

					WebDavResource homeSetAddressBooks = new WebDavResource(principal, pathAddressBooks, true);
					homeSetAddressBooks.propfind(Mode.ADDRESS_BOOK_MEMBERS_COLLECTIONS);

					if (homeSetAddressBooks.getMembers() != null)
						for (WebDavResource resource : homeSetAddressBooks.getMembers())
							if (resource.isAddressBook()) {
								Log.i(TAG, "Found address book: " + resource.getLocation().getRawPath());
								ServerInfo.ResourceInfo info = new ServerInfo.ResourceInfo(
									ServerInfo.ResourceInfo.Type.ADDRESS_BOOK,
									resource.getLocation().getRawPath(),
									resource.getDisplayName(),
									resource.getDescription(), resource.getColor()
								);
								addressBooks.add(info);
							}

					serverInfo.setAddressBooks(addressBooks);
				}
			} catch (URISyntaxException e) {
				e.printStackTrace();
				errorMessage.concat(getContext().getString(R.string.exception_uri_syntax, e.getMessage()));
			}  catch (IOException e) {
				e.printStackTrace();
				errorMessage.concat(getContext().getString(R.string.exception_io, e.getLocalizedMessage()));
			} catch (DavIncapableException e) {
				e.printStackTrace();
				errorMessage.concat(getContext().getString(R.string.exception_incapable_resource, e.getLocalizedMessage()));
			} catch (HttpException e) {
				e.printStackTrace();
				errorMessage.concat(getContext().getString(R.string.exception_http, e.getLocalizedMessage()));
			}

			if(errorMessage != "") {
				errorMessage.concat("\n");
			}

			try {

				if(base == null || !serverInfo.isCalDAV()) {
					if(properties.getProperty(Constants.ACCOUNT_KEY_CALDAV_URL) != null) {
						Log.v("sk", "Has caldav URL " + properties.getProperty(Constants.ACCOUNT_KEY_CALDAV_URL));
						serverInfo.setCarddavURL(properties.getProperty(Constants.ACCOUNT_KEY_CALDAV_URL));
						base= new WebDavResource(new URI(serverInfo.getCarddavURL()), true, authCode);
					} else {
						throw new DavIncapableException(getContext().getString(R.string.neither_caldav_nor_carddav));
					}

					base.options();
					serverInfo.setCalDAV(base.supportsDAV("calendar-access"));

					/*Removed since google doesn't support !base.supportsMethod("REPORT") ||*/
					if (!base.supportsMethod("PROPFIND") || !serverInfo.isCalDAV())
							throw new DavIncapableException(getContext().getString(R.string.neither_caldav_nor_carddav));

					// (2/5) get principal URL
					base.propfind(Mode.CURRENT_USER_PRINCIPAL);
					
					String principalPath = base.getCurrentUserPrincipal();
					if (principalPath != null)
						Log.i(TAG, "Found principal path: " + principalPath);
					else
						throw new DavIncapableException(getContext().getString(R.string.error_principal_path));

					principal = new WebDavResource(base, principalPath);
				}

				principal.propfind(Mode.CALENDAR_HOME_SETS);

				String pathCalendars = null;
				if (serverInfo.isCalDAV()) {
					pathCalendars = principal.getCalendarHomeSet();
					if (pathCalendars != null)
						Log.i(TAG, "Found calendar home set: " + pathCalendars);
					else
						throw new DavIncapableException(getContext().getString(R.string.error_home_set_calendars));
				}


				// (5/5) get calendars
				if (serverInfo.isCalDAV()) {
					WebDavResource homeSetCalendars = new WebDavResource(principal, pathCalendars, true);
					homeSetCalendars.propfind(Mode.CALENDAR_MEMBERS_COLLECTIONS);

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
									resource.getLocation().getRawPath(),
									resource.getDisplayName(),
									resource.getDescription(), resource.getColor()
								);
								info.setTimezone(resource.getTimezone());
								calendars.add(info);
							}

					serverInfo.setCalendars(calendars);
				}

			} catch (URISyntaxException e) {
				errorMessage.concat(getContext().getString(R.string.exception_uri_syntax, e.getMessage()));
			}  catch (IOException e) {
				errorMessage.concat(getContext().getString(R.string.exception_io, e.getLocalizedMessage()));
			} catch (DavIncapableException e) {
				errorMessage.concat(getContext().getString(R.string.exception_incapable_resource, e.getLocalizedMessage()));
			} catch (HttpException e) {
				errorMessage.concat(getContext().getString(R.string.exception_http, e.getLocalizedMessage()));
			}

			if (errorMessage != "") {
				Log.v("sk", "Error message " + errorMessage);
				serverInfo.setErrorMessage(errorMessage);
			}

			return serverInfo;
		}
	}

}
