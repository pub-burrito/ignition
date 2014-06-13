/* Copyright (c) 2009 Matthias Kaeppler
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
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

public class IgnitedHttpResponseImpl implements IgnitedHttpResponse {

	private HttpResponse response;
    private HttpEntity entity;

    public IgnitedHttpResponseImpl(HttpResponse response) throws IOException {
        this.response = response;
        entity = response.getEntity();

        /*
         * Replacing original entity by a Cloud Managed one to prevent resource consumption by other frameworks.
         * We will tell the original entity in this.entity to release its resources when we're done reading from it 
         * via its consumeContent(). 
         */
        if (entity != null) {
        	response.setEntity( new IgnitedManagedHttpEntity(entity) );
        }
    }

	public HttpEntity getEntity()
	{
		return entity;
	}
	
    public HttpResponse unwrap() {
        return response;
    }
    
    public InputStream getResponseBody() throws IOException {
        return new InputStream() {
			private InputStream wrapped = entity.getContent();

			public int available() throws IOException
			{
				return wrapped.available();
			}

			public void close() throws IOException
			{
				wrapped.close();
				
				entity.consumeContent();
			}

			public boolean equals( Object obj )
			{
				return wrapped.equals( obj );
			}

			public int hashCode()
			{
				return wrapped.hashCode();
			}

			public void mark( int readlimit )
			{
				wrapped.mark( readlimit );
			}

			public boolean markSupported()
			{
				return wrapped.markSupported();
			}

			public int read() throws IOException
			{
				return wrapped.read();
			}

			public int read( byte[] b, int off, int len ) throws IOException
			{
				return wrapped.read( b, off, len );
			}

			public int read( byte[] b ) throws IOException
			{
				return wrapped.read( b );
			}

			public void reset() throws IOException
			{
				wrapped.reset();
			}

			public long skip( long n ) throws IOException
			{
				return wrapped.skip( n );
			}

			public String toString()
			{
				return String.format("%s, wrapped[%s]", IgnitedHttpResponseImpl.this.toString(), wrapped.toString());
			}
		};
    }

    public byte[] getResponseBodyAsBytes() throws IOException {
        return EntityUtils.toByteArray(entity);
    }

    public String getResponseBodyAsString() throws IOException {
        return EntityUtils.toString(entity);
    }

    public int getStatusCode() {
        return this.response.getStatusLine().getStatusCode();
    }

    public String getHeader(String header) {
        if (!response.containsHeader(header)) {
            return null;
        }
        return response.getFirstHeader(header).getValue();
    }
}
