/*******************************************************************************
 * Copyright (c) 2014 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package at.bitfire.davdroid.syncadapter;

import lombok.Getter;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ListAdapter;
import at.bitfire.davdroid.R;
import at.bitfire.davdroid.resource.ServerInfo;

public class SelectCollectionsAdapter extends BaseAdapter implements ListAdapter {
	final static int TYPE_ADDRESS_BOOKS_HEADING = 0,
		TYPE_ADDRESS_BOOKS_ROW = 1,
		TYPE_CALENDARS_HEADING = 2,
		TYPE_CALENDARS_ROW = 3;
	
	protected Context context;
	protected ServerInfo serverInfo;
	@Getter protected int nAddressBooks, nCalendars;
	
	
	public SelectCollectionsAdapter(Context context, ServerInfo serverInfo) {
		this.context = context;
		
		this.serverInfo = serverInfo;
		nAddressBooks = (serverInfo.getAddressBooks() == null) ? 0 : serverInfo.getAddressBooks().size();
		nCalendars = (serverInfo.getCalendars() == null) ? 0 : serverInfo.getCalendars().size();
	}
	
	
	// item data
	
	@Override
	public int getCount() {
		return nAddressBooks + nCalendars;
	}

	@Override
	public Object getItem(int position) {
		if (position >= 0 && position < nAddressBooks)
			return serverInfo.getAddressBooks().get(position);
		else if (position >= nAddressBooks)
			return serverInfo.getCalendars().get(position - nAddressBooks);
		return null;
	}
	
	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	
	// item views

	@Override
	public int getViewTypeCount() {
		return 4;
	}

	@Override
	public int getItemViewType(int position) {
		/*if (position == 0)
			return TYPE_ADDRESS_BOOKS_HEADING;
		else*/ if (position < nAddressBooks)
			return TYPE_ADDRESS_BOOKS_ROW;
		/*else if (position == nAddressBooks + 1)
			return TYPE_CALENDARS_HEADING;*/
		else if (position < nAddressBooks + nCalendars)
			return TYPE_CALENDARS_ROW;
		else
			return IGNORE_ITEM_VIEW_TYPE;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// step 1: get view (either by creating or recycling)
		LayoutInflater inflater = LayoutInflater.from(parent.getContext());
		if (convertView == null) {
			switch (getItemViewType(position)) {
			case TYPE_ADDRESS_BOOKS_HEADING:
				convertView = inflater.inflate(R.layout.address_books_heading, parent, false);
				break;
			case TYPE_CALENDARS_HEADING:
				convertView = inflater.inflate(R.layout.calendars_heading, parent, false);
			case TYPE_ADDRESS_BOOKS_ROW:
				convertView = inflater.inflate(R.layout.simple_list_item_single_choice, parent, false);
			case TYPE_CALENDARS_ROW:
				convertView = inflater.inflate(R.layout.simple_list_item_multiple_choice, parent, false);
			}
		}
		
		// step 2: fill view with content
		switch (getItemViewType(position)) {
		case TYPE_ADDRESS_BOOKS_ROW:
			setContent((CheckedTextView)convertView, R.drawable.btn_radio, (ServerInfo.ResourceInfo)getItem(position), TYPE_ADDRESS_BOOKS_ROW);
			break;
		case TYPE_CALENDARS_ROW:
			setContent((CheckedTextView)convertView, R.drawable.btn_check, (ServerInfo.ResourceInfo)getItem(position), TYPE_CALENDARS_ROW);
		}
		
		return convertView;
	}
	
	protected void setContent(CheckedTextView view, int collectionIcon, ServerInfo.ResourceInfo info, int type) {
		// set layout and icons
		view.setCheckMarkDrawable(collectionIcon);
		String title = info.getTitle();
		if (title == null ) {
			if (type == TYPE_ADDRESS_BOOKS_ROW) {
				title = "Address Book";
			} else {
				title = "Calendar";
			}
		}
		view.setText(title);
	}

	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@Override
	public boolean isEnabled(int position) {
		int type = getItemViewType(position);
		return (type == TYPE_ADDRESS_BOOKS_ROW || type == TYPE_CALENDARS_ROW);
	}
}
