package org.magic.gui.dashlet;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.api.mkm.modele.InsightElement;
import org.api.mkm.services.InsightService;
import org.jdesktop.swingx.JXTable;
import org.magic.api.exports.impl.MKMFileWantListExport;
import org.magic.api.interfaces.abstracts.AbstractJDashlet;
import org.magic.gui.abstracts.GenericTableModel;
import org.magic.tools.UITools;

public class MkmOversightDashlet extends AbstractJDashlet {
	

	private static final String INSIGHT_SELECTION_KEY = "INSIGHT_SELECTION";
	private static final String CHANGE_VALUE = "changeValue";
	private static final String PRICE = "price";
	private static final String YESTERDAY_PRICE = "yesterdayPrice";
	private static final String YESTERDAY_STOCK = "yesterdayStock";
	private static final String ED = "ed";
	private static final String CARD_NAME = "cardName";
	private static final long serialVersionUID = 1L;
	private transient InsightService service ;
	private GenericTableModel<InsightElement> model;
	private JComboBox<INSIGHT_SELECTION> comboBox;
	
	
	private enum INSIGHT_SELECTION  { STOCK_REDUCTION,BEST_BARGAIN,TOP_CARDS,BIGGEST_START_PRICE,BIGGEST_START_PRICE_FOIL,BIGGEST_AVG_SALES,BIGGEST_AVG_SALES_FOIL }
	
	
	
	
	@Override
	public ImageIcon getDashletIcon() {
		return new ImageIcon(MKMFileWantListExport.class.getResource("/icons/plugins/magiccardmarket.png"));
	}
	
	
	@Override
	public String getName() {
		return "Mkm Oversight";
	}
	
	@Override
	public STATUT getStatut() {
		return STATUT.DEV;
	}
	
	@Override
	public void initGUI() 
	{
		getContentPane().setLayout(new BorderLayout(0, 0));
		model=new GenericTableModel<>();
		JPanel panneauHaut = new JPanel();
		getContentPane().add(panneauHaut, BorderLayout.NORTH);
		comboBox = UITools.createCombobox(INSIGHT_SELECTION.values());
		panneauHaut.add(comboBox);
		service = new InsightService();
		JXTable table = UITools.createNewTable(model);
		getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);
		comboBox.addItemListener(pcl->{
		
			if (pcl.getStateChange() == ItemEvent.SELECTED) {
				init();
			}
		});
		
		
		if (getProperties().size() > 0) {
			Rectangle r = new Rectangle((int) Double.parseDouble(getString("x")),
					(int) Double.parseDouble(getString("y")), (int) Double.parseDouble(getString("w")),
					(int) Double.parseDouble(getString("h")));
			setBounds(r);
			
			if(getString(INSIGHT_SELECTION_KEY)!=null)
					comboBox.setSelectedItem(INSIGHT_SELECTION.valueOf(getString(INSIGHT_SELECTION_KEY)));
		}
		
	}

	@Override
	public void init() {
		try {
			
			
			setProperty(INSIGHT_SELECTION_KEY, comboBox.getSelectedItem().toString());
			
			switch((INSIGHT_SELECTION)comboBox.getSelectedItem())
			{
				case BEST_BARGAIN:
					model.setColumns(CARD_NAME,ED,PRICE);
					model.init(service.getBestBargain());
					break;
				
				case STOCK_REDUCTION:
					model.setColumns(CARD_NAME,ED,YESTERDAY_STOCK,"stock",CHANGE_VALUE);
					model.init(service.getHighestPercentStockReduction());
					break;
				case BIGGEST_AVG_SALES:
					model.setColumns(CARD_NAME,ED,YESTERDAY_PRICE,PRICE,CHANGE_VALUE);
					model.init(service.getBiggestAvgSalesPriceIncrease(false));
					break;
				case BIGGEST_AVG_SALES_FOIL:
					model.setColumns(CARD_NAME,ED,YESTERDAY_PRICE,PRICE,CHANGE_VALUE);
					model.init(service.getBiggestAvgSalesPriceIncrease(true));
					break;
				case BIGGEST_START_PRICE:
					model.setColumns(CARD_NAME,ED,YESTERDAY_PRICE,PRICE,CHANGE_VALUE);
					model.init(service.getStartingPriceIncrease(false));
					break;
				case BIGGEST_START_PRICE_FOIL:
					model.setColumns(CARD_NAME,ED,YESTERDAY_PRICE,PRICE,CHANGE_VALUE);
					model.init(service.getStartingPriceIncrease(true));
					break;
				case TOP_CARDS:
					model.setColumns(CARD_NAME,ED,PRICE);
					model.init(service.getTopCards(1));
					break;
			default:
				break;
			}
		} catch (IOException e) {
		logger.error(e);
		}
	}

}
