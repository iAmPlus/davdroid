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

public class MainActivity extends Activity {
	
	ListView listView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
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
                      
				  switch(position) {
				  case 0:
					  //Add account
					  addAccount();
					  break;
				  case 1:
					  //Manage account
					  showSyncSettings();
					  break;
				  case 3:
					  showWebsite();
					  //Website
					  break;
				  default:
					  //error
				  }

				
			}
        	
        });
	}

	
	public void addAccount() {
		Intent intent = new Intent(Settings.ACTION_ADD_ACCOUNT);
		startActivity(intent);
	}

	public void showSyncSettings() {
		Intent intent = new Intent(Settings.ACTION_SYNC_SETTINGS);
		startActivity(intent);
	}

	public void showWebsite() {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(Constants.WEB_URL_HELP + "&pk_kwd=main-activity"));
		startActivity(intent);
	}
}
