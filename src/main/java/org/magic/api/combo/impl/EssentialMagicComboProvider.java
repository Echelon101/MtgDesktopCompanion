package org.magic.api.combo.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.magic.api.beans.MTGCombo;
import org.magic.api.beans.MagicCard;
import org.magic.api.interfaces.MTGCardsProvider;
import org.magic.api.interfaces.abstracts.AbstractComboProvider;
import org.magic.services.MTGControler;
import org.magic.tools.RequestBuilder;
import org.magic.tools.RequestBuilder.METHOD;
import org.magic.tools.URLTools;
import org.magic.tools.URLToolsClient;

public class EssentialMagicComboProvider extends AbstractComboProvider {

	
	private static final String BASE ="http://www.essentialmagic.com/";
	
	@Override
	public List<MTGCombo> getComboWith(MagicCard mc) {
		List<MTGCombo> ret = new ArrayList<>();
		Document d;
		try {
			URLToolsClient c = URLTools.newClient();
			d=RequestBuilder.build().url(BASE+"Combos/Search.asp").setClient(c).method(METHOD.POST)
					.addContent("selInvalid", getString("GURU_APPROVED_CODE"))
					.addContent("txtName","")
					.addContent("txtCreator","")
					.addContent("cnbtxtCard", mc.getName()) 
					.addContent("cnbhdnCard","5")
					.addContent("selDateCompare","BETWEEN")
					.addContent("txtDate","") 
					.addContent("selFormat","-1")
					.addContent("selThemes","0")
					.addContent("selRatingCompare",">=")
					.addContent("selRatings","0")
					.addContent("selNumRatingsCompare",">=")
					.addContent("selNumRatings","-1")
					.addContent("selRarities","0")
					.addContent("chkColor0","on")
					.addContent("chkColor1","on")
					.addContent("chkColor2","on")
					.addContent("chkColor3","on")
					.addContent("chkColor4","on")
					.addContent("chkColor5","on")
					.addContent("selColorCompare","OR")
					.addContent("btnSearch", "Begin Search")
					.addHeader("Connection", "keep-alive")
					.addHeader(URLTools.HOST, "www.essentialmagic.com")
					.addHeader(URLTools.UPGR_INSECURE_REQ, "1")
					.addHeader(URLTools.ORIGIN, BASE)
					.addHeader(URLTools.REFERER,BASE+"Combos/Search.asp")
					.addHeader(URLTools.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
					.addHeader(URLTools.ACCEPT_ENCODING, "gzip, deflate")
					.addHeader(URLTools.ACCEPT_LANGUAGE, "fr-FR,fr;q=0.9")
					.toHtml();
			
			
			Elements trs = d.select("table.defaulttablestyle tr");
			
			
			trs.forEach(tr->{
				
				MTGCombo cbo = new MTGCombo();
				cbo.setPlugin(this);
				cbo.setName(tr.select("div.Label").text());
				
				if(!cbo.getName().isEmpty())
				{
					Elements as = tr.select("b>a");
					
					for(Element a : as)
					{
						try {
							MagicCard card = MTGControler.getInstance().getEnabled(MTGCardsProvider.class).searchCardByName(a.text(), null, true).get(0);
							cbo.addCard(card);
						} catch (IOException e) {
							logger.error("No card found for " + a.text());
						}
					}
					tr.select("a").remove();
					cbo.setComment(tr.text().replace("(Submitted by ) Rating:","").trim());
					ret.add(cbo);
				}
			});
		} catch (Exception e) {
			logger.error("Error loading " + BASE,e);
			return ret;
		}
		
		
		return ret;
	}

	@Override
	public String getName() {
		return "EssentialMagic";
	}
	
	@Override
	public void initDefault() {
		setProperty("GURU_APPROVED_CODE", "1");
	}

}
