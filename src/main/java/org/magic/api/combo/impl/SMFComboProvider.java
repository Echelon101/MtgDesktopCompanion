package org.magic.api.combo.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.magic.api.beans.MTGCombo;
import org.magic.api.beans.MagicCard;
import org.magic.api.interfaces.abstracts.AbstractComboProvider;
import org.magic.tools.RequestBuilder;
import org.magic.tools.RequestBuilder.METHOD;
import org.magic.tools.URLTools;
import org.magic.tools.URLToolsClient;

public class SMFComboProvider extends AbstractComboProvider {

	private static final String BASE_URL="https://www.smfcorp.net";
	
	@Override
	public List<MTGCombo> loadComboWith(MagicCard mc) {
		
		List<MTGCombo> cbos = new ArrayList<>();
		URLToolsClient c = URLTools.newClient();
		String cardUri;
		try {
			Document d = RequestBuilder.build().url(BASE_URL+"/include/php/views/moteurCartes.php").method(METHOD.GET).setClient(c)
						  .addContent("ihm", "M1")
						  .addContent("order", "car_nomfr")
						  .addContent("page", "1")
						  .addContent("tri", "asc")
						  .addContent("language", "FR")
						  .addContent("nom", mc.getName())
						  .addHeader(URLTools.ORIGIN, BASE_URL)
						  .addHeader(URLTools.REFERER, BASE_URL)
						  .addHeader(URLTools.X_REQUESTED_WITH,"XMLHttpRequest")
						  .addHeader("sec-fetch-mode","core")
						  .addHeader("sec-fetch-site","same-origin")
						  .toHtml();
			
			cardUri=d.select("tr.contentTrPair>td>a").attr("href");
			cardUri = BASE_URL+cardUri.substring(cardUri.lastIndexOf('/'));
			
		} catch (Exception e) {
			logger.error(e);
			return cbos;
		}
		
		 try {
			Document d = RequestBuilder.build().url(cardUri).method(METHOD.GET).setClient(c).toHtml();
			String idAttribute= d.getElementById("dataAttribute").attr("value");
			
			d = RequestBuilder.build().url(BASE_URL+"/index.php").method(METHOD.GET).setClient(c)
						.addContent("objet", "combos")
						.addContent("action", "refreshPage")
						.addContent("nb", "0")
						.addContent("dataIsValidated", "1")
						.addContent("dataAttribute", idAttribute)
						.toHtml();
			
			
			d.select("div.media-body").forEach(el->{
				
				MTGCombo cbo = new MTGCombo();
						 cbo.setName(el.getElementsByTag("h4").text());
						 cbo.setPlugin(this);
					 try {
						Document details = RequestBuilder.build().url(BASE_URL+"/"+el.getElementsByTag("a").attr("href")).method(METHOD.GET).setClient(c).toHtml();
						Elements article = details.getElementsByTag("article");
						article.select("div.panel").remove();
						cbo.setComment(article.text());
						cbos.add(cbo);				
					} catch (IOException e) {
						logger.error(e);
					}
			});
			
		} catch (IOException e) {
			logger.error(e);
			return cbos;
		}
		
		
		
		
		
		
		return cbos;
	}

	@Override
	public String getName() {
		return "SMF";
	}

}
