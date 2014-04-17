package at.bitfire.davdroid.syncadapter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;

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

import android.os.AsyncTask;
import android.os.Bundle;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.iamplus.aware.AwareSlidingLayout;
import at.bitfire.davdroid.Constants;
import at.bitfire.davdroid.R;

public class AccountAuthenticatorActivity extends Activity {

	final Context myApp = this;
	MyWebView browser;
	Account account = null;
	Properties properties;
	AwareSlidingLayout mSlidingLayer;

	static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	static Random rnd = new Random();

	String randomString( int len ) 
	{
		StringBuilder sb = new StringBuilder( len );
		for( int i = 0; i < len; i++ ) 
			sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
		return sb.toString();
	}

	class GetAuthCode extends AsyncTask<String, Integer, HttpResponse>{

		List<NameValuePair> nameValuePairs;
		String data = "";
		String token_secret = "";

		public GetAuthCode(List<NameValuePair> postData){
			nameValuePairs = postData;
		}

		@Override
		protected HttpResponse doInBackground(String... params) {

			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = null;
			if(properties.getProperty("type") != null && properties.getProperty("type").equals("Yahoo")) {
				if(params[0] != "")
					httppost = new HttpPost(properties.getProperty("token_url"));
				else
					httppost = new HttpPost(properties.getProperty("auth_url"));
			} 
			if(properties.getProperty("type") != null && properties.getProperty("type").equals("Google")) {
				httppost = new HttpPost(properties.getProperty("token_url"));
			}
			HttpResponse response;
			try {
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

				response = httpclient.execute(httppost);
				try {
					data = new BasicResponseHandler().handleResponse(response);
				} catch (HttpResponseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return response;
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (HttpResponseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		@SuppressLint("SetJavaScriptEnabled")
		@Override
		protected void onPostExecute(HttpResponse result) {
			super.onPostExecute(result);
			if(properties.getProperty("type") != null && properties.getProperty("type").equals("Yahoo")) {
				Boolean complete = false;
				StringTokenizer st = new StringTokenizer(data, "&");
				while (st.hasMoreTokens()) {
					StringTokenizer st2 = new StringTokenizer(st.nextToken(), "=");
					if(st2.hasMoreTokens()) {
						String key = st2.nextToken();
						if(key.equals("xoauth_yahoo_guid")) {
							complete = true;
						}
						if(st2.hasMoreTokens()) {
							String value = st2.nextToken();
							if( key.equals("oauth_token_secret") ) {
								token_secret = value;
							}
							if(key.equals("oauth_token") || key.equals("oauth_token_secret") || key.equals("oauth_session_handle")) {
								AccountManager mAccountManager = AccountManager.get(myApp.getApplicationContext());
								Account []accounts = mAccountManager.getAccountsByType(Constants.ACCOUNT_TYPE);
								Boolean found = false;
								for (Account acc : accounts) {
									if(acc.name.equals(account.name))
										break;
								}
								if(!found) {
									Bundle userData = new Bundle();
									mAccountManager.addAccountExplicitly(account, "", userData);
								}
								if(key.equals("oauth_token"))
									mAccountManager.setAuthToken(account, Constants.ACCOUNT_KEY_ACCESS_TOKEN, value);
								else
									mAccountManager.setAuthToken(account, key, value);
							}
							if(key.equals("xoauth_request_auth_url")) {
								try {
									String request_auth_url = URLDecoder.decode(value, "UTF-8");

									initWebView();
									browser.setWebViewClient(new WebViewClient() {
										@Override
										public void onPageFinished(WebView view, String url)
										{
											/* This call inject JavaScript into the page which just finished loading. */
											String auth_token = null;
											String auth_verifier = null;
											if(url.startsWith("http://localhost")) {
												StringTokenizer st = new StringTokenizer (url.substring(url.indexOf("?")+1), "&");
												while(st.hasMoreTokens()) {
													String tmp = st.nextToken();
													if(tmp.startsWith("oauth_token=")) {
														auth_token = tmp.substring(tmp.indexOf("=")+1);
													}
													if(tmp.startsWith("oauth_verifier=")) {
														auth_verifier = tmp.substring(tmp.indexOf("=")+1);
													}
												}

												List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
												if(properties.getProperty("random_string") != null) {
													nameValuePairs.add(
															new BasicNameValuePair(properties.getProperty("random_string"), randomString(rnd.nextInt(32))));
												}
												if(properties.getProperty("timestamp_name") != null) {
													nameValuePairs.add(
															new BasicNameValuePair(properties.getProperty("timestamp_name"), "" + System.currentTimeMillis()));

												}
												if ((properties.getProperty("client_id_name") != null) && (properties.getProperty("client_id_value") != null)) {
													nameValuePairs.add(
															new BasicNameValuePair(properties.getProperty("client_id_name"), properties.getProperty("client_id_value")));
													if ((properties.getProperty("signature_method_name") != null) && (properties.getProperty("signature_method_value") != null)) {
														nameValuePairs.add(
																new BasicNameValuePair(properties.getProperty("signature_method_name"), properties.getProperty("signature_method_value")));
														if ((properties.getProperty("signature_name") != null)) {
															nameValuePairs.add(
																	new BasicNameValuePair(properties.getProperty("signature_name"), properties.getProperty("client_secret_value") + "&" + token_secret));
														}
													}
												}
												nameValuePairs.add(
														new BasicNameValuePair("oauth_version", properties.getProperty("oauth_version")));
												nameValuePairs.add(new BasicNameValuePair("oauth_token", auth_token));
												nameValuePairs.add(new BasicNameValuePair("oauth_verifier", auth_verifier));
												new GetAuthCode(nameValuePairs).execute("token");
												String html="<html><head></head><body> Please wait</body></html>";
												browser.loadData(html, "text/html", "utf-8");
											}
											
										}
									});
									browser.loadUrl(request_auth_url);
								} catch (UnsupportedEncodingException e) {
									e.printStackTrace();
								}
							}
						}
					}
				}
				if (complete) {
					setResult(RESULT_OK);
					finish();
				}
			}
			if(properties.getProperty("type") != null && properties.getProperty("type").equals("Google")) {

				JSONObject responseJson;
				String authCode = null;
				String refreshCode = null;
				try {
					responseJson = new JSONObject(data);
	
					authCode = responseJson.getString("access_token");
					refreshCode = responseJson.getString("refresh_token");
	
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				AccountManager mAccountManager = AccountManager.get(myApp.getApplicationContext());
				setResult(RESULT_CANCELED);
				if(authCode != null) {
					Bundle userData = new Bundle();
					if (mAccountManager.addAccountExplicitly(account, "", userData)) {
						mAccountManager.setAuthToken(account, Constants.ACCOUNT_KEY_ACCESS_TOKEN, authCode);
						setResult(RESULT_OK);
						if(refreshCode != null) {
							mAccountManager.setAuthToken(account, Constants.ACCOUNT_KEY_REFRESH_TOKEN, refreshCode);
						}
					}
				}
	
				finish();
			}

		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		AccountDeatilsReader reader = new AccountDeatilsReader(this);
		Bundle bnd = getIntent().getExtras();
		ServerInfo serverInfo = null;
		String account_name;
		String account_type;
		if(bnd != null) {
			serverInfo = (ServerInfo) bnd.getSerializable(Constants.KEY_SERVER_INFO);
			account_name = serverInfo.getAccountName();
			account_type = Constants.ACCOUNT_TYPE;
			account = new Account(account_name, account_type);
			properties = reader.getProperties(serverInfo.getAccountServer());
			setContentView(R.layout.authenticator);
			mSlidingLayer = (AwareSlidingLayout)findViewById(R.id.slidinglayout);
			mSlidingLayer.setOnActionListener(new AwareSlidingLayout.OnActionListener(){
				@Override
				public void onAction(int type){
					if(type == AwareSlidingLayout.NEGATIVE) {
						onBackPressed();
						if(mSlidingLayer != null){
							mSlidingLayer.reset();
						}
					}
				}
			});
		} else {
			setResult(RESULT_CANCELED);
			finish();
		}

	}

	@Override
	protected void onResume() {
		super.onResume();

		if(properties.getProperty("type") != null && properties.getProperty("type").equals("Yahoo")) {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			if(properties.getProperty("random_string") != null) {
				nameValuePairs.add(
						new BasicNameValuePair(properties.getProperty("random_string"), randomString(rnd.nextInt(32))));
			}
			if(properties.getProperty("timestamp_name") != null) {
				nameValuePairs.add(
						new BasicNameValuePair(properties.getProperty("timestamp_name"), "" + System.currentTimeMillis()));

			}
			if ((properties.getProperty("client_id_name") != null) && (properties.getProperty("client_id_value") != null)) {
				nameValuePairs.add(
						new BasicNameValuePair(properties.getProperty("client_id_name"), properties.getProperty("client_id_value")));
				if ((properties.getProperty("signature_method_name") != null) && (properties.getProperty("signature_method_value") != null)) {
					nameValuePairs.add(
							new BasicNameValuePair(properties.getProperty("signature_method_name"), properties.getProperty("signature_method_value")));
					if ((properties.getProperty("signature_name") != null)) {
						nameValuePairs.add(
								new BasicNameValuePair(properties.getProperty("signature_name"), properties.getProperty("client_secret_value") + "&"));
					}
				}
			}
			nameValuePairs.add(
					new BasicNameValuePair("oauth_version", properties.getProperty("oauth_version")));
			if ((properties.getProperty("redirect_ui_name") != null) && (properties.getProperty("redirect_uri_value") != null)) {
				nameValuePairs.add(
						new BasicNameValuePair(properties.getProperty("redirect_ui_name"), "http://localhost"));
			}
			new GetAuthCode(nameValuePairs).execute("");
		}

		if(properties.getProperty("type") != null && properties.getProperty("type").equals("Google")) {
			initWebView();

			browser.setWebViewClient(new WebViewClient() {

				@Override
				public boolean shouldOverrideUrlLoading(WebView webView, String url) {
					/* This call inject JavaScript into the page which just finished loading. */
					if(url.startsWith("http://localhost")) {
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
						String html="<html><head></head><body> Please wait</body></html>";
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
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			String auth_url = properties.getProperty("auth_url") + "?" + query;
			browser.loadUrl( auth_url);
		}
	}
	
	@SuppressLint("SetJavaScriptEnabled")
	public void initWebView() {
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.browser);
		browser = (MyWebView) findViewById(R.id.browser_view);
		browser.setWebViewClient(new WebViewClient());
		browser.getSettings().setJavaScriptEnabled(true);
		browser.getSettings().setLoadWithOverviewMode(true);
		browser.getSettings().setBuiltInZoomControls(true);
		browser.setInitialScale(1);
		browser.setPadding(0, 0, 0, 0);
		browser.getSettings().setUseWideViewPort(true);
		mSlidingLayer = (AwareSlidingLayout)findViewById(R.id.slidinglayout);
		mSlidingLayer.setOnActionListener(new AwareSlidingLayout.OnActionListener(){
			@Override
			public void onAction(int type){
				if(type == AwareSlidingLayout.POSITIVE) {
					if(mSlidingLayer != null){
						mSlidingLayer.reset();
					}
				}
				else if(type == AwareSlidingLayout.NEGATIVE) {
					onBackPressed();
					if(mSlidingLayer != null){
						mSlidingLayer.reset();
					}
				}
			}
		});
	}

}
