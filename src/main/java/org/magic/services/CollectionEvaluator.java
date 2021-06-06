package org.magic.services;

import static org.magic.tools.MTG.getEnabledPlugin;
import static org.magic.tools.MTG.getPlugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.magic.api.beans.CardShake;
import org.magic.api.beans.EditionsShakers;
import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicCollection;
import org.magic.api.beans.MagicEdition;
import org.magic.api.exports.impl.JsonExport;
import org.magic.api.interfaces.MTGCardsProvider;
import org.magic.api.interfaces.MTGDao;
import org.magic.api.interfaces.MTGDashBoard;
import org.magic.tools.FileTools;
import org.utils.patterns.observer.Observable;

public class CollectionEvaluator extends Observable
{
	private static final String PRICE_JSON = "_price.json";
	protected static Logger logger = MTGLogger.getLogger(CollectionEvaluator.class);
	private MagicCollection collection ;
	private File directory;
	private JsonExport serialiser;
	private Map<MagicEdition,Map<MagicCard,CardShake>> cache;
	private int minPrice=0;
	
	public File getDirectory() {
		return directory;
	}
	
	public CollectionEvaluator() throws IOException {
		init();
	}
	
	public CollectionEvaluator(MagicCollection c) throws IOException {
		collection=c;
		init();
	}
	
	
	public void setCollection(MagicCollection collection) {
		this.collection = collection;
		cache.clear();

	}
	
	private void init() throws IOException
	{
		cache = new HashMap<>();
		directory = Paths.get(MTGConstants.DATA_DIR.getAbsolutePath(), "prices").toFile();
		if(!directory.exists())
			FileUtils.forceMkdir(directory);

		
		
		serialiser= new JsonExport();
	}
	
	
	public void initCache() throws IOException
	{
		getEnabledPlugin(MTGCardsProvider.class).listEditions().forEach(ed->{
			try {
				initCache(ed);
			} catch (IOException e) {
				logger.error("couldn't load " + ed,e);
			}
		});
	}
	
	public void clearUICache()
	{
		cache.clear();
	}
	
	public void initCache(MagicEdition edition,EditionsShakers ret) throws IOException
	{
		try {
			if(!ret.isEmpty())
				FileTools.saveFile(new File(directory,edition.getId()+PRICE_JSON), serialiser.toJsonElement(ret).toString());
		} catch (IOException e) {
			logger.error(edition.getId() + " is not found",e);
		}
		
	}
	
	
	public EditionsShakers initCache(MagicEdition edition,String provider) throws IOException
	{
		var ret = new EditionsShakers();
			try {
				ret= getPlugin(provider, MTGDashBoard.class).getShakesForEdition(edition);
			} catch (FileNotFoundException e) {
				logger.error(edition.getId() + " is not found " + e);
			}
			return ret;
	}
	
	
	public EditionsShakers initCache(MagicEdition edition) throws IOException
	{
		return initCache(edition,getEnabledPlugin(MTGDashBoard.class).getName());
	}
	
	public List<MagicEdition> getEditions()
	{
		List<MagicEdition> eds = new ArrayList<>();
		try {
			getEnabledPlugin(MTGDao.class).listEditionsIDFromCollection(collection).forEach(key->{
				try {
					MagicEdition ed = getEnabledPlugin(MTGCardsProvider.class).getSetById(key);
					eds.add(ed);
				}catch(Exception e)
				{
					logger.error("error get edition " + key,e);
				}});
		} catch (SQLException e) {
			logger.error("error sql get editions ",e);
		}
		return eds;
		}
	
	
	public Map<MagicCard,CardShake> prices()
	{
		Map<MagicCard,CardShake> ret = new HashMap<>();
		getEditions().forEach(ed->
			prices(ed).entrySet().forEach(entry->
					ret.put(entry.getKey(), entry.getValue())
					)
		);
		return ret;
	}
	
	public boolean hasCache(MagicEdition ed)
	{
		return new File(directory,ed.getId()+PRICE_JSON).exists();
	}
	
	
	public Date getCacheDate(MagicEdition ed)
	{
		var fich = new File(directory,ed.getId()+PRICE_JSON);
		if(fich.exists())
		{
			EditionsShakers r = loadFromCache(ed);
			if(!r.isEmpty())
				return r.getDate();
		}
		return null;
	}
	
	
	public synchronized Map<MagicCard,CardShake> prices(MagicEdition ed)
	{
		
		if(cache.get(ed)!=null)
			return cache.get(ed);
		
		logger.trace("caculate prices for" + ed);
		
		
		Map<MagicCard,CardShake> ret = new HashMap<>();
		try {
			var fich = new File(directory,ed.getId()+PRICE_JSON);
			EditionsShakers list;
			if(fich.exists())
			{
				list=loadFromCache(ed);
			}
			else
			{
				logger.trace(fich + " is not found for " + ed.getId() +" : " + ed.getSet());
				list= new EditionsShakers();
				
			}	
			List<MagicCard> cards = getEnabledPlugin(MTGDao.class).listCardsFromCollection(collection, ed);
			for(MagicCard mc : cards) 
			{
					Optional<CardShake> cs = list.getShakes().stream().filter(sk->sk.getName().equals(mc.getName())).findFirst();
					if(cs.isPresent())
					{
						
						CardShake shak = cs.get();
						shak.setCard(mc);
						
						if(shak.getPrice()>=minPrice)
							ret.put(mc, shak);
					}
					else
					{
						var csn = new CardShake();
						csn.setName(mc.getName());
						csn.setCard(mc);
						csn.setPrice(0.0);
						
						if(csn.getPrice()>=minPrice)
							ret.put(mc, csn);
					}
					
			}
			
			setChanged();
			notifyObservers(ed);
			
			
		} catch (SQLException e) {
			logger.error(e);
		}
		
		
		cache.put(ed, ret);
		return ret;
	}
	
	public EditionsShakers loadFromCache(MagicEdition ed) {
		try {
			if(new File(directory,ed.getId()+PRICE_JSON).exists()) {	
				return serialiser.fromJson(FileUtils.readFileToString(new File(directory,ed.getId()+PRICE_JSON),MTGConstants.DEFAULT_ENCODING),EditionsShakers.class);
			}
		}
		catch(Exception e)
		{
			logger.error("error loading " + ed, e);
		}
		
		var eds = new EditionsShakers();
		eds.setEdition(ed);
		return eds;
		
		
	}

	public void export(File f) throws IOException{
		
		var temp = new StringBuilder("EDITION;CARDNAME;PRICE");
		temp.append(System.lineSeparator());
		for(Entry<MagicCard, CardShake> e : prices().entrySet())
		{
			if(e.getValue()!=null) 
			{
				temp.append(e.getKey().getCurrentSet()).append(";").append(e.getKey().getName()).append(";").append(e.getValue().getPrice()).append(System.lineSeparator());
			}
			else 
			{
				temp.append(e.getKey().getCurrentSet()).append(";").append(e.getKey().getName()).append(";").append("NC").append(System.lineSeparator());
			}
		}
		FileTools.saveFile(f, temp.toString());
		
	}
	
	
	public Double total(MagicEdition ed) {
		return prices(ed).values().stream().mapToDouble(CardShake::getPrice).sum();
	}

	public Double total() {
		Double total=0.0;
		for(MagicEdition ed : getEditions())
			total=total+total(ed);
		
		return total;
	}

	public void setMinPrice(int i) {
		this.minPrice=i;
		
	}
}
