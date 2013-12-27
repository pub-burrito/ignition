/* Copyright (c) 2009-2011 Matthias Kaeppler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.ignition.support.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

import com.github.ignition.support.IgnitedStrings;
import com.github.ignition.support.cache.AbstractCache;
import com.github.ignition.support.http.cache.CachedHttpRequest;
import com.github.ignition.support.http.cache.HttpResponseCache;
import com.github.ignition.support.http.gzip.GzipHttpRequestInterceptor;
import com.github.ignition.support.http.gzip.GzipHttpResponseInterceptor;
//import android.Manifest;
//import android.content.Context;
//import android.content.IntentFilter;
//import android.net.ConnectivityManager;
//import android.net.NetworkInfo;
//import android.net.Proxy;
//import android.util.Log;

public class IgnitedHttp {

    static final String LOG_TAG = IgnitedHttp.class.getSimpleName();

    public static final int DEFAULT_MAX_CONNECTIONS = 4;
    public static final int DEFAULT_SOCKET_TIMEOUT = 15 * 60 * 1000;
    public static final int DEFAULT_WAIT_FOR_CONNECTION_TIMEOUT = 30 * 1000;
    public static final String DEFAULT_HTTP_USER_AGENT = "Android/Ignition";

    public static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
    public static final String ENCODING_GZIP = "gzip";

    public static final String CHARSET = HTTP.UTF_8;
    
    private HashMap<String, String> defaultHeaders = new HashMap<String, String>();
    private AbstractHttpClient httpClient;

    private HttpResponseCache responseCache;

    public IgnitedHttp() {
        setupHttpClient();
    }

    protected void setupHttpClient() {
        BasicHttpParams httpParams = new BasicHttpParams();

        ConnManagerParams.setTimeout(httpParams, DEFAULT_WAIT_FOR_CONNECTION_TIMEOUT);
        ConnManagerParams.setMaxConnectionsPerRoute(httpParams, new ConnPerRouteBean(
                DEFAULT_MAX_CONNECTIONS));
        ConnManagerParams.setMaxTotalConnections(httpParams, DEFAULT_MAX_CONNECTIONS);
        HttpConnectionParams.setSoTimeout(httpParams, DEFAULT_SOCKET_TIMEOUT);
        HttpConnectionParams.setTcpNoDelay(httpParams, true);
        HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setUserAgent(httpParams, DEFAULT_HTTP_USER_AGENT);

        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));

        ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(httpParams, schemeRegistry);
        httpClient = new DefaultHttpClient(cm, httpParams);
    }

//    /**
//     * Registers a broadcast receiver with the application context that will take care of updating
//     * proxy settings when failing over between 3G and Wi-Fi. <b>This requires the
//     * {@link Manifest.permission#ACCESS_NETWORK_STATE} permission</b>.
//     * 
//     * @param context
//     *            the context used to retrieve the app context
//     */
//    public void listenForConnectivityChanges(Context context) {
//        context.getApplicationContext().registerReceiver(
//                new ConnectionChangedBroadcastReceiver(this),
//                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
//    }

    public void setGzipEncodingEnabled(boolean enabled) {
        if (enabled) {
            httpClient.addRequestInterceptor(new GzipHttpRequestInterceptor());
            httpClient.addResponseInterceptor(new GzipHttpResponseInterceptor());
        } else {
            httpClient.removeRequestInterceptorByClass(GzipHttpRequestInterceptor.class);
            httpClient.removeResponseInterceptorByClass(GzipHttpResponseInterceptor.class);
        }
    }

    /**
     * Enables caching of HTTP responses. This will only enable the in-memory cache. If you also
     * want to enable the disk cache, see {@link #enableResponseCache(int, long, int, int)}
     * .
     * 
     * @param initialCapacity
     *            the initial element size of the cache
     * @param expirationInMinutes
     *            time in minutes after which elements will be purged from the cache
     * @param maxConcurrentThreads
     *            how many threads you think may at once access the cache; this need not be an exact
     *            number, but it helps in fragmenting the cache properly
     * @see HttpResponseCache
     */
    public void enableResponseCache(int initialCapacity, long expirationInMinutes,
            int maxConcurrentThreads) {
        responseCache = new HttpResponseCache(initialCapacity, expirationInMinutes,
                maxConcurrentThreads);
    }

    /**
     * Enables caching of HTTP responses. This will also enable the disk cache.
	 *
     * @param initialCapacity
     *            the initial element size of the cache
     * @param expirationInMinutes
     *            time in minutes after which elements will be purged from the cache (NOTE: this
     *            only affects the memory cache, the disk cache does currently NOT handle element
     *            TTLs!)
     * @param maxConcurrentThreads
     *            how many threads you think may at once access the cache; this need not be an exact
     *            number, but it helps in fragmenting the cache properly
     * @param diskCacheStorageDevice
     *            where files should be cached persistently (
     *            {@link AbstractCache#DISK_CACHE_INTERNAL}, {@link AbstractCache#DISK_CACHE_SDCARD}
     *            )
     * @see HttpResponseCache
     */
    public void enableResponseCache(String rootDir, int initialCapacity, long expirationInMinutes,
            int maxConcurrentThreads ) {
        enableResponseCache(initialCapacity, expirationInMinutes, maxConcurrentThreads);
        responseCache.enableDiskCache( rootDir);
    }

    /**
     * Disables caching of HTTP responses. You may also choose to wipe any files that may have been
     * written to disk.
     */
    public void disableResponseCache(boolean wipe) {
        if (responseCache != null && wipe) {
            responseCache.clear();
        }
        responseCache = null;
    }

    /**
     * @return the response cache, if enabled, otherwise null
     */
    public synchronized HttpResponseCache getResponseCache() {
        return responseCache;
    }

    public void setHttpClient(AbstractHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public AbstractHttpClient getHttpClient() {
        return httpClient;
    }

//    /**
//     * Updates the underlying HTTP client's proxy settings with what the user has entered in the APN
//     * settings. This will be called automatically if {@link #listenForConnectivityChanges(Context)}
//     * has been called. <b>This requires the {@link Manifest.permission#ACCESS_NETWORK_STATE}
//     * permission</b>.
//     * 
//     * @param context
//     *            the current context
//     */
//    public void updateProxySettings(Context context) {
//        if (context == null) {
//            return;
//        }
//        HttpParams httpParams = httpClient.getParams();
//        ConnectivityManager connectivity = (ConnectivityManager) context
//                .getSystemService(Context.CONNECTIVITY_SERVICE);
//        NetworkInfo nwInfo = connectivity.getActiveNetworkInfo();
//        if (nwInfo == null) {
//            return;
//        }
//        Log.i(LOG_TAG, nwInfo.toString());
//        if (nwInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
//            String proxyHost = Proxy.getHost(context);
//            if (proxyHost == null) {
//                proxyHost = Proxy.getDefaultHost();
//            }
//            int proxyPort = Proxy.getPort(context);
//            if (proxyPort == -1) {
//                proxyPort = Proxy.getDefaultPort();
//            }
//            if (proxyHost != null && proxyPort > -1) {
//                HttpHost proxy = new HttpHost(proxyHost, proxyPort);
//                httpParams.setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
//            } else {
//                httpParams.setParameter(ConnRoutePNames.DEFAULT_PROXY, null);
//            }
//        } else {
//            httpParams.setParameter(ConnRoutePNames.DEFAULT_PROXY, null);
//        }
//    }

    public IgnitedHttpRequest get(String url) {
        return get(url, false);
    }

    public IgnitedHttpRequest get(String url, boolean cached) {
        if (cached && responseCache != null && responseCache.containsKey(url)) {
            return new CachedHttpRequest(responseCache, url);
        }
        return new HttpGet(this, url, defaultHeaders);
    }

    public IgnitedHttpRequest post(String url) {
        return new HttpPost(this, url, defaultHeaders);
    }

    public IgnitedHttpRequest post(String url, HttpEntity payload) {
        return new HttpPost(this, url, payload, defaultHeaders);
    }

    public IgnitedHttpRequest put(String url) {
        return new HttpPut(this, url, defaultHeaders);
    }

    public IgnitedHttpRequest put(String url, HttpEntity payload) {
        return new HttpPut(this, url, payload, defaultHeaders);
    }

    public IgnitedHttpRequest delete(String url) {
        return new HttpDelete(this, url, defaultHeaders);
    }
    
    /**
	 * Builds an HTTP request given the passed parameters.
	 * 
	 * @param method
	 * @param url
	 * @param params
	 * @param headers
	 * @param entity
	 * @param credentials
	 * 
	 * @return an {@link IgnitedHttpRequest} based on the method passed, initialized with all parameters, ready to execute.
	 * 
	 * @throws URISyntaxException
	 * @throws UnsupportedEncodingException
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	protected Map.Entry<IgnitedHttpRequest, URI> buildRequest( final HttpMethod method, final String url, Map<String, Object> params, Map<String, Object> headers, HttpEntity entity )
		throws URISyntaxException,
			UnsupportedEncodingException,
			InvalidKeyException,
			NoSuchAlgorithmException,
			IOException
	{
		
		if ( !method.bodyAllowed && entity != null )
		{
			throw new ClientProtocolException( "Sending an Entity/Body with an HTTP method that does not expect it is currently not supported." ); 
		}
		
		IgnitedHttpRequest request = null;
		
		URI uri = new URI( url );
		
		List<NameValuePair> parameters = params == null ? new ArrayList<NameValuePair>() : convertParameters( params );
		
		// append parameters to URI only if not using POST (in which case it will encode it into the body - unless receiving different body, ie: JSON)
		if ( !parameters.isEmpty() )
		{
			if ( !method.bodyAllowed || entity != null )
			{
				String encodedParams = IgnitedStrings.urlEncode( parameters );
				
				uri = URIUtils.createURI( uri.getScheme(), uri.getHost(), uri.getPort(), uri.getRawPath(), encodedParams, null );
			}
		}
		
		if ( method.bodyAllowed && entity == null )
		{
			entity = new UrlEncodedFormEntity( parameters, CHARSET );
		}
		
		switch ( method )
		{
			case GET:
				request = get( uri.toString() );
				
				break;
			
			case POST:
				request = post( uri.toString(), entity );
				
				break;
			
			case PUT:
				request = put( uri.toString(), entity );
				
				break;
			
			case DELETE:
				request = delete( uri.toString() );
				
				break;
			
			default:
				throw new UnsupportedOperationException( String.format( "HTTP Method not supported yet: %s", method ) );
		}
		
		// Headers
		HttpUriRequest rawRequest = request.unwrap();
				
		if ( entity != null )
		{
			Header contentTypeHeader = entity.getContentType();
			
			if ( !rawRequest.containsHeader( contentTypeHeader.getName() ) )
			{
				rawRequest.addHeader( contentTypeHeader );
			}
		}
		
		if ( headers != null )
		{
			for ( Map.Entry<String, Object> header : headers.entrySet() )
			{
				rawRequest.addHeader( header.getKey(), header.getValue().toString() );
			}
			
			//adding back headers that were generated to original map
			for ( Header header : rawRequest.getAllHeaders() )
			{
				if ( !headers.containsKey( header.getName() ) ) 
				{
					headers.put( header.getName(), header.getValue() );
				}
			}
		}
		
		return new SimpleEntry<IgnitedHttpRequest, URI>( request, uri );
	}
	
	public static List<NameValuePair> convertParameters( Map<String, Object> params ) throws UnsupportedEncodingException
	{
		List<NameValuePair> nvp = new ArrayList<NameValuePair>();
		
		for ( Map.Entry<String, Object> param : params.entrySet() )
		{
			String paramName = param.getKey().replace( "?", "" );
			Object paramValue = param.getValue();
			
			if ( paramValue != null &&  paramName != null && !paramName.isEmpty() )
			{
				if ( paramValue instanceof List )
				{
					for ( Object value : (List<?>) paramValue )
					{
						if ( value == null )
							continue;
						
						String listParamName = String.format( "%s[]", paramName );
						String listParamValue = value.toString();
						
						nvp.add( new BasicNameValuePair( listParamName, listParamValue ) );
					}
				}
				else if ( paramValue instanceof Map )
				{
					for ( Map.Entry<?, ?> value : ( (Map<?, ?>) paramValue ).entrySet() )
					{
						if ( value.getValue() == null )
							continue;
						
						String mapParamName = String.format( "%s[%s]", paramName, value.getKey() );
						String mapParamValue = value.getValue().toString();
						
						nvp.add( new BasicNameValuePair( mapParamName, mapParamValue ) );
					}
				}
				else
				{
					nvp.add( new BasicNameValuePair( paramName, paramValue.toString() ) );
				}
			}
		}
		
		return nvp;
	}

    public void setMaximumConnections(int maxConnections) {
        ConnManagerParams.setMaxTotalConnections(httpClient.getParams(), maxConnections);
    }

    /**
     * Adjust the connection timeout, i.e. the amount of time that may pass in order to establish a
     * connection with the server. Time unit is milliseconds.
     * 
     * @param connectionTimeout
     *            the timeout in milliseconds
     * @see CoreConnectionPNames#CONNECTION_TIMEOUT
     */
    public void setConnectionTimeout(int connectionTimeout) {
        ConnManagerParams.setTimeout(httpClient.getParams(), connectionTimeout);
    }

    /**
     * Adjust the socket timeout, i.e. the amount of time that may pass when waiting for data coming
     * in from the server. Time unit is milliseconds.
     * 
     * @param socketTimeout
     *            the timeout in milliseconds
     * @see CoreConnectionPNames#SO_TIMEOUT
     */
    public void setSocketTimeout(int socketTimeout) {
        HttpConnectionParams.setSoTimeout(httpClient.getParams(), socketTimeout);
    }

    public IgnitedHttp setDefaultHeader(String header, String value) {
        defaultHeaders.put(header, value);
        return this;
    }

    public HashMap<String, String> getDefaultHeaders() {
        return defaultHeaders;
    }

    public void setPortForScheme(String scheme, int port) {
        Scheme _scheme = new Scheme(scheme, PlainSocketFactory.getSocketFactory(), port);
        httpClient.getConnectionManager().getSchemeRegistry().register(_scheme);
    }
    
    /**
	 * Supported HTTP methods for calls, according to RFC 2616 and underlying HTTP client implementation, ie: Apache HTTP client.
	 * 
	 * http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html
	 */
	public static enum HttpMethod
	{
		//@formatter:off
		GET 	( false ),
		POST	( true ),
		PUT		( true ),
		DELETE	( false );
		//@formatter:on
		
		// The RFC doesn't prohibit a body to be sent although most server/client software do not support it for anything but POST/PUT, Apache HTTP client for instance enforces that convention.
		public boolean bodyAllowed;
		
		private HttpMethod( boolean bodyAllowed )
		{
			this.bodyAllowed = bodyAllowed;
		}
	}

}
