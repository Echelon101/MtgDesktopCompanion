package org.magic.game.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameManager {

	private List<Player> players;
	
	private static GameManager instance;
	private List<Turn> turns;

	private SpellStack stack;

	private GameManager()
	{
		turns = new ArrayList<>();
		players=new ArrayList<>();
		stack=new SpellStack();
	}
	
	public List<Turn> getTurns() {
		return turns;
	}

	public Turn getActualTurn()
	{
		if(turns.isEmpty())
			return new Turn();
		return turns.get(turns.size()-1);
	}
	
	public Player getCurrentPlayer()
	{
		return players.get(0);
	}
	
	public void endTurn(Player p)
	{
		p.logAction("End the turn " + turns.size());
		turns.add(new Turn());
		Collections.rotate(players, 1);
	}
	
	public void logAction(Player p, String action)
	{
		p.logAction(action);
	}
	
	public void setPlayer(Player p)
	{
		players.clear();
		players.add(p);
	}
	
	public static GameManager getInstance()
	{
		if(instance==null)
			instance = new GameManager();
		
		return instance;
	}
	
	
	public void addPlayer(Player p) {
		players.add(p);
	
	}
	

	public List<Player> getPlayers() {
		return players;
	}

	

	public void initGame() {
		for(Player player : players)
			player.init();
		
		turns = new ArrayList<>();
		
		turns.add(new Turn());
	}


	public void removePlayers() {
		players.clear();
		
	}

	public SpellStack getStack() {
		return stack;
	}
	

}
