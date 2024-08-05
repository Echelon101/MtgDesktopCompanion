package test.network;

import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.magic.api.beans.MTGCard;
import org.magic.api.beans.MTGCardStock;
import org.magic.api.beans.game.Player;
import org.magic.api.beans.messages.SearchAnswerMessage;
import org.magic.api.beans.messages.SearchMessage;
import org.magic.api.interfaces.MTGCardsProvider;
import org.magic.api.network.impl.ActiveMQNetworkClient;
import org.magic.servers.impl.ActiveMQServer;
import org.magic.services.MTGControler;
import org.magic.services.tools.CryptoUtils;
import org.magic.services.tools.MTG;

public class VirtualUser {
	
	private MTGCardsProvider prov;
	private ArrayList<MTGCardStock> stock;




	@Before
	public void initTest() throws IOException
	{
		MTGControler.getInstance();
		prov = MTG.getEnabledPlugin(MTGCardsProvider.class);
		prov.init();
		
		initStock();
		
	}
	
	public void initStock() throws IOException
	{
		
		stock = new ArrayList<MTGCardStock>();
		
		for(var card : prov.searchCardByName(JOptionPane.showInputDialog("card name for stock initialization"), null, false))
		{
			var mcs = MTGControler.getInstance().getDefaultStock();
			mcs.setProduct(card);
			mcs.setQte(CryptoUtils.randomInt(20));
			mcs.setPrice(CryptoUtils.randomDouble(0.0,50000.0));
			stock.add(mcs);
		}
		
	}
	
	
	
	
	@Test
	public void join() throws IOException
	{
		var client = new ActiveMQNetworkClient();
		var address = "tcp://mtgcompanion.me:61616";
		var p = new Player(JOptionPane.showInputDialog("Player Name"));
		client.join(p,address,ActiveMQServer.DEFAULT_ADDRESS);
		while(client.isActive())
		{
			var msg = client.consume();
			
			System.out.println(p.getName() + " read " + msg);
			
			if(msg instanceof SearchMessage search)
			{
				var c = (MTGCard)search.getItem();
				var ret = stock.stream().filter(mcs->mcs.getProduct().getId().equals(c.getId())).toList();
				if(!ret.isEmpty())
				{
					client.sendMessage(new SearchAnswerMessage(search, ret));
				}
				
			}
		}
	}
	
}
