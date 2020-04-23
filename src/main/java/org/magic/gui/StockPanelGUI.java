package org.magic.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.SystemColor;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;

import org.jdesktop.swingx.JXTable;
import org.magic.api.beans.MTGNotification;
import org.magic.api.beans.MTGNotification.MESSAGE_TYPE;
import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicCardStock;
import org.magic.api.beans.MagicCollection;
import org.magic.api.beans.MagicEdition;
import org.magic.api.beans.enums.EnumCondition;
import org.magic.api.interfaces.MTGCardsExport;
import org.magic.api.interfaces.MTGCardsExport.MODS;
import org.magic.api.interfaces.MTGDao;
import org.magic.api.interfaces.MTGDashBoard;
import org.magic.gui.abstracts.AbstractBuzyIndicatorComponent;
import org.magic.gui.abstracts.MTGUIComponent;
import org.magic.gui.components.CardsDeckCheckerPanel;
import org.magic.gui.components.GedPanel;
import org.magic.gui.components.GradingEditorPane;
import org.magic.gui.components.JExportButton;
import org.magic.gui.components.MagicCardDetailPanel;
import org.magic.gui.components.ObjectViewerPanel;
import org.magic.gui.components.PricesTablePanel;
import org.magic.gui.components.charts.HistoryPricesPanel;
import org.magic.gui.components.dialog.CardSearchImportDialog;
import org.magic.gui.editor.ComboBoxEditor;
import org.magic.gui.editor.IntegerCellEditor;
import org.magic.gui.models.CardStockTableModel;
import org.magic.gui.renderer.MagicEditionJLabelRenderer;
import org.magic.gui.renderer.StockTableRenderer;
import org.magic.services.MTGConstants;
import org.magic.services.MTGControler;
import org.magic.services.threads.ThreadManager;
import org.magic.services.workers.AbstractObservableWorker;
import org.magic.tools.UITools;

public class StockPanelGUI extends MTGUIComponent {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JXTable table;
	private CardStockTableModel model;
	private JButton btnDelete;
	private JButton btnSave;
	private boolean multiselection = false;
	private MagicCardDetailPanel magicCardDetailPanel;
	private HistoryPricesPanel historyPricePanel;
	private PricesTablePanel pricePanel;
	private ObjectViewerPanel jsonPanel;
	private JButton btnReload;
	private AbstractBuzyIndicatorComponent lblLoading;
	private JPanel rightPanel;
	private JSpinner spinner;
	private JComboBox<String> cboLanguages;
	private JTextPane textPane;
	private JComboBox<Boolean> cboFoil;
	private JComboBox<Boolean> cboSigned;
	private JComboBox<Boolean> cboAltered;
	private JButton btnshowMassPanel;
	private JButton btnApplyModification;
	private CardsDeckCheckerPanel deckPanel;
	private GradingEditorPane gradePanel;
	private GedPanel gedPanel;
	
	private static Boolean[] values = { null, true, false };
	private JComboBox<EnumCondition> cboQuality;
	private JButton btnImport;
	private JComboBox<MagicCollection> cboCollection;
	private JExportButton btnExport;
	private JButton btnGeneratePrice;
	private JLabel lblCount;

	private JComboBox<String> cboSelections;
	private String[] selections = new String[] { "", MTGControler.getInstance().getLangService().get("NEW"),MTGControler.getInstance().getLangService().get("UPDATED"),MTGControler.getInstance().getLangService().get("ALL") };
	private File fileImport;
	
	@Override
	public ImageIcon getIcon() {
		return MTGConstants.ICON_STOCK;
	}
	
	@Override
	public String getTitle() {
		return MTGControler.getInstance().getLangService().getCapitalize("STOCK_MODULE");
	}
	
	

