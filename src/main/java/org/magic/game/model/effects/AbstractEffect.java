package org.magic.game.model.effects;

import org.magic.game.model.AbstractSpell;

public abstract class AbstractEffect extends AbstractSpell {

	protected String effectDescription;
	protected AbstractEffect childEffect;
	
	
	@Override
	public void resolve() {
			
	}
	
	
	@Override
	public boolean isStackable() {
		return true;
	}
	
	public boolean hasChild()
	{
		return childEffect!=null;
	}
	
	
	public AbstractEffect getChildEffect() {
		return childEffect;
	}
	
	public void setChildEffect(AbstractEffect childEffect) {
		this.childEffect = childEffect;
	}
	
	
	public String getEffectDescription() {
		return effectDescription;
	}
	
	public void setEffectDescription(String effectDescription) {
		this.effectDescription = effectDescription;
	}
	
	@Override
	public String toString() {
		StringBuilder build = new StringBuilder();
		build.append(getEffectDescription());
		if(hasChild())
			build.append("\nAND ").append(getChildEffect());
		
		return build.toString();
		
	}
	
	
}
