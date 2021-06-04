package org.magic.api.cache.impl;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.magic.api.beans.MagicCard;
import org.magic.api.interfaces.abstracts.AbstractCacheProvider;
import org.magic.tools.MemoryTools;

public class Cache2kCache extends AbstractCacheProvider {

	Cache<String, BufferedImage> cache;
	
	public Cache2kCache() {
		cache = new Cache2kBuilder<String, BufferedImage>() {}
	    .expireAfterWrite(getInt("EXPIRATION_MINUTE"), TimeUnit.MINUTES)
	    .entryCapacity(getLong("CAPACITY"))
	    .build();
	}
	
	
	@Override
	public BufferedImage getItem(MagicCard mc) {
		return cache.get(generateIdIndex(mc));
	}

	@Override
	public void put(BufferedImage im, MagicCard mc) throws IOException {
		cache.put(generateIdIndex(mc), im);

	}

	@Override
	public void clear() {
		cache.clear();

	}


	@Override
	public long size() {
		return cache.asMap().entrySet().stream().mapToLong(MemoryTools::sizeOf).sum();
	}

	@Override
	public void initDefault() {
		setProperty("EXPIRATION_MINUTE", "10");
		setProperty("CAPACITY","100");
	}
	
	@Override
	public String getName() {
		return "Cache2k";
	}

}
