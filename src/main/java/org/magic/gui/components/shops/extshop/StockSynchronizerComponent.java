package org.magic.gui.components.shops.extshop;

import java.awt.BorderLayout;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.jdesktop.swingx.JXTable;
import org.magic.api.interfaces.MTGExternalShop;
import org.magic.api.interfaces.MTGStockItem;
import org.magic.gui.abstracts.AbstractBuzyIndicatorComponent;
import org.magic.gui.abstracts.MTGUIComponent;
import org.magic.gui.models.StockItemTableModel;
import org.magic.gui.renderer.StockTableRenderer;
import org.magic.services.MTGConstants;
import org.magic.services.threads.ThreadManager;
import org.magic.services.workers.AbstractObservableWorker;
import org.magic.tools.UITools;


public class StockSynchronizerComponent extends MTGUIComponent {

	private static final long serialVersionUID = 1L;
	private JComboBox<MTGExternalShop> cboInput;
	private JXTable tableInput;
	private StockItemTableModel modelInput;
	
	private AbstractBuzyIndicatorComponent buzy;
	
	public StockSynchronizerComponent() {
		setLayout(new BorderLayout(0, 0));

		var panelCenter = new JPanel();
		var txtSearch = new JTextField(15);
		var panelNorth = new JPanel();
	
		panelCenter.setLayout(new BorderLayout());
		
		cboInput = UITools.createCombobox(MTGExternalShop.class,true);
		buzy = AbstractBuzyIndicatorComponent.createProgressComponent();
		modelInput = new StockItemTableModel();
		tableInput = UITools.createNewTable(modelInput);
		UITools.initTableFilter(tableInput);
		UITools.setDefaultRenderer(tableInput, new StockTableRenderer());
		
		
		for(var i : new int[] {4,5,8})
			tableInput.getColumnExt(modelInput.getColumnName(i)).setVisible(false);
	
		
		panelNorth.add(cboInput);
		panelNorth.add(buzy);
		
		add(panelNorth, BorderLayout.NORTH);
		add(panelCenter,BorderLayout.CENTER);
		
		panelNorth.add(txtSearch);
		panelCenter.add(new JScrollPane(tableInput), BorderLayout.CENTER);
		
		txtSearch.addActionListener(il->loadProducts((MTGExternalShop)cboInput.getSelectedItem(),modelInput,txtSearch.getText()));
	}

	private void loadProducts(MTGExternalShop ext,StockItemTableModel model,String search) {
		model.clear();
		
		AbstractObservableWorker<List<MTGStockItem>,MTGStockItem,MTGExternalShop> sw = new AbstractObservableWorker<>(buzy,ext)
		{
			@Override
			protected List<MTGStockItem> doInBackground() throws Exception {
					return plug.listStock(search);
			}
			
			@Override
			protected void done() {
				try {
					super.done();
					model.addItems(get());
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} catch (ExecutionException e) {
					logger.error(e);
				}
			}
			
		};
		
		
		
		ThreadManager.getInstance().runInEdt(sw,"load stock");
	}

	@Override
	public ImageIcon getIcon() {
		return MTGConstants.ICON_TAB_STOCK;
	}

	@Override
	public String getTitle() {
		return "STOCK";
	}

}
