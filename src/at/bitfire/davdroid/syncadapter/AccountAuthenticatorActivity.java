package at.bitfire.davdroid.syncadapter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
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

import android.os.AsyncTask;
import android.os.Bundle;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import at.bitfire.davdroid.Constants;
import at.bitfire.davdroid.R;

public class AccountAuthenticatorActivity extends Activity {
	
	final Context myApp = this;
	WebView browser;// = (WebView)findViewById(R.id.browser);
	ListeningThread lT = null;
	Account account = null;
	private int listeningPort;
	
	final class ListeningThread extends AsyncTask<String, Integer, HttpResponse> implements Runnable {
	    private ServerSocket serverSocket;
	    private String code = null;
	    private int port = 0;
	    
	    public String getCode() {
	    	return code;
	    }

		@Override
		public void run() {
			boolean done = false;

		    Socket socket = null;

			while(!done) {
	            try {
	                socket = serverSocket.accept();
	            	String response = "";
	            	InputStream in = socket.getInputStream();
	                PrintWriter out = new PrintWriter(
					        new BufferedWriter(
					           new OutputStreamWriter(socket.getOutputStream())), 
					        true);
	                //byte[] b = new byte[1024];
	                StringBuilder sb = new StringBuilder();
	                while (true) {
	                    int ch = in.read();
	                    if ((ch < 0) || (ch == '\n')) {
	                        break;
	                    }
	                    sb.append((char) ch);
	                }
	                response = sb.toString();
	                Log.v("sk", response);
	                int index = -1;

	            	Date currentTime = new Date();

	                out.write("HTTP/1.0 200 OK\r\n");
		            out.write("Date: " + currentTime.toGMTString());
		            out.write("Server: Apache/0.8.4\r\n");
		            out.write("Content-Type: text/html\r\n");
		            out.write("Content-Length: 59\r\n");
		            out.write("\r\n");
		            
		            if( (index = response.indexOf("code")) > 0) {
	                	String tmpCode = response.substring(index + 5);
	                	done = true;
	                	Log.v("sk", "index = " + index);
	                	int index2 = -1;
	                	if( (index2 = tmpCode.indexOf("&")) < 0) {
	                		if( (index2 = tmpCode.indexOf(" ")) < 0)
	                			index2 = tmpCode.length() - 1;
	                	}
	                	Log.v("sk", "index2 = " + index2);
	                	tmpCode = tmpCode.substring(0, index2);
	                	Log.v("sk", "code = " + tmpCode);
	                	code = tmpCode;
			            out.write("<TITLE>Success</TITLE>");
			            out.write("<P>Please wait while the application is authorized</P>");
	                } else {
	                	out.write("<TITLE>Failed</TITLE>");
			            out.write("<P>Please close this window to return to the application and try again.</P>");
	                }
		            out.close();
	            	in.close();
	            	socket.close();
	                Log.v("end reached", "");
	            } catch (IOException e) {
	                e.printStackTrace();
	            }

	        }
			try {
				serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}

		@Override
		protected HttpResponse doInBackground(String... arg0) {
			try {
	            serverSocket = new ServerSocket(port);
	            port = serverSocket.getLocalPort();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
			return null;
		}
		
		@Override
		protected void onPostExecute(HttpResponse result) {
			loadUrl(port);
		}
	}
	
	class GetAuthCode extends AsyncTask<String, Integer, HttpResponse>{

		List<NameValuePair> nameValuePairs;
		String data = "";
		
		public GetAuthCode(List<NameValuePair> postData){
			nameValuePairs = postData;
		}
		
		
		@Override
		protected HttpResponse doInBackground(String... params) {
			

			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost("https://accounts.google.com/o/oauth2/token");
			HttpResponse response;
			try {
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				Log.v("sk", httppost.getURI().toString());
			
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
				/*Gson gSon = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create();
			    gSon.fromJson(IOUtils.toString(result.getEntity().getContent()), LogonInfo.class).fill(form);*/
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (HttpResponseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(HttpResponse result) {
			super.onPostExecute(result);
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
			//Account []accounts = mAccountManager.getAccountsByType(Constants.ACCOUNT_TYPE);
			setResult(RESULT_CANCELED);
			Log.v("sk", "name = " + account.name + "  type = " + account.type);
			if(authCode != null) {
				Bundle userData = new Bundle();
				//userData.putString(Constants.ACCOUNT_KEY_BASE_URL, serverInfo.getBaseURL());
				if (mAccountManager.addAccountExplicitly(account, "", userData)) {
					mAccountManager.setAuthToken(account, Constants.ACCOUNT_KEY_ACCESS_TOKEN, authCode);
					Log.v("sk", "access_token = " + authCode);
					setResult(RESULT_OK);
					if(refreshCode != null) {
						mAccountManager.setAuthToken(account, Constants.ACCOUNT_KEY_REFRESH_TOKEN, refreshCode);
						Log.v("sk", "refresh_token = " + refreshCode);
					}
				}
			}

			finish();
			
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		Bundle bnd = getIntent().getExtras();
		String account_name;
		String account_type;
		Log.v("sk", "onCreate");
		if(bnd != null) {
			account_name = bnd.getString(Constants.ACCOUNT_KEY_ACCOUNT_NAME);
			account_type = bnd.getString(Constants.ACCOUNT_KEY_ACCOUNT_TYPE);
			account = new Account(account_name, account_type);
			setContentView(R.layout.activity_main);
		} else {
			Log.v("sk", "onCreate null bundle");
			setResult(RESULT_CANCELED);
			finish();
		}
		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
	                                WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.browser);
		browser = (WebView) findViewById(R.id.browser_view);
		browser.setWebViewClient(new WebViewClient());
		browser.getSettings().setJavaScriptEnabled(true);
		browser.getSettings().setLoadWithOverviewMode(true);
		browser.getSettings().setBuiltInZoomControls(true);
		browser.setInitialScale(1);
		browser.setPadding(0, 0, 0, 0);
	    browser.getSettings().setUseWideViewPort(true);
	    browser.setWebViewClient(new WebViewClient() {
	        @Override
	        public void onPageFinished(WebView view, String url)
	        {
	            /* This call inject JavaScript into the page which just finished loading. */
	            if(url.contains("localhost")) {
	            	if(lT != null && lT.getCode() != null) {
	        			String code = lT.getCode();
	        			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();  
	        			nameValuePairs.add(new BasicNameValuePair("code", code));
	        			nameValuePairs.add(new BasicNameValuePair("client_id", "240498934020.apps.googleusercontent.com"));
	        			nameValuePairs.add(new BasicNameValuePair("client_secret", "HuScmc9E5sIp-3epayh7g3ge"));
	        			nameValuePairs.add(new BasicNameValuePair("redirect_uri", "http://localhost:" + listeningPort));
	        			nameValuePairs.add(new BasicNameValuePair("grant_type", "authorization_code"));
	        			new GetAuthCode(nameValuePairs).execute("");
	        		}
	            }
	        }
	    });
		lT = new ListeningThread();
		lT.execute("");
	}
	
	public void loadUrl(int port) {
		Log.v("sk", "Port is " + port);
		new Thread(lT).start();
		listeningPort = port;
		if (port != 0) {
			browser.loadUrl("https://accounts.google.com/o/oauth2/auth?" + 
				"scope=https%3A%2F%2Fwww.google.com%2Fcalendar%2Ffeeds%2F&" +
				"redirect_uri=http%3A%2F%2Flocalhost%3A" + port + "&response_type=code&" +
				"client_id=240498934020.apps.googleusercontent.com");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		return true;
	}

}
