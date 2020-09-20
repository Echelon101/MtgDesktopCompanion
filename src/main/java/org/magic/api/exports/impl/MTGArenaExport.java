package org.magic.api.exports.impl;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.apache.commons.lang3.StringUtils;
import org.magic.api.beans.MTGNotification;
import org.magic.api.beans.MTGNotification.MESSAGE_TYPE;
import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicDeck;
import org.magic.api.beans.MagicEdition;
import org.magic.api.interfaces.MTGCardsProvider;
import org.magic.api.interfaces.abstracts.AbstractFormattedFileCardExport;
import org.magic.services.MTGControler;

public class MTGArenaExport extends AbstractFormattedFileCardExport {
	
	Map<String,String> correpondance;
	boolean side=false;
	
	public MTGArenaExport() {
		correpondance = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		correpondance.put("DOM", "DAR");
		
		
	}
	
	@Override
	public String getFileExtension() {
		return "";
	}
	
	@Override
	public boolean needFile() {
		return false;
	}

	@Override
	public void exportDeck(MagicDeck deck, File dest) throws IOException {
		
		StringBuilder temp = new StringBuilder();
		
		for(Map.Entry<MagicCard, Integer> entry : deck.getMain().entrySet())
		{
			temp.append(entry.getValue())
				.append(" ")
				.append(entry.getKey())
				.append(" (")
				.append(translate(entry.getKey().getCurrentSet().getId()).toUpperCase())
				.append(")")
				.append(" ")
				.append(entry.getKey().getCurrentSet().getNumber())
				.append("\r\n");
			notify(entry.getKey());
			
		}
		
		if(!deck.getSideBoard().isEmpty())
			for(Map.Entry<MagicCard, Integer> entry : deck.getSideBoard().entrySet())
			{
				temp.append("\r\n")
					.append(entry.getValue())
					.append(" ")
					.append(entry.getKey())
					.append(" (")
					.append(translate(entry.getKey().getCurrentSet().getId()).toUpperCase())
					.append(")")
					.append(" ")
					.append(entry.getKey().getCurrentSet().getNumber());
				notify(entry.getKey());
			}

		StringSelection selection = new StringSelection(temp.toString());
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
		
		logger.debug("saved in clipboard");

	}
	

	private String reverse(String s) {
		
		if(correpondance.containsValue(s))
			for(Entry<String, String> k : correpondance.entrySet())
				if(k.getValue().equalsIgnoreCase(s))
					return k.getKey();
		
		return s;
	}
	
	
	private String translate(String s) {
		
		if(correpondance.get(s)!=null)
			return correpondance.get(s);
		else
			return s;
	}

	@Override
	public MagicDeck importDeck(String f,String dname) throws IOException {
		MagicDeck deck = new MagicDeck();
		deck.setName(dname);
		side=false;
		Transferable trf = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this);
		
		if(trf==null)
		{
			MTGControler.getInstance().notify(new MTGNotification("Error", "Clipboard is empty", MESSAGE_TYPE.INFO));
			return deck;
		}
		
		String txt ="";
		try {
			txt =trf.getTransferData(DataFlavor.stringFlavor).toString();
		
			logger.debug("copy from clipboard ok : " + txt);
		} catch (UnsupportedFlavorException e) {
			throw new IOException(e);
		} 
	
		matches(txt,false).forEach(m->
		{
			if(StringUtils.isAllEmpty(m.group()))
			{
				side=true;
			}
			else
			{
				try {
					int qte = Integer.parseInt(m.group(1));
					String name = m.group(2).trim();
					String ed =  reverse( m.group(3).trim());
					MagicEdition me = MTGControler.getInstance().getEnabled(MTGCardsProvider.class).getSetById(ed);
					MagicCard mc = MTGControler.getInstance().getEnabled(MTGCardsProvider.class).searchCardByName( name.trim(), me, true).get(0);
					notify(mc);
					if(!side)
						deck.getMain().put(mc, qte);
					else
						deck.getSideBoard().put(MTGControler.getInstance().getEnabled(MTGCardsProvider.class).searchCardByName( name.trim(), me, true).get(0), qte);
				
				}
				catch(Exception e)
				{
					logger.error("Error loading cards " + m.group(),e);
				}
				
			}
			
		});
		
		return deck;
		
	}


	@Override
	public String getName() {
		return "MTGArena";
	}

	@Override
	public STATUT getStatut() {
		return STATUT.DEV;
	}
	

	@Override
	public int hashCode() {
		return getName().hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if(obj ==null)
			return false;
		
		return hashCode()==obj.hashCode();
	}

	@Override
	protected boolean skipFirstLine() {
		return false;
	}

	@Override
	protected String[] skipLinesStartWith() {
		return new String[] {"//","Deck","Sideboard"};
	}

	@Override
	protected String getStringPattern() {
		return "^\\s*$|(\\d+) (.*?) \\((.*?)\\) (\\d+)$";
	}

	@Override
	protected String getSeparator() {
		return " ";
	}

	@Override
	public Icon getIcon() {
		return new ImageIcon(MTGArenaExport.class.getResource("/icons/plugins/mtgarena.png"));
	}

}
