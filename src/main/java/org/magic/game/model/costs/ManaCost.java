package org.magic.game.model.costs;

import java.util.Map;
import java.util.TreeMap;

public class ManaCost implements Cost {

	private Map<String,Integer> mcost;
	
	
	public ManaCost() {
		mcost = new TreeMap<>();
	}
	
	public void add(String mana, int qty)
	{
		mcost.computeIfAbsent(mana, k -> 0);
		mcost.put(mana, mcost.get(mana)+qty);
	}
	
	public void add(String mana)
	{
		add(mana, 1);
	}
	
	
	@Override
	public String toString() {
		
		StringBuilder build = new StringBuilder();
		
		mcost.entrySet().forEach(e->
		{
			for(int i=0;i<e.getValue();i++)
				build.append(e.getKey());
		});
		return build.toString();
		
		
	}
}
