package org.magic.api.pricers.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicPrice;
import org.magic.api.interfaces.abstracts.AbstractPricesProvider;
import org.magic.tools.RequestBuilder;
import org.magic.tools.RequestBuilder.METHOD;
import org.magic.tools.URLTools;

import com.google.gson.JsonElement;

public class EbayPricer extends AbstractPricesProvider {

	
	private static final String URL_BASE ="https://svcs.ebay.com/services/search/FindingService/v1";

	public List<MagicPrice> getLocalePrice(MagicCard card) throws IOException {
		List<MagicPrice> prices = new ArrayList<>();
		String keyword = card.getName();
			   keyword += " " + card.getCurrentSet().getSet();
		
		
			   var b = RequestBuilder.build().setClient(URLTools.newClient()).method(METHOD.GET)
				.url(URL_BASE)
				.addContent("SECURITY-APPNAME", getString("API_KEY"))
				.addContent("OPERATION-NAME", "findItemsByKeywords")
				.addContent("RESPONSE-DATA-FORMAT", "JSON")
				.addContent("GLOBAL-ID", getString("COUNTRY"))
				.addContent("paginationInput.entriesPerPage", getString("MAX"))
				.addContent("keywords", URLTools.encode(keyword));
		
		if(getBoolean("FIXEDPRICE_ONLY"))
		{	
			b.addContent("itemFilter(0).name", "ListingType");
			b.addContent("itemFilter(0).value(1)", "FixedPrice");
		}
	
		logger.info(getName() + " looking for " + keyword + " (" + b.getUrl()+")");
		
		JsonElement root = b.toJson();

		JsonElement articles = root.getAsJsonObject().entrySet().iterator().next().getValue().getAsJsonArray().get(0).getAsJsonObject().get("searchResult");

		if (articles.getAsJsonArray().get(0).getAsJsonObject().get("item") == null) {
			logger.info(getName() + " find nothing");
			return prices;
		}

		var items = articles.getAsJsonArray().get(0).getAsJsonObject().get("item").getAsJsonArray();

		logger.trace(items);

		for (JsonElement el : items) {
			var mp = new MagicPrice();
		
			var etat = "";
			var title = el.getAsJsonObject().get("title").getAsString();
			var consultURL = el.getAsJsonObject().get("viewItemURL").getAsString();
			var country = el.getAsJsonObject().get("location").getAsJsonArray().toString();
			var price = el.getAsJsonObject().get("sellingStatus").getAsJsonArray().get(0).getAsJsonObject()
					.get("currentPrice").getAsJsonArray().get(0).getAsJsonObject().get("__value__").getAsDouble();
			var currency = el.getAsJsonObject().get("sellingStatus").getAsJsonArray().get(0).getAsJsonObject()
					.get("currentPrice").getAsJsonArray().get(0).getAsJsonObject().get("@currencyId").getAsString();
			try {
				etat = el.getAsJsonObject().get("condition").getAsJsonArray().get(0).getAsJsonObject()
						.get("conditionDisplayName").getAsString();
			} catch (NullPointerException e) {
				etat = "";
			}
			
			
			
			mp.setMagicCard(card);
			mp.setCountry(country);
			mp.setSeller(title);
			mp.setUrl(consultURL);
			mp.setCurrency(currency);
			mp.setValue(price);
			mp.setSite(getName());
			mp.setQuality(etat);
			mp.setFoil(mp.getSeller().toLowerCase().contains("foil"));
			prices.add(mp);
		}

		logger.info(getName() + " find " + prices.size() + " item(s)");
		
		return prices;
	}

	@Override
	public String getName() {
		return "Ebay";
	}


	@Override
	public void initDefault() {
		setProperty("MAX", "10");
		setProperty("COUNTRY", "EBAY-FR");
		setProperty("API_KEY", "none04674-8d13-4421-af9e-ec641c7ee59");
		setProperty("WEBSITE", "https://www.ebay.com/");
		setProperty("FIXEDPRICE_ONLY","false");

	}

	@Override
	public String getVersion() {
		return "1.13.0";
	}

}
