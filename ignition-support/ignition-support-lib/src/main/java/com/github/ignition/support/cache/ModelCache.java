package com.github.ignition.support.cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Allows caching Model objects using the features provided by {@link AbstractCache}. The key into
 * the cache will be based around the cached object's key, and the object will be able to save and
 * reload itself from the cache.
 * 
 * @author Michael England
 * 
 */
public class ModelCache extends AbstractCache<String, CachedModel> {

    /**
     * Creates an {@link AbstractCache} with params provided and name 'ModelCache'.
     * 
     * @see com.github.droidfu.cachefu.AbstractCache#AbstractCache(java.lang.String, int, long, int)
     */
    public ModelCache(int initialCapacity, long expirationInMinutes, int maxConcurrentThreads) {
        super("ModelCache", initialCapacity, expirationInMinutes, maxConcurrentThreads, true);
    }

    // Counter for all saves to cache. Used to determine if newer object in cache
    private long transactionCount = Long.MIN_VALUE + 1;

    /**
     * @see com.github.droidfu.cachefu.AbstractCache#put(java.lang.Object, java.lang.Object)
     */
    @Override
    public synchronized CachedModel put(String key, CachedModel value) {
        // Set transaction id for checking validity against other values with same key
        value.setTransactionId(transactionCount++);
        return super.put(key, value);
    }

    /**
     * Removes all cached objects with key prefix.
     * 
     * @param prefix
     *            Prefix of all cached object keys to be removed
     */
    public synchronized void removeAllWithPrefix(String prefix) {
        CacheHelper.removeAllWithStringPrefix(this, prefix);
    }

    /**
     * @see com.github.droidfu.cachefu.AbstractCache#getFileNameForKey(java.lang.Object)
     */
    @Override
    public String getFileNameForKey(String url) {
        return CacheHelper.getFileNameFromUrl(url);
    }

    /**
     * @see com.github.droidfu.cachefu.AbstractCache#readValueFromDisk(java.io.File)
     */
    @Override
    protected CachedModel readValueFromDisk(File file) throws IOException {
    	FileInputStream in = new FileInputStream( file );
		
		GZIPInputStream gzipIn = new GZIPInputStream( in );
		
		ObjectInputStream input = new ObjectInputStream( gzipIn );
		
		try
		{
			CachedModel value = (CachedModel) input.readObject();
			return value;
		}
		catch ( ClassNotFoundException e )
		{
			throw new IOException( e );
		}
		finally
		{
			input.close();
		}
    }

    /**
     * @see com.github.droidfu.cachefu.AbstractCache#writeValueToDisk(java.io.File,
     *      java.lang.Object)
     */
    @Override
    protected void writeValueToDisk(File file, CachedModel data) throws IOException {
    	
    	FileOutputStream out = new FileOutputStream( file );
		
		GZIPOutputStream gzipOut = new GZIPOutputStream( out );
		
		ObjectOutputStream output = new ObjectOutputStream( gzipOut );
		
		try
		{			
			output.writeObject( data );
		}
		finally
		{			
			output.close();
		}
    }

}
