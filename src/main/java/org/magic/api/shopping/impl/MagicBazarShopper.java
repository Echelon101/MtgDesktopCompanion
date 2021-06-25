package org.magic.api.shopping.impl;

import static org.magic.tools.MTG.getEnabledPlugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.magic.api.beans.OrderEntry;
import org.magic.api.beans.Transaction.TYPE_TRANSACTION;
import org.magic.api.beans.enums.EnumItems;
import org.magic.api.interfaces.MTGCardsProvider;
import org.magic.api.interfaces.abstracts.AbstractMagicShopper;
import org.magic.tools.UITools;
import org.magic.tools.URLTools;
import org.magic.tools.URLToolsClient;

public class MagicBazarShopper extends AbstractMagicShopper {

	
	String urlBase= "https://en.play-in.com/";

	String urlListOrders = urlBase + "/user/list_order.php";
	String urlLogin = urlBase+"/user/signin.php";
	
	@Override
	public List<OrderEntry> listOrders() throws IOException {
		URLToolsClient client = URLTools.newClient();
		List<OrderEntry> entries = new ArrayList<>();
		
		Map<String, String> nvps = client.buildMap().put("_email", getString("LOGIN"))
															 .put("_pwd", getString("PASS"))
															 .put("_submit_login", "Me connecter").build();
		client.doPost(urlLogin, nvps, null);
		
		Document listOrders = URLTools.toHtml(client.doGet(urlListOrders));
		Elements e = listOrders.select("div.total_list a");
		
		logger.debug("Found " + e.size() + " orders");
		for(var i=0;i<e.size();i++)
		{
			String id = e.get(i).select("div.num_commande").text();
			String link = e.get(i).attr("href");
			String date = e.get(i).select("div.hide_mobile").first().html();
			entries.addAll(parse(URLTools.toHtml(client.doGet(urlBase+link)),id,UITools.parseDate(date,"MM/dd/yy")));
			
		}
		return entries;
	}

	private List<OrderEntry> parse(Document doc, String id, Date date) {
		List<OrderEntry> entries = new ArrayList<>();
		Elements table = doc.select("div.table div.tr");
		table.remove(0);
		
		
		for(var i=0;i<table.size();i++)
		{
			Element e = table.get(i);
			boolean iscard=e.hasClass("filterElement");
			String name = e.select("div.td.name").text();
			
			
			if(!name.isEmpty())
			{

				var entrie = new OrderEntry();
					entrie.setIdTransation(id);
					entrie.setSource(getName());
					entrie.setCurrency(Currency.getInstance("EUR"));
					entrie.setSeller(getName());
					entrie.setTypeTransaction(TYPE_TRANSACTION.BUY);
					entrie.setTransactionDate(date);
					entrie.setDescription(name);
					if(iscard)
					{
						entrie.setType(EnumItems.CARD);
						entrie.setDescription(e.select("div.td.name.name_mobile").text());
						entrie.setItemPrice(UITools.parseDouble(e.attr("attribute_price")));
						String set = e.select("div.td.ext img").attr("title");
						try {
							
							entrie.setEdition(getEnabledPlugin(MTGCardsProvider.class).getSetByName(set));
						}
						catch(Exception ex)
						{
							logger.error(set + " is not found");
						}
						
						
					}
					else
					{
						String price =e.select("div.new_price").html().replaceAll("&nbsp;"+Currency.getInstance("EUR").getSymbol(), "").trim(); 
						entrie.setItemPrice(UITools.parseDouble(price));
						if(entrie.getDescription().contains("Set")||entrie.getDescription().toLowerCase().contains("collection"))
							entrie.setType(EnumItems.FULLSET);
						else if(entrie.getDescription().toLowerCase().contains("booster"))
							entrie.setType(EnumItems.BOOSTER);
						else if(entrie.getDescription().toLowerCase().startsWith("boite de") || entrie.getDescription().contains("Display") )
							entrie.setType(EnumItems.BOX);
						else
							entrie.setType(EnumItems.LOTS);
					}
					notify(entrie);
					entries.add(entrie);	
			}
			
			
			
		}
		
		
		
		return entries;
	}


	@Override
	public String getName() {
		return "MagicBazar";
	}
	
	
	@Override
	public void initDefault() {
		setProperty("LOGIN", "");
		setProperty("PASS", "");
	}
	

}
