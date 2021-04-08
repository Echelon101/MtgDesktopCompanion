package org.magic.gui.dashlet;

import java.awt.BorderLayout;
import java.awt.Rectangle;

import javax.swing.ImageIcon;
import javax.swing.JTextField;

import org.apache.commons.lang3.StringUtils;
import org.magic.api.interfaces.abstracts.AbstractJDashlet;
import org.magic.gui.abstracts.MTGUIBrowserComponent;
import org.magic.services.MTGConstants;

public class JIFrameDashlet extends AbstractJDashlet {

	MTGUIBrowserComponent comp;
	JTextField txtUrl;
	
	@Override
	public void initGUI() {

		setLayout(new BorderLayout());
		txtUrl = new JTextField();
		comp = MTGUIBrowserComponent.createBrowser();
		
		
		
		if (getProperties().size() > 0) 
		{
			Rectangle r = new Rectangle((int) Double.parseDouble(getString("x")),
					(int) Double.parseDouble(getString("y")), (int) Double.parseDouble(getString("w")),
					(int) Double.parseDouble(getString("h")));
			
			
			txtUrl.setText(getString("URL"));
			
			setBounds(r);
		}
		
		add(comp,BorderLayout.CENTER);
		add(txtUrl,BorderLayout.NORTH);
		
		if(!StringUtils.isBlank(getString("URL")))
			comp.loadURL(getString("URL"));
		
		
		
		txtUrl.addActionListener(al->{
			init();
		});
		
		
	}

	@Override
	public void init() {
		
		if(!txtUrl.getText().startsWith("http"))
			txtUrl.setText("https://"+txtUrl.getText());
		
		comp.loadURL(txtUrl.getText());
		
		txtUrl.setText(comp.getCurrentURL());
		setProperty("URL", txtUrl.getText());
	}

	@Override
	public String getName() {
		return "IFrame";
	}
	
	
	@Override
	public ImageIcon getDashletIcon() {
		return MTGConstants.ICON_CHROME;
	}

}
