package org.magic.gui.components;

import static org.magic.tools.MTG.capitalize;
import static org.magic.tools.MTG.getEnabledPlugin;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.jdesktop.swingx.painter.MattePainter;
import org.magic.api.beans.MTGNotification;
import org.magic.api.beans.MTGNotification.MESSAGE_TYPE;
import org.magic.api.beans.MagicCollection;
import org.magic.api.beans.MagicEdition;
import org.magic.api.interfaces.MTGCardsIndexer;
import org.magic.api.interfaces.MTGCardsProvider;
import org.magic.api.interfaces.MTGDao;
import org.magic.api.interfaces.MTGPictureProvider;
import org.magic.api.interfaces.MTGPicturesCache;
import org.magic.api.interfaces.MTGServer;
import org.magic.api.interfaces.abstracts.AbstractWebServer;
import org.magic.game.gui.components.GamePanelGUI;
import org.magic.gui.abstracts.AbstractBuzyIndicatorComponent;
import org.magic.gui.abstracts.MTGUIComponent;
import org.magic.gui.components.dialog.DefaultStockEditorDialog;
import org.magic.services.MTGConstants;
import org.magic.services.MTGControler;
import org.magic.services.MTGLogger;
import org.magic.services.providers.IconSetProvider;
import org.magic.services.providers.PackagesProvider;
import org.magic.services.threads.ThreadManager;
import org.magic.tools.FileTools;
import org.magic.tools.ImageTools;
import org.magic.tools.InstallCert;
import org.magic.tools.MTG;
import org.magic.tools.UITools;

public class ConfigurationPanel extends JXTaskPaneContainer {

	private static final String EXPORT = "EXPORT";
	private static final String LANGAGE = "langage";
	private static final String DEFAULT_LIBRARY = "default-library";
	private static final String CURRENCY = "currency";
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JTextFieldFileChooser txtDAOBackup;
	private JComboBox<MTGDao> cboTargetDAO;
	private JComboBox<MagicCollection> cboCollections;
	private JComboBox<Level> cboLogLevels;
	private JTextFieldFileChooser txtdirWebsite;
	private JComboBox<MagicEdition> cboEditionLands;
	private JTextField txtMinPrice;
	private JCheckBox cbojsonView;
	private JTextField txtWebSiteCertificate;
	private JCheckBox chkToolTip;
	private AbstractBuzyIndicatorComponent lblLoading;
	private JTextField txtName;
	private JLabel lblIconAvatar;
	private JCheckBox chckbxIconset;
	private JCheckBox chckbxIconcards;
	private JCheckBox chckbxSearch;
	private JCheckBox chckbxCollection;
	private JCheckBox chckbxDashboard;
	private JCheckBox chckbxGame;
	private JCheckBox chckbxDeckBuilder;
	private JCheckBox chckbxShopper;
	private JCheckBox chckbxAlert;
	private JCheckBox chckbxRss;
	private JCheckBox chckbxCardBuilder;
	private JCheckBox chckbxStock;
	private JLabel dateCurrencyCache;
	private JResizerPanel resizerPanel;
	private transient Logger logger = MTGLogger.getLogger(this.getClass());
	private JCheckBox chckbxWallpaper;
	private JCheckBox chckbxHistory;
	private JCheckBox chckbxPackages;
	private JCheckBox chckbxSealed;
	private JCheckBox chckbxEvents;
	private JComboBox<MTGServer> cboServers;
	private JCheckBox chckbxShopping;
	public void loading(boolean show, String text) {
		if (show) {
			lblLoading.start();
			lblLoading.setText(text);
		} else {
			lblLoading.end();
		}
	}
	
	private JPanel createBoxPanel(String keyName, Icon ic, GridBagLayout layout,boolean collapsed)
	{
		JXTaskPane pane = new JXTaskPane();
		pane.setTitle(capitalize(keyName));
		pane.setIcon(ic);
		pane.setCollapsed(collapsed);
		pane.setLayout(layout);
		return pane;
	}
	
	
	
	public ConfigurationPanel() {
		lblLoading = AbstractBuzyIndicatorComponent.createLabelComponent();

		setBackgroundPainter(new MattePainter(MTGConstants.PICTURE_PAINTER, true));
		
/////////////CONFIG PANEL BOX		
		
	
		GridBagLayout daoPanelLayout = new GridBagLayout();
					daoPanelLayout.columnWidths = new int[] { 172, 130, 0, 0 };
					daoPanelLayout.rowHeights = new int[] { 0, 0, 0, 0, 0, 0 };
					daoPanelLayout.columnWeights = new double[] { 0.0, 1.0, 0.0, Double.MIN_VALUE };
					daoPanelLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };			
		
