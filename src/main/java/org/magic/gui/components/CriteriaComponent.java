package org.magic.gui.components;

import static org.magic.tools.MTG.getEnabledPlugin;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.magic.api.beans.MagicCollection;
import org.magic.api.beans.MagicEdition;
import org.magic.api.beans.enums.MTGColor;
import org.magic.api.beans.enums.MTGFrameEffects;
import org.magic.api.beans.enums.MTGLayout;
import org.magic.api.beans.enums.MTGPromoType;
import org.magic.api.beans.enums.MTGRarity;
import org.magic.api.criterias.MTGCrit;
import org.magic.api.criterias.MTGCrit.OPERATOR;
import org.magic.api.criterias.QueryAttribute;
import org.magic.api.interfaces.MTGCardsProvider;
import org.magic.api.interfaces.abstracts.AbstractCardsProvider;
import org.magic.tools.UITools;


public class CriteriaComponent extends JComponent implements ActionListener{
	


	private static final long serialVersionUID = 1L;

	private JComponent selector;
	
	private QueryAttribute c;
	private transient Object val;
	private JComboBox<OPERATOR> cboOperator;
	private boolean showComparator;
	private JButton button;
	
	
	public CriteriaComponent() {
		showComparator=true;
		initGui();
	}

	public CriteriaComponent(boolean showComparator) {
		this.showComparator=showComparator;
		initGui();
	}
	
	
	private void initGui() {
		
		setLayout(new FlowLayout(FlowLayout.LEFT));
		
		JComboBox<QueryAttribute> cboAttributes = UITools.createCombobox(getEnabledPlugin(MTGCardsProvider.class).getQueryableAttributs());
			cboAttributes.setPreferredSize(new Dimension(200,25));
		
		c=getEnabledPlugin(MTGCardsProvider.class).getQueryableAttributs()[0];
		selector = getComponentFor(c); 
		
		add(cboAttributes);
		
		if(showComparator)
		{
			cboOperator = UITools.createCombobox(MTGCrit.OPERATOR.values());
			add(cboOperator);
		}
		
		add(selector);
		
		cboAttributes.addItemListener(il->{
			if(il.getStateChange() == ItemEvent.SELECTED)
			{
				
				int pos = UITools.getComponentIndex(selector);
				remove(selector);
				c = ((QueryAttribute)cboAttributes.getSelectedItem());
				
				SwingUtilities.invokeLater(()->{
					selector = getComponentFor(c);
					add(selector,pos);
					revalidate();
					repaint();
				});
				
			
			}
		});
	}

	private JComponent getComponentFor(QueryAttribute c) {
	
		if(c.getType() == Integer.class || c.getType() == Float.class)
		{
			var s= new JSpinner(new SpinnerNumberModel(0,0,1000000,1));
			s.setValue(0);
			s.addChangeListener(l->val = s.getValue());
			val=0;
			return s;
		}
		else
		if(c.getType() == Boolean.class)
		{
			var ch = new JCheckBox();
			val=false;
			ch.setSelected(false);
			ch.addItemListener(l->val=ch.isSelected());
			return ch;
		}
		else
		if(c.getType() == MagicEdition.class)
			return init(UITools.createComboboxEditions());
		else
		if(c.getType() == MagicCollection.class)
			return init(UITools.createComboboxCollection());
		else
		if(c.getType() == MTGColor.class)
			return init(UITools.createCombobox(MTGColor.values()));
		else
		if(c.getType() == MTGLayout.class)
			return init(UITools.createCombobox(MTGLayout.values()));
		else
		if(c.getType() == MTGRarity.class)
			return init(UITools.createCombobox(MTGRarity.values()));
		else
		if(c.getType() == MTGFrameEffects.class)
			return init(UITools.createCombobox(MTGFrameEffects.values()));
		else 
		if(c.getType() == MTGPromoType.class)
			return init(UITools.createCombobox(MTGPromoType.values()));
		else
		if(c.getName().equalsIgnoreCase("name")) {
			JTextField f= UITools.createSearchField();
			f.setColumns(50);
			
			f.getDocument().addDocumentListener(new DocumentListener() {
				
				@Override
				public void removeUpdate(DocumentEvent e) {
					val=f.getText().trim();
					
				}
				
				@Override
				public void insertUpdate(DocumentEvent e) {
					val=f.getText().trim();
					
				}
				
				@Override
				public void changedUpdate(DocumentEvent e) {
					val=f.getText().trim();
					
				}
			});
			
			
			f.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					
					val=f.getText().trim();
					
					if(e.getKeyCode()==KeyEvent.VK_ENTER && button!=null)
					{
						button.doClick();
					}
					
					
					
				}
			});
			return f;
		}
		
		//else
		
		var f= new JTextField(50);
		f.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				val=f.getText();
			}
		});
		return f;
	}
	
	private JComponent init(JComboBox<?> createCombobox) {
		createCombobox.addItemListener(il->{
			if(il.getStateChange() == ItemEvent.SELECTED)
				val=createCombobox.getSelectedItem();
		});
		val=createCombobox.getSelectedItem();
		return createCombobox;
	}

	public MTGCrit<?> getMTGCriteria(){
		if(!showComparator)
			return new MTGCrit<>(c, OPERATOR.LIKE, val);
		
		
		return new MTGCrit<>(c, OPERATOR.valueOf(cboOperator.getSelectedItem().toString()), val);
	}
	
	public boolean isCollectionSearch()
	{
		return c.getType() == MagicCollection.class;
	}
	
	public boolean isSetSearch()
	{
		return c.getType() == MagicEdition.class;
	}
	
	public boolean isAllCardsSearch()
	{
		return c.getName().equals(AbstractCardsProvider.ALL);
	}
	
	

	public void addButton(JButton b,boolean right) {
		
		button=b;
		
		if(right)
			add(button);
		else
			add(button,0);
	}

	public void addComponentListener(ActionListener al) {
		listenerList.add(ActionListener.class, al);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		for(ActionListener al : listenerList.getListeners(ActionListener.class))
		{
			al.actionPerformed(e);
		}
		
	}
	
	
	
}
