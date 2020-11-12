package org.magic.gui.components.dialog;

import static org.magic.tools.MTG.listEnabledPlugins;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXTable;
import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicDeck;
import org.magic.api.beans.RetrievableDeck;
import org.magic.api.interfaces.MTGDeckSniffer;
import org.magic.gui.abstracts.AbstractBuzyIndicatorComponent;
import org.magic.gui.abstracts.AbstractDelegatedImporterDialog;
import org.magic.gui.models.DeckSnifferTableModel;
import org.magic.gui.renderer.ManaCellRenderer;
import org.magic.services.MTGConstants;
import org.magic.services.MTGControler;
import org.magic.services.MTGLogger;
import org.magic.services.threads.ThreadManager;
import org.magic.services.workers.AbstractObservableWorker;
import org.magic.tools.UITools;

public class DeckSnifferDialog extends AbstractDelegatedImporterDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JXTable table;
	private JComboBox<MTGDeckSniffer> cboSniffers;
	private JComboBox<String> cboFormats;
	private DeckSnifferTableModel model;
	private MagicDeck importedDeck;
	private AbstractBuzyIndicatorComponent lblLoad = AbstractBuzyIndicatorComponent.createLabelComponent();
	private JButton btnImport;
	private transient MTGDeckSniffer selectedSniffer;
	private JButton btnConnect;
	private transient Logger logger = MTGLogger.getLogger(this.getClass());
	private JPanel panelChoose;

	public DeckSnifferDialog() {

		importedDeck = new MagicDeck();
		setSize(new Dimension(500, 300));
		setTitle(MTGControler.getInstance().getLangService().getCapitalize("DECKS_IMPORTER"));
		setIconImage(MTGConstants.ICON_DECK.getImage());
		setModal(true);
		getContentPane().setLayout(new BorderLayout(0, 0));
		model = new DeckSnifferTableModel();
		table = UITools.createNewTable(model);
		
		getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);


		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.NORTH);

		selectedSniffer = listEnabledPlugins(MTGDeckSniffer.class).get(0);
		panel.setLayout(new BorderLayout(0, 0));

		
		panel.add(lblLoad, BorderLayout.CENTER);
		
		panelChoose = new JPanel();
		panel.add(panelChoose, BorderLayout.WEST);
				cboSniffers =UITools.createCombobox(MTGDeckSniffer.class,false);
				panelChoose.add(cboSniffers);
				
				btnConnect = new JButton(MTGControler.getInstance().getLangService().getCapitalize("CONNECT"));
				panelChoose.add(btnConnect);
				
						cboFormats = new JComboBox<>();
						panelChoose.add(cboFormats);
						cboFormats.addActionListener(e -> {
							try {
								lblLoad.start();
								selectedSniffer.setProperty("FORMAT", cboFormats.getSelectedItem());
								model.init(selectedSniffer.getDeckList());
								model.fireTableDataChanged();
								lblLoad.end();
							} catch (Exception e1) {
								lblLoad.end();
								logger.error("error change cboFormat", e1);
								MTGControler.getInstance().notify(e1);
							}
						});
			
				btnConnect.addActionListener(e -> ThreadManager.getInstance().executeThread(() -> {
					try {
						lblLoad.start();
						selectedSniffer.connect();
						cboFormats.removeAllItems();

						for (String s : selectedSniffer.listFilter())
							cboFormats.addItem(s);

						
						lblLoad.end();

					} catch (Exception e1) {
						lblLoad.end();
						MTGControler.getInstance().notify(e1);
					}
				}, "Connection to " + selectedSniffer));
				
						cboSniffers.addActionListener(e -> selectedSniffer = (MTGDeckSniffer) cboSniffers.getSelectedItem());
	
		JPanel panelButton = new JPanel();
		getContentPane().add(panelButton, BorderLayout.SOUTH);

		JButton btnClose = new JButton(MTGConstants.ICON_CANCEL);
		btnClose.setToolTipText(MTGControler.getInstance().getLangService().getCapitalize("CANCEL"));
		
		btnClose.addActionListener(e -> dispose());
		panelButton.add(btnClose);

		setLocationRelativeTo(null);

		table.getColumnModel().getColumn(1).setCellRenderer(new ManaCellRenderer());
		table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		btnImport = new JButton(MTGConstants.ICON_CHECK);
		btnImport.setToolTipText(MTGControler.getInstance().getLangService().getCapitalize("IMPORT"));
		
		panelButton.add(btnImport);

		
		
		btnImport.addActionListener(e ->{
		
			AbstractObservableWorker<MagicDeck, MagicCard, MTGDeckSniffer> sw = new AbstractObservableWorker<>(lblLoad,selectedSniffer) {

				@Override
				protected void process(List<MagicCard> chunks) {
					buzy.progressSmooth(chunks.size());
					buzy.setText(chunks.toString());
				}
				
				@Override
				protected MagicDeck doInBackground() throws Exception {
					return plug.getDeck((RetrievableDeck)UITools.getTableSelection(table, 0)); 
				}

				@Override
				protected void done() {
					super.done();
					
					importedDeck = getResult();
					btnImport.setEnabled(true);
					dispose();
				}
				
				@Override
				protected void error(Exception e) {
					MTGControler.getInstance().notify(e);
				}
				
				
			};
			
		ThreadManager.getInstance().runInEdt(sw, "Import deck");
		
		});

		
	}

	public MagicDeck getSelectedDeck() {
		return importedDeck;
	}

}
