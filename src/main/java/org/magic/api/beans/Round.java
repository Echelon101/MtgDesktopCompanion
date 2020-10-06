package org.magic.api.beans;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.magic.game.model.Player;

public class Round implements Serializable {

	private static final long serialVersionUID = 1L;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private Map<Player,Integer> score;
	private int roundNumber;
	
	
	public void start()
	{
		startTime=LocalDateTime.now();
	}
	
	public void stop()
	{
		endTime=LocalDateTime.now();
	}
	
	public Round(int roundNumber) {
		score = new HashMap<>();
		this.roundNumber=roundNumber;
	}
	
	public int getRoundNumber() {
		return roundNumber;
	}
	
	public long duration()
	{
		 return Duration.between(startTime, endTime).toMinutes();
	}
	
	
	public Integer getScoreFor(Player p)
	{
		return score.get(p);
	}
	
	public Player getWinner()
	{
		Optional<Entry<Player, Integer>> opt = score.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1);
		
		if(opt.isPresent())
			return opt.get().getKey();
		
		return null;
	}
	
	
	
}
