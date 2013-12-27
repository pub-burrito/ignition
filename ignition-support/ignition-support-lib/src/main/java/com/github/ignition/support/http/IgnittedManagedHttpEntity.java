/**
 * 
 */
package com.github.ignition.support.http;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.entity.HttpEntityWrapper;

/**
 * <!--
 * IgnittedManagedHttpEntity.java
 * -->
 * TODO Description
 * 
 * @author <a href="mailto:steven.berlanga@cloud.com">Steven Berlanga</a>
 * @since TODO
 * @date Dec 27, 2013
 * @copyright Copyright (c) 2013 Cloud.com. All rights reserved.
 */
public class IgnittedManagedHttpEntity extends HttpEntityWrapper
{

	public IgnittedManagedHttpEntity( HttpEntity wrapped )
	{
		super( wrapped );
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
