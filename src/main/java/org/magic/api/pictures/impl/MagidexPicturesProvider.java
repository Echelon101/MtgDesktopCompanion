package org.magic.api.pictures.impl;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;

import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicEdition;
import org.magic.api.interfaces.abstracts.AbstractPicturesProvider;
import org.magic.tools.ImageTools;
import org.magic.tools.URLTools;

public class MagidexPicturesProvider extends AbstractPicturesProvider {

	@Override
	public STATUT getStatut() {
		return STATUT.DEV;
	}

	
	@Override
	public String generateUrl(MagicCard mc) {
		String cardName = mc.getName().toLowerCase();
		String cardSet = mc.getCurrentSet().getId();

		if (mc.getName().contains("//")) {
			cardName = cardName.replace("//", "");
		}

		try {
			return new URI("http", "magidex.com", "/extstatic/card/" + cardSet.toUpperCase() + '/' + cardName + ".jpg",null, null).toString();
		} catch (URISyntaxException e1) {
			return "";
		}
	}


	@Override
	public String getName() {
		return "MagiDex";
	}

	@Override
	public BufferedImage extractPicture(MagicCard mc) throws IOException {
		return null;
	}

	

}
