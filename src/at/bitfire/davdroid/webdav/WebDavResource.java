/*******************************************************************************
 * Copyright (c) 2014 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package at.bitfire.davdroid.webdav;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import lombok.Cleanup;
import lombok.Getter;
import lombok.ToString;

import org.apache.commons.lang.StringUtils;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import android.util.Log;
import at.bitfire.davdroid.URIUtils;
import at.bitfire.davdroid.resource.Event;
import at.bitfire.davdroid.webdav.DavProp.DavPropComp;
import ch.boye.httpclientandroidlib.Header;
import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpHost;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.HttpStatus;
import ch.boye.httpclientandroidlib.StatusLine;
import ch.boye.httpclientandroidlib.auth.AuthScope;
import ch.boye.httpclientandroidlib.auth.UsernamePasswordCredentials;
import ch.boye.httpclientandroidlib.client.AuthCache;
import ch.boye.httpclientandroidlib.client.methods.CloseableHttpResponse;
import ch.boye.httpclientandroidlib.client.methods.HttpDelete;
import ch.boye.httpclientandroidlib.client.methods.HttpGet;
import ch.boye.httpclientandroidlib.client.methods.HttpOptions;
import ch.boye.httpclientandroidlib.client.methods.HttpPut;
import ch.boye.httpclientandroidlib.client.protocol.HttpClientContext;
import ch.boye.httpclientandroidlib.entity.ByteArrayEntity;
import ch.boye.httpclientandroidlib.impl.auth.BasicScheme;
import ch.boye.httpclientandroidlib.impl.client.BasicAuthCache;
import ch.boye.httpclientandroidlib.impl.client.BasicCredentialsProvider;
import ch.boye.httpclientandroidlib.impl.client.CloseableHttpClient;
import ch.boye.httpclientandroidlib.message.BasicLineParser;
import ch.boye.httpclientandroidlib.util.EntityUtils;


/**
 * Represents a WebDAV resource (file or collection).
 * This class is used for all CalDAV/CardDAV communcation.
 */
@ToString
public class WebDavResource {
	private static final String TAG = "davdroid.WebDavResource";
	
	public enum Property {
		CURRENT_USER_PRINCIPAL,
		READ_ONLY,
		DISPLAY_NAME, DESCRIPTION, COLOR,
		TIMEZONE, SUPPORTED_COMPONENTS,
		ADDRESSBOOK_HOMESET, CALENDAR_HOMESET,
		IS_ADDRESSBOOK, IS_CALENDAR,
		CTAG, ETAG,
		CONTENT_TYPE
	}
	public enum PutMode {
		ADD_DONT_OVERWRITE,
		UPDATE_DONT_OVERWRITE
	}

	// location of this resource
	@Getter protected URI location;
	
	// DAV capabilities (DAV: header) and allowed DAV methods (set for OPTIONS request)
	protected Set<String>	capabilities = new HashSet<String>(),
							methods = new HashSet<String>();
	
	// DAV properties
	protected HashMap<Property, String> properties = new HashMap<Property, String>();
	@Getter protected List<String> supportedComponents;
	
	// list of members (only for collections)
	@Getter protected List<WebDavResource> members;

	// content (available after GET)
	@Getter protected byte[] content;

	protected CloseableHttpClient httpClient;
	protected HttpClientContext context;
	private String authBearer = null;
	
	
	public WebDavResource(CloseableHttpClient httpClient, URI baseURL) throws URISyntaxException {
		this.httpClient = httpClient;
		location = baseURL.normalize();
		
		context = HttpClientContext.create();
		context.setCredentialsProvider(new BasicCredentialsProvider());
	}
	
