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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

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
import android.widget.Toast;
import at.bitfire.davdroid.Constants;
import at.bitfire.davdroid.R;
import at.bitfire.davdroid.webdav.DavException;
import at.bitfire.davdroid.webdav.DavHttpClient;
import at.bitfire.davdroid.webdav.HttpPropfind.Mode;
import at.bitfire.davdroid.webdav.DavIncapableException;
import at.bitfire.davdroid.webdav.WebDavResource;
import ch.boye.httpclientandroidlib.HttpException;
import ch.boye.httpclientandroidlib.impl.client.CloseableHttpClient;
import ch.boye.httpclientandroidlib.util.TextUtils;

public class QueryServerDialogFragment extends DialogFragment
		implements LoaderCallbacks<ServerInfo> {
	private static final String TAG = "davdroid.QueryServerDialogFragment";
	static ServerInfo serverInfo = null;
	static Context mContext;
	static Bundle userData = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Holo);
		setCancelable(false);
	}

	public void createLoader() {

		Loader<ServerInfo> loader =
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
		if (serverInfo.getErrorMessage() != null)
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
			String authBearer = null;
			String userName = null;
			String password = null;
			AccountDeatilsReader reader = new AccountDeatilsReader(context);
			Properties properties =
				reader.getProperties(serverInfo.getAccountServer());

			if(serverInfo.getAccountServer().equals("Google")) {
				userData.putString("client_id", properties.getProperty("client_id_value"));
				userData.putString(properties.getProperty("client_secret_name"), properties.getProperty("client_secret_value"));
				userData.putString("token_url", properties.getProperty("token_url"));
				userData.putString(Constants.ACCOUNT_SERVER, properties.getProperty("type"));
				authBearer = "Bearer " + userData.getString(Constants.ACCOUNT_KEY_ACCESS_TOKEN);
			} else {
				userName = serverInfo.getUserName();
				password = serverInfo.getPassword();
			}
			String errorMessage = "";

			// disable compression and enable network logging for debugging purposes 
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
							context.getString(
								R.string.no_carddav));
				}

				if(base == null)
					throw new DavIncapableException(context.getString(R.string.no_carddav));
				
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
						throw new DavIncapableException(context.getString(R.string.error_home_set_address_books));
					
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
					context.getString(
						R.string.exception_uri_syntax, e.getMessage()));
			} catch (DavIncapableException e) {
				errorMessage.concat(
					context.getString(
						R.string.exception_incapable_resource,
						e.getLocalizedMessage()));
			} catch (DavException e) {
				errorMessage.concat(
					context.getString(
						R.string.exception_io, e.getLocalizedMessage()));
			} catch (HttpException e) {
				errorMessage.concat(
					context.getString(
						R.string.exception_http, e.getLocalizedMessage()));
			} catch (IOException e) {
				errorMessage.concat(
					context.getString(
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
							context.getString(
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
						throw new DavIncapableException(context.getString(R.string.error_home_set_calendars));
					
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
								calendars.add(info);
							}
					serverInfo.setCalendars(calendars);
				}

			} catch (URISyntaxException e) {
				errorMessage.concat(
					context.getString(
						R.string.exception_uri_syntax, e.getMessage()));
			}  catch (IOException e) {
				errorMessage.concat(
					context.getString(
						R.string.exception_io, e.getLocalizedMessage()));
			} catch (DavIncapableException e) {
				errorMessage.concat(
					context.getString(
						R.string.exception_incapable_resource,
						e.getLocalizedMessage()));
			} catch (HttpException e) {
				errorMessage.concat(
					context.getString(
						R.string.exception_http, e.getLocalizedMessage()));
			} catch (DavException e) {
				errorMessage.concat(
					context.getString(
						R.string.exception_http, e.getLocalizedMessage()));
			}

			if (errorMessage != "") {
				serverInfo.setErrorMessage(errorMessage);
			}

			return serverInfo;
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
					android.util.Log.v("SK", "**********************Got user principal. Returning");
					WebDavResource principal = new WebDavResource(wellKnown, wellKnown.getCurrentUserPrincipal());
					if (checkCapabilities(principal, davCapability))
						return principal;
					else
						Log.w(TAG, "Current-user-principal " + resource.getLocation() + " found via well-known service, but it doesn't support required DAV facilities");
				}
					android.util.Log.v("SK", "**********************User principal null");
			} catch (HttpException e) {
				Log.d(TAG, "Well-known service detection failed with HTTP error", e);
			} catch (DavException e) {
				Log.d(TAG, "Well-known service detection failed at DAV level", e);
			}

			try {
				// fall back to user-given initial context path 
				resource.propfind(Mode.CURRENT_USER_PRINCIPAL);
				if (resource.getCurrentUserPrincipal() != null) {
					android.util.Log.v("SK", "**********************Got user principal. Returning");
					WebDavResource principal = new WebDavResource(resource, resource.getCurrentUserPrincipal());
					if (checkCapabilities(principal, davCapability))
						return principal;
					else
						Log.w(TAG, "Current-user-principal " + resource.getLocation() + " found at user-given location, but it doesn't support required DAV facilities");
				}
					android.util.Log.v("SK", "**********************User principal null");
			} catch (HttpException e) {
				Log.d(TAG, "Service detection failed with HTTP error", e);
			} catch (DavException e) {
				Log.d(TAG, "Service detection failed at DAV level", e);
			}
			return null;
		}
		
		private boolean checkCapabilities(WebDavResource resource, String davCapability) throws IOException {
			// check for necessary capabilities
			try {
				resource.options();
				if (resource.supportsDAV(davCapability) &&
					resource.supportsMethod("PROPFIND") &&
					resource.supportsMethod("REPORT") )
					//resource.supportsMethod("GET") &&
					//resource.supportsMethod("PUT") &&
					//resource.supportsMethod("DELETE"))
					return true;
			} catch(HttpException e) {
				// for instance, 405 Method not allowed
			}
			return false;
		}
	}

}
