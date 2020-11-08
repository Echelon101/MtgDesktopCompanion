package org.magic.api.interfaces.abstracts;

import org.magic.api.beans.MagicCard;
import org.magic.api.interfaces.MTGPicturesCache;
import org.magic.tools.IDGenerator;

public abstract class AbstractCacheProvider extends AbstractMTGPlugin implements MTGPicturesCache {

	@Override
	public PLUGINS getType() {
		return PLUGINS.CACHE;
	}


	protected String generateIdIndex(MagicCard mc) {
		return IDGenerator.generate(mc, mc.getCurrentSet());
	}

}
