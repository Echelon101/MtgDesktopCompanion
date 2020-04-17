package org.magic.api.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.magic.api.beans.enums.MTGColor;

public class MagicDeck implements Serializable {

	private static final long serialVersionUID = 1L;
	private Map<MagicCard, Integer> mapDeck;
	private Map<MagicCard, Integer> mapSideBoard;

	private String description;
	private String name;
	private Date dateCreation;
	private Date dateUpdate;
	private double averagePrice;
	private List<String> tags;
	private MagicCard commander;

	public MagicDeck() 
	{
		mapDeck = new HashMap<>();
		mapSideBoard = new HashMap<>();
		tags = new ArrayList<>();
		averagePrice = 0;
		dateCreation=new Date();
		dateUpdate=new Date();
	}
	
	public MagicCard getValueAt(int pos) {
		return new ArrayList<>(getMain().keySet()).get(pos);
	}

	public MagicCard getSideValueAt(int pos) {
		return new ArrayList<>(getSideBoard().keySet()).get(pos);
	}
	
	public int getNbCards() {
		return getMain().entrySet().stream().mapToInt(Entry::getValue).sum();
	}
	
	public boolean isEmpty() {
		return getMain().isEmpty() && getSideBoard().isEmpty();
	}

	public List<MagicCard> getUniqueCards() {
		return getMain().keySet().stream().collect(Collectors.toList());
	}

	public void remove(MagicCard mc) {
		if (getMain().get(mc) == 0)
			getMain().remove(mc);
		else
			getMain().put(mc, getMain().get(mc) - 1);
	}
	
	public void removeSide(MagicCard mc) {
		if (getSideBoard().get(mc) == 0)
			getSideBoard().remove(mc);
		else
			getSideBoard().put(mc, getSideBoard().get(mc) - 1);
	}
	
	public void delete(MagicCard mc) {
		mapDeck.remove(mc);
	}
		
	public void add(MagicCard mc) {
		getMain().compute(mc, (k,v)->(v==null)?1:v+1);
	}
	
	public void addSide(MagicCard mc) {
		getSideBoard().compute(mc, (k,v)->(v==null)?1:v+1);
	}

	public boolean hasCard(MagicCard mc) {
		return getMain().keySet().stream().filter(k->k.getName().equalsIgnoreCase(mc.getName())).findAny().isEmpty();
	}

	public Set<MagicFormat> getLegality() {
		Set<MagicFormat> cmap = new LinkedHashSet<>();
		for (MagicCard mc : getMain().keySet()) {
			for (MagicFormat mf : mc.getLegalities()) {
				cmap.add(mf);
			}
		}
		return cmap;
	}

	public String getColors() {
		
		Set<MTGColor> cmap = new LinkedHashSet<>();
		for (MagicCard mc : getUniqueCards())
		{
			if ((mc.getCmc() != null))
			{
				for (MTGColor c : mc.getColors())
				{
					if(c!=null)
						cmap.add(c);
				}
			}
		}
		StringBuilder tmp = new StringBuilder();
		
		cmap.stream().sorted().map(MTGColor::toManaCode).forEach(tmp::append);
		return tmp.toString();
	}
	
	public List<MagicCard> getAsList() {
		return toList(getMain().entrySet());
	}


	public List<MagicCard> getSideAsList() {
		return toList(getSideBoard().entrySet());
	}
	
	private List<MagicCard> toList(Set<Entry<MagicCard, Integer>> entrySet) {
		ArrayList<MagicCard> deck = new ArrayList<>();

		for (Entry<MagicCard, Integer> c : entrySet)
			for (int i = 0; i < c.getValue(); i++)
				deck.add(c.getKey());
		
		return deck;
		
	}
	
	public boolean isCompatibleFormat(MagicFormat mf) {
		for (MagicCard mc : mapDeck.keySet()) 
		{
			if(mc.getLegalities().stream().filter(f->f.equals(mf)).noneMatch(MagicFormat::isLegal))
					return false;
		}
		return true;
	}


	public static MagicDeck toDeck(List<MagicCard> cards) {
		MagicDeck d = new MagicDeck();
		d.setName("export");
		d.setDescription("");

		if (cards == null)
			return d;

		cards.forEach(d::add);

		return d;
	}
	
	
	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public double getAveragePrice() {
		return averagePrice;
	}

	public void setAveragePrice(double averagePrice) {
		this.averagePrice = averagePrice;
	}

	public Date getDateCreation() {
		return dateCreation;
	}
	
	public Date getDateUpdate() {
		return dateUpdate;
	}

	public void setDateUpdate(Date dateUpdate) {
		this.dateUpdate = dateUpdate;
	}

	public String toString() {
		return getName();
	}

	public void setMain(Map<MagicCard, Integer> mapDeck) {
		this.mapDeck = mapDeck;
	}

	public Map<MagicCard, Integer> getSideBoard() {
		return mapSideBoard;
	}

	public void setSideBoard(Map<MagicCard, Integer> mapSideBoard) {
		this.mapSideBoard = mapSideBoard;
	}

	public String getName() {
		return name;
	}
	
	public Map<MagicCard, Integer> getMain() {
		return mapDeck;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public void setCreationDate(Date date) {
		this.dateCreation=date;
	}

	public void setCommander(MagicCard mc) {
		this.commander=mc;
	}

	public MagicCard getCommander() {
		return commander;
	}

	
}
