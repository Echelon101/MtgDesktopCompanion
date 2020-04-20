package org.magic.game.actions.cards;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import org.apache.log4j.Logger;
import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicRuling;
import org.magic.api.interfaces.MTGCardsProvider;
import org.magic.api.interfaces.MTGPictureProvider;
import org.magic.game.gui.components.DisplayableCard;
import org.magic.services.MTGControler;
import org.magic.services.MTGLogger;
import org.magic.services.keywords.AbstractKeyWordsManager;

public class MorphActions extends AbstractAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private DisplayableCard card;
	private String cost;
	private static String k = "Morph";
	private transient Logger logger = MTGLogger.getLogger(this.getClass());

	public MorphActions(DisplayableCard card) {
		super(k);
		putValue(SHORT_DESCRIPTION, k);
		putValue(MNEMONIC_KEY, KeyEvent.VK_M);
		this.card = card;
		parse();
	}

	private String parse() {
		try {
			String regex = "/*" + k + " \\{(.*?)\\ ";
			String text = card.getMagicCard().getText();
			Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
			Matcher m = p.matcher(text);

			if (m.find())
				cost = m.group().replaceAll(k, "").trim();
			else
				cost = text.substring(text.indexOf(k + "\u2014") + k.length(), text.indexOf('('));

		} catch (Exception e) {
			logger.error(e);
			cost = "";
		}
		return cost;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (!card.isRotated()) {
			MagicCard mc = new MagicCard();
			mc.setName("Morphed Creature");
			mc.setPower("2");
			mc.setToughness("2");
			mc.getTypes().add("Creature");
			mc.setCost("{3}");
			mc.setEditions(card.getMagicCard().getEditions());
			mc.setRotatedCardName(card.getMagicCard().getName());
			MagicRuling r = new MagicRuling();
					r.setText(AbstractKeyWordsManager.getInstance().generateFromKeyString(k).toString());
			mc.getRulings().add(r);
			mc.setText(k + " " + cost);
			mc.setLayout(card.getMagicCard().getLayout());
			mc.setId(card.getMagicCard().getId());
			card.setMagicCard(mc);
			card.setRotated(true);
			card.showPT(true);
			card.initActions();
			try {
				card.setImage(new ImageIcon(MTGControler.getInstance().getEnabled(MTGPictureProvider.class).getBackPicture()
						.getScaledInstance(card.getWidth(), card.getHeight(), Image.SCALE_SMOOTH)));
			} catch (Exception e1) {
				logger.error(e1);
			}
		} else {
			try {
				MagicCard mc = MTGControler.getInstance().getEnabled(MTGCardsProvider.class).searchCardByName(
						card.getMagicCard().getRotatedCardName(), card.getMagicCard().getCurrentSet(), true)
						.get(0);
				card.setMagicCard(mc);
				card.setRotated(false);
				card.removeAllCounters();
				card.showPT(false);
				card.initActions();
			} catch (Exception e1) {
				logger.error(e1);
			}
		}

		card.revalidate();
		card.repaint();

	}

}
