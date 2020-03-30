package org.magic.sorters;

import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicDeck;
import org.magic.api.interfaces.MTGComparator;

public class CardsDeckSorter implements MTGComparator<MagicCard> {

	MagicDeck deck;
	
	
	@Override
	public String toString() {
		return "Deck Sorter";
	}
	
	@Override
	public int compare(MagicCard o1, MagicCard o2) {
		int ret =0;
		try 
		{
			ret = test(o1, o2);
			if (ret == 0)
				ret = name(o1, o2);
		}
		catch(Exception e)
		{
			ret = 0;
		}
		
		return ret;
	}
	
	public CardsDeckSorter(MagicDeck d)
	{
		this.deck=d;
	}
	

	private int test(MagicCard o1, MagicCard o2) {

		if (getWeight(o1) < getWeight(o2))
			return -1;

		if (getWeight(o1) == getWeight(o2))
			return 0;

		return 1;

	}

	private int land(MagicCard mc) {
		if (mc.getName().equalsIgnoreCase("Plains"))
			return 6;

		if (mc.getName().equalsIgnoreCase("Island"))
			return 7;

		if (mc.getName().equalsIgnoreCase("Swamp"))
			return 8;

		if (mc.getName().equalsIgnoreCase("Mountain"))
			return 9;

		return 10; 
	}

	private int name(MagicCard o1, MagicCard o2) {
		return o1.getName().compareTo(o2.getName());
	}

	public int getWeight(MagicCard mc) {

		
		
		if(deck.getCommander()==mc)
			return 0;
		
		if(mc.isPlaneswalker())
			return 1;
		
		if(mc.isCreature())
			return 2;
		
		if(mc.isArtifact())
			return 3;
		
		if(mc.isEnchantment())
			return 4;
		
		if(mc.isLand() && !mc.isBasicLand())
			return 5;
		
		if(mc.isBasicLand())
			return land(mc);
		
		
		return 100;
	}

}