	public WebDavResource(CloseableHttpClient httpClient, URI baseURL, String username, String password, boolean preemptive) throws URISyntaxException {
		this(httpClient, baseURL);
		
		HttpHost host = new HttpHost(baseURL.getHost(), baseURL.getPort(), baseURL.getScheme());
		context.getCredentialsProvider().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
		
		if (preemptive) {
			Log.d(TAG, "Using preemptive authentication (not compatible with Digest auth)");
			AuthCache authCache = context.getAuthCache();
			if (authCache == null)
				authCache = new BasicAuthCache();
			authCache.put(host, new BasicScheme());
			context.setAuthCache(authCache);
		}
	}
	
	WebDavResource(WebDavResource parent) {		// copy constructor: based on existing WebDavResource, reuse settings
		httpClient = parent.httpClient;
		context = parent.context;
		location = parent.location;
		authBearer = parent.authBearer;
	}

	protected WebDavResource(WebDavResource parent, URI uri) {
		this(parent);
		location = uri;
	}
	
	public WebDavResource(WebDavResource parent, String member) {
		this(parent);
		location = parent.location.resolve(URIUtils.sanitize(member));
	}
	
	public WebDavResource(WebDavResource parent, String member, String ETag) {
		this(parent, member);
		properties.put(Property.ETAG, ETag);
	}

	public WebDavResource(CloseableHttpClient httpClient, URI baseURL, String accessToken) throws URISyntaxException {
		this(httpClient, baseURL);
		authBearer = accessToken;
	}
	
	/* feature detection */

	public void options() throws IOException, HttpException {
		HttpOptions options = new HttpOptions(location);
		if(authBearer != null)
			options.addHeader("Authorization", authBearer);

		CloseableHttpResponse response = httpClient.execute(options, context);
		try {
			checkResponse(response);
			
			Header[] allowHeaders = response.getHeaders("Allow");
			for (Header allowHeader : allowHeaders)
				methods.addAll(Arrays.asList(allowHeader.getValue().split(", ?")));
	
			Header[] capHeaders = response.getHeaders("DAV");
			for (Header capHeader : capHeaders)
				capabilities.addAll(Arrays.asList(capHeader.getValue().split(", ?")));
		} finally {
			response.close();
		}
	}

	public boolean supportsDAV(String capability) {
		return capabilities.contains(capability);
	}

	public boolean supportsMethod(String method) {
		return methods.contains(method);
	}
	
	
	/* file hierarchy methods */
	
	public String getName() {
		String[] names = StringUtils.split(location.getRawPath(), "/");
		return names[names.length - 1];
	}
	
	
	/* property methods */
	
	public String getCurrentUserPrincipal() {
		return properties.get(Property.CURRENT_USER_PRINCIPAL);
	}
	
	public boolean isReadOnly() {
		return properties.containsKey(Property.READ_ONLY);
	}
	
	public String getDisplayName() {
		return properties.get(Property.DISPLAY_NAME);
	}
	
	public String getDescription() {
		return properties.get(Property.DESCRIPTION);
	}
	
	public String getColor() {
		return properties.get(Property.COLOR);
	}
	
	public String getTimezone() {
		return properties.get(Property.TIMEZONE);
	}
	
	public String getAddressbookHomeSet() {
		return properties.get(Property.ADDRESSBOOK_HOMESET);
	}
	
	public String getCalendarHomeSet() {
		return properties.get(Property.CALENDAR_HOMESET);
	}

	public String getCTag() {
		return properties.get(Property.CTAG);
	}
	public void invalidateCTag() {
		properties.remove(Property.CTAG);
	}
	
	public String getETag() {
		return properties.get(Property.ETAG);
	}
	
	public String getContentType() {
		return properties.get(Property.CONTENT_TYPE);
	}
	
	public void setContentType(String mimeType) {
		properties.put(Property.CONTENT_TYPE, mimeType);
	}
	
	public boolean isAddressBook() {
		return properties.containsKey(Property.IS_ADDRESSBOOK);
	}
	
	public boolean isCalendar() {
		return properties.containsKey(Property.IS_CALENDAR);
	}
	
	
	/* collection operations */
	
