package org.magic.gui.models;

import java.net.URL;
import java.util.Currency;

import org.magic.api.beans.MagicPrice;
import org.magic.gui.abstracts.GenericTableModel;
import org.magic.tools.UITools;

public class CardsPriceTableModel extends GenericTableModel<MagicPrice> {

	private static final long serialVersionUID = 1L;


	public CardsPriceTableModel() {
		columns=new String[] { 
				"CARD",
				"QTY",
				"WEBSITE",
				"PRICE",
				"CURRENCY",
				"SELLER",
				"QUALITY",
				"FOIL",
				"CARD_LANGUAGE",
				"COUNTRY",
				"URL"};
	}
	
	@Override
	public int[] defaultHiddenColumns() {
		return new int[] {0,1};
	}
	

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
		case 0:
			return MagicPrice.class;
		case 1:
			return Integer.class;
		case 2:
			return String.class;
		case 3:
			return Double.class;
		case 4:
			return Currency.class;
		case 5:
			return String.class;
		case 6:
			return String.class;
		case 7:
			return Boolean.class;
		case 8:
			return String.class;
		case 9:
			return String.class;
		default:
			return URL.class;
		}
	}

	@Override
	public Object getValueAt(int row, int column) {
		try {

			MagicPrice mp = items.get(row);

			switch (column) {
			case 0:
				return mp;
			case 1:
				return mp.getQty();
			case 2:
				return mp.getSite();
			case 3:
				return UITools.roundDouble(mp.getValue());
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
			case 10:
				return mp.getUrl();
			default:
				return 0;
			}
		} catch (IndexOutOfBoundsException ioob) {
			return null;
		}
	}

}
