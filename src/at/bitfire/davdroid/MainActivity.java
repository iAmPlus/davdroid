/*******************************************************************************
 * Copyright (c) 2014 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package at.bitfire.davdroid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.iamplus.aware.AwareSlidingLayout;
import at.bitfire.davdroid.syncadapter.AddAccountActivity;
import android.widget.TextView;

public class MainActivity extends Activity {

	AwareSlidingLayout mSlidingLayer = null;
	Context mContext;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ListView listView;

		mContext = this.getApplicationContext();

		setContentView(R.layout.activity_main);

		listView = (ListView) findViewById(R.id.main_activity_list);

		setTitle( this.getString(R.string.app_title) + " " + Constants.APP_VERSION);

		String[] values = getResources().getStringArray(R.array.main_activity_actions);

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
					intent = new Intent(mContext, AddAccountActivity.class);
					break;
				case 1:
					intent = new Intent(Settings.ACTION_SYNC_SETTINGS);
					break;
				default:
					//error
				}

				if (intent != null ) {
					startActivity(intent);
				}

			}

		});

		mSlidingLayer = (AwareSlidingLayout)findViewById(R.id.slidinglayout_main);
		mSlidingLayer.setOnActionListener(new AwareSlidingLayout.OnActionListener(){
			@Override
			public void onAction(int type){
				if(type == AwareSlidingLayout.NEGATIVE) {
					finish();
				}
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if(mSlidingLayer != null)
			mSlidingLayer.reset();
	}

}
