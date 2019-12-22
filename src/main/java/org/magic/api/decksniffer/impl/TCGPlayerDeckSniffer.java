package org.magic.api.decksniffer.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicDeck;
import org.magic.api.beans.MagicEdition;
import org.magic.api.beans.RetrievableDeck;
import org.magic.api.interfaces.MTGCardsProvider;
import org.magic.api.interfaces.abstracts.AbstractDeckSniffer;
import org.magic.services.MTGConstants;
import org.magic.services.MTGControler;
import org.magic.tools.IncapsulaParser;
import org.magic.tools.URLTools;

public class TCGPlayerDeckSniffer extends AbstractDeckSniffer {
	private static final String PAUPER = "pauper";
	private static final String PIONNEER = "pionneer";
	private static final String MAX_PAGE = "MAX_PAGE";
	private static final String URL = "URL";
	private static final String SUBDECK_GROUP_CARD_QTY = "subdeck-group__card-qty";
	private static final String COMMANDER = "commander";
	private static final String VINTAGE = "vintage";
	private static final String LEGACY = "legacy";
	private static final String MODERN = "modern";
	private static final String STANDARD = "standard";
	private static final String FORMAT = "FORMAT";


	@Override
	public String[] listFilter() {
		return new String[] { STANDARD, MODERN, LEGACY, VINTAGE, COMMANDER,PIONNEER,PAUPER};
	}
	
	@Override
	public MagicDeck getDeck(RetrievableDeck info) throws IOException {
		logger.debug("get deck at " + info.getUrl());
		MagicDeck deck = new MagicDeck();
		deck.setName(info.getName());
		deck.setDescription(info.getUrl().toString());
		Document d = URLTools.toHtml(IncapsulaParser.readUrl(info.getUrl().toString()));
		logger.debug(d);
		for (Element e : d.select("span.singleTag")) {
			deck.getTags().add(e.text());
		}

		Elements main = d.getElementsByClass("subdeck");

		int taille = main.get(0).getElementsByClass(SUBDECK_GROUP_CARD_QTY).size();
		for (int i = 0; i < taille; i++) {
			int qte = Integer.parseInt(main.get(0).getElementsByClass(SUBDECK_GROUP_CARD_QTY).get(i).text());
			String cardName = main.get(0).getElementsByClass("subdeck-group__card-name").get(i).text();

			MagicEdition ed = null;
			if (MagicCard.isBasicLand(cardName)) {
				ed = new MagicEdition(MTGControler.getInstance().get("default-land-deck"));
			}

			if (cardName.contains("//"))
				cardName = cardName.substring(0, cardName.indexOf("//")).trim();

			MagicCard mc = MTGControler.getInstance().getEnabled(MTGCardsProvider.class)
					.searchCardByName( cardName, ed, true).get(0);

			deck.getMap().put(mc, qte);

			notify(mc);
		}

		if (main.size() > 1) {
			int tailleSide = main.get(1).getElementsByClass(SUBDECK_GROUP_CARD_QTY).size();
			for (int i = 0; i < tailleSide; i++) {
				int qte = Integer.parseInt(main.get(1).getElementsByClass(SUBDECK_GROUP_CARD_QTY).get(i).text());
				String cardName = main.get(1).getElementsByClass("subdeck-group__card-name").get(i).text();

				MagicEdition ed = null;
				if (MagicCard.isBasicLand(cardName)) {
					ed = new MagicEdition(MTGControler.getInstance().get("default-land-deck"));
				}

				if (cardName.contains("//"))
					cardName = cardName.substring(0, cardName.indexOf("//")).trim();

				MagicCard mc = MTGControler.getInstance().getEnabled(MTGCardsProvider.class)
						.searchCardByName( cardName, ed, true).get(0);
				deck.getMapSideBoard().put(mc, qte);
			}
		}

		return deck;

	}

	@Override
	public List<RetrievableDeck> getDeckList() throws IOException {
		String url = getString(URL) + "/magic/deck/search?format=" + getString(FORMAT);
		logger.debug("get List deck at " + url);
		List<RetrievableDeck> list = new ArrayList<>();
		int maxPage = getInt(MAX_PAGE);

		for (int i = 1; i <= maxPage; i++) {
			url = getString(URL) + "/magic/deck/search?format=" + getString(FORMAT) + "&page=" + i;
			Document d = Jsoup.parse(IncapsulaParser.readUrl(url));
			
			for (Element tr : d.getElementsByClass("gradeA")) {
				RetrievableDeck deck = new RetrievableDeck();

				String mana = "";

				Element manaEl = tr.getElementsByTag(MTGConstants.HTML_TAG_TD).get(0);
				if (manaEl.toString().contains("white-mana"))
					mana += "{W}";
				if (manaEl.toString().contains("blue-mana"))
					mana += "{U}";
				if (manaEl.toString().contains("black-mana"))
					mana += "{B}";
				if (manaEl.toString().contains("red-mana"))
					mana += "{R}";
				if (manaEl.toString().contains("green-mana"))
					mana += "{G}";

				String deckName = tr.getElementsByTag(MTGConstants.HTML_TAG_TD).get(1).text();
				String link = getString(URL) + tr.getElementsByTag(MTGConstants.HTML_TAG_TD).get(1).getElementsByTag("a").attr("href");
				String deckPlayer = tr.getElementsByTag(MTGConstants.HTML_TAG_TD).get(2).text();
				String deckDesc = tr.getElementsByTag(MTGConstants.HTML_TAG_TD).get(3).text();
				
				deck.setColor(mana);
				deck.setAuthor(deckPlayer);
				deck.setName(deckName);
				deck.setDescription(deckDesc);
				
				try {
					deck.setUrl(new URI(link));
				} catch (URISyntaxException e) {
					deck.setUrl(null);
				}

				list.add(deck);

			}

		}

		return list;

	}


	@Override
	public String getName() {
		return "TCGPlayer";
	}

	@Override
	public void initDefault() {
		setProperty(FORMAT, STANDARD);
		setProperty(URL, "https://decks.tcgplayer.com");
		setProperty(MAX_PAGE, "1");

	}

}
