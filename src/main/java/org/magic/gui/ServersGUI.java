package org.magic.gui;

import java.awt.GridLayout;
import java.util.List;

import javax.swing.ImageIcon;

import org.magic.api.interfaces.MTGServer;
import org.magic.gui.abstracts.MTGUIComponent;
import org.magic.gui.components.ServerStatePanel;
import org.magic.services.MTGConstants;
import org.magic.services.MTGControler;
import org.magic.services.ThreadManager;

public class ServersGUI extends MTGUIComponent {

	private static final long serialVersionUID = 1L;

	@Override
	public ImageIcon getIcon() {
		return MTGConstants.ICON_TAB_ACTIVESERVER;
	}
	
	@Override
	public String getTitle() {
		return MTGControler.getInstance().getLangService().getCapitalize("ACTIVE_SERVERS");
	}
	
	
	public ServersGUI() {
		
		List<MTGServer> list = MTGControler.getInstance().getPlugins(MTGServer.class);
		
		
		setLayout(new GridLayout(list.size(), 1, 0, 0));
		
		ThreadManager.getInstance().invokeLater(()->{
				for (MTGServer s : list) {
					add(new ServerStatePanel(s));
				}
		});
		
		
	}
}
