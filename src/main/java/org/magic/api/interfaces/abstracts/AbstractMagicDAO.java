package org.magic.api.interfaces.abstracts;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicCardAlert;
import org.magic.api.beans.MagicCardStock;
import org.magic.api.beans.MagicCollection;
import org.magic.api.beans.MagicEdition;
import org.magic.api.beans.MagicNews;
import org.magic.api.beans.OrderEntry;
import org.magic.api.exports.impl.JsonExport;
import org.magic.api.interfaces.MTGDao;
import org.magic.api.interfaces.MTGPool;
import org.magic.tools.TCache;


public abstract class AbstractMagicDAO extends AbstractMTGPlugin implements MTGDao {

	protected static final String LOGIN = "LOGIN";
	protected static final String PASS = "PASS";
	protected static final String DB_NAME = "DB_NAME";
	protected static final String PARAMS = "PARAMS";
	protected static final String SERVERPORT = "SERVERPORT";
	protected static final String SERVERNAME = "SERVERNAME";
	
	protected JsonExport serialiser;
	
	protected TCache<MagicCardAlert> listAlerts;
	protected TCache<OrderEntry> listOrders;

	protected abstract void initAlerts();
	protected abstract void initOrders();
	
	@Override
	public boolean isSQL() {
		return false;
	}
	
	@Override
	public PLUGINS getType() {
		return PLUGINS.DAO;
	}
	
	@Override
	protected String getConfigDirectoryName() {
		return "dao";
	}

	
	@Override
	public void init(MTGPool pool) throws SQLException {
		logger.debug("Pool isn't necessary");
		init();
	}


	protected AbstractMagicDAO() {
		listAlerts = new TCache<>("alerts");
		listOrders = new TCache<>("orders");
		serialiser=new JsonExport();
	
	}
	
	@Override
	public void moveCard(MagicCard mc, MagicCollection from, MagicCollection to) throws SQLException {
		removeCard(mc, from);
		saveCard(mc, to);
		
		listStocks(mc, from,true).forEach(cs->{
		
			try {
				cs.setMagicCollection(to);
				saveOrUpdateStock(cs);
			} catch (SQLException e) {
				logger.error("Error saving stock for" + mc + " from " + from + " to " + to);
			}
		});
	}
	
	@Override
	public List<MagicCardStock> listStocks(List<MagicCollection> cols) throws SQLException {
		return listStocks().stream().filter(st->cols.contains(st.getMagicCollection())).collect(Collectors.toList());
	}
	
	@Override
	public MagicCardStock getStockById(Integer id) throws SQLException {
		return listStocks().stream().filter(mc->mc.getIdstock()==id).findAny().orElse(null);
	}
	
	
	
	@Override
	public void saveCollection(String name) throws SQLException {
		saveCollection(new MagicCollection(name));
	}
	
	@Override
	public List<MagicCard> listCardsFromCollection(String collectionName) throws SQLException {
		return listCardsFromCollection(new MagicCollection(collectionName));
	}
	
	@Override
	public List<MagicCard> listCardsFromCollection(String collectionName, MagicEdition me) throws SQLException {
		return listCardsFromCollection(new MagicCollection(collectionName), me);
	}


	@Override
	public void deleteOrderEntry(OrderEntry state) throws SQLException
	{
		ArrayList<OrderEntry> orders = new ArrayList<>();
		orders.add(state);
		deleteOrderEntry(orders);
	}
	
	@Override
	public List<OrderEntry> listOrdersByDescription(String desc, boolean strict) {
				
		if(strict)
			return listOrders().stream().filter(o->o.getDescription().equalsIgnoreCase(desc)).collect(Collectors.toList());
		else
			return listOrders().stream().filter(o->o.getDescription().contains(desc)).collect(Collectors.toList());
		
	}
	
	
	@Override
	public List<MagicCardStock> listStocks(MagicCard mc) throws SQLException {
		return listStocks().stream().filter(st->st.getMagicCard().getName().equals(mc.getName())).collect(Collectors.toList());
	}
		
