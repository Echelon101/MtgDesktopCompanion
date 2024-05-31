package org.magic.gui.models;

import org.magic.api.beans.MTGPrice;
import org.magic.api.beans.technical.MoneyValue;
import org.magic.api.interfaces.MTGPlugin;
import org.magic.api.interfaces.MTGPricesProvider;
import org.magic.gui.abstracts.GenericTableModel;
import org.magic.services.tools.MTG;

public class CardsPriceTableModel extends GenericTableModel<MTGPrice> {
	private static final long serialVersionUID = 1L;


	public CardsPriceTableModel() {
		columns=new String[] {
				"CARD",
				"QTY",
				"WEBSITE",
				"PRICE",
				"SELLER",
				"QUALITY",
				"FOIL",
				"CARD_LANGUAGE",
				"COUNTRY"};
	}

	@Override
	public int[] defaultHiddenColumns() {
		return new int[] {0,1};
	}


	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
		case 0:
			return MTGPrice.class;
		case 1:
			return Integer.class;
		case 2:
			return MTGPlugin.class;
		case 3:
			return MoneyValue.class;
		case 4:
			return String.class;
		case 5:
			return Boolean.class;
		case 6:
			return String.class;
		case 7:
			return String.class;
		default:
			return super.getColumnClass(columnIndex);
		}
	}

	@Override
	public Object getValueAt(int row, int column) {
		try {

			MTGPrice mp = items.get(row);

			switch (column) {
			case 0:
				return mp;
			case 1:
				return mp.getQty();
			case 2:
				return MTG.getPlugin(mp.getSite(),MTGPricesProvider.class);
			case 3:
				return mp.getPriceValue();
			case 4:
				return mp.getCurrency();
			case 5:
				return mp.getSeller();
			case 6:
				return mp.getQuality();
			case 7:
				return mp.isFoil();
			case 8:
				return mp.getLanguage();
			case 9:
				return mp.getCountry();
			default:
				return 0;
			}
		} catch (IndexOutOfBoundsException ioob) {
			return null;
		}
	}

}