	public void propfind(HttpPropfind.Mode mode) throws IOException, DavException, HttpException {
		CloseableHttpResponse response = null;
		
		// processMultiStatus() requires knowledge of the actual content location,
		// so we have to handle redirections manually and create a new request for the new location
		for (int i = context.getRequestConfig().getMaxRedirects(); i > 0; i--) {
			HttpPropfind propfind = new HttpPropfind(location, mode);
			if(authBearer != null)
				propfind.addHeader("Authorization", authBearer);
			response = httpClient.execute(propfind, context);
			
			if (response.getStatusLine().getStatusCode()/100 == 3) {
				location = DavRedirectStrategy.getLocation(propfind, response, context);
				Log.i(TAG, "Redirection on PROPFIND; trying again at new content URL: " + location);
				// don't forget to throw away the unneeded response content
				HttpEntity entity = response.getEntity();
				if (entity != null) { @Cleanup InputStream content = entity.getContent(); }
			} else
				break;		// answer was NOT a redirection, continue
		}
		if (response == null)
			throw new DavNoContentException();
		
		try {
			checkResponse(response);		// will also handle Content-Location
			processMultiStatus(response);
		} finally {
			response.close();
		}
	}

	public void multiGet(DavMultiget.Type type, String[] names) throws IOException, DavException, HttpException {
		CloseableHttpResponse response = null;
		
		// processMultiStatus() requires knowledge of the actual content location,
		// so we have to handle redirections manually and create a new request for the new location
		for (int i = context.getRequestConfig().getMaxRedirects(); i > 0; i--) {
			// build multi-get XML request 
			List<String> hrefs = new LinkedList<String>();
			for (String name : names)
				hrefs.add(location.resolve(name).getRawPath());
			DavMultiget multiget = DavMultiget.newRequest(type, hrefs.toArray(new String[0]));
			
			StringWriter writer = new StringWriter();
			try {
				Serializer serializer = new Persister();
				serializer.write(multiget, writer);
			} catch (Exception ex) {
				Log.e(TAG, "Couldn't create XML multi-get request", ex);
				throw new DavException("Couldn't create multi-get request");
			}
	
			// submit REPORT request
			HttpReport report = new HttpReport(location, writer.toString());
			if(authBearer != null)
				report.addHeader("Authorization", authBearer);
			response = httpClient.execute(report, context);
			
			if (response.getStatusLine().getStatusCode()/100 == 3) {
				location = DavRedirectStrategy.getLocation(report, response, context);
				Log.i(TAG, "Redirection on REPORT multi-get; trying again at new content URL: " + location);
				
				// don't forget to throw away the unneeded response content
				HttpEntity entity = response.getEntity();
				if (entity != null) { @Cleanup InputStream content = entity.getContent(); }
			} else
				break;		// answer was NOT a redirection, continue
		}
		if (response == null)
			throw new DavNoContentException();
		
		try {
			checkResponse(response);		// will also handle Content-Location
			processMultiStatus(response);
		} finally {
			response.close();
		}
	}

	
	/* resource operations */
	
	public void get() throws IOException, HttpException, DavException {
		HttpGet get = new HttpGet(location);
		if(authBearer != null)
			get.addHeader("Authorization", authBearer);
		CloseableHttpResponse response = httpClient.execute(get, context);
		try {
			checkResponse(response);
			
			HttpEntity entity = response.getEntity();
			if (entity == null)
				throw new DavNoContentException();
			
			content = EntityUtils.toByteArray(entity);
		} finally {
			response.close();
		}
	}
	
	// returns the ETag of the created/updated resource, if available (null otherwise)
	public String put(byte[] data, PutMode mode) throws IOException, HttpException {
		HttpPut put = new HttpPut(location);
		put.setEntity(new ByteArrayEntity(data));

		switch (mode) {
		case ADD_DONT_OVERWRITE:
			put.addHeader("If-None-Match", "*");
			break;
		case UPDATE_DONT_OVERWRITE:
			put.addHeader("If-Match", (getETag() != null) ? getETag() : "*");
			break;
		}
		
		if (getContentType() != null)
			put.addHeader("Content-Type", getContentType());
		if(authBearer != null)
			put.addHeader("Authorization", authBearer);

		CloseableHttpResponse response = httpClient.execute(put, context);
		try {
			checkResponse(response);

			Header eTag = response.getLastHeader("ETag");
			if (eTag != null)
				return eTag.getValue();
		} finally {
			response.close();
		}
		
		return null;
	}
	
