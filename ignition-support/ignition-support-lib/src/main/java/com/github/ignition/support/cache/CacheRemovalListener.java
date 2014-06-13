package com.github.ignition.support.cache;

public interface CacheRemovalListener<K, V>
{	
	public void onRemoval( K key, V value );
}
