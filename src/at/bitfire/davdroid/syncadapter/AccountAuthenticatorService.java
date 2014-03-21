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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import at.bitfire.davdroid.Constants;

public class AccountAuthenticatorService extends Service {
	private static AccountAuthenticator accountAuthenticator;
	private static final String TAG = "davdroid:AccountAuthenticatorService";

	private AccountAuthenticator getAuthenticator() {
		if (accountAuthenticator != null)
			return accountAuthenticator;
		return accountAuthenticator = new AccountAuthenticator(this);
	}

	@Override
	public IBinder onBind(Intent intent) {
		if (intent.getAction().equals(android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT))
			return getAuthenticator().getIBinder();
		return null;
	}


	private static class AccountAuthenticator extends AbstractAccountAuthenticator {
		Context context;

		public AccountAuthenticator(Context context) {
			super(context);
			this.context = context;
		}

		@Override
		public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType,
				String[] requiredFeatures, Bundle options) throws NetworkErrorException {
			Intent intent = new Intent(context, AddAccountActivity.class);
			intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
			Bundle bundle = new Bundle();
			bundle.putParcelable(AccountManager.KEY_INTENT, intent);
			return bundle;
		}

		@Override
		public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options)
				throws NetworkErrorException {
			return null;
		}

		@Override
		public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
			return null;
		}

		@SuppressLint("NewApi")
		@Override
		public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options)
				throws NetworkErrorException {
			Log.d(TAG, "> getAuthToken");

			// If the caller requested an authToken type we don't support, then
			// return an error
			//if (!authTokenType.equals(AccountGeneral.AUTHTOKEN_TYPE_READ_ONLY) && !authTokenType.equals(AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS)) {
			if (!authTokenType.equals(Constants.ACCOUNT_KEY_ACCESS_TOKEN) && !authTokenType.equals(Constants.ACCOUNT_KEY_REFRESH_TOKEN)) {
				final Bundle result = new Bundle();
				result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
				return result;
			}

			// Extract the username and password from the Account Manager, and ask
			// the server for an appropriate AuthToken.
			final AccountManager am = AccountManager.get(context.getApplicationContext());

			String authToken = am.peekAuthToken(account, authTokenType);

			Log.d(TAG, "peekAuthToken returned - " + authToken);

			// Lets give another try to authenticate the user
			if (TextUtils.isEmpty(authToken)) {

				String refreshToken = am.peekAuthToken(account, Constants.ACCOUNT_KEY_REFRESH_TOKEN);
				if (TextUtils.isEmpty(refreshToken)) {
					List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
					nameValuePairs.add(new BasicNameValuePair("client_id", "240498934020.apps.googleusercontent.com"));
					nameValuePairs.add(new BasicNameValuePair("client_secret", "HuScmc9E5sIp-3epayh7g3ge"));
					nameValuePairs.add(new BasicNameValuePair("refresh_token", refreshToken));
					nameValuePairs.add(new BasicNameValuePair("grant_type", "refresh_token"));
					HttpClient httpclient = new DefaultHttpClient();
					HttpPost httppost = new HttpPost("https://accounts.google.com/o/oauth2/token");
					HttpResponse httpResponse;
					String data = null;
					try {
						httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

						httpResponse = httpclient.execute(httppost);
						try {
							data = new BasicResponseHandler().handleResponse(httpResponse);
						} catch (HttpResponseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						/*Gson gSon = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create();
						gSon.fromJson(IOUtils.toString(result.getEntity().getContent()), LogonInfo.class).fill(form);*/
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					} catch (HttpResponseException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					JSONObject responseJson;
					try {
						responseJson = new JSONObject(data);
						authToken = responseJson.getString(authTokenType);
						am.setAuthToken(account, authTokenType, authToken);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

			// If we get an authToken - we return it
			if (!TextUtils.isEmpty(authToken)) {
				final Bundle result = new Bundle();
				result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
				result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
				result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
				return result;
			}

			// If we get here, then we couldn't access the user's password - so we
			// need to re-prompt them for their credentials. We do that by creating
			// an intent to display our AuthenticatorActivity.
			final Intent intent = new Intent(context, AccountAuthenticatorActivity.class);
			intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
			intent.putExtra(AddAccountActivity.ARG_ACCOUNT_TYPE, account.type);
			intent.putExtra(AddAccountActivity.ARG_AUTH_TYPE, authTokenType);
			intent.putExtra(AddAccountActivity.ARG_ACCOUNT_NAME, account.name);
			final Bundle bundle = new Bundle();
			bundle.putParcelable(AccountManager.KEY_INTENT, intent);
			return bundle;
		}

		@Override
		public String getAuthTokenLabel(String authTokenType) {
			return null;
		}

		@Override
		public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features)
				throws NetworkErrorException {
			return null;
		}

		@Override
		public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType,
				Bundle options) throws NetworkErrorException {
			return null;
		}
	}
}
