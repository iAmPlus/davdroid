/*
 * Copyright © 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package at.bitfire.davdroid;

import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Constants {
	public static final String
		ACCOUNT_TYPE = "com.android.email",
		ACCOUNT_TYPE_ICLOUD = "bitfire.at.davdroid",
		WEB_URL_MAIN = "https://davdroid.bitfire.at/?pk_campaign=davdroid-app",
		WEB_URL_HELP = "https://davdroid.bitfire.at/configuration?pk_campaign=davdroid-app",

		ACCOUNT_KEY_USERNAME = "user_name",
		ACCOUNT_KEY_PASSWORD = "password",
		ACCOUNT_KEY_AUTH_PREEMPTIVE = "auth_preemptive",
		ACCOUNT_SERVER = "account_server";
	public static final String ACCOUNT_KEY_REFRESH_TOKEN = "google_refresh_token";
	public static final String ACCOUNT_KEY_ACCESS_TOKEN = "google_access_token";
	public static final String ACCOUNT_KEY_ACCOUNT_NAME = "account_name";
	public static final String ACCOUNT_KEY_ACCOUNT_TYPE = "account_type";
	public static final String KEY_SERVER_INFO = "server_info";
	public static final String EXTRA_IS_DEVICE_SETUP = "isDeviceSetup";

    public static final Logger log = LoggerFactory.getLogger("davdroid");

    // notification IDs
    public final static int
            NOTIFICATION_ANDROID_VERSION_UPDATED = 0,
            NOTIFICATION_ACCOUNT_SETTINGS_UPDATED = 1,
            NOTIFICATION_CONTACTS_SYNC = 10,
            NOTIFICATION_CALENDAR_SYNC = 11,
            NOTIFICATION_TASK_SYNC = 12;

    public final static Uri donationUri = Uri.parse("https://davdroid.bitfire.at/donate?pk_campaign=davdroid-app");
	public static final String ACCOUNT_KEY_SETTINGS_VERSION = "version";
}