		GridBagLayout configPanelLayout = new GridBagLayout();
					configPanelLayout.columnWidths = new int[] { 0, 0, 0, 0 };
					configPanelLayout.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0 };
					configPanelLayout.columnWeights = new double[] { 0.0, 1.0, 0.0, Double.MIN_VALUE };
					configPanelLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
	
		GridBagLayout websitePanelLayout = new GridBagLayout();
					websitePanelLayout.columnWidths = new int[] { 0, 0, 0, 0, 0 };
					websitePanelLayout.rowHeights = new int[] { 0, 0, 0 };
					websitePanelLayout.columnWeights = new double[] { 0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE };
					websitePanelLayout.rowWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
		
		GridBagLayout gameProfilPanelLayout = new GridBagLayout();
					gameProfilPanelLayout.columnWidths = new int[] { 0, 71, 0, 0 };
					gameProfilPanelLayout.rowHeights = new int[] { 0, 0, 29, 0, 0 };
					gameProfilPanelLayout.columnWeights = new double[] { 0.0, 0.0, 1.0, Double.MIN_VALUE };
					gameProfilPanelLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		
		GridBagLayout modulesPanelLayout = new GridBagLayout();
					modulesPanelLayout.columnWidths = new int[] { 0, 0, 0, 103, 0, 121, 0, 0 };
					modulesPanelLayout.rowHeights = new int[] { 0, 0, 0, 0,  };
					modulesPanelLayout.columnWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
					modulesPanelLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, Double.MIN_VALUE };
		
		GridBagLayout currencyPanelLayout = new GridBagLayout();
					currencyPanelLayout.columnWidths = new int[] { 106, 67, 0, 0 };
					currencyPanelLayout.rowHeights = new int[] { 23, 0, 0, 0 };
					currencyPanelLayout.columnWeights = new double[] { 0.0, 1.0, 0.0, Double.MIN_VALUE };
					currencyPanelLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, Double.MIN_VALUE };
		
		GridBagLayout guiPanelLayout = new GridBagLayout();
					guiPanelLayout.columnWidths = new int[] { 188, 38, 0, 0 };
					guiPanelLayout.rowHeights = new int[] { 23, 0, 0, 0, 0, 0, 0, 0 };
					guiPanelLayout.columnWeights = new double[] { 0.0, 1.0, 0.0, 0.0 };
					guiPanelLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
	
		JPanel panelDAO = createBoxPanel("DATABASES",MTGConstants.ICON_TAB_DAO,daoPanelLayout,true);
		JPanel panelConfig = createBoxPanel("CONFIGURATION",MTGConstants.ICON_TAB_ADMIN,configPanelLayout,false);
		JPanel panelWebSite = createBoxPanel("WEBSITE",MTGConstants.ICON_WEBSITE,websitePanelLayout,true);
		JPanel panelGameProfil = createBoxPanel("GAME",MTGConstants.ICON_TAB_GAME,gameProfilPanelLayout,true);
		JPanel panelModule = createBoxPanel("Modules",MTGConstants.ICON_TAB_PLUGIN,modulesPanelLayout,true);
		JPanel panelCurrency = createBoxPanel("CURRENCY",MTGConstants.ICON_TAB_PRICES,currencyPanelLayout,true);
		JPanel panelGUI = createBoxPanel("GUI",MTGConstants.ICON_TAB_PICTURE,guiPanelLayout,true);
		
		add(panelConfig);
		add(panelGUI);
		add(panelModule);
		add(panelDAO);
		add(panelWebSite);
		add(panelGameProfil);
		add(panelCurrency);
		
		
		GridBagConstraints gbclblLoading = UITools.createGridBagConstraints(null, GridBagConstraints.BOTH,  0, 4);
		gbclblLoading.gridwidth = 2;
		add(lblLoading,gbclblLoading);
		
		
/////////////DAO BOX		
		JLabel lblBackupDao = new JLabel(capitalize("DAO_BACKUP") + " : ");
		txtDAOBackup = new JTextFieldFileChooser(10,JFileChooser.FILES_AND_DIRECTORIES,MTGConstants.DATA_DIR.getAbsolutePath());
		JButton btnBackup = new JButton(capitalize(EXPORT));
		JLabel lblDuplicateDb = new JLabel(capitalize("DUPLICATE_TO",getEnabledPlugin(MTGDao.class)));
		JButton btnDuplicate = new JButton((capitalize(EXPORT)));
		JLabel lblLocation = new JLabel(capitalize("LOCATION") + " : ");
		JLabel lbldbLocationValue = new JLabel(getEnabledPlugin(MTGDao.class).getDBLocation());
		JLabel lblSize = new JLabel(capitalize("SIZE") + " : ");
		JLabel lblSizeValue  = new JLabel(String.valueOf(getEnabledPlugin(MTGDao.class).getDBSize() / 1024 / 1024) + "MB");
		JLabel lblIndexation = new JLabel("Indexation : ");
		JLabel lblIndexSize = new JLabel(UITools.formatDate(getEnabledPlugin(MTGCardsIndexer.class).getIndexDate()));
		JButton btnIndexation = new JButton("Reindexation");
		cboTargetDAO = UITools.createCombobox(MTGDao.class, true);
		cboTargetDAO.removeItem(getEnabledPlugin(MTGDao.class));

		
		
		panelDAO.add(lblBackupDao, UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  0, 0));
		panelDAO.add(txtDAOBackup, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  1, 0));
		panelDAO.add(btnBackup, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL, 2, 0));
		panelDAO.add(lblDuplicateDb, UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  0, 1));
		panelDAO.add(cboTargetDAO, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  1, 1));
		panelDAO.add(btnDuplicate, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL, 2, 1));
		panelDAO.add(lblLocation, UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  0, 2));
		panelDAO.add(lbldbLocationValue, UITools.createGridBagConstraints(null, null,  1, 2));
		panelDAO.add(lblSize, UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  0, 3));
		panelDAO.add(lblSizeValue, UITools.createGridBagConstraints(null, null,  1, 3));
		panelDAO.add(lblIndexation, UITools.createGridBagConstraints(GridBagConstraints.WEST, null, 0, 4));
		panelDAO.add(lblIndexSize, UITools.createGridBagConstraints(null, null, 1, 4));
		panelDAO.add(btnIndexation, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL, 2, 4));
		
		
