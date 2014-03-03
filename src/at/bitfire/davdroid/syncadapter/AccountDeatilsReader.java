package at.bitfire.davdroid.syncadapter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

public class AccountDeatilsReader {

	private Context context;
	private Properties properties;

	public AccountDeatilsReader(Context context) {
		this.context = context;
		/**
		 * Constructs a new Properties object.
		 */
		properties = new Properties();
	}

	public Properties getProperties(String ServerName) {

		try {
			/**
			 * getAssets() Return an AssetManager instance for your
			 * application's package. AssetManager Provides access to an
			 * application's raw asset files;
			 */
			AssetManager assetManager = context.getAssets();
			/**
			 * Open an asset using ACCESS_STREAMING mode. This
			 */
			InputStream inputStream = assetManager.open(ServerName + ".properties");
			/**
			 * Loads properties from the specified InputStream,
			 */
			properties.load(inputStream);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.e("AssetsPropertyReader",e.toString());
		}
		return properties;

	}

}
