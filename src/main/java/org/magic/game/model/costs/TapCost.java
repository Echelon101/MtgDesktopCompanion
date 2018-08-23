package org.magic.game.model.costs;

public class TapCost implements Cost {

	
	public enum DIR {TAP,UNTAP}
	
	
	private DIR t;
	
	public TapCost(DIR t) {
		this.t=t;
	}
	
	public TapCost() {
		this.t=DIR.TAP;
	}
	
	@Override
	public String toString() {
		return t.name();
	}
	
	
}


