package org.magic.gui.models;

import java.util.Map;

import org.magic.api.beans.MagicCollection;
import org.magic.api.beans.MagicEdition;
import org.magic.api.beans.enums.EnumItems;
import org.magic.api.interfaces.MTGStockItem;
import org.magic.gui.abstracts.GenericTableModel;
import org.magic.tools.UITools;

public class StockItemTableModel extends GenericTableModel<MTGStockItem> {
	
	private static final long serialVersionUID = 1L;

	public StockItemTableModel() {
		setWritable(true);
		columns = new String[] { "ID",
				"PRODUCT",
				"EDITION",
				"LANGUAGE",
				"COLLECTION",
				"TYPE",
				"QTY",
				"PRICE",
				"COMMENT",
				"IDS"
			};
	}
	
	@Override
	public void addItem(MTGStockItem t) {
		if(t.getId()==-1)
		{
			items.add(t);
		}
		else
		{
			for(var i=0;i<=items.size();i++)
			{
				if(items.get(i).getId().equals(t.getId()))
				{
					items.set(i, t);
					break;
				}
			}
		}
	}
	
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
		case 0:
			return MTGStockItem.class;
		case 1:
			return String.class;
		case 2:
			return MagicEdition.class;
		case 3:
			return String.class;
		case 4:
			return MagicCollection.class;
		case 5:
			return EnumItems.class;
		case 6:
			return Integer.class;
		case 7:
			return Double.class;
		case 8:
			return String.class;
		case 9:
			return Map.class;
			
		default:
			return super.getColumnClass(columnIndex);
		}
	}
	
	@Override
	public boolean isCellEditable(int row, int column) {
		
		if(writable)
			return (column ==6 || column==7);
		else
			return false;
	}

	@Override
	public Object getValueAt(int row, int column) {

		switch (column) {
		case 0:
			return items.get(row);
		case 1:
			return items.get(row).getProductName();
		case 2:
			return items.get(row).getEdition();
		case 3 :
			return items.get(row).getLanguage();
		case 4:
			return items.get(row).getMagicCollection();
		case 5:
			return items.get(row).getTypeStock();
		case 6:
			return items.get(row).getQte();
		case 7:
			return UITools.roundDouble(items.get(row).getPrice());
		case 8:
			return items.get(row).getComment();
		case 9:
			return items.get(row).getTiersAppIds();

		default:
			return "";
		}
	}

	@Override
	public void setValueAt(Object aValue, int row, int column) {
		
		switch (column) {
		case 6:
			items.get(row).setQte((Integer) aValue);
			break;
		case 7:
			items.get(row).setPrice(Double.parseDouble(aValue.toString()));
			break;
			
		default:
			break;
		}
		items.get(row).setUpdated(true);

		
	}


}
