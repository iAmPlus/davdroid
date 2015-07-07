package at.bitfire.davdroid.syncadapter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import ch.boye.httpclientandroidlib.util.TextUtils;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import at.bitfire.davdroid.Constants;
import at.bitfire.davdroid.R;
import at.bitfire.davdroid.resource.ServerInfo;

import aneeda.app.ActivityHelper;
import aneeda.content.ContextHelper;

public class AuthenticatorActivity extends AccountAuthenticatorActivity {

	//private static final String TAG = "AccountAuthenticatorActivity";
	final Context myApp = this;
	MyWebView browser;
	Account reauth_account = null;
	Properties properties;
	private String authCode;
	private String refreshCode;
	private String expires;

	class GetAuthCode extends AsyncTask<String, Integer, HttpResponse>{

		List<NameValuePair> nameValuePairs;
		String data = "";

		public GetAuthCode(List<NameValuePair> postData){
			nameValuePairs = postData;
		}

		@Override
		protected HttpResponse doInBackground(String... params) {

			HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
			HttpConnectionParams.setSoTimeout(httpParams, 5000);
			HttpClient httpclient = new DefaultHttpClient(httpParams);
			HttpPost httppost = null;
			HttpGet httpget = null;
			if(properties.getProperty("type") != null && properties.getProperty("type").equals("Google")) {
				if(params[0] != "") {
					httpget = new HttpGet(properties.getProperty("get_email_url"));
					httpget.addHeader("Authorization", "Bearer " + authCode);
				} else
					httppost = new HttpPost(properties.getProperty("token_url"));
			}
			HttpResponse response = null;
			try {
				if(httppost != null) {
					httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
					response = httpclient.execute(httppost);
				} else if(httpget != null) {
					response = httpclient.execute(httpget);
				}

				try {
					data = new BasicResponseHandler().handleResponse(response);
				} catch (HttpResponseException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (HttpResponseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return response;
		}

        private void showErrorDialog() {
            AlertDialog dialog;
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    myApp);
            builder.setTitle(
                    myApp.getResources().getString(
                            R.string.no_token))
                    .setMessage(
                            myApp.getResources().getString(
                                    R.string.oauth_token_error))
                    .setCancelable(false)
                    .setNegativeButton(R.string.network_dialog_back,
                            new DialogInterface.OnClickListener() {
                                public void onClick(
                                        DialogInterface dialog, int id) {
                                    dialog.cancel();
                                    setResult(RESULT_CANCELED);
                                    finish();
                                }
                            });
            dialog = builder.create();
            dialog.show();
        }

		@SuppressLint("SetJavaScriptEnabled")
		@Override
		protected void onPostExecute(HttpResponse result) {
			super.onPostExecute(result);
			if(properties.getProperty("type") != null && properties.getProperty("type").equals("Google")) {

				JSONObject responseJson;
				String email = null;
				String tokenData = data;
				StringTokenizer st = new StringTokenizer(tokenData, "&");
				while(st.hasMoreTokens()) {
					String token = st.nextToken();
					if(token.startsWith("email")) {
						email = token.substring(token.indexOf("=") + 1);
					}
				}

				if( email == null) {
					if(!TextUtils.isEmpty(data)) {
						try {
							responseJson = new JSONObject(data);
		
							authCode = responseJson.getString("access_token");
							refreshCode = responseJson.getString("refresh_token");
							expires = responseJson.getString("expires_in");
							long expiry = Long.parseLong(expires);
							expiry += (System.currentTimeMillis()/1000);
							expires = Long.valueOf(expiry).toString();
		
						} catch (JSONException e) {
							e.printStackTrace();
							showErrorDialog();
						}
						new GetAuthCode(nameValuePairs).execute("get_email");
					} else {
						showErrorDialog();
					}
				} else {
					AccountManager mAccountManager = AccountManager.get(myApp.getApplicationContext());
					if(authCode != null) {
						if(reauth_account != null) {
							mAccountManager.setAuthToken(reauth_account, Constants.ACCOUNT_KEY_ACCESS_TOKEN, authCode);
							if(refreshCode != null) {
								mAccountManager.setAuthToken(reauth_account, Constants.ACCOUNT_KEY_REFRESH_TOKEN, refreshCode);
								if(expires != null)
									mAccountManager.setUserData(reauth_account, "oauth_expires_in", expires);
							}
							setResult(RESULT_OK);
						} else {
							Bundle userData = new Bundle();

							userData.putString(Constants.ACCOUNT_KEY_ACCESS_TOKEN, authCode);
							Intent intent = new Intent();
							intent.putExtra("account_name", email);
							setResult(RESULT_OK, intent);

							if(refreshCode != null) {
								userData.putString(Constants.ACCOUNT_KEY_REFRESH_TOKEN, refreshCode);
							}
							userData.putString("client_id", properties.getProperty("client_id_value"));
							userData.putString(properties.getProperty("client_secret_name"), properties.getProperty("client_secret_value"));
							userData.putString("token_url", properties.getProperty("token_url"));
							userData.putString(Constants.ACCOUNT_SERVER, properties.getProperty("type"));
							if(expires != null)
								userData.putString("oauth_expires_in", expires);
							intent.putExtra(Constants.ACCOUNT_BUNDLE, userData);
						}
						finish();
					} else {
						showErrorDialog();
					}
				}

			}

		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    /*overridePendingTransition(android.R.anim.quick_enter_in,
                android.R.anim.quick_enter_out);*/
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		AccountDeatilsReader reader = new AccountDeatilsReader(this);
		Bundle bnd = getIntent().getExtras();
		ServerInfo serverInfo = null;
	
		//Check for re-authentication intent
		if(getIntent().hasExtra(Constants.ACCOUNT_KEY_ACCESS_TOKEN)) {
			AccountManager mgr = AccountManager.get(getApplicationContext());
			reauth_account = getIntent().getParcelableExtra(Constants.ACCOUNT_PARCEL);
			properties = reader.getProperties(mgr.getUserData(reauth_account, Constants.ACCOUNT_SERVER));
		} else if(bnd != null) {
			serverInfo = (ServerInfo) bnd.getSerializable(Constants.KEY_SERVER_INFO);
			properties = reader.getProperties(serverInfo.getAccountServer());
		} else {
			setResult(RESULT_CANCELED);
			finish();
		}
		setContentView(R.layout.pref_title_layout);

		if(properties.getProperty("type") != null && properties.getProperty("type").equals("Google")) {
			initWebView();

			browser.setWebViewClient(new WebViewClient() {

				@Override
				public boolean shouldOverrideUrlLoading(WebView webView, String url) {
					/* This call inject JavaScript into the page which just finished loading. */
					if(url.startsWith(properties.getProperty("redirect_uri_value"))) {
						browser.stopLoading();
						StringTokenizer st = new StringTokenizer (url.substring(url.indexOf("?")+1), "&");
						String code = null;
						while(st.hasMoreTokens()) {
							code = st.nextToken();
							if(code.startsWith("code=")) {
								code = code.substring(code.indexOf("=")+1);
								break;
							}
						}
						List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
						nameValuePairs.add(new BasicNameValuePair("code", code));
						nameValuePairs.add(new BasicNameValuePair(properties.getProperty("client_id_name"), properties.getProperty("client_id_value")));
						nameValuePairs.add(new BasicNameValuePair(properties.getProperty("client_secret_name"), properties.getProperty("client_secret_value")));
						nameValuePairs.add(new BasicNameValuePair(properties.getProperty("redirect_uri_name"), properties.getProperty("redirect_uri_value")));
						nameValuePairs.add(new BasicNameValuePair(properties.getProperty("grant_type_name"), properties.getProperty("grant_type_value")));
						new GetAuthCode(nameValuePairs).execute("");
						String html="<html><head></head><body><font size=\"32\">Signing in to " + properties.getProperty("type") + ". Please wait</font></body></html>";
						browser.clearHistory();
						browser.loadData(html, "text/html", "utf-8");
					}

					return super.shouldOverrideUrlLoading(webView, url);
				}
			});
			String query = "";
			try {
				if(properties.getProperty("scope") != null)
					query = query.concat("scope=" + URLEncoder.encode(properties.getProperty("scope"), "utf-8"));
				if( (properties.getProperty("redirect_uri_name") != null)
						&& (properties.getProperty("redirect_uri_value") != null))
					query = query.concat("&" + properties.getProperty("redirect_uri_name") + "=" +
							URLEncoder.encode(properties.getProperty("redirect_uri_value"), "utf-8"));
				if((properties.getProperty("response_type_name") != null) && 
						(properties.getProperty("response_type_value") != null))
					query = query.concat("&" + properties.getProperty("response_type_name") + "=" +
							URLEncoder.encode(properties.getProperty("response_type_value"), "utf-8"));
				if((properties.getProperty("client_id_name") != null) && properties.getProperty("client_id_value") != null)
					query = query.concat("&" + properties.getProperty("client_id_name") + "=" +
							URLEncoder.encode(properties.getProperty("client_id_value"), "utf-8"));
				//Required to re-generate refresh token.
				//Without this sync will work only till access token expires
				query = query.concat("&approval_prompt=force&include_granted_scopes=true&access_type=offline");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			String auth_url = properties.getProperty("auth_url") + "?" + query;
			browser.loadUrl( auth_url);
		}

		if((getIntent()!= null) && 
				(getIntent().getBooleanExtra(Constants.EXTRA_IS_DEVICE_SETUP, false))) {

			/* Disable In-app menu */
			ActivityHelper.disableInAppMenu(this);
		} 
	}

	@Override
	protected void onPause() {
		super.onPause();
		if(browser != null)
			browser.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if(browser != null)
			browser.onResume();
	}

	@SuppressLint("SetJavaScriptEnabled")
	public void initWebView() {
		setContentView(R.layout.browser);
		browser = (MyWebView) findViewById(R.id.browser_view);
		Button cancel = (Button) findViewById(R.id.cancel);

		//Make sure No cookies are created
		CookieManager.getInstance().removeAllCookie();

		//Make sure no caching is done
		browser.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		browser.getSettings().setAppCacheEnabled(false);
		browser.clearHistory();
		browser.clearCache(true);

		//Make sure no autofill for Forms/ user-name password happens for the app
		browser.clearFormData();
		browser.getSettings().setSavePassword(false);
		browser.getSettings().setSaveFormData(false);

		browser.getSettings().setJavaScriptEnabled(true);
		browser.getSettings().setLoadWithOverviewMode(true);
		browser.getSettings().setBuiltInZoomControls(true);
		browser.getSettings().setDisplayZoomControls(false);
		browser.setInitialScale(1);
		browser.setPadding(0, 0, 0, 0);
		browser.getSettings().setUseWideViewPort(true);
	}
	
    public void finish(View view) {
        finish();
        /*overridePendingTransition(android.R.anim.quick_exit_in,
                android.R.anim.quick_exit_out);*/
    }

}