/////////////CONFIG BOX
		cboCollections = UITools.createComboboxCollection();
		cboLogLevels = UITools.createCombobox(MTGLogger.getLevels());
		txtMinPrice = new JTextField(MTGControler.getInstance().get("min-price-alert"),25);
		JButton btnSavePrice = new JButton(capitalize("SAVE"));
		JLabel lblCleancache = new JLabel(capitalize("CLEAN_CACHE") + " :");
		JButton btnClean = new JButton(capitalize("CLEAN"));
		JPanel panelCheckCache = new JPanel();
		JLabel lblAutoStock = new JLabel(capitalize("AUTO_STOCK") + ": ");
		JCheckBox chkboxAutoAdd = new JCheckBox(capitalize("AUTO_STOCK_ADD"));
		chckbxIconset = new JCheckBox(capitalize("IMG_SET"));
		chckbxIconcards = new JCheckBox(capitalize("IMG_CARD"));
		chckbxPackages = new JCheckBox(capitalize("PACKAGES"));
		JCheckBox chkboxAutoDelete = new JCheckBox(capitalize("AUTO_STOCK_DELETE"));
		JButton btnDefaultStock = new JButton("Default Stock");
		JCheckBox chkboxPrerelease = new JCheckBox();		
	
		
		JPanel panelAutoStock = new JPanel();
		cboEditionLands = UITools.createComboboxEditions();
		
		JPanel panelBtnConfigBackup = new JPanel();
		JButton btnExportConfig = new JButton(capitalize(EXPORT));
		JButton btnImportConfig = new JButton(capitalize("IMPORT"));
		
		
		((FlowLayout) panelAutoStock.getLayout()).setAlignment(FlowLayout.LEFT);
		
		
		chckbxIconset.setSelected(true);
		chckbxIconcards.setSelected(true);
		chckbxPackages.setSelected(true);

		chkboxPrerelease.getModel().setSelected( MTGControler.getInstance().get("notifyPrerelease").equals("true"));
		chkboxAutoAdd.setSelected(MTGControler.getInstance().get("collections/stockAutoAdd").equals("true"));
		chkboxAutoDelete.setSelected(MTGControler.getInstance().get("collections/stockAutoDelete").equals("true"));
		
		
		panelCheckCache.add(chckbxIconset);
		panelCheckCache.add(chckbxIconcards);
		panelCheckCache.add(chckbxPackages);
		
		panelAutoStock.add(chkboxAutoAdd);
		panelAutoStock.add(chkboxAutoDelete);
	
		panelBtnConfigBackup.add(btnExportConfig);
		panelBtnConfigBackup.add(btnImportConfig);
		
		panelConfig.add(new JLabel(capitalize("MAIN_COLLECTION") + " :"), UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  0, 0));
		panelConfig.add(cboCollections, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  1, 0));
		panelConfig.add(new JLabel(capitalize("DEFAULT_LAND_IMPORT") + " :"), UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  0, 1));
		panelConfig.add(cboEditionLands, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  1, 1));
		panelConfig.add( new JLabel(capitalize("LOG_LEVEL") + " :"), UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  0, 2));
		panelConfig.add(cboLogLevels, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  1, 2));
		panelConfig.add(new JLabel(capitalize("SHOW_LOW_PRICES") + " :"), UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  0, 3));
		panelConfig.add(txtMinPrice, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  1, 3));
		panelConfig.add(btnSavePrice, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL, 2, 3));
		panelConfig.add(lblCleancache, UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  0, 4));
		panelConfig.add(panelCheckCache, UITools.createGridBagConstraints(GridBagConstraints.WEST, GridBagConstraints.VERTICAL,  1, 4));
		panelConfig.add(btnClean, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL, 2, 4));
		panelConfig.add(lblAutoStock, UITools.createGridBagConstraints(GridBagConstraints.WEST, null, 0, 5));
		panelConfig.add(panelAutoStock, UITools.createGridBagConstraints(null, GridBagConstraints.BOTH, 1, 5));
		panelConfig.add(btnDefaultStock, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL, 2, 5));
		panelConfig.add(new JLabel(capitalize("CONFIG_BACKUP")+" :"), UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL, 0, 6));
		panelConfig.add(panelBtnConfigBackup, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL, 1, 6));

		panelConfig.add(new JLabel(capitalize("UPDATE_PRERELEASE")+" :"), UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL, 0, 7));
		panelConfig.add(chkboxPrerelease, UITools.createGridBagConstraints(null, GridBagConstraints.WEST, 1, 7));
		
		

		

/////////////WEBSITE BOX		
	
		txtdirWebsite = new JTextFieldFileChooser(10,JFileChooser.DIRECTORIES_ONLY,MTGControler.getInstance().get("default-website-dir"));
		JTextFieldFileChooser txtdirWebsserver = new JTextFieldFileChooser(10,JFileChooser.DIRECTORIES_ONLY);
		JButton btnWebsiteSave = new JButton(capitalize("SAVE"));
		JButton btnWebServerExport = new JButton(capitalize(EXPORT));
		
		txtWebSiteCertificate = new JTextField("www.",10);
		
		JButton btnAdd = new JButton(capitalize("SAVE"));
		cboServers = UITools.createCombobox(MTG.listEnabledPlugins(MTGServer.class).stream().filter(s->s instanceof AbstractWebServer).collect(Collectors.toList()));
		
		panelWebSite.add(new JLabel(capitalize("DIRECTORY") + " :"), UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  0, 0));
		panelWebSite.add(txtdirWebsite, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  1, 0));
		panelWebSite.add(btnWebsiteSave, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  3, 0));
		panelWebSite.add(new JLabel(capitalize("ADD_CERTIFICATE") + " :"), UITools.createGridBagConstraints(null, null,  0, 1));
		panelWebSite.add(txtWebSiteCertificate, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  1, 1));
		panelWebSite.add(btnAdd, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  3, 1));

		panelWebSite.add(new JLabel(capitalize("WEB_SERVER_UI") + " :"), UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  0, 2));
		panelWebSite.add(cboServers, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  2, 2));
		panelWebSite.add(txtdirWebsserver, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  1, 2));
		panelWebSite.add(btnWebServerExport, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  3, 2));
		
		
		
		
