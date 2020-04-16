package org.magic.api.exports.impl;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;

import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicDeck;
import org.magic.api.interfaces.MTGCardsProvider;
import org.magic.api.interfaces.abstracts.AbstractFormattedFileCardExport;
import org.magic.services.MTGControler;
import org.magic.tools.FileTools;

public class MTGODeckExport extends AbstractFormattedFileCardExport {


	@Override
	public String getName() {
		return "MTGO";
	}

	@Override
	public String getFileExtension() {
		return ".dek";
	}

	@Override
	public void exportDeck(MagicDeck deck, File dest) throws IOException {
		StringBuilder temp = new StringBuilder();

		temp.append("//NAME: " + deck.getName() + " from MTGDeskTopCompanion\n");
		temp.append("//MAIN\n");
		for (MagicCard mc : deck.getMain().keySet()) {
			temp.append(deck.getMain().get(mc)).append(" ").append(mc.getName()).append("\n");
			notify(mc);
		}
		temp.append("\n//Sideboard\n");
		for (MagicCard mc : deck.getSideBoard().keySet()) {
			temp.append(deck.getSideBoard().get(mc)).append(" ").append(mc.getName()).append("\n");
			notify(mc);
		}

		FileTools.saveFile(dest, temp.toString());
	}
	
	@Override
	public MagicDeck importDeck(String f, String deckName) throws IOException {
			MagicDeck deck = new MagicDeck();
			deck.setName(deckName);
			boolean side=false;
			
			for(Matcher m : matches(f,false))
			{
				if(m.group().isEmpty())
				{
					side=true;
				}
				else
				{
					String cname = cleanName(m.group(2));
					try{
						MagicCard mc = MTGControler.getInstance().getEnabled(MTGCardsProvider.class).searchCardByName(cname, null, true).get(0);
						int qty = Integer.parseInt(m.group(1));
						
						if(side)
						{
							deck.getSideBoard().put(mc, qty);
						}
						else
						{
							deck.getMain().put(mc, qty);
						}
						
						notify(mc);
					}
					catch(Exception e)
					{
						logger.error(cname + " is not found : " + e.getMessage());
					}
				}
			}
			return deck;
		

	}

	@Override
	protected boolean skipFirstLine() {
		return false;
	}

	@Override
	protected String[] skipLinesStartWith() {
		return new String[] {"//"};
	}

	@Override
	protected String getStringPattern() {
		return "^\\s*$|(\\d+) (.*?)$";
	}

	@Override
	protected String getSeparator() {
		return " ";
	}

	

}
