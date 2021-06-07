package org.magic.gui.components;

import static org.magic.tools.MTG.capitalize;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jdesktop.swingx.JXTreeTable;
import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicCardAlert;
import org.magic.api.beans.MagicPrice;
import org.magic.api.interfaces.MTGPricesProvider;
import org.magic.gui.abstracts.AbstractBuzyIndicatorComponent;
import org.magic.gui.abstracts.MTGUIComponent;
import org.magic.gui.models.GroupedPriceTreeTableModel;
import org.magic.gui.renderer.MagicPriceShoppingTreeCellRenderer;
import org.magic.gui.renderer.standard.BooleanCellEditorRenderer;
import org.magic.gui.renderer.standard.DoubleCellEditorRenderer;
import org.magic.services.MTGConstants;
import org.magic.services.threads.ThreadManager;
import org.magic.services.workers.AbstractObservableWorker;
import org.magic.tools.UITools;

public class GroupedShoppingPanel extends MTGUIComponent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JComboBox<MTGPricesProvider> cboPricers;
	private transient List<MagicCard> cards;
	private JButton btnCheckPrice;
	private AbstractBuzyIndicatorComponent buzy;
	private JLabel lblitems;
	
		
	
	public void initList(List<MagicCardAlert> d) {
		this.cards = d.stream().map(MagicCardAlert::getCard).collect(Collectors.toList());
		
		lblitems.setText(capitalize("X_ITEMS_IMPORTED",cards.size()));
		enableControle(true);
	}
	
	public void initListCards(List<MagicCard> d) {
		this.cards = d;
		lblitems.setText(capitalize("X_ITEMS_IMPORTED",cards.size()));
		
		enableControle(true);
	}
	
	public void enableControle(boolean b)
	{
		cboPricers.setEnabled(b);
		btnCheckPrice.setEnabled(b);
	}
	
	public JButton getBtnCheckPrice() {
		return btnCheckPrice;
	}


	public GroupedShoppingPanel() {
		setLayout(new BorderLayout(0, 0));
		buzy = AbstractBuzyIndicatorComponent.createProgressComponent();
		btnCheckPrice = new JButton(MTGConstants.ICON_EURO);
		var panel = new JPanel();
		
		add(panel, BorderLayout.NORTH);

		cboPricers = UITools.createCombobox(MTGPricesProvider.class,false);
		panel.add(cboPricers);

		
		enableControle(false);
		
		panel.add(btnCheckPrice);
		panel.add(buzy);
		
		lblitems = new JLabel();
		panel.add(lblitems);
		
		var treetModel = new GroupedPriceTreeTableModel();
		
		var tree = new JXTreeTable(treetModel);
		tree.setTreeCellRenderer(new MagicPriceShoppingTreeCellRenderer());
		tree.setShowGrid(true, false);
		tree.setDefaultRenderer(Boolean.class, new BooleanCellEditorRenderer());
		tree.setDefaultRenderer(Double.class, new DoubleCellEditorRenderer());
		tree.setSortable(true);
		
		add(new JScrollPane(tree), BorderLayout.CENTER);
		
		
		tree.addMouseListener(new MouseAdapter() {
			
			 @Override
			public void mouseClicked(MouseEvent e) {
				 if (e.getClickCount() == 2) 
				 {
					 
					Object o = UITools.getTableSelection(tree, 0);
					
					if(o instanceof MagicPrice)
					{
						UITools.browse(((MagicPrice)o).getSellerUrl());
					}
					 
					 
			     }
			}
			
		});
		
		btnCheckPrice.addActionListener(ae -> {
			
			AbstractObservableWorker<Map<String, List<MagicPrice>>, MagicPrice, MTGPricesProvider> sw = new AbstractObservableWorker<>(buzy,(MTGPricesProvider)cboPricers.getSelectedItem(),cards.size()) {

				@Override
				protected Map<String, List<MagicPrice>> doInBackground() throws Exception {
					return plug.getPricesBySeller(cards);
				}
			
				@Override
				protected void done() {
					super.done();
					try {
						treetModel.init(get());
						} 
					catch(InterruptedException ex)
					{
						Thread.currentThread().interrupt();
					}
					catch (Exception e) {
						logger.error("error",e);
					}
				}
			
			};

			ThreadManager.getInstance().runInEdt(sw, "loading deck price");

		});
		
	}

	@Override
	public String getTitle() {
		return "GROUPED_BUY";
	}

}