/////////////PROFIL BOX		
		
		JLabel lblName = new JLabel(capitalize("NAME") + " :");
		txtName = new JTextField(MTGControler.getInstance().get("/game/player-profil/name"),10);
		JLabel lblAvatar = new JLabel("Avatar :");
		lblIconAvatar = new JLabel();
		lblIconAvatar.setBorder(new LineBorder(Color.RED, 1, true));
		JResizerPanel gamePicsResizerPanel = new JResizerPanel(MTGControler.getInstance().getCardsGameDimension());
		JButton btnSaveProfilGame = new JButton(capitalize("SAVE"));
	
		panelGameProfil.add(lblName, UITools.createGridBagConstraints(GridBagConstraints.EAST, null,  0, 0));
		panelGameProfil.add(txtName, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  1, 0));
		panelGameProfil.add(lblAvatar, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  0, 1));
		panelGameProfil.add(lblIconAvatar, UITools.createGridBagConstraints(null, GridBagConstraints.BOTH,  1, 1));
		panelGameProfil.add(gamePicsResizerPanel, UITools.createGridBagConstraints(null, GridBagConstraints.BOTH,  2, 0));
		panelGameProfil.add(btnSaveProfilGame,UITools.createGridBagConstraints(null, GridBagConstraints.BOTH,  3, 0));
		
		loadIcon();

/////////////MODULES BOX				

		chckbxSearch = new JCheckBox(capitalize("SEARCH_MODULE"));
		chckbxCollection = new JCheckBox("Collection");
		chckbxDashboard = new JCheckBox(capitalize("DASHBOARD_MODULE"));
		chckbxShopper = new JCheckBox(capitalize("FINANCIAL_MODULE"));
		chckbxCardBuilder = new JCheckBox(capitalize("BUILDER_MODULE"));
		chckbxHistory = new JCheckBox(capitalize("HISTORY_MODULE"));
		chckbxDashboard.setSelected(MTGControler.getInstance().get("modules/dashboard").equals("true"));
		chckbxSearch.setSelected(MTGControler.getInstance().get("modules/search").equals("true"));
		chckbxCollection.setSelected(MTGControler.getInstance().get("modules/collection").equals("true"));
		chckbxStock = new JCheckBox(capitalize("STOCK_MODULE"));
		chckbxAlert = new JCheckBox(capitalize("ALERT_MODULE"));
		chckbxGame = new JCheckBox(capitalize("GAME_MODULE"));
		chckbxDeckBuilder = new JCheckBox(capitalize("DECK_MODULE"));
		chckbxRss = new JCheckBox(capitalize("RSS_MODULE"));
		chckbxWallpaper = new JCheckBox(capitalize("WALLPAPER"));
		chckbxSealed = new JCheckBox(capitalize("PACKAGES"));
		chckbxEvents = new JCheckBox(capitalize("EVENTS"));
		chckbxShopping = new JCheckBox(capitalize("SHOP"));
		
		chckbxStock.setSelected(MTGControler.getInstance().get("modules/stock").equals("true"));
		chckbxAlert.setSelected(MTGControler.getInstance().get("modules/alarm").equals("true"));
		chckbxGame.setSelected(MTGControler.getInstance().get("modules/game").equals("true"));
		chckbxDeckBuilder.setSelected(MTGControler.getInstance().get("modules/deckbuilder").equals("true"));
		chckbxRss.setSelected(MTGControler.getInstance().get("modules/rss").equals("true"));
		chckbxWallpaper.setSelected(MTGControler.getInstance().get("modules/wallpaper").equals("true"));
		chckbxShopper.setSelected(MTGControler.getInstance().get("modules/shopper").equals("true"));
		chckbxHistory.setSelected(MTGControler.getInstance().get("modules/history").equals("true"));
		chckbxCardBuilder.setSelected(MTGControler.getInstance().get("modules/cardbuilder").equals("true"));
		chckbxSealed.setSelected(MTGControler.getInstance().get("modules/sealed").equals("true"));
		chckbxEvents.setSelected(MTGControler.getInstance().get("modules/event").equals("true"));
		chckbxShopping.setSelected(MTGControler.getInstance().get("modules/webshop").equals("true"));
		
		chckbxDashboard.addItemListener(ie -> MTGControler.getInstance().setProperty("modules/dashboard", chckbxDashboard.isSelected()));
		chckbxStock.addItemListener(ie -> MTGControler.getInstance().setProperty("modules/stock", chckbxStock.isSelected()));
		chckbxAlert.addItemListener(ie -> MTGControler.getInstance().setProperty("modules/alarm", chckbxAlert.isSelected()));
		chckbxGame.addItemListener(ie -> MTGControler.getInstance().setProperty("modules/game", chckbxGame.isSelected()));
		chckbxDeckBuilder.addItemListener(ie -> MTGControler.getInstance().setProperty("modules/deckbuilder", chckbxDeckBuilder.isSelected()));
		chckbxRss.addItemListener(ie -> MTGControler.getInstance().setProperty("modules/rss", chckbxRss.isSelected()));
		chckbxWallpaper.addItemListener(ie -> MTGControler.getInstance().setProperty("modules/wallpaper", chckbxWallpaper.isSelected()));
		chckbxShopper.addItemListener(ie -> MTGControler.getInstance().setProperty("modules/shopper", chckbxShopper.isSelected()));
		chckbxHistory.addItemListener(ie -> MTGControler.getInstance().setProperty("modules/history", chckbxHistory.isSelected()));
		chckbxCardBuilder.addItemListener(ie -> MTGControler.getInstance().setProperty("modules/cardbuilder", chckbxCardBuilder.isSelected()));
		chckbxCollection.addItemListener(ie -> MTGControler.getInstance().setProperty("modules/collection", chckbxCollection.isSelected()));
		chckbxSearch.addItemListener(ie -> MTGControler.getInstance().setProperty("modules/search", chckbxSearch.isSelected()));
		chckbxSealed.addItemListener(ie -> MTGControler.getInstance().setProperty("modules/sealed",chckbxSealed.isSelected()));
		chckbxEvents.addItemListener(ie -> MTGControler.getInstance().setProperty("modules/event",chckbxEvents.isSelected()));
		chckbxShopping.addItemListener(ie -> MTGControler.getInstance().setProperty("modules/webshop",chckbxShopping.isSelected()));
		
		
		
		panelModule.add(chckbxSearch, UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  1, 0));
		panelModule.add(chckbxCollection, UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  3, 0));
		panelModule.add(chckbxStock, UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  5, 0));
		panelModule.add(chckbxAlert, UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  6, 0));
		panelModule.add(chckbxDashboard, UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  1, 1));
		panelModule.add(chckbxGame, UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  3, 1));
		panelModule.add(chckbxRss, UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  5, 1));
		panelModule.add(chckbxWallpaper, UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  6, 1));
		panelModule.add(chckbxDeckBuilder, UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  1, 2));
		panelModule.add(chckbxShopper, UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  3, 2));
		panelModule.add(chckbxHistory, UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  5, 2));
		panelModule.add(chckbxCardBuilder, UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  6, 2));
		panelModule.add(chckbxSealed, UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  1, 3));
		panelModule.add(chckbxEvents, UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  3, 3));
		panelModule.add(chckbxShopping, UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  5, 3));
		

	
