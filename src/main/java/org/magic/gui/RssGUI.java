package org.magic.gui;

import static org.magic.tools.MTG.capitalize;
import static org.magic.tools.MTG.getEnabledPlugin;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.jdesktop.swingx.JXTable;
import org.magic.api.beans.MagicNews;
import org.magic.api.beans.MagicNewsContent;
import org.magic.api.interfaces.MTGDao;
import org.magic.gui.abstracts.AbstractBuzyIndicatorComponent;
import org.magic.gui.abstracts.MTGUIBrowserComponent;
import org.magic.gui.abstracts.MTGUIComponent;
import org.magic.gui.components.NewsEditorPanel;
import org.magic.gui.models.MagicNewsTableModel;
import org.magic.gui.renderer.NewsTreeCellRenderer;
import org.magic.services.MTGConstants;
import org.magic.services.MTGControler;
import org.magic.services.threads.ThreadManager;
import org.magic.tools.UITools;



public class RssGUI extends MTGUIComponent {
	
	private static final long serialVersionUID = 1L;
	private JXTable table;
	private MagicNewsTableModel model;
	private MTGUIBrowserComponent editorPane;
	private DefaultMutableTreeNode curr;
	private NewsEditorPanel newsPanel;
	private DefaultMutableTreeNode rootNode;
	private JTree tree;
	private AbstractBuzyIndicatorComponent lblLoading;
	private JButton btnNewButton;
	private JButton btnSave;
	private JButton btnDelete;
	
	
	@Override
	public ImageIcon getIcon() {
		return MTGConstants.ICON_NEWS;
	}
	
	@Override
	public String getTitle() {
		return capitalize("RSS_MODULE");
	}
	
	public RssGUI() {
		

		model = new MagicNewsTableModel();
		table = UITools.createNewTable(model);
		tree = new JTree();
		JSplitPane splitNews = new JSplitPane();
		editorPane = MTGUIBrowserComponent.createBrowser();
		JSplitPane splitTreeTable = new JSplitPane();
		JPanel leftPanel = new JPanel();
		rootNode = new DefaultMutableTreeNode(capitalize("RSS_MODULE"));
		JPanel panelControl = new JPanel();
		btnNewButton = UITools.createBindableJButton(null,MTGConstants.ICON_NEW,KeyEvent.VK_N,"new news entry");
		btnSave = UITools.createBindableJButton(null,MTGConstants.ICON_SAVE,KeyEvent.VK_S,"save news entry");
		btnDelete =UITools.createBindableJButton(null,MTGConstants.ICON_DELETE,KeyEvent.VK_D,"delete news entry");
		lblLoading = AbstractBuzyIndicatorComponent.createLabelComponent();
		newsPanel = new NewsEditorPanel();
		
		setLayout(new BorderLayout(0, 0));
		tree.setPreferredSize(new Dimension(150, 64));
		splitNews.setOrientation(JSplitPane.VERTICAL_SPLIT);
		leftPanel.setLayout(new BorderLayout(0, 0));
		tree.setModel(new DefaultTreeModel(rootNode));
		tree.setCellRenderer(new NewsTreeCellRenderer());

		
		splitNews.setLeftComponent(new JScrollPane(table));
		splitNews.setRightComponent(new JScrollPane(editorPane));
		add(splitTreeTable, BorderLayout.CENTER);
		splitTreeTable.setRightComponent(splitNews);
		splitTreeTable.setLeftComponent(leftPanel);
		leftPanel.add(new JScrollPane(tree), BorderLayout.CENTER);
		
		leftPanel.add(panelControl, BorderLayout.NORTH);
		panelControl.add(btnNewButton);
		panelControl.add(btnSave);
		panelControl.add(btnDelete);
		panelControl.add(lblLoading);
		leftPanel.add(newsPanel, BorderLayout.SOUTH);
				
	
		initTree();

		initActions();
	

	}

