/*******************************************************************************
 * Copyright (c) 2014 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package at.bitfire.davdroid.syncadapter;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.View;
import android.widget.TextView;
import at.bitfire.davdroid.R;

public class GeneralSettingsActivity extends Activity {
	final static String URL_REPORT_ISSUE = "https://github.com/rfc2822/davdroid/blob/master/CONTRIBUTING.md"; 
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.pref_settings);
        View quickFrame = findViewById(R.id.quick_frame);
        quickFrame.setBackgroundColor(getApplicationColor());

        TextView title = (TextView) findViewById(R.id.quick_title);
        title.setText(getTitle());

        quickFrame.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
                overridePendingTransition(
                        android.R.anim.quick_exit_in,
                        android.R.anim.quick_exit_out);
            }
        });
		
		getFragmentManager().beginTransaction()
			.add(R.id.fragments, new GeneralSettingsFragment())
        	.commit();
	}
	
	
	public static class GeneralSettingsFragment extends PreferenceFragment {
	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        
			getPreferenceManager().setSharedPreferencesMode(Context.MODE_MULTI_PROCESS);
	        addPreferencesFromResource(R.xml.general_settings);
	        
	        setHasOptionsMenu(false);
	    }

	}
}
