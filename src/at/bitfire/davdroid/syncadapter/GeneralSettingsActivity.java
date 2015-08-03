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
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import at.bitfire.davdroid.R;

import aneeda.content.ContextHelper;

public class GeneralSettingsActivity extends Activity {
	final static String URL_REPORT_ISSUE = "https://github.com/rfc2822/davdroid/blob/master/CONTRIBUTING.md"; 
	private static final String KEY_TITLE = "title";
	@Override
	public void onCreate(Bundle savedInstanceState) {
        
        requestWindowFeature(Window.FEATURE_ANEEDA_ACTION_BAR);
		super.onCreate(savedInstanceState);
        setContentView(R.layout.pref_settings);
        View quickFrame = findViewById(R.id.quick_frame);
        quickFrame.setBackgroundColor(ContextHelper.getApplicationColor(this));
		
		getFragmentManager().beginTransaction()
			.add(R.id.fragments, new GeneralSettingsFragment())
        	.commit();
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}
	
	
	public static class GeneralSettingsFragment extends PreferenceFragment {
	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        /*getActivity().overridePendingTransition(android.R.anim.quick_enter_in,
	                android.R.anim.quick_enter_out);*/
	        super.onCreate(savedInstanceState);
	        
			getPreferenceManager().setSharedPreferencesMode(Context.MODE_MULTI_PROCESS);
	        addPreferencesFromResource(R.xml.general_settings);
	        
	        setHasOptionsMenu(false);
	    }

	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            setResult(RESULT_CANCELED);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