	@Override
	public void deleteStock(MagicCardStock state) throws SQLException
	{
		ArrayList<MagicCardStock> stock = new ArrayList<>();
		stock.add(state);
		deleteStock(stock);
	}
	
	@Override
	public boolean hasAlert(MagicCard mc) {
		return listAlerts().stream().anyMatch(a->a.getCard().equals(mc));
	}
	
	@Override
	public List<MagicCardAlert> listAlerts() {
		if (listAlerts.isEmpty())
			initAlerts();
		
		return listAlerts.values();
	}
	
	@Override
	public List<OrderEntry> listOrders() {
		if (listOrders.isEmpty())
			initOrders();
		
		return listOrders.values();
	}
	
	

	
	@Override
	public MagicCardStock getStockWithTiersID(String key, String id) throws SQLException {
		
		if(key==null)
			return null;
		
		return listStocks().stream().filter(st->id.equals(st.getTiersAppIds(key))).findAny().orElse(null);
	}
	
	

	@Override
	public void duplicateTo(MTGDao dao) throws SQLException {
		
		dao.init(null);
		logger.debug("duplicate collection");
		for (MagicCollection col : listCollections())
		{
			try {
				dao.saveCollection(col);
			}catch(Exception e)
			{
				logger.error(col +" already exist");
			}
			
			for (MagicCard mc : listCardsFromCollection(col)) {
				try {
					dao.saveCard(mc, col);
				}catch(Exception e)
				{
					logger.error("error saving " + mc + " in "+ col + " :"+e);
				}
			}
		}
		
		logger.debug("duplicate stock");
		for(MagicCardStock stock : listStocks())
		{
			stock.setIdstock(-1);
			dao.saveOrUpdateStock(stock);
		}
			
		logger.debug("duplicate alerts");
		for(MagicCardAlert alert : listAlerts())
			dao.saveAlert(alert);
		
		logger.debug("duplicate news");
		for(MagicNews news : listNews())
		{
			news.setId(-1);
			dao.saveOrUpdateNews(news);
		}
		
		logger.debug("duplicate orders");
		for(OrderEntry oe : listOrders())
		{
			oe.setId(-1);
			dao.saveOrUpdateOrderEntry(oe);
		}
		
	}

	
	@Override
	public List<MagicCard> synchronizeCollection(MagicCollection col) throws SQLException {
		
		List<MagicCard> cols = listCardsFromCollection(col);
		
		List<MagicCard> toSave = listStocks().stream()
				  .filter(st->st.getMagicCollection().equals(col))
				  .filter(st->!cols.contains(st.getMagicCard()))
				  .map(MagicCardStock::getMagicCard)
				  .collect(Collectors.toList());
		
		List<MagicCard> ret = new ArrayList<>();
		
		toSave.forEach(mc->{
			try {
				saveCard(mc, col);
				ret.add(mc);
			} catch (SQLException e) {
				logger.error("error saving " + mc , e);
			}
		});
		
		return ret;
		
	}
	
	
	public List<OrderEntry> listOrderForEdition(MagicEdition ed) 
	{
		return listOrders().stream().filter(o->o.getEdition()!=null && o.getEdition().equals(ed)).collect(Collectors.toList());
	}
	
	@Override
	public List<OrderEntry> listOrdersAt(Date d) {
		return listOrders().stream().filter(o->o.getTransactionDate().equals(d)).collect(Collectors.toList());
		
	}
	
	@Override
	public List<Date> listDatesOrders() {
		Set<Date> d = new HashSet<>();
			listOrders().forEach(o->d.add(o.getTransactionDate()));
	
			return new ArrayList<>(d);
	}
	
	@Override
	public void updateCard(MagicCard c, MagicCard newC, MagicCollection col) throws SQLException {
		saveCard(newC,col);
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if(obj ==null)
			return false;
		
		return hashCode()==obj.hashCode();
	}
	
	@Override
	public int hashCode() {
		return getName().hashCode();
	}

	
}
