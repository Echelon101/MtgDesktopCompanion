package org.magic.gui.components.shops;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.api.mkm.modele.Category;
import org.api.mkm.modele.Product;
import org.magic.api.interfaces.MTGCardsProvider;
import org.magic.api.interfaces.MTGExternalShop;
import org.magic.gui.abstracts.AbstractBuzyIndicatorComponent;
import org.magic.gui.abstracts.MTGUIComponent;
import org.magic.gui.renderer.ProductListRenderer;
import org.magic.gui.tools.JListFilterDecorator;
import org.magic.services.MTGConstants;
import org.magic.services.threads.ThreadManager;
import org.magic.services.workers.AbstractObservableWorker;
import org.magic.tools.MTG;
import org.magic.tools.UITools;


public class ProductsCreatorComponent extends MTGUIComponent {

	private static final long serialVersionUID = 1L;
	private JTextField txtSearchProduct;
	private JComboBox<MTGExternalShop> cboInput;
	private JComboBox<MTGExternalShop> cboOutput;
	
	private JList<Product> listInput;
	private DefaultListModel<Product> modelInput;
	
	private JList<Product> listOutput;
	private DefaultListModel<Product> modelOutput;
	
	private AbstractBuzyIndicatorComponent buzy;
	private JPanel panel;
	private JButton btnSend;
	private JComboBox<String> cboLanguages;
	private JComboBox<Category> cboCategory;
	
	
	public ProductsCreatorComponent() {
		setLayout(new BorderLayout(0, 0));

		panel = new JPanel();
		btnSend = UITools.createBindableJButton("Export", MTGConstants.ICON_EXPORT, KeyEvent.VK_S,"searchProduct");
		var btnSearch = UITools.createBindableJButton("", MTGConstants.ICON_SEARCH_24, KeyEvent.VK_F,"searchProduct");
		
		var panelNorth = new JPanel();
		var panelWest = new JPanel();
		panelWest.setLayout(new BorderLayout());
		var panelEast = new JPanel();
		panelEast.setLayout(new BorderLayout());
		
		cboInput = UITools.createCombobox(MTGExternalShop.class,true);
		cboOutput= UITools.createCombobox(MTGExternalShop.class,true);
		cboLanguages = UITools.createCombobox(MTG.getEnabledPlugin(MTGCardsProvider.class).getLanguages());
		cboCategory = UITools.createCombobox(new ArrayList<>());
		
		
		initCategory();
		
		buzy = AbstractBuzyIndicatorComponent.createProgressComponent();
		txtSearchProduct = new JTextField(25);
		modelInput = new DefaultListModel<>();
		listInput = new JList<>(modelInput);
		modelOutput= new DefaultListModel<>();
		listOutput = new JList<>(modelOutput);
		listInput.setCellRenderer(new ProductListRenderer());
		listOutput.setCellRenderer(new ProductListRenderer());
		
		
		var deco = JListFilterDecorator.decorate(listInput,(p, s)->p.getEnName().toLowerCase().contains(s.toLowerCase()));
		
		
		panelNorth.add(txtSearchProduct);
		panelNorth.add(btnSearch);
		panelNorth.add(buzy);
		
		add(panelNorth, BorderLayout.NORTH);
		add(panelWest,BorderLayout.WEST);
		add(panelEast,BorderLayout.EAST);
		
		panelWest.add(cboInput, BorderLayout.NORTH);
		panelEast.add(cboOutput, BorderLayout.NORTH);
		
		panelWest.add(new JScrollPane(deco.getContentPanel()), BorderLayout.CENTER);
		panelEast.add(new JScrollPane(listOutput), BorderLayout.CENTER);
		
		
		add(panel, BorderLayout.CENTER);
		panel.add(cboLanguages);
		panel.add(cboCategory);
		panel.add(btnSend);

		
		btnSearch.addActionListener(e->loadProducts());
		txtSearchProduct.addActionListener(e->loadProducts());
		btnSend.addActionListener(e->sendProducts());
		btnSend.setEnabled(false);
		
		
		cboOutput.addItemListener(il->initCategory());
		
		listInput.addListSelectionListener(lll->{
			btnSend.setEnabled(listInput.getSelectedIndex()>=0);
			btnSend.setText("send "+ listInput.getSelectedValuesList().size() + " items");
		});
	}


	private void initCategory() {
		try {

			cboCategory.removeAllItems();
			((DefaultComboBoxModel<Category>)cboCategory.getModel()).addAll(((MTGExternalShop)cboOutput.getSelectedItem()).listCategories().stream().sorted(Comparator.comparing(Category::getCategoryName)).collect(Collectors.toList()));
			
		} catch (IOException e1) {
			logger.error(e1);
		}
	}


	private void sendProducts() {
		
		List<Product> list = listInput.getSelectedValuesList();
		
		
		AbstractObservableWorker<Void,Product,MTGExternalShop> sw = new AbstractObservableWorker<>(buzy,(MTGExternalShop)cboOutput.getSelectedItem(),list.size())
		{
			@Override
			protected Void doInBackground() throws Exception {
					for(Product p : list)
						{
							int id = plug.createProduct((MTGExternalShop)cboInput.getSelectedItem(),p,cboLanguages.getSelectedItem().toString(),(Category)cboCategory.getSelectedItem());
							p.setIdProduct(id);
							publish(p);
						}
					return null;
			}
			@Override
			protected void process(List<Product> chunks) {
				super.process(chunks);
				modelOutput.addAll(chunks);
			}
			@Override
			protected void done() {
				super.done();
				listOutput.updateUI();
			}
		};
		
		ThreadManager.getInstance().runInEdt(sw,"search Products");
	}


	private void loadProducts() {
	
		String search = txtSearchProduct.getText();
		
		modelInput.removeAllElements();
		
		AbstractObservableWorker<List<Product>,Product,MTGExternalShop> sw = new AbstractObservableWorker<>(buzy,(MTGExternalShop)cboInput.getSelectedItem())
		{
			@Override
			protected List<Product> doInBackground() throws Exception {
					return plug.listProducts(search);
			}
			
			@Override
			protected void done() {
				super.done();
				try {
					modelInput.addAll(get());
					listInput.updateUI();
				} catch (InterruptedException | ExecutionException e) {
					Thread.currentThread().interrupt();
				} 
			}
		};
		
		ThreadManager.getInstance().runInEdt(sw,"search Products");
	}

	@Override
	public ImageIcon getIcon() {
		return MTGConstants.ICON_TAB_SEALED;
	}

	@Override
	public String getTitle() {
		return "Product Creation";
	}

}
