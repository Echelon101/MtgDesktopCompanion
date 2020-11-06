package org.magic.api.interfaces.abstracts;

import java.awt.Image;
import java.awt.Rectangle;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JInternalFrame;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.magic.api.beans.MTGDocumentation;
import org.magic.api.beans.MTGNotification.FORMAT_NOTIFICATION;
import org.magic.api.interfaces.MTGDashlet;
import org.magic.services.MTGConstants;
import org.magic.services.MTGLogger;
import org.utils.patterns.observer.Observer;

public abstract class AbstractJDashlet extends JInternalFrame implements MTGDashlet{

	private static final long serialVersionUID = 1L;
	public static final File confdir= new File(MTGConstants.DATA_DIR, "dashlets");
	private Properties props;
	protected transient Logger logger = MTGLogger.getLogger(this.getClass());
	private boolean loaded=false;
	public abstract ImageIcon getDashletIcon();
	
	@Override
	public ObjectName getObjectName() {
		try {
			return new ObjectName("org.magic.api:type="+getType()+",name="+getName());
		} catch (MalformedObjectNameException e) {
			return null;
		}
	}
	
	
	
	
	@Override
	public Icon getIcon() {
		try {
			return new ImageIcon(getDashletIcon().getImage().getScaledInstance(MTGConstants.MENU_ICON_SIZE, MTGConstants.MENU_ICON_SIZE, Image.SCALE_SMOOTH));
		}
		catch(Exception e)
		{
			return MTGConstants.ICON_DEFAULT_PLUGIN;
		}
	}

	@Override
	public String termsAndCondition() {
		return null;
	}
	
	@Override
	public void unload() {
		// do nothing
		
	}
	
	@Override
	public boolean isPartner() {
		return false;
	}
	
	@Override
	public boolean isLoaded() {
		return loaded;
	}
	
	
	@Override
	public MTGDocumentation getDocumentation() {
		try {
			return new MTGDocumentation(new URL(MTGConstants.MTG_DESKTOP_WIKI_RAW_URL+"/"+getName().replace(" ", "-")+".md"),FORMAT_NOTIFICATION.MARKDOWN);	
		}
		catch(Exception e)
		{
			return null;	
		}
		
	}
	
	
	protected AbstractJDashlet() {
		props = new Properties();
		
		if (!confdir.exists())
		{
			boolean ret = confdir.mkdirs();
			logger.debug(confdir + " doesn't exist, create it="+ret);
			
		}
	
		addInternalFrameListener(new InternalFrameAdapter() {
			@Override
			public void internalFrameClosed(InternalFrameEvent e) {
				AbstractJDashlet dash = (AbstractJDashlet) e.getInternalFrame();
				dash.onDestroy();
				if (dash.getProperties().get("id") != null)
					FileUtils.deleteQuietly(new File(confdir, dash.getProperties().get("id") + ".conf"));
			}
		});
		
		loaded=true;
		setFrameIcon(getIcon());
		setTitle(getName());
		setResizable(true);
		setClosable(true);
		setIconifiable(true);
		setMaximizable(true);
		setBounds(new Rectangle(5,5, 536,346));
	}


	protected void onDestroy() {
		//do nothing
	}


	@Override
	public String getVersion() {
		return "1.0";
	}
	
	@Override
	public boolean isEnable() {
		return true;
	}
	
	public void setProperties(Properties p) {
		this.props = p;
	}

	public String getProperty(String k, String d) {
		return props.getProperty(k, d);
	}

	public void setProperty(Object k, Object v) {
		props.put(k, v);
	}
	
	public void setProperty(String k,String v) {
		props.put(k, v);
	}
	
	@Override
	public String getString(String k) {
		return props.getProperty(k);
	}


	public Properties getProperties() {
		return props;
	}

	

	@Override
	public String toString() {
		return getName();
	}
	
	@Override
	public void setProperty(String k, Object value) {
		props.put(k, value);
		
	}

	

	@Override
	public void save() {
		// do nothing, managed in DashboardGUI
		
	}

	@Override
	public void load() {
		// do nothing, managed in DashboardGUI
		
	}

	@Override
	public STATUT getStatut() {
		return STATUT.STABLE;
	}

	@Override
	public PLUGINS getType() {
		return PLUGINS.DASHLET;
	}

	@Override
	public File getConfFile() {
		return null;
	}

	@Override
	public void initDefault() {
		// doNothing
		
	}

	@Override
	public List<Observer> listObservers() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addObserver(Observer o) {
		// doNothing
		
	}

	@Override
	public void removeObservers() {
		// doNothing
		
	}

	@Override
	public void removeObserver(Observer o) {
		// doNothing
		
	}
}
