package org.magic.gui.models;

import static org.magic.tools.MTG.getEnabledPlugin;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.magic.api.beans.MagicCollection;
import org.magic.api.beans.MagicEdition;
import org.magic.api.interfaces.MTGDao;
import org.magic.gui.abstracts.GenericTableModel;
import org.magic.services.MTGControler;
import org.magic.services.providers.IconSetProvider;

public class MagicEditionsTableModel extends GenericTableModel<MagicEdition> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Map<MagicEdition, Integer> mapCount;

	int countTotal = 0;
	int countDefaultLibrary = 0;
	
	@Override
	public void init(List<MagicEdition> editions) {
		this.items = editions;
		mapCount = new TreeMap<>();

		try {
			calculate();
		} catch (Exception e) {
			logger.error("error calculate", e);
		}
		fireTableDataChanged();
	}
	

	public void calculate() {

		MagicCollection mc = new MagicCollection(MTGControler.getInstance().get("default-library"));
		Map<String, Integer> temp;
		try {
			temp = getEnabledPlugin(MTGDao.class).getCardsCountGlobal(mc);
		
		countDefaultLibrary = 0;
		countTotal = 0;
		for (MagicEdition me : items) {
			mapCount.put(me, (temp.get(me.getId()) == null) ? 0 : temp.get(me.getId()));
			countDefaultLibrary += mapCount.get(me);
		}

		for (MagicEdition me : items)
			countTotal += me.getCardCount();
		
		} catch (SQLException e) {
			logger.error("error in calculation",e);
		}
	}

	public Map<MagicEdition, Integer> getMapCount() {
		return mapCount;
	}

	public int getCountTotal() {
		return countTotal;
	}

	public void setCountTotal(int countTotal) {
		this.countTotal = countTotal;
	}

	public int getCountDefaultLibrary() {
		return countDefaultLibrary;
	}

	public void setCountDefaultLibrary(int countDefaultLibrary) {
		this.countDefaultLibrary = countDefaultLibrary;
	}

	public MagicEditionsTableModel() {
		columns=new String[] { "EDITION_CODE",
				"EDITION",
				"EDITION_SIZE",
				"DATE_RELEASE",
				"PC_COMPLETE",
				"QTY",
				"EDITION_TYPE",
				"EDITION_BLOCK",
				"EDITION_ONLINE",
				"PREVIEW"};

	}

	@Override
	public Object getValueAt(int row, int column) {
		MagicEdition e = items.get(row);
		if (column == 0)
			return IconSetProvider.getInstance().get24(e.getId());

		if (column == 1)
			return e;

		if (column == 2)
			return e.getCardCount();

		if (column == 3)
			return e.getReleaseDate();

		if (column == 4) {
			if (e.getCardCount() > 0)
				return (double) mapCount.get(e) / e.getCardCount();
			else
				return (double) mapCount.get(e) / 1;
		}

		if (column == 5)
			return mapCount.get(e);

		if (column == 6)
			return e.getType();

		if (column == 7)
			return e.getBlock();

		if (column == 8)
			return e.isOnlineOnly();
		
		if (column == 9)
			return e.isPreview();

		return "";

	}



}
