package org.magic.gui.components;

import static org.magic.tools.MTG.capitalize;
import static org.magic.tools.MTG.getEnabledPlugin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

import org.jdesktop.swingx.JXTable;
import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicCardStock;
import org.magic.api.beans.MagicCollection;
import org.magic.api.beans.MagicDeck;
import org.magic.api.interfaces.MTGCardsExport.MODS;
import org.magic.api.interfaces.MTGDao;
import org.magic.gui.abstracts.AbstractBuzyIndicatorComponent;
import org.magic.gui.abstracts.MTGUIComponent;
import org.magic.gui.models.DeckStockComparisonModel;
import org.magic.gui.renderer.standard.IntegerCellEditorRenderer;
import org.magic.services.MTGControler;
import org.magic.services.threads.ThreadManager;
import org.magic.tools.UITools;
public class DeckStockComparatorPanel extends MTGUIComponent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JComboBox<MagicCollection> cboCollections;
	private MagicDeck currentDeck;
	private DeckStockComparisonModel model;
	private JButton btnCompare;
	private AbstractBuzyIndicatorComponent buzyLabel;
	private DeckPricePanel pricesPan;
	private JCheckBox chkEditionStrict ;
	private JExportButton btnExportMissing;
	private JCheckBox chkCollectionCheck;
	
	public void setCurrentDeck(MagicDeck c) {
		this.currentDeck = c;
	}
	
	public DeckStockComparatorPanel() {
		initGUI();
		initActions();
	}

	private void initGUI() {
		
		setLayout(new BorderLayout(0, 0));
		btnCompare = new JButton("Compare");
		JPanel panneauHaut = new JPanel();
		cboCollections = UITools.createComboboxCollection();
		buzyLabel = AbstractBuzyIndicatorComponent.createProgressComponent();
		model = new DeckStockComparisonModel();
		btnExportMissing = new JExportButton(MODS.EXPORT);
		btnExportMissing.setText("Export Missing");
		UITools.bindJButton(btnExportMissing, KeyEvent.VK_M, "ExportMissing");
		
		JSplitPane pan = new JSplitPane();
		pan.setDividerLocation(0.5);
		pan.setResizeWeight(0.5);
		
		pan.setOrientation(JSplitPane.VERTICAL_SPLIT);
		pricesPan = new DeckPricePanel();
		
		JXTable table = UITools.createNewTable(model);
		UITools.initCardToolTipTable(table, 0,null);
		
		add(panneauHaut, BorderLayout.NORTH);
		panneauHaut.add(cboCollections);
		
		chkCollectionCheck = new JCheckBox(capitalize("CHECK_COLLECTION"));
		panneauHaut.add(chkCollectionCheck);
		
		chkEditionStrict = new JCheckBox(capitalize("EDITION_STRICT"));
		panneauHaut.add(chkEditionStrict);
		
		
		panneauHaut.add(btnCompare);
		panneauHaut.add(buzyLabel);

		btnExportMissing.setEnabled(false);
		btnExportMissing.initCardsExport(new Callable<MagicDeck>() {
			
			@Override
			public MagicDeck call() throws Exception {
				
				MagicDeck d = new MagicDeck();
				d.setName(currentDeck.getName());
				d.setDescription("Missing cards for deck " + d.getName());
				model.getItems().forEach(l->d.getMain().put(l.getMc(), l.getResult()));
				
				return d;
			}
		}, buzyLabel);
		
		panneauHaut.add(btnExportMissing);
		
		pan.setLeftComponent(new JScrollPane(table));
		pan.setRightComponent(pricesPan);
		
		add(pan,BorderLayout.CENTER);
		
		table.setDefaultRenderer(Integer.class, (JTable t, Object value, boolean isSelected, boolean hasFocus,int row, int column)->{
				Integer val = (Integer)value;
				if(column==4)
				{
					JLabel c = new JLabel(value.toString(),SwingConstants.CENTER);
					c.setOpaque(true);
					if(val==0)
					{
						c.setBackground(Color.GREEN);
						c.setForeground(Color.BLACK);
					}
					
					else
					{
						c.setBackground(Color.RED);
						c.setForeground(Color.WHITE);
					}
						
					return c;
					
				}
				return new IntegerCellEditorRenderer().getTableCellRendererComponent(table, value, isSelected, hasFocus,row,column);
		});
		
		
		try {
			cboCollections.setSelectedItem(new MagicCollection(MTGControler.getInstance().get("default-library")));
		} catch (Exception e) {
			logger.error("Error retrieving collections",e);
		}
		
		table.packAll();
		
	}

	private void initActions() {
		
		btnCompare.addActionListener(ae-> {
			model.clear();
			if(currentDeck!=null)
			{
				MagicCollection col = (MagicCollection)cboCollections.getSelectedItem();
				buzyLabel.start(currentDeck.getMain().entrySet().size());
				SwingWorker<Void, MagicCard> sw = new SwingWorker<>()
						{
						@Override
						protected Void doInBackground() throws Exception {
							currentDeck.getMain().entrySet().forEach(entry->
							{
								try {
									boolean has = false;
									
									if(chkCollectionCheck.isSelected())
										has = getEnabledPlugin(MTGDao.class).listCollectionFromCards(entry.getKey()).contains(col);
									
									List<MagicCardStock> stocks = getEnabledPlugin(MTGDao.class).listStocks(entry.getKey(), col,chkEditionStrict.isSelected());
									int qty = currentDeck.getMain().get(entry.getKey());
									model.addItem(entry.getKey(),qty,has, stocks);
									publish(entry.getKey());
								} catch (SQLException e) {
									logger.error("Error SQL",e);
								}
							});
							
							return null;
						}

						@Override
						protected void done() {
							buzyLabel.end();
					
							List<MagicCard> pricList = new ArrayList<>();
							model.getItems().stream().filter(l->l.getResult()>0).forEach(l->{
								for(int i=0;i<l.getResult();i++)
									pricList.add(l.getMc());
							});
							
							pricesPan.initDeck(MagicDeck.toDeck(pricList));
							btnExportMissing.setEnabled(!model.isEmpty());
						}

						@Override
						protected void process(List<MagicCard> chunks) {
							buzyLabel.progressSmooth(chunks.size());
						}
				};
				
				
				ThreadManager.getInstance().runInEdt(sw, "compare deck and stock");
				
				
			}
		});
		
	}

	@Override
	public String getTitle() {
		return "Stock Comparison";
	}


}
