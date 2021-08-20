package org.magic.api.exports.impl;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicDeck;
import org.magic.api.interfaces.abstracts.AbstractFormattedFileCardExport;
import org.magic.tools.FileTools;

public class MKMFileWantListExport extends AbstractFormattedFileCardExport {

	@Override
	public MagicDeck importDeck(String f,String dname) throws IOException {

		var deck = new MagicDeck();
		deck.setName(dname);
		
		matches(f,true).forEach(m->{
			var qty = Integer.parseInt(m.group(1));
			var mc = parseMatcherWithGroup(m, 2, 3, false, FORMAT_SEARCH.NAME,FORMAT_SEARCH.NAME);
			if(mc!=null)
			{
				deck.getMain().put(mc, qty);
				notify(mc);
			}
		});
		return deck;

	}


	@Override
	public void exportDeck(MagicDeck deck, File dest) throws IOException {

		var temp = new StringBuilder();
			
			for (MagicCard mc : deck.getMain().keySet()) {
				if (mc.getCurrentSet().getMkmName() != null)
					temp.append(deck.getMain().get(mc)).append(getSeparator()).append(mc.getName()).append(getSeparator()).append("(").append(mc.getCurrentSet().getMkmName());
				else
					temp.append(deck.getSideBoard().get(mc)).append(getSeparator()).append(mc.getName()).append(getSeparator()).append("(").append(mc.getCurrentSet().getSet());
			
				try {
					
					if(mc.isPromoCard())
					{
						temp.append(": Promos");
					}
					else
					{
						if(mc.isExtraCard())
							temp.append(": Extras");
					}					
				}catch (Exception e) {
					//do nothing
				}
				
				temp.append(")\n");
				
				
				notify(mc);
			}
			
			for (MagicCard mc : deck.getSideBoard().keySet()) 
			{
				if (mc.getCurrentSet().getMkmName() != null)
					temp.append(deck.getSideBoard().get(mc)).append(getSeparator()).append(mc.getName()).append(getSeparator()).append("(").append(mc.getCurrentSet().getMkmName()).append(")\n");
				else
					temp.append(deck.getSideBoard().get(mc)).append(getSeparator()).append(mc.getName()).append(getSeparator()).append("(").append(mc.getCurrentSet().getSet()).append(")\n");
				notify(mc);
			}
			FileTools.saveFile(dest, temp.toString());
		
	}

	

	@Override
	public String getFileExtension() {
		return ".txt";
	}
	
	
	@Override
	public String getName() {
		return "MKM File";
	}

	@Override
	public Icon getIcon() {
		return new ImageIcon(MKMFileWantListExport.class.getResource("/icons/plugins/magiccardmarket.png"));
	}

	@Override
	protected boolean skipFirstLine() {
		return false;
	}

	@Override
	protected String[] skipLinesStartWith() {
		return new String[0];
	}

	@Override
	protected String getStringPattern() {
		return "(\\d+)"+getSeparator()+"(.*?)"+getSeparator()+"\\((.*?)\\)";
	}

	@Override
	protected String getSeparator() {
		return " ";
	}
	
	
	@Override
	public Map<String, String> getDefaultAttributes() {
		
		var m = super.getDefaultAttributes();
		m.put("SEPARATOR", getSeparator());
		return m;
	}
	

}
