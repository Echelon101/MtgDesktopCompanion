package org.magic.game.actions.cards;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;

import org.apache.log4j.Logger;
import org.magic.api.beans.MagicCard;
import org.magic.game.gui.components.DisplayableCard;
import org.magic.game.gui.components.GamePanelGUI;
import org.magic.services.MTGControler;
import org.magic.services.MTGLogger;

public class TransformActions extends AbstractAction {

	private DisplayableCard card;
	private transient Logger logger = MTGLogger.getLogger(this.getClass());

	public TransformActions(DisplayableCard card) {
		super("Transform");
		putValue(SHORT_DESCRIPTION, "Transform the card");
		putValue(MNEMONIC_KEY, KeyEvent.VK_A);
		this.card = card;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		GamePanelGUI.getInstance().getPlayer().logAction("Transform " + card.getMagicCard());

		try {

			card.removeAllCounters();

			MagicCard mc = MTGControler
					.getInstance().getEnabledCardsProviders().searchCardByCriteria("name",
							card.getMagicCard().getRotatedCardName(), card.getMagicCard().getCurrentSet(), true)
					.get(0);
			mc.setRulings(card.getMagicCard().getRulings());
			card.setMagicCard(mc);
			card.revalidate();
			card.repaint();
			card.initActions();

		} catch (Exception ex) {
			logger.error(ex);
		}

	}

}
