package org.magic.gui.components.shops;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JScrollPane;

import org.jdesktop.swingx.JXTable;
import org.magic.api.beans.MTGNotification;
import org.magic.api.beans.MTGNotification.MESSAGE_TYPE;
import org.magic.api.interfaces.MTGStockItem;
import org.magic.gui.abstracts.MTGUIComponent;
import org.magic.gui.models.StockItemTableModel;
import org.magic.gui.renderer.StockTableRenderer;
import org.magic.services.MTGConstants;
import org.magic.services.MTGControler;
import org.magic.tools.UITools;


public class StockItemPanel extends MTGUIComponent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JXTable table;
	private StockItemTableModel model;

	
	public StockItemPanel() {
		setLayout(new BorderLayout(0, 0));
		model = new StockItemTableModel();
		table = UITools.createNewTable(model);
		
		UITools.setDefaultRenderer(table, new StockTableRenderer());

		for(int i : model.defaultHiddenColumns())
		{
			table.getColumnExt(model.getColumnName(i)).setVisible(false);	
		}
		
		add(new JScrollPane(table),BorderLayout.CENTER);
		
		
	}

		
	@Override
	public void onHide() {
		boolean isUpdatedModel = model.getItems().stream().anyMatch(MTGStockItem::isUpdated);
		
		if(isUpdatedModel)
		{
			MTGControler.getInstance().notify(new MTGNotification("Item Updated", "Don't forget to save your updates", MESSAGE_TYPE.WARNING));
		}
		
	}
	
	
	public void initItems(List<MTGStockItem> st) {

		try {
			model.init(st);
			table.packAll();
		} catch (Exception e) {
			logger.error(e);
		}

	}

	
	

	@Override
	public String getTitle() {
		return "Products";
	}
	
	@Override
	public ImageIcon getIcon() {
		return MTGConstants.ICON_TAB_STOCK;
	}

	
}