	private void initActions() {
		btnNewButton.addActionListener(ae -> {
			newsPanel.setMagicNews(new MagicNews());
			newsPanel.setVisible(true);
		});

		

		btnSave.addActionListener(ae -> {
			try {
				getEnabledPlugin(MTGDao.class).saveOrUpdateNews(newsPanel.getMagicNews());
				initTree();
			} catch (SQLException ex) {
				logger.error("Error saving news", ex);
				MTGControler.getInstance().notify(ex);
			}
		});

		
		btnDelete.addActionListener(ae -> {
			try {
				getEnabledPlugin(MTGDao.class).deleteNews(newsPanel.getMagicNews());
				initTree();
			} catch (SQLException ex) {
				logger.error("Error delete news", ex);
				MTGControler.getInstance().notify(ex);
			}
		});

		tree.addTreeSelectionListener(tse -> {
			TreePath path = tse.getPath();
			curr = (DefaultMutableTreeNode) path.getLastPathComponent();

			if (curr.getUserObject() instanceof MagicNews)
			{	
				SwingWorker<List<MagicNewsContent>, MagicNews> sw = new SwingWorker<>()
				{

					@Override
					protected List<MagicNewsContent> doInBackground() throws Exception {
						MagicNews n = (MagicNews) curr.getUserObject();
						return n.getProvider().listNews(n);
					}

					@Override
					protected void done() {
						try {
							model.init(get());
							model.fireTableDataChanged();
							
						} catch (Exception e) {
							logger.error(e);
						} 
						lblLoading.end();
					}

					
				
				};
				
				lblLoading.start();
				newsPanel.setMagicNews((MagicNews) curr.getUserObject());
				ThreadManager.getInstance().runInEdt(sw,"loading rss");
				
			}
		});

		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent me) {
				List<MagicNewsContent> sels = UITools.getTableSelections(table, 0);
				
				if(sels.isEmpty())
					return;
				
				MagicNewsContent sel = sels.get(0);
				if (me.getClickCount() == 2) {
					try {
						Desktop.getDesktop().browse(sel.getLink().toURI());
						
					} catch (Exception e1) {
						logger.error(e1);
					}
				} else {
					
					SwingWorker<Void, URL> sw = new SwingWorker<>()
					{
						@Override
						protected void done() {
							lblLoading.end();
							
						}
						
						@Override
						protected void process(java.util.List<URL> chunks) {
							try {
								editorPane.loadURL(chunks.get(0).toString());
							} catch (Exception e) {
								logger.error("error loading " + chunks.get(0),e);
							}
							
						}
						
						@Override
						protected Void doInBackground() throws Exception {
							publish(sel.getLink());
							return null;
						}

					};
					
					lblLoading.start();
					ThreadManager.getInstance().runInEdt(sw,"loading "+sel.getLink());
				}
			}
		});
		
	}

	private void initTree() {
		rootNode.removeAllChildren();
		
		
		SwingWorker<Void, MagicNews> sw = new SwingWorker<>() {
			
			@Override
			protected void process(List<MagicNews> chunks) {
				chunks.forEach(cat->add(cat.getCategorie(), cat));
			}
			
			@Override
			protected Void doInBackground() throws Exception {
				for (MagicNews cat : getEnabledPlugin(MTGDao.class).listNews())
					publish(cat);
			
				return null;
			}

			@Override
			protected void done() {
				((DefaultTreeModel) tree.getModel()).reload();

				for (int i = 0; i < tree.getRowCount(); i++)
					tree.expandRow(i + 1);

			}
			
			
			
		};
		
		
		ThreadManager.getInstance().runInEdt(sw,"Loading News Tree");
	}

	private void add(String cat, MagicNews n) {
		DefaultMutableTreeNode node = getNodeCateg(cat);
		node.add(new DefaultMutableTreeNode(n));
		rootNode.add(node);
	}

	private DefaultMutableTreeNode getNodeCateg(String cat) {
		Enumeration<TreeNode> e = rootNode.breadthFirstEnumeration();
		while (e.hasMoreElements()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
			if (node.getUserObject().toString().equalsIgnoreCase(cat)) {
				return node;
			}
		}
		return new DefaultMutableTreeNode(cat);
	}

}