/////////////CURRENCY BOX				

		JLabel lblCurrency = new JLabel(capitalize("CURRENCY") + " :");
		JComboBox<Currency> cboCurrency = UITools.createCombobox(new ArrayList<>(Currency.getAvailableCurrencies()));
		JLabel lclCodeCurrency = new JLabel("CurrencyLayer API code :");
		JTextField txtCurrencyFieldApiCode = new JTextField(MTGControler.getInstance().get("/currencylayer-access-api"),10);
		JButton btnSaveCode = new JButton(capitalize("SAVE"));
		JButton btnUpdateCurrency = new JButton("Update Currency");
		JCheckBox chkEnablePriceConversion = new JCheckBox(capitalize("ENABLED"));
		dateCurrencyCache = new JLabel(UITools.formatDate(MTGControler.getInstance().getCurrencyService().getCurrencyDateCache()));
		
		if (MTGControler.getInstance().get(CURRENCY).isEmpty())
			cboCurrency.setSelectedItem(Currency.getInstance(Locale.getDefault()));
		else
			cboCurrency.setSelectedItem(Currency.getInstance(MTGControler.getInstance().get(CURRENCY)));
		
		panelCurrency.add(chkEnablePriceConversion, UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  0, 0));
		panelCurrency.add(lblCurrency, UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  1, 0));
		panelCurrency.add(cboCurrency, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  2, 0));
		panelCurrency.add(lclCodeCurrency, UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  0, 1));
		panelCurrency.add(txtCurrencyFieldApiCode, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  1, 1));
		panelCurrency.add(btnSaveCode, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  2, 1));
		panelCurrency.add(btnUpdateCurrency, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  2, 2));
		panelCurrency.add(dateCurrencyCache, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  1, 2));
		
		
		
