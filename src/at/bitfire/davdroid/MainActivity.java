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
package at.bitfire.davdroid;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SlidingFrameLayout;
import android.widget.SlidingLayer;

public class MainActivity extends Activity {

	SlidingLayer mSlidingLayer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ListView listView;

		setContentView(R.layout.activity_main);

		listView = (ListView) findViewById(R.id.main_activity_list);

		setTitle("DAVdroid " + Constants.APP_VERSION);

		String[] values = new String[] { "Add Account", 
				"Manage accounts",
				"Website"
		};

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				R.layout.activity_main_item, android.R.id.text1, values);

		listView.setAdapter(adapter); 

		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				Intent intent = null;
	
				switch(position) {
				case 0:
					intent = new Intent(Settings.ACTION_ADD_ACCOUNT);
					break;
				case 1:
					intent = new Intent(Settings.ACTION_SYNC_SETTINGS);
					break;
				case 3:
					intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse(Constants.WEB_URL_HELP + "&pk_kwd=main-activity"));
					break;
				default:
					//error
				}
				
				if (intent != null )
					startActivity(intent);

			}

		});

		mSlidingLayer = (SlidingLayer)findViewById(R.id.slidinglayout_main);
		mSlidingLayer.setPositiveText(" ");
		mSlidingLayer.setPositiveButtonVisibility(View.INVISIBLE);
		mSlidingLayer.setNegativeText(getResources().getString(R.string.message_cancel_text));
		mSlidingLayer.setOnInteractListener(new SlidingFrameLayout.OnInteractListener(){
			@Override
			public void onPositiveAction(){
			}
			@Override
			public void onNegativeAction(){
				//Cancel
				finish();
			}
			@Override
			public boolean onPositiveActionStart() {
				return false;
			}

			@Override
			public boolean onNegativeActionStart() {
				return false;
			}

		});
	}

}
