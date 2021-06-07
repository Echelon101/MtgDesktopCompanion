package org.magic.gui.components;

import static org.magic.tools.MTG.capitalize;
import static org.magic.tools.MTG.getEnabledPlugin;

import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.DefaultRowSorter;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXTable;
import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicCollection;
import org.magic.api.beans.MagicEdition;
import org.magic.api.interfaces.MTGCardsProvider;
import org.magic.api.interfaces.MTGDao;
import org.magic.gui.abstracts.AbstractBuzyIndicatorComponent;
import org.magic.gui.models.MagicCardTableModel;
import org.magic.gui.renderer.MagicEditionsJLabelRenderer;
import org.magic.gui.renderer.ManaCellRenderer;
import org.magic.services.MTGConstants;
import org.magic.services.MTGControler;
import org.magic.services.MTGLogger;
import org.magic.services.threads.ThreadManager;
import org.magic.services.workers.AbstractObservableWorker;
import org.magic.sorters.CardsEditionSorter;
import org.magic.sorters.NumberSorter;
import org.magic.tools.UITools;

public class CardsEditionTablePanel extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JXTable table;
	private MagicCardTableModel model;
	private MagicEdition currentEdition;
	private AbstractBuzyIndicatorComponent buzy;
	private transient Logger logger = MTGLogger.getLogger(this.getClass());
	private JButton btnImport;
	private JComboBox<MagicCollection> cboCollection;
	private transient AbstractObservableWorker<List<MagicCard>, MagicCard,MTGCardsProvider> sw;
	private JCheckBox chkNeededCards;
	
	
	public CardsEditionTablePanel() {
		setLayout(new BorderLayout(0, 0));
		
		var panneauHaut = new JPanel();
		model = new MagicCardTableModel();
		
		table = UITools.createNewTable(model);
		buzy=AbstractBuzyIndicatorComponent.createProgressComponent();
		
		table.getColumnModel().getColumn(2).setCellRenderer(new ManaCellRenderer());
		table.getColumnModel().getColumn(6).setCellRenderer(new MagicEditionsJLabelRenderer());
		table.setColumnControlVisible(true);
		
		for(int i : model.defaultHiddenColumns())
			table.getColumnExt(model.getColumnName(i)).setVisible(false);

		DefaultRowSorter<TableModel, Integer> sorterCards;
		sorterCards = new TableRowSorter<>(model);
		sorterCards.setComparator(7, new NumberSorter());
		table.setRowSorter(sorterCards);
		
		UITools.initTableFilter(table);
		
		panneauHaut.add(buzy);
		add(new JScrollPane(table), BorderLayout.CENTER);
		add(panneauHaut,BorderLayout.NORTH);
		
		var panneauBas = new JPanel();
		add(panneauBas, BorderLayout.SOUTH);
		
		cboCollection =  UITools.createComboboxCollection();
		panneauBas.add(cboCollection);
		
		btnImport = new JButton(MTGConstants.ICON_MASS_IMPORT_SMALL);
		btnImport.setEnabled(false);
		panneauBas.add(btnImport);
		
		chkNeededCards = new JCheckBox(capitalize("FILTER_NEEDED"));
		panneauBas.add(chkNeededCards);
		
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent componentEvent) {
				init(currentEdition);
			}

		});
		
		chkNeededCards.addActionListener(il->{
			
			
			if(chkNeededCards.isSelected()) {
				AbstractObservableWorker<List<MagicCard>,MagicCard, MTGDao> work = new AbstractObservableWorker<>(buzy,getEnabledPlugin(MTGDao.class),model.getRowCount()) {

					@Override
					protected List<MagicCard> doInBackground() throws Exception {
						return plug.listCardsFromCollection(MTGControler.getInstance().get("default-library"),currentEdition);
					}

					@Override
					protected void notifyEnd() {
						try {
							model.removeItem(get());
						} catch(InterruptedException ex)
						{
							Thread.currentThread().interrupt();
						}catch (Exception e) {
							logger.error(e);
						}
					}

					
					
				};
				
				ThreadManager.getInstance().runInEdt(work, "filtering missing cards");
			}
			else
			{
				init(currentEdition);
			}
		});
		
		
		btnImport.addActionListener(ae->{
			List<MagicCard> list = getSelectedCards();
			
			int res = JOptionPane.showConfirmDialog(null,capitalize("COLLECTION_IMPORT") + " :" + list.size() + " cards in " + cboCollection.getSelectedItem());
			if(res==JOptionPane.YES_OPTION)
			{
				buzy.start(list.size());
				
				SwingWorker<Void, MagicCard> swImp = new SwingWorker<>()
				{
				@Override
					protected void done() {
						buzy.end();
					}

					@Override
					protected void process(List<MagicCard> chunks) {
						buzy.progressSmooth(chunks.size());
					}

					@Override
					protected Void doInBackground() throws Exception {
						for(MagicCard mc : list)
							try {
								MTGControler.getInstance().saveCard(mc, (MagicCollection)cboCollection.getSelectedItem(),null);
								publish(mc);
							} catch (SQLException e) {
								logger.error("couln't save " + mc,e);
							}
						return null;
						}
					
						};
				
				
				
				ThreadManager.getInstance().runInEdt(swImp, "import cards in "+cboCollection.getSelectedItem());
			}
		});
	}
	
	public MagicCard getSelectedCard()
	{
		if(table.getSelectedRow()>-1)
		{
			return UITools.getTableSelection(table, 0);
		}
		
		return null;
	}
	
	public List<MagicCard> getSelectedCards()
	{
		return UITools.getTableSelections(table,0);
	}
	
	
	public JXTable getTable() {
		return table;
	}
	
	public void init(MagicEdition ed)
	{
		this.currentEdition=ed;
		chkNeededCards.setSelected(false);
		if(isVisible())
			refresh();
	}
	
	public void enabledImport(boolean t)
	{
		btnImport.setEnabled(t);
	}
	
	private void refresh()
	{
		if(currentEdition==null)
			return;
	
		
		btnImport.setEnabled(false);
		
		
		if(sw!=null && !sw.isDone())
		{
			sw.cancel(true);
		}
		
		
		sw = new AbstractObservableWorker<>(buzy,getEnabledPlugin(MTGCardsProvider.class),currentEdition.getCardCount()) {
			
			@Override
			protected List<MagicCard> doInBackground() {
				List<MagicCard> cards = new ArrayList<>();
				try {
					cards = getEnabledPlugin(MTGCardsProvider.class).searchCardByEdition(currentEdition);
					Collections.sort(cards, new CardsEditionSorter() );
					return cards;
				} catch (IOException e) {
					logger.error(e);
					return cards;
				}
				
			}
			
			@Override
			protected void process(List<MagicCard> chunks) {
				super.process(chunks);
				model.addItems(chunks);
			}
			
			
			@Override
			protected void done() {
				super.done();
				try {
					model.init(get());
				} catch(InterruptedException ex)
				{
					Thread.currentThread().interrupt();
				}catch (Exception e) {
					logger.error(e);
				}
			}
			
			
			
		};
		
		ThreadManager.getInstance().runInEdt(sw, "loading edition "+currentEdition);
	}
	
	

}
