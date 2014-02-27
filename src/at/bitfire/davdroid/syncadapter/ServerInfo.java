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
package at.bitfire.davdroid.syncadapter;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(suppressConstructorProperties=true)
@Data
public class ServerInfo implements Serializable {
	private static final long serialVersionUID = 6744847358282980437L;
	
	final private String authURL = "https://accounts.google.com/o/oauth2/auth";
	final private String tokenURL = "https://accounts.google.com/o/oauth2/token";
	//final private String baseURL = "https://www.googleapis.com/.well-known/carddav";
	final private String baseURL = "https://apidata.googleusercontent.com/caldav/v2/";
	//final private String baseURL = "https://apidata.googleusercontent.com/.well-known/caldav/";
	final private String client_id = "240498934020.apps.googleusercontent.com'";
	final private String client_secret = "HuScmc9E5sIp-3epayh7g3ge";
	final private String redirect_uri = "http://localhost";
	final private String accountType;
	private String accountName;
	
	private String errorMessage;
	
	private boolean calDAV, cardDAV;
	private List<ResourceInfo>
		addressBooks = new LinkedList<ResourceInfo>(),
		calendars  = new LinkedList<ResourceInfo>();
	
	public boolean hasEnabledCalendars() {
		for (ResourceInfo calendar : calendars)
			if (calendar.enabled)
				return true;
		return false;
	}
	
	
	@RequiredArgsConstructor(suppressConstructorProperties=true)
	@Data
	public static class ResourceInfo implements Serializable {
		private static final long serialVersionUID = -5516934508229552112L;
		
		enum Type {
			ADDRESS_BOOK,
			CALENDAR
		}
		
		boolean enabled = false;
		
		final Type type;
		final String path, title, description, color;
		
		String timezone;
	}
}
