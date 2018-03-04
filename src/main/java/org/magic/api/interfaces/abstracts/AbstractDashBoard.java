package org.magic.api.interfaces.abstracts;

import java.io.File;
import java.util.Properties;

import org.magic.api.interfaces.MTGDashBoard;
import org.magic.services.MTGConstants;

public abstract class AbstractDashBoard extends AbstractMTGPlugin implements MTGDashBoard {

	public enum FORMAT { STANDARD,LEGACY,VINTAGE,MODERN}
	
	@Override
	public PLUGINS getType() {
		return PLUGINS.DASHBOARD;
	}
	
		
	public AbstractDashBoard() {
		super();
		confdir = new File(MTGConstants.CONF_DIR, "dashboards");
		if(!confdir.exists())
			confdir.mkdir();
		load();
	}
}
