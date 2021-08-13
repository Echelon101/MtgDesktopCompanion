package test.providers;

import static org.magic.tools.MTG.getEnabledPlugin;

import java.io.IOException;

import org.apache.log4j.Level;
import org.junit.Test;
import org.magic.api.beans.MagicEdition;
import org.magic.api.interfaces.MTGCardsProvider;
import org.magic.services.MTGControler;
import org.magic.services.MTGLogger;
import org.magic.services.providers.SealedProductProvider;
import org.magic.tools.URLTools;

public class BoosterProviderTests {

	
	@Test
	public void test()
	{
		MTGControler.getInstance();
		getEnabledPlugin(MTGCardsProvider.class).init();
		MTGLogger.changeLevel(Level.OFF);
		SealedProductProvider prov = SealedProductProvider.inst();
		for(MagicEdition id : prov.listEditions())
		{
			prov.getItemsFor(id).forEach(e->{
				try {
					URLTools.extractImage(e.getUrl());
					System.out.println(e+";"+e.getType()+";"+e.getEdition().getId()+";OK");
				} catch (IOException e1) {
					System.out.println(e+";"+e.getType()+";"+e.getEdition().getId()+";KO;"+e1);
				}
			});
			
			
			
			
		}
		
	}
	
		
	
}
