package org.magic.api.exports.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicCardStock;
import org.magic.api.beans.MagicDeck;
import org.magic.api.beans.MagicEdition;
import org.magic.api.interfaces.MTGCardsProvider;
import org.magic.api.interfaces.abstracts.AbstractFormattedFileCardExport;
import org.magic.services.MTGControler;
import org.magic.tools.FileTools;

public class DelverLensExport extends AbstractFormattedFileCardExport{

	
	private String columns= "Count,Tradelist Count,Name,Edition,Card Number,Condition,Language,Foil,Signed,Artist Proof,Altered Art,Misprint,Promo,Textless,My Price";
	
	@Override
	public String getFileExtension() {
		return ".csv";
	}

	@Override
	public void exportDeck(MagicDeck deck, File dest) throws IOException {
		exportStock(importFromDeck(deck), dest);
	}
	
	@Override
	public void exportStock(List<MagicCardStock> stock, File f) throws IOException {
		StringBuilder temp = new StringBuilder(columns);
		temp.append(System.lineSeparator());
		stock.forEach(st->{
			temp.append("\"").append(st.getQte()).append("\"").append(getSeparator());
			temp.append("\"").append(st.getQte()).append("\"").append(getSeparator());
			temp.append("\"").append(st.getMagicCard().getName()).append("\"").append(getSeparator());
			temp.append("\"").append(st.getMagicCard().getCurrentSet().getSet()).append("\"").append(getSeparator());
			temp.append("\"").append(st.getMagicCard().getCurrentSet().getNumber()).append("\"").append(getSeparator());
			temp.append("\"").append(st.getCondition()).append("\"").append(getSeparator());
			temp.append("\"").append(st.getLanguage()).append("\"").append(getSeparator());
			temp.append("\"").append(st.isFoil()?"foil":"").append("\"").append(getSeparator());
			temp.append("\"").append(st.isSigned()?"signed":"").append("\"").append(getSeparator());
			temp.append("\"").append("").append("\"").append(getSeparator());
			temp.append("\"").append("").append("\"").append(getSeparator());
			temp.append("\"").append("").append("\"").append(getSeparator());
			temp.append("\"").append("").append("\"").append(getSeparator());
			temp.append("\"").append("").append("\"").append(getSeparator());
			temp.append("\"").append(st.getPrice()).append("\"").append(getSeparator());
			temp.append(System.lineSeparator());
		});
		FileTools.saveFile(f, temp.toString());
	}
	
	@Override
	public List<MagicCardStock> importStock(String content) throws IOException {
		List<MagicCardStock> list = new ArrayList<>();
		
		matches(content,true).forEach(m->{
			
			MagicEdition ed = null;
			
			try {			   
				ed = MTGControler.getInstance().getEnabled(MTGCardsProvider.class).getSetByName(m.group(4));
			}
			catch(Exception e)
			{
				logger.error("Edition not found for " + m.group(4));
			}
			
			String cname = cleanName(m.group(3));
			
			String number=null;
			try {
				number = m.group(5);
			}
			catch(IndexOutOfBoundsException e)
			{
				//do nothing
			}
			
			MagicCard mc=null;
			
			if(number!=null && ed !=null)
			{
				try {
					mc = MTGControler.getInstance().getEnabled(MTGCardsProvider.class).getCardByNumber(number, ed);
				} catch (Exception e) {
					logger.error("no card found with number " + number + "/"+ ed);
				}
			}
			
			if(mc==null)
			{
				try {
					mc = parseMatcherWithGroup(m, 3, 4, true, false,FORMAT_SEARCH.NAME);
				} catch (Exception e) {
					logger.error("no card found for" + cname + "/"+ ed);
				}
			}

			if(mc!=null)
			{
				MagicCardStock st = MTGControler.getInstance().getDefaultStock();
				st.setMagicCard(mc);
				st.setLanguage(m.group(7));
				st.setQte(Integer.parseInt(m.group(1)));
				st.setFoil(m.group(8)!=null);
				st.setSigned(m.group(9)!=null);
				st.setAltered(m.group(11)!=null);
				
				if(!m.group(15).isEmpty())
					st.setPrice(Double.parseDouble(m.group(15)));
				
				list.add(st);
			}
		});
		
		return list;
	}
	
	
	@Override
	public MagicDeck importDeck(String content, String name) throws IOException {
		MagicDeck d = new MagicDeck();
		d.setName(name);
		
		for(MagicCardStock st : importStock(content))
		{
			d.getMap().put(st.getMagicCard(), st.getQte());
		}
		return d;
	}

	@Override
	public String getName() {
		return "DelverLens";
	}

	@Override
	protected boolean skipFirstLine() {
		return true;
	}

	@Override
	protected String[] skipLinesStartWith() {
		return new String[0];
	}

	@Override
	protected String getStringPattern() {
		return "\"(\\d+)\",\"(\\d+)\",\"(.*?)\",\"(.*?)\",\"(\\d+)\",\"(.*?)\",\"(.*?)\",\"(foil)?\",\"(signed)?\",\"(.*?)\",\"(.*?)\",\"(.*?)\",\"(.*?)\",\"(.*?)\",\"(.*?)\"";
	}

	@Override
	protected String getSeparator() {
		return getString("SEPARATOR");
	}
	
	@Override
	public void initDefault() {
		setProperty("SEPARATOR", ",");
	}
	

}