	public StockPanelGUI() {
	
		initGUI();

		btnSave.addActionListener(e ->{
			List<MagicCardStock> updates = model.getItems().stream().filter(MagicCardStock::isUpdate).collect(Collectors.toList());
			AbstractObservableWorker<Void, MagicCardStock,MTGDao> sw = new AbstractObservableWorker<>(lblLoading, MTGControler.getInstance().getEnabled(MTGDao.class),updates.size())
			{
				@Override
				protected void done() {
					super.done();
					model.fireTableDataChanged();
				}

				@Override
				protected Void doInBackground(){
					for (MagicCardStock ms : updates) 
					{
						try {
							plug.saveOrUpdateStock(ms);
							ms.setUpdate(false);
						} catch (Exception e1) {
							logger.error(e1);
						}
					}
					return null;
				}
			};
			
			ThreadManager.getInstance().runInEdt(sw,"Batch stock save");
			
		});

		table.getSelectionModel().addListSelectionListener(event -> {
			if (!multiselection && !event.getValueIsAdjusting()) {
				int viewRow = table.getSelectedRow();
				if (viewRow > -1) {
					MagicCardStock selectedStock = UITools.getTableSelection(table, 0);
					btnDelete.setEnabled(true);
					updatePanels(selectedStock);
				}
			}
		});

		btnDelete.addActionListener(event -> {
			int res = JOptionPane.showConfirmDialog(null,
					MTGControler.getInstance().getLangService().getCapitalize("CONFIRM_DELETE",table.getSelectedRows().length + " item(s)"),
					MTGControler.getInstance().getLangService().getCapitalize("DELETE") + " ?",JOptionPane.YES_NO_OPTION);
			
			if (res == JOptionPane.YES_OPTION) {
				
				List<MagicCardStock> stocks = UITools.getTableSelections(table, 0);
				model.removeItem(stocks);
				AbstractObservableWorker<Void, MagicCardStock, MTGDao> sw = new AbstractObservableWorker<>(lblLoading,MTGControler.getInstance().getEnabled(MTGDao.class),stocks.size()) {
					@Override
					protected Void doInBackground(){
						stocks.removeIf(st->st.getIdstock()==-1);
						if(!stocks.isEmpty())
						{
							try {
								plug.deleteStock(stocks);
								
							} catch (Exception e) {
								logger.error(e);
							}
						}
						
						return null;
					}
					
					@Override
					protected void process(List<MagicCardStock> chunks) {
						super.process(chunks);
						model.removeItem(chunks);
					}

					@Override
					protected void done() {
						super.done();
						model.fireTableDataChanged();
						updateCount();
					}
				};
				ThreadManager.getInstance().runInEdt(sw,"delete stocks");
			}
		});

		btnReload.addActionListener(event -> {
			int res = JOptionPane.showConfirmDialog(null,
					MTGControler.getInstance().getLangService().getCapitalize("CANCEL_CHANGES"),MTGControler.getInstance().getLangService().getCapitalize("CONFIRM_UNDO"),JOptionPane.YES_NO_OPTION);
			if (res == JOptionPane.YES_OPTION)
			{
				logger.debug("reload collection");
				AbstractObservableWorker<Void, MagicCardStock, MTGDao> sw = new AbstractObservableWorker<>(lblLoading, MTGControler.getInstance().getEnabled(MTGDao.class), -1) {
					@Override
					protected Void doInBackground() throws Exception {
						model.init(plug.listStocks());
						return null;
					}
					@Override
					protected void done()
					{
						super.done();
						updateCount();
					}
				};
				ThreadManager.getInstance().runInEdt(sw, "reload stock");

			}

		});

		btnshowMassPanel.addActionListener(event -> rightPanel.setVisible(!rightPanel.isVisible()));

		btnImport.addActionListener(ae -> {
			JPopupMenu menu = new JPopupMenu();

			JMenuItem mnuImportSearch = new JMenuItem(MTGControler.getInstance().getLangService()
					.getCapitalize("IMPORT_FROM", MTGControler.getInstance().getLangService().get("SEARCH_MODULE")));
			mnuImportSearch.setIcon(MTGConstants.ICON_SEARCH);

			mnuImportSearch.addActionListener(importAE -> {
				CardSearchImportDialog cdSearch = new CardSearchImportDialog();
				cdSearch.setVisible(true);
				if (cdSearch.getSelection() != null) {
					for (MagicCard mc : cdSearch.getSelection())
						addCard(mc);
				}
			});
			menu.add(mnuImportSearch);

			for (final MTGCardsExport exp : MTGControler.getInstance().listEnabled(MTGCardsExport.class)) {
				if (exp.getMods() == MODS.BOTH || exp.getMods() == MODS.IMPORT) {

					JMenuItem it = new JMenuItem();
					it.setIcon(exp.getIcon());
					it.setText(exp.getName());
					it.addActionListener(itemEvent -> {
						JFileChooser jf = new JFileChooser(".");
						jf.setFileFilter(new FileFilter() {

							@Override
							public String getDescription() {
								return exp.getName();
							}

							@Override
							public boolean accept(File f) {
								if (f.isDirectory())
									return true;
								return f.getName().endsWith(exp.getFileExtension());
							}
						});

						int res = -1;

						if (!exp.needDialogForStock(MODS.IMPORT) && exp.needFile()) {
							res = jf.showOpenDialog(null);
							fileImport = jf.getSelectedFile();
						} 
						else if(!exp.needFile() && !exp.needDialogForStock(MODS.IMPORT))
						{
							logger.debug(exp + " need no file. Skip");
							res = JFileChooser.APPROVE_OPTION;
						}
						else 
						{
							try {
								res=-1;
								exp.importStockFromFile(null).forEach(this::addStock);
							} catch (IOException e1) {
								logger.error(e1);
							}
						}
						
						if (res == JFileChooser.APPROVE_OPTION)
						{
							AbstractObservableWorker<List<MagicCardStock>, MagicCard, MTGCardsExport> sw = new AbstractObservableWorker<>(lblLoading,exp) 
							{
								@Override
								protected List<MagicCardStock> doInBackground() throws Exception {
									return plug.importStockFromFile(fileImport);
								}

								@Override
								protected void notifyEnd() {
									MTGControler.getInstance().notify(new MTGNotification(
										MTGControler.getInstance().getLangService().combine("IMPORT", "FINISHED"),
										exp.getName() + " "+ MTGControler.getInstance().getLangService().getCapitalize("FINISHED"),
										MESSAGE_TYPE.INFO
									));
								}

								@Override
								protected void done() {
									super.done();
									if(getResult()!=null)
									{
										for (MagicCardStock mc : getResult()) {
											addStock(mc);
										}
										model.fireTableDataChanged();
										updateCount();
									}
								}
							};
							ThreadManager.getInstance().runInEdt(sw,"import stocks from " + fileImport);
							
						}
					});
					menu.add(it);
				}
			}

			Component b = (Component) ae.getSource();
			Point p = b.getLocationOnScreen();
			menu.show(b, 0, 0);
			menu.setLocation(p.x, p.y + b.getHeight());
		});
		
		
		btnExport.initStockExport(new Callable<List<MagicCardStock>>() {
			
			@Override
			public List<MagicCardStock> call() throws Exception {
				
				List<MagicCardStock> export = UITools.getTableSelections(table,0);
				
				if(export.isEmpty())
					return model.getItems();
				else
					return export;
			}
		}, lblLoading);

		
		
		
		btnGeneratePrice.addActionListener(ae -> {
			lblLoading.start(table.getSelectedRows().length);
			
			SwingWorker<Void,MagicCardStock> sw = new SwingWorker<>() {
				
				@Override
				public void done() {
					lblLoading.end();
				}

				@Override
				protected void process(List<MagicCardStock> chunks) {
					lblLoading.progressSmooth(chunks.size());
					model.fireTableDataChanged();
				} 
				
				
				@Override
				protected Void doInBackground(){
					
					for (int i : table.getSelectedRows())
					{
						MagicCardStock s = (MagicCardStock) table.getModel().getValueAt(table.convertRowIndexToModel(i), 0);
						logger.debug("prices for" + s.getMagicCard());
						
						Collection<Double> prices;
						Double price = 0.0;
						try {
							prices = MTGControler.getInstance().getEnabled(MTGDashBoard.class).getPriceVariation(s.getMagicCard(), null).values();
							if (!prices.isEmpty())
								price = (Double) prices.toArray()[prices.size() - 1];
							else
								price = 0.0;
							
							
						} catch (IOException e) {
							logger.error("error getting price for " + s.getMagicCard(),e);
							price = 0.0;
						}
						double old = s.getPrice();
						s.setPrice(price);
						if (old != s.getPrice())
							s.setUpdate(true);
					
						publish(s);
					}
					return null;
				}
				
				
			};
			
			ThreadManager.getInstance().runInEdt(sw, "generate prices for stock");
		});
		
		cboSelections.addItemListener(ie -> {
			multiselection = true;
			if (String.valueOf(cboSelections.getSelectedItem()).equals(selections[1])) {
				table.clearSelection();

				for (int i = 0; i < table.getRowCount(); i++) {
					if (table.getValueAt(i, 0).toString().equals("-1")) {
						table.addRowSelectionInterval(i, i);
					}
				}

			} else if (String.valueOf(cboSelections.getSelectedItem()).equals(selections[2])) {
				table.clearSelection();

				for (int i = 0; i < table.getRowCount(); i++) {
					if (((MagicCardStock) table.getValueAt(i, 0)).isUpdate())
						table.addRowSelectionInterval(i, i);
				}
			}
			else if (String.valueOf(cboSelections.getSelectedItem()).equals(selections[3])) {
				table.clearSelection();
				table.addRowSelectionInterval(0, table.getRowCount()-1);
			}
			multiselection = false;
		});

		btnApplyModification.addActionListener(event -> {
			int res = JOptionPane.showConfirmDialog(null,
					MTGControler.getInstance().getLangService().getCapitalize("CHANGE_X_ITEMS",
							table.getSelectedRowCount()),
					MTGControler.getInstance().getLangService().getCapitalize("CONFIRMATION"),
					JOptionPane.YES_NO_CANCEL_OPTION);
			if (res == JOptionPane.YES_OPTION) {
				
				List<MagicCardStock> list = UITools.getTableSelections(table,0);
				
				for (MagicCardStock  s : list) {
					s.setUpdate(true);
					if (((Integer) spinner.getValue()).intValue() > 0)
						s.setQte((Integer) spinner.getValue());
					if (!textPane.getText().equals(""))
						s.setComment(textPane.getText());
					if (cboAltered.getSelectedItem() != null)
						s.setAltered((Boolean) cboAltered.getSelectedItem());
					if (cboSigned.getSelectedItem() != null)
						s.setSigned((Boolean) cboSigned.getSelectedItem());
					if (cboFoil.getSelectedItem() != null)
						s.setFoil((Boolean) cboFoil.getSelectedItem());
					if (cboLanguages != null)
						s.setLanguage(String.valueOf(cboLanguages.getSelectedItem()));
					if (cboQuality.getSelectedItem() != null)
						s.setCondition((EnumCondition) cboQuality.getSelectedItem());
					if (cboCollection.getSelectedItem() != null)
						s.setMagicCollection((MagicCollection) cboCollection.getSelectedItem());

				}
				model.fireTableDataChanged();
			}
		});

	}

