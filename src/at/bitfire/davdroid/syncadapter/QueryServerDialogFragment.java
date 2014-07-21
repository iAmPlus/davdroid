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
import at.bitfire.davdroid.webdav.PermanentlyMovedException;
import at.bitfire.davdroid.webdav.WebDavResource;
import ch.boye.httpclientandroidlib.HttpException;
import ch.boye.httpclientandroidlib.impl.client.CloseableHttpClient;
import ch.boye.httpclientandroidlib.util.TextUtils;

public class QueryServerDialogFragment extends DialogFragment
		implements LoaderCallbacks<ServerInfo> {
	private static final String TAG = "davdroid.QueryServerDialogFragment";
	static ServerInfo serverInfo = null;
	static Context mContext;
	static boolean hasAddressBook = false;
	static boolean hasCalendar = false;
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
		if (hasAddressBook || hasCalendar) {
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
			AccountDeatilsReader reader = new AccountDeatilsReader(mContext);
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
			WebDavResource base = null;
			WebDavResource principal = null;
			String errorMessage = "";

			// disable compression and enable network logging for debugging purposes 
			CloseableHttpClient httpClient = DavHttpClient.create(true, true);
			
			try {

				
				String principalPath = null;
				try {
					if (!TextUtils.isEmpty(properties.getProperty(
							Constants.ACCOUNT_KEY_CARDDAV_URL))) {
						serverInfo.setCarddavURL(properties.getProperty(
							Constants.ACCOUNT_KEY_CARDDAV_URL));

						if(authBearer != null)
							base= new WebDavResource(httpClient, new URI(
									serverInfo.getCarddavURL()), authBearer);
						else
							base = new WebDavResource(httpClient, new URI(serverInfo.getCarddavURL()), userName,
									password, true, true);
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
										new URI(serverInfo.getBaseURL()), userName, password, true, true);
						} else 
							throw new DavIncapableException(
								getContext().getString(
									R.string.no_carddav));
					}

					if(base != null) {

						// Google gived 401 for OPTIONS but 301 moved for
						// PROPFIND. So first try emply PROPFIND
						base.propfind(Mode.EMPTY_PROPFIND);
						// (1/5) detect capabilities
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
								getContext().getString(
									R.string.no_carddav));
						}
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
						getContext().getString(R.string.error_principal_path));

				principal = new WebDavResource(base, principalPath);
				principal.propfind(Mode.ADDRESS_BOOK_HOME_SETS);

				String pathAddressBooks = null;
				if (serverInfo.isCardDAV()) {
					pathAddressBooks = principal.getAddressbookHomeSet();
					if (pathAddressBooks != null) {
						Log.i(TAG, "Found address book home set: "
								+ pathAddressBooks);
					} else
						throw new DavIncapableException(
							getContext().getString(
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
								addressBooks.add(info);
								hasAddressBook = true;
							}

					serverInfo.setAddressBooks(addressBooks);
				}
			} catch (URISyntaxException e) {
				e.printStackTrace();
				errorMessage.concat(
					getContext().getString(
						R.string.exception_uri_syntax, e.getMessage()));
			} catch (DavIncapableException e) {
				e.printStackTrace();
				errorMessage.concat(
					getContext().getString(
						R.string.exception_incapable_resource,
						e.getLocalizedMessage()));
			} catch (DavException e) {
				e.printStackTrace();
				errorMessage.concat(
					getContext().getString(
						R.string.exception_io, e.getLocalizedMessage()));
			} catch (HttpException e) {
				e.printStackTrace();
				errorMessage.concat(
					getContext().getString(
						R.string.exception_http, e.getLocalizedMessage()));
			} catch (IOException e) {
				errorMessage.concat(
					getContext().getString(
						R.string.exception_http, e.getLocalizedMessage()));
			} catch (PermanentlyMovedException e) {
				errorMessage.concat(
					getContext().getString(
						R.string.exception_http, e.getLocalizedMessage()));
			}

			if(errorMessage != "") {
				errorMessage.concat("\n");
			}

			try {

				if (!TextUtils.isEmpty(properties.getProperty(
						Constants.ACCOUNT_KEY_CALDAV_URL))) {
					serverInfo.setCaldavURL(properties.getProperty(
						Constants.ACCOUNT_KEY_CALDAV_URL));

					if(authBearer != null)
						base= new WebDavResource(httpClient, new URI(
								serverInfo.getCaldavURL()), authBearer);
					else
						base = new WebDavResource(httpClient, new URI(serverInfo.getCaldavURL()), userName,
								password, true, true);
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
									new URI(serverInfo.getBaseURL()), userName, password, true, true);
					} else 
						throw new DavIncapableException(
							getContext().getString(
								R.string.no_caldav));
				}

				if(base != null) {

					base.options();
					base.propfind(Mode.EMPTY_PROPFIND);
					// (1/5) detect capabilities
					serverInfo.setCalDAV(base.supportsDAV("calendar-access"));

					/*Removed since google doesn't 
					 *support !base.supportsMethod("REPORT") ||*/
					if (!base.supportsMethod("PROPFIND")
							|| !serverInfo.isCalDAV())
						throw new DavIncapableException(
							getContext().getString(
								R.string.no_caldav));

					// (2/5) get principal URL
					base.propfind(Mode.CURRENT_USER_PRINCIPAL);

					String principalPath = base.getCurrentUserPrincipal();
					if (principalPath != null)
						Log.i(TAG, "Found principal path: " + principalPath);
					else
						throw new DavIncapableException(
							getContext().getString(
								R.string.error_principal_path));

					principal = new WebDavResource(base, principalPath);
				}

				principal.propfind(Mode.CALENDAR_HOME_SETS);

				String pathCalendars = null;
				if (serverInfo.isCalDAV()) {
					pathCalendars = principal.getCalendarHomeSet();
					if (pathCalendars != null)
						Log.i(TAG, "Found calendar home set: " + pathCalendars);
					else
						throw new DavIncapableException(
							getContext().getString(
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
								info.setTimezone(resource.getTimezone());
								calendars.add(info);
								hasCalendar = true;
							}

					serverInfo.setCalendars(calendars);
				}

			} catch (URISyntaxException e) {
				errorMessage.concat(
					getContext().getString(
						R.string.exception_uri_syntax, e.getMessage()));
			}  catch (IOException e) {
				errorMessage.concat(
					getContext().getString(
						R.string.exception_io, e.getLocalizedMessage()));
			} catch (DavIncapableException e) {
				errorMessage.concat(
					getContext().getString(
						R.string.exception_incapable_resource,
						e.getLocalizedMessage()));
			} catch (HttpException e) {
				errorMessage.concat(
					getContext().getString(
						R.string.exception_http, e.getLocalizedMessage()));
			} catch (DavException e) {
				errorMessage.concat(
					getContext().getString(
						R.string.exception_http, e.getLocalizedMessage()));
			} catch (PermanentlyMovedException e) {
				errorMessage.concat(
					getContext().getString(
						R.string.exception_http, e.getLocalizedMessage()));
			}

			if (errorMessage != "") {
				serverInfo.setErrorMessage(errorMessage);
			}

			return serverInfo;
		}
	}

}
