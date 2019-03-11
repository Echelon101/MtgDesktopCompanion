package test.providers;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;
import org.magic.api.beans.MagicEdition;
import org.magic.api.interfaces.MTGCardsProvider;
import org.magic.services.MTGConstants;
import org.magic.services.MTGControler;
import org.magic.services.MTGLogger;
import org.magic.services.PluginRegistry;

public class CardsProviderTests {

	@Before
	public void initTest() throws IOException, URISyntaxException
	{
		MTGConstants.CONF_DIR = new File(System.getProperty("user.home") + "/.magicDeskCompanion-test/");
		MTGControler.getInstance();
		MTGLogger.changeLevel(Level.DEBUG);
	}
	
	@Test
	public void launch()
	{
		PluginRegistry.inst().listPlugins(MTGCardsProvider.class).forEach(p->{
			testPlugin(p);	
		});
	}
	
	
	
	public void testPlugin(MTGCardsProvider p)
	{
		
			p.init();
			System.out.println("*****************************"+p.getName());
			System.out.println("STAT "+p.getStatut());
			System.out.println("PROP "+p.getProperties());
			System.out.println("TYPE "+p.getType());
			System.out.println("ENAB "+p.isEnable());
			System.out.println("ICON "+p.getIcon());
			System.out.println("VERS "+p.getVersion());
			System.out.println("JMX NAME "+p.getObjectName());
			System.out.println("CONF FILE " + p.getConfFile());
			System.out.println("ATTS " + p.getQueryableAttributs());
			System.out.println("LANG " + p.getLanguages());
		
			
			try {
				System.out.println("WEBSITE " + p.getWebSite());
			} catch (MalformedURLException e1) {
				System.err.println(e1);
			}
			
			
			
			try {
				p.loadEditions();
				System.out.println("LOAD EDITION :OK");
			} catch (Exception e) {
				System.out.println("LOAD EDITION :ERROR " + e);
				e.printStackTrace();
			}
			try {
				p.searchCardByName( "Black Lotus", new MagicEdition("LEA"), true);
				System.out.println("SEARCH CARD :OK");
			} catch (Exception e) {
				System.out.println("SEARCH CARD :ERROR " + e);
			}
			try {
				p.searchCardByName( "Black Lotus", null, false);
				System.out.println("SEARCH CARD :OK");
			} catch (Exception e) {
				System.out.println("SEARCH CARD :ERROR " + e);
			}
			try {
				p.getSetById("LEA");
				System.out.println("SET BY ID :OK");
			} catch (Exception e) {
				System.out.println("SET BY ID :ERROR " + e);
			}
			
			try {
				p.getCardByNumber("124", new MagicEdition("LEA"));
				System.out.println("CARD BY NUMBER :OK");
			} catch (Exception e) {
				System.out.println("CARD BY NUMBER :ERROR " + e);
			}
		
			try {
				p.generateBooster(new MagicEdition("LEA"));
				System.out.println("BOOSTER GEN :OK");
			} catch (Exception e) {
				System.out.println("BOOSTER GEN :ERROR " + e);
			}
			
			try {
				p.getSetByName("Futur Sight");
				System.out.println("Search set by Name :OK");
			} catch (Exception e) {
				System.out.println("Search set by Name " + e);
			}
			
			
			
		
	}
	
	
}
