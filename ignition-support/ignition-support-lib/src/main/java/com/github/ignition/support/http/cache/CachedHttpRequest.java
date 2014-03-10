package com.github.ignition.support.http.cache;

import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpUriRequest;

import com.github.ignition.support.http.IgnitedHttpRequest;
import com.github.ignition.support.http.IgnitedHttpResponse;

public class CachedHttpRequest implements IgnitedHttpRequest {

    private String url;

    private HttpResponseCache responseCache;

    public CachedHttpRequest(HttpResponseCache responseCache, String url) {
        this.responseCache = responseCache;
        this.url = url;
    }

    @Override
    public String getRequestUrl() {
        return url;
    }

    @Override
    public IgnitedHttpRequest expecting(Integer... statusCodes) {
        return this;
    }

    @Override
    public IgnitedHttpRequest retries(int retries) {
        return this;
    }

    @Override
    public IgnitedHttpResponse send() throws ConnectException {
        return new CachedHttpResponse(responseCache.get(url));
    }

    @Override
    public HttpUriRequest unwrap() {
        return null;
    }

    @Override
    public IgnitedHttpRequest withTimeout(int timeout) {
        return this;
    }

	@Override
	public URI getRequestUri()
	{
		try
		{
			return new URI( url );
		}
		catch ( URISyntaxException e )
		{
			e.printStackTrace();
			return null;
		}
	}
	
	@Override
    public HttpEntity getEntity()
    {//Currently only built for GETs no entity.    	
    	return null;
    }
}
