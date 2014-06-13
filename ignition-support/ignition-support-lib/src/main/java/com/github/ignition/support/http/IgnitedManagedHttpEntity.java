/**
 * 
 */
package com.github.ignition.support.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.entity.HttpEntityWrapper;

/**
 * <!--
 * IgnitedManagedHttpEntity.java
 * -->
 * Used to pass down an Entity that can't be consumed, so the HTTPClient framework doesn't automatically consume the entity, given we'll need to read from it.
 * 
 * @author <a href="mailto:steven.berlanga@cloud.com">Steven Berlanga</a>
 * @since TODO
 * @date Dec 27, 2013
 * @copyright Copyright (c) 2013 Cloud.com. All rights reserved.
 */
public class IgnitedManagedHttpEntity extends HttpEntityWrapper
{

	public IgnitedManagedHttpEntity( HttpEntity wrapped )
	{
		super( wrapped );
	}	
	
	@Override
	public InputStream getContent() throws IOException
	{
		/*
		 * This stream is a fake so HTTPClient can consume it. The real one is still in the wrapped entity.
		 */
		return new ByteArrayInputStream( new byte[0] );
	}

	@Override
	public void consumeContent() throws IOException
	{
		/*
		 * Not consuming content on purpose to keep stream opened while we consume it.
		 * Apache's AbstractHttpClient implementation seems to call this to enable releasing of the resource soon after 
		 * the response has been handled by a ResponseHandler implementation.
		 * 
		 * Resources will be deallocated by framework as soon as it's done reading the content.
		 */
	}	
}