	public void delete() throws IOException, HttpException {
		HttpDelete delete = new HttpDelete(location);
		
		if (getETag() != null)
			delete.addHeader("If-Match", getETag());
		
		if(authBearer != null)
			delete.addHeader("Authorization", authBearer);
		CloseableHttpResponse response = httpClient.execute(delete, context);
		try {
			checkResponse(response);
		} finally {
			response.close();
		}
	}
	

	/* helpers */
	
	protected void checkResponse(HttpResponse response) throws HttpException {
		checkResponse(response.getStatusLine());
		
		// handle Content-Location header (see RFC 4918 5.2 Collection Resources)
		Header contentLocationHdr = response.getFirstHeader("Content-Location");
		if (contentLocationHdr != null)
			try {
				// Content-Location was set, update location correspondingly
				location = location.resolve(new URI(contentLocationHdr.getValue()));
				Log.d(TAG, "Set Content-Location to " + location);
			} catch (URISyntaxException e) {
				Log.w(TAG, "Ignoring invalid Content-Location", e);
			}
	}
	
	protected static void checkResponse(StatusLine statusLine) throws HttpException {
		int code = statusLine.getStatusCode();
		
		if (code/100 == 1 || code/100 == 2)		// everything OK
			return;
		
		String reason = code + " " + statusLine.getReasonPhrase();
		switch (code) {
		case HttpStatus.SC_NOT_FOUND:
			throw new NotFoundException(reason);
		case HttpStatus.SC_PRECONDITION_FAILED:
			throw new PreconditionFailedException(reason);
		default:
			throw new HttpException(code, reason);
		}
	}
	
	protected void processMultiStatus(HttpResponse response) throws IOException, HttpException, DavException {
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_MULTI_STATUS)
			throw new DavNoMultiStatusException();
		
		HttpEntity entity = response.getEntity();
		if (entity == null)
			throw new DavNoContentException();
		@Cleanup InputStream content = entity.getContent();
		
		DavMultistatus multiStatus;
		try {
			Serializer serializer = new Persister();
			multiStatus = serializer.read(DavMultistatus.class, content, false);
		} catch (Exception ex) {
			throw new DavException("Couldn't parse Multi-Status response on REPORT multi-get", ex);
		}

		if (multiStatus.response == null)	// empty response
			throw new DavNoContentException();
		
		// member list will be built from response
		List<WebDavResource> members = new LinkedList<WebDavResource>();
		