	private void updatePanels(MagicCardStock selectedStock) {
		
		if(selectedStock!=null) {
		magicCardDetailPanel.setMagicCard(selectedStock.getMagicCard());
		historyPricePanel.init(selectedStock.getMagicCard(), null, selectedStock.getMagicCard().getName());
		pricePanel.init(selectedStock.getMagicCard(), selectedStock.getMagicCard().getCurrentSet());
		jsonPanel.show(selectedStock);
		deckPanel.init(selectedStock.getMagicCard());
		gradePanel.setGrading(selectedStock.getGrade());
		gedPanel.init(MagicCardStock.class, selectedStock);
		}
	}

	public void addStock(MagicCardStock mcs) {
		mcs.setIdstock(-1);
		mcs.setUpdate(true);
		model.addItem(mcs);
	}

	public void addCard(MagicCard mc) {
		MagicCardStock ms = MTGControler.getInstance().getDefaultStock();
		ms.setIdstock(-1);
		ms.setUpdate(true);
		ms.setMagicCard(mc);
		model.addItem(ms);

	}


	private void initGUI() {

		JLabel lblSelect;
		JPanel bottomPanel;
		JLabel lblCollection;
		JLabel lblQuality;
		JLabel lblFoil;
		JLabel lblSigned;
		JLabel lblAltered;
		JSplitPane splitPane;
		JLabel lblQte;
		JLabel lblLanguage;
		JLabel lblComment;
		gradePanel = new GradingEditorPane();
		gedPanel = new GedPanel<>();
		JTabbedPane tabPanel = new JTabbedPane();
		setLayout(new BorderLayout(0, 0));

		deckPanel = new CardsDeckCheckerPanel();
		model = new CardStockTableModel();
		magicCardDetailPanel = new MagicCardDetailPanel();
		historyPricePanel = new HistoryPricesPanel(true);
		pricePanel = new PricesTablePanel();
		
		JPanel centerPanel = new JPanel();
		add(centerPanel, BorderLayout.CENTER);
		centerPanel.setLayout(new BorderLayout(0, 0));
		JPanel actionPanel = new JPanel();
		centerPanel.add(actionPanel, BorderLayout.NORTH);
		btnDelete = UITools.createBindableJButton(null, MTGConstants.ICON_DELETE, KeyEvent.VK_D, "stock delete");
		btnDelete.setEnabled(false);
		actionPanel.add(btnDelete);

		btnSave = UITools.createBindableJButton(null, MTGConstants.ICON_SAVE, KeyEvent.VK_S, "stock save");
		btnSave.setToolTipText(MTGControler.getInstance().getLangService().getCapitalize("BATCH_SAVE"));
		actionPanel.add(btnSave);

		btnReload = UITools.createBindableJButton(null, MTGConstants.ICON_REFRESH, KeyEvent.VK_R, "stock reload");
		btnReload.setToolTipText(MTGControler.getInstance().getLangService().getCapitalize("RELOAD"));
		actionPanel.add(btnReload);

		lblLoading = AbstractBuzyIndicatorComponent.createProgressComponent();

		btnshowMassPanel = UITools.createBindableJButton(null, MTGConstants.ICON_MANUAL, KeyEvent.VK_M, "stock mass panel show");

		btnImport = UITools.createBindableJButton(null, MTGConstants.ICON_IMPORT, KeyEvent.VK_I, "stock import");
		btnImport.setToolTipText(MTGControler.getInstance().getLangService().getCapitalize("IMPORT"));
		actionPanel.add(btnImport);

		btnExport = new JExportButton(MODS.EXPORT);
		UITools.bindJButton(btnExport, KeyEvent.VK_E, "stock export");
		btnExport.setToolTipText(MTGControler.getInstance().getLangService().getCapitalize("EXPORT"));
		actionPanel.add(btnExport);

		btnGeneratePrice = UITools.createBindableJButton(null, MTGConstants.ICON_EURO, KeyEvent.VK_E, "stock price suggestion");
		jsonPanel = new ObjectViewerPanel();
		btnGeneratePrice.setToolTipText(MTGControler.getInstance().getLangService().getCapitalize("GENERATE_PRICE"));
		actionPanel.add(btnGeneratePrice);
		btnshowMassPanel.setToolTipText(MTGControler.getInstance().getLangService().getCapitalize("MASS_MODIFICATION"));
		
		btnApplyModification = UITools.createBindableJButton(MTGControler.getInstance().getLangService().getCapitalize("APPLY"), MTGConstants.ICON_CHECK, KeyEvent.VK_A, "stock apply");

		
		actionPanel.add(btnshowMassPanel);
		actionPanel.add(lblLoading);

		table = new JXTable(model);
		StockTableRenderer render = new StockTableRenderer();

		table.setDefaultRenderer(Object.class, render);
		table.setDefaultRenderer(Boolean.class, render);
		table.setDefaultRenderer(Double.class, render);
		table.setDefaultRenderer(MagicEdition.class, new MagicEditionJLabelRenderer());
		table.setDefaultEditor(EnumCondition.class, new ComboBoxEditor<>(EnumCondition.values()));
		table.setDefaultEditor(Integer.class, new IntegerCellEditor());
		try {
			table.setDefaultEditor(MagicCollection.class, new ComboBoxEditor<>(MTGControler.getInstance().getEnabled(MTGDao.class).listCollections()));
		} catch (SQLException e1) {
			logger.error(e1);
		}
		
		
		table.setRowHeight(MTGConstants.TABLE_ROW_HEIGHT);
		
		
		table.packAll();
		UITools.initTableFilter(table);

		magicCardDetailPanel.enableThumbnail(true);

		splitPane = new JSplitPane();
		splitPane.setDividerLocation(0.5);
		splitPane.setResizeWeight(0.5);

		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		centerPanel.add(splitPane, BorderLayout.CENTER);
		splitPane.setLeftComponent(new JScrollPane(table));
		
		splitPane.setRightComponent(tabPanel);

		
		tabPanel.addTab(MTGControler.getInstance().getLangService().get("DETAILS"),MTGConstants.ICON_TAB_DETAILS, magicCardDetailPanel);
		tabPanel.addTab(MTGControler.getInstance().getLangService().getCapitalize("GRADING"), MTGConstants.ICON_TAB_GRADING,gradePanel);
		tabPanel.addTab(MTGControler.getInstance().getLangService().get("PRICES"),MTGConstants.ICON_TAB_PRICES, pricePanel);
		tabPanel.addTab(MTGControler.getInstance().getLangService().get("PRICE_VARIATIONS"),MTGConstants.ICON_TAB_VARIATIONS,historyPricePanel);
		tabPanel.addTab(MTGControler.getInstance().getLangService().getCapitalize("DECK_MODULE"), MTGConstants.ICON_TAB_DECK,deckPanel);
		tabPanel.addTab(MTGControler.getInstance().getLangService().getCapitalize("GED"), MTGConstants.ICON_TAB_GED,gedPanel);
		
		
		
		if (MTGControler.getInstance().get("debug-json-panel").equalsIgnoreCase("true"))
			tabPanel.addTab("Object", MTGConstants.ICON_TAB_JSON, jsonPanel, null);

		
		rightPanel = new JPanel();
		rightPanel.setBackground(SystemColor.inactiveCaption);
		rightPanel.setVisible(false);
		add(rightPanel, BorderLayout.EAST);
		GridBagLayout gblrightPanel = new GridBagLayout();
		gblrightPanel.columnWidths = new int[] { 84, 103, 0 };
		gblrightPanel.rowHeights = new int[] { 83, 56, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		gblrightPanel.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		gblrightPanel.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
				Double.MIN_VALUE };
		rightPanel.setLayout(gblrightPanel);

		lblSelect = new JLabel("Select :");
		GridBagConstraints gbclblSelect = new GridBagConstraints();
		gbclblSelect.anchor = GridBagConstraints.NORTHEAST;
		gbclblSelect.insets = new Insets(0, 0, 5, 5);
		gbclblSelect.gridx = 0;
		gbclblSelect.gridy = 1;
		rightPanel.add(lblSelect, gbclblSelect);

		cboSelections = UITools.createCombobox(selections);
		GridBagConstraints gbccomboBox = new GridBagConstraints();
		gbccomboBox.anchor = GridBagConstraints.NORTH;
		gbccomboBox.insets = new Insets(0, 0, 5, 0);
		gbccomboBox.fill = GridBagConstraints.HORIZONTAL;
		gbccomboBox.gridx = 1;
		gbccomboBox.gridy = 1;
		rightPanel.add(cboSelections, gbccomboBox);

		lblQte = new JLabel(MTGControler.getInstance().getLangService().getCapitalize("QTY") + " :");
		GridBagConstraints gbclblQte = new GridBagConstraints();
		gbclblQte.anchor = GridBagConstraints.EAST;
		gbclblQte.insets = new Insets(0, 0, 5, 5);
		gbclblQte.gridx = 0;
		gbclblQte.gridy = 2;
		rightPanel.add(lblQte, gbclblQte);

		spinner = new JSpinner();
		spinner.setModel(new SpinnerNumberModel(0, 0, null, 1));
		GridBagConstraints gbcspinner = new GridBagConstraints();
		gbcspinner.fill = GridBagConstraints.HORIZONTAL;
		gbcspinner.insets = new Insets(0, 0, 5, 0);
		gbcspinner.gridx = 1;
		gbcspinner.gridy = 2;
		rightPanel.add(spinner, gbcspinner);

		lblLanguage = new JLabel(MTGControler.getInstance().getLangService().getCapitalize("CARD_LANGUAGE") + " :");
		GridBagConstraints gbclblLanguage = new GridBagConstraints();
		gbclblLanguage.anchor = GridBagConstraints.EAST;
		gbclblLanguage.insets = new Insets(0, 0, 5, 5);
		gbclblLanguage.gridx = 0;
		gbclblLanguage.gridy = 3;
		rightPanel.add(lblLanguage, gbclblLanguage);

		DefaultComboBoxModel<String> lModel = new DefaultComboBoxModel<>();
		lModel.addElement(null);
		for (Locale l : Locale.getAvailableLocales())
			lModel.addElement(l.getDisplayLanguage(Locale.US));

		cboLanguages = new JComboBox<>(lModel);
		GridBagConstraints gbccboLanguages = new GridBagConstraints();
		gbccboLanguages.insets = new Insets(0, 0, 5, 0);
		gbccboLanguages.fill = GridBagConstraints.HORIZONTAL;
		gbccboLanguages.gridx = 1;
		gbccboLanguages.gridy = 3;
		rightPanel.add(cboLanguages, gbccboLanguages);

		lblFoil = new JLabel(MTGControler.getInstance().getLangService().getCapitalize("FOIL") + " :");
		GridBagConstraints gbclblFoil = new GridBagConstraints();
		gbclblFoil.anchor = GridBagConstraints.EAST;
		gbclblFoil.insets = new Insets(0, 0, 5, 5);
		gbclblFoil.gridx = 0;
		gbclblFoil.gridy = 4;
		rightPanel.add(lblFoil, gbclblFoil);

		cboFoil = UITools.createCombobox(values);
		GridBagConstraints gbccboFoil = new GridBagConstraints();
		gbccboFoil.insets = new Insets(0, 0, 5, 0);
		gbccboFoil.fill = GridBagConstraints.HORIZONTAL;
		gbccboFoil.gridx = 1;
		gbccboFoil.gridy = 4;
		rightPanel.add(cboFoil, gbccboFoil);

		lblSigned = new JLabel(MTGControler.getInstance().getLangService().getCapitalize("SIGNED") + " :");
		GridBagConstraints gbclblSigned = new GridBagConstraints();
		gbclblSigned.anchor = GridBagConstraints.EAST;
		gbclblSigned.insets = new Insets(0, 0, 5, 5);
		gbclblSigned.gridx = 0;
		gbclblSigned.gridy = 5;
		rightPanel.add(lblSigned, gbclblSigned);

		cboSigned = UITools.createCombobox(values);
		GridBagConstraints gbccboSigned = new GridBagConstraints();
		gbccboSigned.insets = new Insets(0, 0, 5, 0);
		gbccboSigned.fill = GridBagConstraints.HORIZONTAL;
		gbccboSigned.gridx = 1;
		gbccboSigned.gridy = 5;
		rightPanel.add(cboSigned, gbccboSigned);

		lblAltered = new JLabel(MTGControler.getInstance().getLangService().getCapitalize("ALTERED") + " :");
		GridBagConstraints gbclblAltered = new GridBagConstraints();
		gbclblAltered.anchor = GridBagConstraints.EAST;
		gbclblAltered.insets = new Insets(0, 0, 5, 5);
		gbclblAltered.gridx = 0;
		gbclblAltered.gridy = 6;
		rightPanel.add(lblAltered, gbclblAltered);

		cboAltered = UITools.createCombobox(values);
		GridBagConstraints gbccboAltered = new GridBagConstraints();
		gbccboAltered.insets = new Insets(0, 0, 5, 0);
		gbccboAltered.fill = GridBagConstraints.HORIZONTAL;
		gbccboAltered.gridx = 1;
		gbccboAltered.gridy = 6;
		rightPanel.add(cboAltered, gbccboAltered);

		lblQuality = new JLabel(MTGControler.getInstance().getLangService().getCapitalize("QUALITY") + " :");
		GridBagConstraints gbclblQuality = new GridBagConstraints();
		gbclblQuality.anchor = GridBagConstraints.EAST;
		gbclblQuality.insets = new Insets(0, 0, 5, 5);
		gbclblQuality.gridx = 0;
		gbclblQuality.gridy = 7;
		rightPanel.add(lblQuality, gbclblQuality);

		cboQuality = UITools.createCombobox(EnumCondition.values());
		GridBagConstraints gbccboQuality = new GridBagConstraints();
		gbccboQuality.insets = new Insets(0, 0, 5, 0);
		gbccboQuality.fill = GridBagConstraints.HORIZONTAL;
		gbccboQuality.gridx = 1;
		gbccboQuality.gridy = 7;
		rightPanel.add(cboQuality, gbccboQuality);

		lblCollection = new JLabel(MTGControler.getInstance().getLangService().getCapitalize("COLLECTION") + " :");
		GridBagConstraints gbclblCollection = new GridBagConstraints();
		gbclblCollection.anchor = GridBagConstraints.EAST;
		gbclblCollection.insets = new Insets(0, 0, 5, 5);
		gbclblCollection.gridx = 0;
		gbclblCollection.gridy = 8;
		rightPanel.add(lblCollection, gbclblCollection);

		cboCollection = UITools.createComboboxCollection();
		GridBagConstraints gbccboCollection = new GridBagConstraints();
		gbccboCollection.insets = new Insets(0, 0, 5, 0);
		gbccboCollection.fill = GridBagConstraints.HORIZONTAL;
		gbccboCollection.gridx = 1;
		gbccboCollection.gridy = 8;
		rightPanel.add(cboCollection, gbccboCollection);

		lblComment = new JLabel("Comment :");
		GridBagConstraints gbclblComment = new GridBagConstraints();
		gbclblComment.insets = new Insets(0, 0, 5, 5);
		gbclblComment.gridx = 0;
		gbclblComment.gridy = 9;
		rightPanel.add(lblComment, gbclblComment);

		textPane = new JTextPane();
		GridBagConstraints gbctextPane = new GridBagConstraints();
		gbctextPane.insets = new Insets(0, 0, 5, 0);
		gbctextPane.gridwidth = 2;
		gbctextPane.gridheight = 3;
		gbctextPane.fill = GridBagConstraints.BOTH;
		gbctextPane.gridx = 0;
		gbctextPane.gridy = 10;
		rightPanel.add(textPane, gbctextPane);

	
		
		
		GridBagConstraints gbcbtnApplyModification = new GridBagConstraints();
		gbcbtnApplyModification.gridwidth = 2;
		gbcbtnApplyModification.gridx = 0;
		gbcbtnApplyModification.gridy = 13;
		rightPanel.add(btnApplyModification, gbcbtnApplyModification);

		bottomPanel = new JPanel();
		add(bottomPanel, BorderLayout.SOUTH);

		lblCount = new JLabel();
		bottomPanel.add(lblCount);

		
		gradePanel.getBtnSave().addActionListener(al->{
			try{
				MagicCardStock st = UITools.getTableSelection(table, 0);
				gradePanel.saveTo(st);
				model.fireTableDataChanged();
			}
			catch(Exception e)
			{
				MTGControler.getInstance().notify(new MTGNotification("ERROR", "Choose a stock", MESSAGE_TYPE.ERROR));
			}
		});
		
	}
	
	@Override
	public void onFirstShowing() {
		
		ThreadManager.getInstance().executeThread(() -> {
			try {
				lblLoading.start();
				model.init(MTGControler.getInstance().getEnabled(MTGDao.class).listStocks());
			} catch (SQLException e1) {
				MTGControler.getInstance().notify(e1);
			}
			lblLoading.end();
			updateCount();

		}, "init stock");

	}
	

	public void updateCount() {
		lblCount.setText(MTGControler.getInstance().getLangService().getCapitalize("ITEMS_IN_STOCK") + ": "+ table.getRowCount());
	}

}