/////////////GUI BOX			
		
		JLabel lblGuiLocal = new JLabel(capitalize("LOCALISATION") + " :");
		JComboBox<Locale> cboLocales = UITools.createCombobox(MTGControler.getInstance().getLangService().getAvailableLocale());
		JLabel lblCardsLanguage = new JLabel(capitalize("CARD_LANGUAGE") + " :");
		JComboBox<String> cboLanguages = UITools.createCombobox(getEnabledPlugin(MTGCardsProvider.class).getLanguages());
		JLabel lblLook = new JLabel(capitalize("LOOK") + " :");
		JComboBox<LookAndFeelInfo> cboLook = UITools.createCombobox(MTGControler.getInstance().getLafService().getAllLookAndFeel());
		JLabel lblPicsSize = new JLabel(capitalize("THUMBNAIL_SIZE") + ": ");
		JButton btnSavePicSize = new JButton(capitalize("SAVE"));
		resizerPanel = new JResizerPanel(MTGControler.getInstance().getPictureProviderDimension());
		JLabel lblShowJsonPanel = new JLabel(capitalize("SHOW_OBJECT_PANEL") + " :");
		cbojsonView = new JCheckBox();
		JLabel lblShowTooltip = new JLabel(capitalize("SHOW_TOOLTIP") + " :");
		chkToolTip = new JCheckBox("");
		JLabel lblToolPosition = new JLabel(capitalize("TAB_POSITION") + " :");
		JComboBox<String> cboToolPosition = UITools.createCombobox(new String[] { "TOP", "LEFT", "RIGHT", "BOTTOM" });
		JLabel lblFont = new JLabel(capitalize("FONT") + " :");
		JFontChooser chooseFontPanel = new JFontChooser();
		chooseFontPanel.initFont(MTGControler.getInstance().getFont());
		JButton btnSaveFont = new JButton(capitalize("SAVE"));
		JCheckBox chkEnabledAutocomplete = new JCheckBox();
		JCheckBox chkEnabledChrome = new JCheckBox();
		JButton btnShortKeys = new JButton(capitalize("SHORTKEYS"));
		
		
		cboLocales.getModel().setSelectedItem(MTGControler.getInstance().getLocale());
		cbojsonView.getModel().setSelected(MTGControler.getInstance().get("debug-json-panel").equals("true"));
		chkToolTip.getModel().setSelected(MTGControler.getInstance().get("tooltip").equals("true"));
		cboToolPosition.getModel().setSelectedItem(MTGControler.getInstance().get("ui/moduleTabPosition", "LEFT"));
		chkEnabledAutocomplete.getModel().setSelected( MTGControler.getInstance().get("autocompletion").equals("true"));
		chkEnabledChrome.getModel().setSelected( MTGControler.getInstance().get("ui/chromedisabled").equals("true"));
		chkEnablePriceConversion.getModel().setSelected(MTGControler.getInstance().get("/currencylayer-converter-enable").equals("true"));
		
		panelGUI.add(lblGuiLocal, UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  0, 0));
		panelGUI.add(cboLocales, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  1, 0));
		panelGUI.add(lblCardsLanguage, UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  0, 1));
		panelGUI.add(cboLanguages, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  1, 1));
		panelGUI.add(lblLook,  UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  0, 2));
		panelGUI.add(cboLook,  UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  1, 2));
		panelGUI.add(lblPicsSize, UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  0, 3));
		panelGUI.add(resizerPanel, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  1, 3));
		panelGUI.add(btnSavePicSize, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  2, 3));
		panelGUI.add(lblShowJsonPanel, UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  0, 4));
		panelGUI.add(cbojsonView, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  1, 4));
		panelGUI.add(lblShowTooltip, UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  0, 5));
		panelGUI.add(chkToolTip, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  1, 5));
		panelGUI.add(lblToolPosition, UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  0, 6));
		panelGUI.add(cboToolPosition, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  1, 6));
		panelGUI.add(lblFont, UITools.createGridBagConstraints(GridBagConstraints.WEST, null,  0, 7));
		panelGUI.add(chooseFontPanel, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  1, 7));
		panelGUI.add(btnSaveFont, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  2, 7));
		panelGUI.add(new JLabel(capitalize("ENABLE_AUTOCOMPLETION") + " :"), UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  0, 8));
		panelGUI.add(chkEnabledAutocomplete, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  1, 8));
		panelGUI.add(new JLabel(capitalize("DISABLE_CHROME_RENDERING") + " :"), UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  0, 9));
		panelGUI.add(chkEnabledChrome, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  1, 9));
		
		panelGUI.add(new JLabel(capitalize("SHORTKEYS_CONFIGURATION") + " :"), UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  0, 10));
		panelGUI.add(btnShortKeys, UITools.createGridBagConstraints(null, GridBagConstraints.HORIZONTAL,  2, 10));
		
		
		
		