		for (DavResponse singleResponse : multiStatus.response) {
			URI href;
			try {
				href = location.resolve(URIUtils.sanitize(singleResponse.getHref().href));
			} catch(IllegalArgumentException ex) {
				Log.w(TAG, "Ignoring illegal member URI in multi-status response", ex);
				continue;
			}
			Log.d(TAG, "Processing multi-status element: " + href);
			
			// about which resource is this response?
			WebDavResource referenced = null;
			
			// "this" resource is either at "location" …
			if (location.equals(href)) {	// -> ourselves
				referenced = this;
			} else {
				// … or at location + "/" (in case of a collection where the server has implicitly appended the trailing slash)
				if (!location.getRawPath().endsWith("/"))	// this is only possible if location doesn't have a trailing slash
					try {
						URI locationAsCollection = new URI(location.getScheme(), location.getAuthority(), location.getPath() + "/", location.getQuery(), null);
						if (locationAsCollection.equals(href)) {
							Log.d(TAG, "Server implicitly appended trailing slash to " + locationAsCollection);
							referenced = this;
						}
					} catch (URISyntaxException e) {
						Log.wtf(TAG, "Couldn't understand our own URI", e);
					}
				
				// otherwise, the referenced resource is a member
				if (referenced == null) {
					referenced = new WebDavResource(this, href);
					members.add(referenced);
				}
			}
			
			for (DavPropstat singlePropstat : singleResponse.getPropstat()) {
				StatusLine status = BasicLineParser.parseStatusLine(singlePropstat.status, new BasicLineParser());
				
				// ignore information about missing properties etc.
				if (status.getStatusCode()/100 != 1 && status.getStatusCode()/100 != 2)
					continue;
				
				DavProp prop = singlePropstat.prop;
				properties = referenced.properties;

				if (prop.currentUserPrincipal != null && prop.currentUserPrincipal.getHref() != null)
					properties.put(Property.CURRENT_USER_PRINCIPAL, prop.currentUserPrincipal.getHref().href);
				
				if (prop.currentUserPrivilegeSet != null) {
					// privilege info available
					boolean mayAll = false,
							mayBind = false,
							mayUnbind = false,
							mayWrite = false,
							mayWriteContent = false;
					for (DavProp.DavPropPrivilege privilege : prop.currentUserPrivilegeSet) {
						if (privilege.getAll() != null) mayAll = true;
						if (privilege.getBind() != null) mayBind = true;
						if (privilege.getUnbind() != null) mayUnbind = true;
						if (privilege.getWrite() != null) mayWrite = true;
						if (privilege.getWriteContent() != null) mayWriteContent = true;
					}
					if (!mayAll && !mayWrite && !(mayWriteContent && mayBind && mayUnbind))
						properties.put(Property.READ_ONLY, "1");
				}
				
				if (prop.addressbookHomeSet != null && prop.addressbookHomeSet.getHref() != null)
					properties.put(Property.ADDRESSBOOK_HOMESET, prop.addressbookHomeSet.getHref().href);
				
				if (singlePropstat.prop.calendarHomeSet != null && prop.calendarHomeSet.getHref() != null)
					properties.put(Property.CALENDAR_HOMESET, prop.calendarHomeSet.getHref().href);
				
				if (prop.displayname != null)
					properties.put(Property.DISPLAY_NAME, prop.displayname.getDisplayName());
				
				if (prop.resourcetype != null) {
					if (prop.resourcetype.getAddressbook() != null) {
						properties.put(Property.IS_ADDRESSBOOK, "1");
						
						if (prop.addressbookDescription != null)
							properties.put(Property.DESCRIPTION, prop.addressbookDescription.getDescription());
					}
					if (prop.resourcetype.getCalendar() != null) {
						properties.put(Property.IS_CALENDAR, "1");
						
						if (prop.calendarDescription != null)
							properties.put(Property.DESCRIPTION, prop.calendarDescription.getDescription());
						
						if (prop.calendarColor != null)
							properties.put(Property.COLOR, prop.calendarColor.getColor());
						
						if (prop.calendarTimezone != null)
							properties.put(Property.TIMEZONE, Event.TimezoneDefToTzId(prop.calendarTimezone.getTimezone()));
						
						if (prop.supportedCalendarComponentSet != null) {
							referenced.supportedComponents = new LinkedList<String>();
							for (DavPropComp component : prop.supportedCalendarComponentSet)
								referenced.supportedComponents.add(component.getName());
						}
					}
				}
				
				if (prop.getctag != null)
					properties.put(Property.CTAG, prop.getctag.getCTag());

				if (prop.getetag != null)
					properties.put(Property.ETAG, prop.getetag.getETag());
				
				if (prop.calendarData != null && prop.calendarData.ical != null)
					referenced.content = prop.calendarData.ical.getBytes();
				else if (prop.addressData != null && prop.addressData.vcard != null)
					referenced.content = prop.addressData.vcard.getBytes();
			}
		}
		
		this.members = members;
	}

}
