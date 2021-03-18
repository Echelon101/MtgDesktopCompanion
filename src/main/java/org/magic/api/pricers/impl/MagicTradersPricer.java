package org.magic.api.pricers.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicPrice;
import org.magic.api.interfaces.abstracts.AbstractPricesProvider;
import org.magic.tools.URLTools;

public class MagicTradersPricer extends AbstractPricesProvider {

	public List<MagicPrice> getLocalePrice(MagicCard card) throws IOException {

		URL link = new URL(getString("URL"));
		logger.info(getName() + " looking for prices " + link);

		InputStream is = URLTools.openConnection(link).getInputStream();
		try (BufferedReader read = new BufferedReader(new InputStreamReader(is))) {
			String line;
			List<MagicPrice> list = new ArrayList<>();

			while ((line = read.readLine()) != null) {
				String[] fields = line.split("\\|");
				if (fields.length < 8)
					continue;

				String name = fields[0].trim();
				String price = fields[1].trim();
				double f =0;
				try {
					f = Double.parseDouble(price);
				} catch (NumberFormatException e) {
					logger.error("error parsing "+ price);
				}
					String cname = getCorrectName(card.getName());
					if (name.startsWith(cname)) {
						logger.info(getName() + " found " + cname);
						MagicPrice mp = new MagicPrice();
						mp.setMagicCard(card);
						mp.setSeller(getName());
						mp.setUrl("http://store.eudogames.com/products/search?query="
								+ URLTools.encode(card.getName()));
						mp.setSite(getName());
						mp.setValue(f);
						mp.setCurrency("USD");
						list.add(mp);

						return list;
					}
				

			}
			return list;
		}

	}

	private String getCorrectName(String cname) {
		if (cname.contains("AE")) {
			cname = cname.replace("AE", "Æ");
		}
		int sl = cname.indexOf('/');
		if (sl >= 0) {
			cname = cname.replaceFirst("/", " // ");
			cname += " (" + cname.substring(0, sl) + ")";
		}
		return cname;
	}

	public String getName() {
		return "Magic Traders";
	}

	@Override
	public STATUT getStatut() {
		return STATUT.DEPRECATED;
	}
	
	
	@Override
	public void initDefault() {
		setProperty("URL", "http://classic.magictraders.com/pricelists/current-magic-excel.txt");
		setProperty("WEBSITE", "http://classic.magictraders.com");
		setProperty("KEYWORD", "");

	}

}