/////////////EVENTS	
		btnShortKeys.addActionListener(l->MTGUIComponent.createJDialog(new ShortKeyManagerUI(),true,false).setVisible(true));
		
		
		cboToolPosition.addItemListener(ie -> {
			if (ie.getStateChange() == ItemEvent.SELECTED)
				MTGControler.getInstance().setProperty("ui/moduleTabPosition",
						cboToolPosition.getSelectedItem().toString());
		});
		
		cboCurrency.addItemListener(ie -> {
			if (ie.getStateChange() == ItemEvent.SELECTED)
				MTGControler.getInstance().setProperty(CURRENCY, cboCurrency.getSelectedItem());
		});
		
		
		cboLocales.addItemListener(ie -> {
			if (ie.getStateChange() == ItemEvent.SELECTED)
				MTGControler.getInstance().setProperty("locale", cboLocales.getSelectedItem());
		});
				
		cboLanguages.addItemListener(ie -> {
			if (ie.getStateChange() == ItemEvent.SELECTED)
				MTGControler.getInstance().setProperty(LANGAGE, cboLanguages.getSelectedItem().toString());
		});
		
		cboLook.addItemListener(ie ->{
			if (ie.getStateChange() == ItemEvent.SELECTED)
				MTGControler.getInstance().getLafService().setLookAndFeel(SwingUtilities.getAncestorOfClass(JFrame.class, this), (LookAndFeelInfo) cboLook.getSelectedItem(),true);
		});

		cboLogLevels.addItemListener(ie -> {
			if (ie.getStateChange() == ItemEvent.SELECTED) {
				MTGControler.getInstance().setProperty("loglevel", cboLogLevels.getSelectedItem());
				MTGLogger.changeLevel((Level) cboLogLevels.getSelectedItem());
			}
		});
		
		cbojsonView.addItemListener(ae -> MTGControler.getInstance().setProperty("debug-json-panel", cbojsonView.isSelected()));

		
		btnExportConfig.addActionListener(ae->{
			try {
				File f =  new File(MTGConstants.DATA_DIR,"config.backup.zip");
				
				FileTools.extractConfig(f);
				
				MTGControler.getInstance().notify(new MTGNotification(EXPORT, "Export "+f, MESSAGE_TYPE.INFO));
			} catch (IOException e2) {
				MTGControler.getInstance().notify(e2);
			}
		});
		
		btnImportConfig.addActionListener(ae->{
			
				JFileChooser chooser = new JFileChooser(MTGConstants.DATA_DIR);
				int res = chooser.showOpenDialog(null);
				chooser.setFileFilter(new FileFilter() {
					
					@Override
					public String getDescription() {
						return "Zip File";
					}
					
					@Override
					public boolean accept(File f) {
						return FilenameUtils.isExtension(f.getName(), "zip");
					}
				});
				
				if(res == JFileChooser.APPROVE_OPTION)
				{
					try {
						FileTools.unzip(chooser.getSelectedFile(),MTGConstants.CONF_DIR);
						MTGControler.getInstance().notify(new MTGNotification("IMPORT", "Import config done", MESSAGE_TYPE.INFO));
					} catch (IOException e1) {
						MTGControler.getInstance().notify(e1);
					}
				}
		});
		

		cboEditionLands.addItemListener(ie ->{
			if (ie.getStateChange() == ItemEvent.SELECTED) 
				MTGControler.getInstance().setProperty("default-land-deck",((MagicEdition) cboEditionLands.getSelectedItem()).getId());
		});

		cboCollections.addItemListener(ie -> {
			if (ie.getStateChange() == ItemEvent.SELECTED)
				MTGControler.getInstance().setProperty(DEFAULT_LIBRARY,cboCollections.getSelectedItem());
		});
		
		btnWebsiteSave.addActionListener(ae -> MTGControler.getInstance().setProperty("default-website-dir", txtdirWebsite.getFile().getAbsolutePath()));

		btnSavePrice.addActionListener(ae -> MTGControler.getInstance().setProperty("min-price-alert", txtMinPrice.getText()));
		
		btnSavePicSize.addActionListener(ae -> {
			MTGControler.getInstance().setProperty("/card-pictures-dimension/width",(int) resizerPanel.getDimension().getWidth());
			MTGControler.getInstance().setProperty("/card-pictures-dimension/height",(int) resizerPanel.getDimension().getHeight());
			resizerPanel.setValue(0);
			getEnabledPlugin(MTGPictureProvider.class).setSize(resizerPanel.getDimension());
		});
		
		
		chkEnablePriceConversion.addItemListener(ie -> MTGControler.getInstance().setProperty("currencylayer-converter-enable", chkEnablePriceConversion.isSelected()));
		
		chkToolTip.addItemListener(ie -> MTGControler.getInstance().setProperty("tooltip", chkToolTip.isSelected()));
		chkEnabledAutocomplete.addItemListener(ie -> MTGControler.getInstance().setProperty("autocompletion", chkEnabledAutocomplete.isSelected()));
		chkEnabledChrome.addItemListener(ie -> MTGControler.getInstance().setProperty("ui/chromedisabled", chkEnabledChrome.isSelected()));
		chkboxPrerelease.addItemListener(ie -> MTGControler.getInstance().setProperty("notifyPrerelease", chkboxPrerelease.isSelected()));

		
		btnSaveCode.addActionListener(e -> MTGControler.getInstance().setProperty("currencylayer-access-api",txtCurrencyFieldApiCode.getText()));
		btnUpdateCurrency.addActionListener(ae -> {
			try {
				MTGControler.getInstance().getCurrencyService().clean();
				MTGControler.getInstance().getCurrencyService().init();
				dateCurrencyCache.setText(UITools.formatDate(MTGControler.getInstance().getCurrencyService().getCurrencyDateCache()));
			} catch (IOException e) {
				logger.error(e);
			}

		});
		
		btnAdd.addActionListener(ae -> {
			try {
				InstallCert.installCert(txtWebSiteCertificate.getText());
			} catch (Exception e) {
				MTGControler.getInstance().notify(e);
			}
		});

		
		btnSaveProfilGame.addActionListener(ae -> {
			MTGControler.getInstance().setProperty("/game/player-profil/name", txtName.getText());
			MTGControler.getInstance().setProperty("/game/cards/card-width",
					(int) gamePicsResizerPanel.getDimension().getWidth());
			MTGControler.getInstance().setProperty("/game/cards/card-height",
					(int) gamePicsResizerPanel.getDimension().getHeight());
			resizerPanel.setValue(0);
			GamePanelGUI.getInstance().getHandPanel().setSize(gamePicsResizerPanel.getDimension());
		});	
		
		
		btnSaveFont.addActionListener(ae -> {
			MTGControler.getInstance().setProperty("/ui/font/family", chooseFontPanel.getFont().getFamily());
			MTGControler.getInstance().setProperty("/ui/font/style",chooseFontPanel.getFont().getStyle());
			MTGControler.getInstance().setProperty("/ui/font/size",chooseFontPanel.getFont().getSize());
		});	
		
		
		
		
		btnClean.addActionListener(ae -> {

			try {
				loading(true, capitalize("CLEAN"));
				
				if(chckbxIconset.isSelected())
					IconSetProvider.getInstance().clean();
				
				if(chckbxIconcards.isSelected())
					getEnabledPlugin(MTGPicturesCache.class).clear();
				
				if(chckbxPackages.isSelected())
					PackagesProvider.inst().clear();
				
				
				loading(false, "");
			} catch (Exception e) {
				logger.error(e);
				loading(false, "");
			}
		});
		
		btnIndexation.addActionListener(ae ->
			ThreadManager.getInstance().executeThread(() -> {
				try {
					loading(true, "Indexation");
					btnIndexation.setEnabled(false);
					getEnabledPlugin(MTGCardsIndexer.class).initIndex();
					lblIndexSize.setText(UITools.formatDate(getEnabledPlugin(MTGCardsIndexer.class).getIndexDate()));
				} catch (Exception e) {
					logger.error("error indexation", e);
					MTGControler.getInstance().notify(e);
				} finally {
					loading(false, "");
					btnIndexation.setEnabled(true);
				}
			}, "Indexation")
		);
		
		btnDuplicate.addActionListener(ae ->{ 
			
			btnDuplicate.setEnabled(false);
			ThreadManager.getInstance().executeThread(() -> {
			try {
				MTGDao dao = (MTGDao) cboTargetDAO.getSelectedItem();
				loading(true, capitalize("DUPLICATE_TO",getEnabledPlugin(MTGDao.class)) + " " + dao);
				getEnabledPlugin(MTGDao.class).duplicateTo(dao);
			} catch (Exception e) {
				logger.error(e);
			}
			finally {
				loading(false, "");
				btnDuplicate.setEnabled(true);
			}
			
		}, "duplicate " + getEnabledPlugin(MTGDao.class) + " to " + cboTargetDAO.getSelectedItem());
		}

		);
		
		
		chkboxAutoAdd.addActionListener(e -> MTGControler.getInstance().setProperty("collections/stockAutoAdd",String.valueOf(chkboxAutoAdd.isSelected())));
		chkboxAutoDelete.addActionListener(e -> MTGControler.getInstance().setProperty("collections/stockAutoDelete",String.valueOf(chkboxAutoDelete.isSelected())));
		
		
		btnDefaultStock.addActionListener(ae -> {
			DefaultStockEditorDialog diag = new DefaultStockEditorDialog();
			diag.setMagicCardStock(MTGControler.getInstance().getDefaultStock());
			diag.setVisible(true);

		});

		lblIconAvatar.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent paramMouseEvent) {
				JFileChooser jf = new JFileChooser();
				jf.setFileFilter(new FileNameExtensionFilter("Images", "bmp", "gif", "jpg", "jpeg", "png"));
				int result = jf.showOpenDialog(null);
				if (result == JFileChooser.APPROVE_OPTION) {
					MTGControler.getInstance().setProperty("/game/player-profil/avatar",
							jf.getSelectedFile().getAbsolutePath());
					loadIcon();
				}
			}
		});
		
		
		btnWebServerExport.addActionListener(ae->{
			try {
				((AbstractWebServer)cboServers.getSelectedItem()).exportWeb(txtdirWebsserver.getFile());
				MTGControler.getInstance().notify(new MTGNotification(EXPORT, "Export ok : " + txtdirWebsserver.getFile(), MESSAGE_TYPE.INFO));
			} catch (Exception e1) {
				
				logger.error("error copy ",e1);
				MTGControler.getInstance().notify(e1);
			}
		});
		
		
		btnBackup.addActionListener(ae ->

		ThreadManager.getInstance().executeThread(() -> {
			try {
				loading(true, "backup db " + getEnabledPlugin(MTGDao.class) + " database");
				getEnabledPlugin(MTGDao.class).backup(txtDAOBackup.getFile());
				loading(false, "");

			} catch (Exception e1) {
				logger.error(e1);
			}
		}, "backup " + getEnabledPlugin(MTGDao.class) + " database"));
		
		lclCodeCurrency.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
					UITools.browse(MTGConstants.CURRENCY_API);
			}
			@Override
			public void mouseEntered(MouseEvent e) {
				lclCodeCurrency.setCursor(new Cursor(Cursor.HAND_CURSOR));
				
			}
			@Override
			public void mouseExited(MouseEvent e) {
				lclCodeCurrency.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}
			
		});

		if (MTGControler.getInstance().get(LANGAGE) != null) {
			cboLanguages.setSelectedItem(MTGControler.getInstance().get(LANGAGE));
		}
		if (MTGControler.getInstance().get(DEFAULT_LIBRARY) != null) {
			cboCollections.setSelectedItem(new MagicCollection(MTGControler.getInstance().get(DEFAULT_LIBRARY)));
		}
		
		
		for (int i = 0; i < cboLogLevels.getItemCount(); i++) {
			if (cboLogLevels.getItemAt(i).toString().equals(MTGControler.getInstance().get("loglevel")))
				cboLogLevels.setSelectedIndex(i);

		}
		try {
			for (MagicEdition col : getEnabledPlugin(MTGCardsProvider.class).listEditions()) {
				if (col.getId().equalsIgnoreCase(MTGControler.getInstance().get("default-land-deck"))) {
					cboEditionLands.setSelectedItem(col);
				}
			}

		} catch (Exception e1) {
			logger.error(e1);
		}
	}
	
	

	private void loadIcon() {
		try {
			lblIconAvatar.setIcon(new ImageIcon(ImageTools.resize(
					ImageTools.read(new File(MTGControler.getInstance().get("/game/player-profil/avatar"))), 100, 100)));

		} catch (Exception e) {
			lblIconAvatar.setIcon(null);
		}

	}

}
