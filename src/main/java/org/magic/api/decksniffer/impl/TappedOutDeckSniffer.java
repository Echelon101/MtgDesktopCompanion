package org.magic.api.decksniffer.impl;

import static org.magic.tools.MTG.getEnabledPlugin;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.RegExUtils;
import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicDeck;
import org.magic.api.beans.MagicEdition;
import org.magic.api.beans.RetrievableDeck;
import org.magic.api.interfaces.MTGCardsProvider;
import org.magic.api.interfaces.abstracts.AbstractDeckSniffer;
import org.magic.services.MTGControler;
import org.magic.tools.InstallCert;
import org.magic.tools.RequestBuilder;
import org.magic.tools.RequestBuilder.METHOD;
import org.magic.tools.URLTools;
import org.magic.tools.URLToolsClient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class TappedOutDeckSniffer extends AbstractDeckSniffer {

	private static final String URL_JSON = "URL_JSON";
	private static final String FORMAT = "FORMAT";
	private static final String PASS = "PASS";
	private static final String LOGIN2 = "LOGIN";
	private static final String URI_BASE="https://tappedout.net";
	private URLToolsClient httpclient;


	@Override
	public STATUT getStatut() {
		return STATUT.BETA;
	}

	public TappedOutDeckSniffer() {
		super();

		if(getBoolean("LOAD_CERTIFICATE"))
		{
			try {
				InstallCert.installCert("tappedout.net");
				setProperty("LOAD_CERTIFICATE", "false");
			} catch (Exception e1) {
				logger.error(e1);
			}
		}
	}

	@Override
	public String[] listFilter() {
		return new String[] { "latest", "standard", "modern", "legacy", "vintage", "edh", "tops", "pauper", "aggro","budget", "control" };
	}

	@Override
	public String getName() {
		return "TappedOut";
	}

	private void initConnexion() throws IOException {
		httpclient = URLTools.newClient();
		httpclient.doGet(URI_BASE+"/accounts/login/?next=/");
		
		RequestBuilder b = httpclient.build().method(METHOD.POST)
						  .url(URI_BASE+"/accounts/login/")
						  .addContent("username", getString(LOGIN2))
						  .addContent("password", getString(PASS))
						  .addContent("csrfmiddlewaretoken", httpclient.getCookieValue("csrftoken"))
						  .addHeader(URLTools.REFERER, URI_BASE+"/accounts/login/?next=/")
						  .addHeader(URLTools.UPGR_INSECURE_REQ, "1")
				          .addHeader(URLTools.ORIGIN, URI_BASE)
						  .addHeader(URLTools.REFERER_POLICY,"strict-origin-when-cross-origin")
						  .addHeader(URLTools.ACCEPT_LANGUAGE, "fr-FR,fr;q=0.9,en;q=0.8")
						  .addHeader(URLTools.ACCEPT_ENCODING, "gzip, deflate, br")
						  .addHeader("pragma","no-cache")
						  .addHeader("sec-fetch-dest", "document")
						  .addHeader("sec-fetch-mode", "navigate")
						  .addHeader("sec-fetch-site", "same-origin")
						  .addHeader("sec-fetch-user", "?1")
						  .addHeader("cache-control","no-cache");
		
		httpclient.execute(b);

		logger.debug("Connection : " + getString(LOGIN2) + " " + httpclient.getResponse().getStatusLine().getReasonPhrase());
		
	}

	@Override
	public MagicDeck getDeck(RetrievableDeck info) throws IOException {
		if(httpclient==null)
			initConnexion();
		
		logger.debug("sniff deck at " + info.getUrl());
		

		String responseBody = httpclient.doGet(info.getUrl().toString());
		logger.debug("sniff deck : "+ httpclient.getResponse().getStatusLine().getReasonPhrase());
		
		MagicDeck deck = info.toBaseDeck();
		JsonElement root = URLTools.toJson(responseBody);
		deck.setName(root.getAsJsonObject().get("name").getAsString());
		deck.setDescription(root.getAsJsonObject().get("url").getAsString());
		for (int i = 0; i < root.getAsJsonObject().get("inventory").getAsJsonArray().size(); i++) {
			JsonArray inv = root.getAsJsonObject().get("inventory").getAsJsonArray().get(i).getAsJsonArray();
			String cardName = inv.get(0).getAsString();
			String position = inv.get(1).getAsJsonObject().get("b").getAsString();
			int qte = inv.get(1).getAsJsonObject().get("qty").getAsInt();

			// remove foil if present
			cardName = RegExUtils.replaceAll(cardName, "\\*.+?\\*", "").trim();

			// ged ed if present
			String idSet = null;
			Matcher m = Pattern.compile("\\(([^)]+)\\)").matcher(cardName);
			while (m.find()) {
				idSet = (m.group(1));
			}
			cardName = RegExUtils.replaceAll(cardName, "\\(([^)]+)\\)", "").trim();

			// remove behavior if present
			if (cardName.contains("#"))
				cardName = cardName.substring(0, cardName.indexOf('#')).trim();

			if (cardName.contains("//"))
				cardName = cardName.substring(0, cardName.indexOf("//")).trim();

			List<MagicCard> ret;
			if (idSet == null) {
				if (MagicCard.isBasicLand(cardName)) {
					MagicEdition ed = new MagicEdition(MTGControler.getInstance().get("default-land-deck"));
					ret = getEnabledPlugin(MTGCardsProvider.class).searchCardByName( cardName, ed,
							true);
				} else {
					ret = getEnabledPlugin(MTGCardsProvider.class).searchCardByName( cardName, null,
							true);
				}

			} else {
				MagicEdition ed = new MagicEdition(idSet);
				ret = getEnabledPlugin(MTGCardsProvider.class).searchCardByName( cardName, ed, true);
			}

			if (!ret.isEmpty()) {
				notify(ret.get(0));

				if (position.equalsIgnoreCase("main"))
					deck.getMain().put(ret.get(0), qte);
				else
					deck.getSideBoard().put(ret.get(0), qte);
			}
		}
		return deck;

	}

	public List<RetrievableDeck> getDeckList() throws IOException {

		if(httpclient==null)
			initConnexion();
		
		String tappedJson = RegExUtils.replaceAll(getString(URL_JSON), "%FORMAT%", getString(FORMAT));
		logger.debug("sniff url : " + tappedJson);

		String responseBody = httpclient.doGet(tappedJson);
		
		JsonElement root = URLTools.toJson(responseBody);
		List<RetrievableDeck> list = new ArrayList<>();

		for (int i = 0; i < root.getAsJsonArray().size(); i++) {
			JsonObject obj = root.getAsJsonArray().get(i).getAsJsonObject();
			RetrievableDeck deck = new RetrievableDeck();
			deck.setName(obj.get("name").getAsString());
			try {
				deck.setUrl(new URI(obj.get("resource_uri").getAsString()));
			} catch (URISyntaxException e) {
				deck.setUrl(null);
			}
			deck.setAuthor(obj.get("user").getAsString());
			deck.setColor("");
			list.add(deck);
		}
		return list;
	}


	@Override
	public void initDefault() {
		setProperty(LOGIN2, "login@mail.com");
		setProperty(PASS, "changeme");
		setProperty(FORMAT, "standard");
		setProperty(URL_JSON, URI_BASE+"/api/deck/latest/%FORMAT%");
	}


}
