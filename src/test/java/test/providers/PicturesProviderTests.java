package test.providers;

import static org.magic.tools.MTG.listPlugins;

import java.util.List;

import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;
import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicEdition;
import org.magic.api.beans.enums.MTGBorder;
import org.magic.api.beans.enums.MTGLayout;
import org.magic.api.beans.enums.MTGRarity;
import org.magic.api.cache.impl.NoCache;
import org.magic.api.interfaces.MTGPictureCache;
import org.magic.api.interfaces.MTGPictureProvider;
import org.magic.api.pictures.impl.DeckMasterPicturesProvider;
import org.magic.api.pictures.impl.GathererPicturesProvider;
import org.magic.api.pictures.impl.MagidexPicturesProvider;
import org.magic.api.pictures.impl.MythicSpoilerPicturesProvider;
import org.magic.api.pictures.impl.PersonalSetPicturesProvider;
import org.magic.api.pictures.impl.ScryFallPicturesProvider;
import org.magic.services.MTGLogger;


public class PicturesProviderTests {

	MagicCard mc;
	MagicEdition ed;
	
	
	public void removeCache()
	{
		MTGLogger.changeLevel(Level.ERROR);
		
		List<MTGPictureCache> caches = listPlugins(MTGPictureCache.class);
		listPlugins(MTGPictureCache.class).removeAll(caches);
		
		MTGPictureCache cache = new NoCache();
		cache.enable(true);
		
		listPlugins(MTGPictureCache.class).add(cache);
	}

	
	@Before
	public void createCards()
	{
		mc = new MagicCard();
		mc.setName("Black Lotus");
		mc.setLayout(MTGLayout.NORMAL);
		mc.setCost("{0}");
		mc.setCmc(0);
		mc.getTypes().add("Artifact");
		mc.setReserved(true);
		mc.setText("{T}, Sacrifice Black Lotus: Add three mana of any one color to your mana pool.");
		mc.setRarity(MTGRarity.RARE);
		mc.setArtist("Christopher Rush");
		mc.setId("c944c7dc960c4832604973844edee2a1fdc82d98");
		mc.setBorder(MTGBorder.BLACK);
				
		MagicEdition ed = new MagicEdition();
					 ed.setId("lea");
					 ed.setSet("Limited Edition Alpha");
					 ed.setMultiverseid("3");
					 ed.setNumber("232");
		
		mc.getEditions().add(ed);
	}
	
	
	@Test
	public void test()
	{
		testProviders(new ScryFallPicturesProvider());
		testProviders(new GathererPicturesProvider());
		testProviders(new ScryFallPicturesProvider());
		testProviders(new MagidexPicturesProvider());
		testProviders(new MythicSpoilerPicturesProvider());
		testProviders(new DeckMasterPicturesProvider());
		testProviders(new PersonalSetPicturesProvider());
		
		removeCache();
		
		testProviders(new ScryFallPicturesProvider());
		testProviders(new GathererPicturesProvider());
		testProviders(new ScryFallPicturesProvider());
		testProviders(new MagidexPicturesProvider());
		testProviders(new MythicSpoilerPicturesProvider());
		testProviders(new DeckMasterPicturesProvider());
		testProviders(new PersonalSetPicturesProvider());
		
	}
	
	public void testProviders(MTGPictureProvider p)
	{
		
		
		System.out.println("****************"+p);
		System.out.println(p.getStatut());
		System.out.println(p.getType());
		System.out.println("VERS "+p.getVersion());
		
		
		try {
			p.getPicture(mc);
			System.out.println("getPictures OK" );
		} catch (Exception e) {
			System.out.println("getPictures ERROR "+e );
		}

		try {
			p.extractPicture(mc);
			System.out.println("extractPicture OK" );
		} catch (Exception e) {
			System.out.println("getPictures ERROR " +e);
		}

		try {
			p.getSetLogo(ed.getId(), "Rare");
			System.out.println("getLogo OK" );
		} catch (Exception e) {
			System.out.println("getLogo ERROR "+e );
		}
		
		try {
			p.getPicture(mc);
			System.out.println("getPictures cache OK" );
		} catch (Exception e) {
			System.out.println("getPictures ERROR "+e );
		}
		
		p.getBackPicture();

		
		
	}
	
	
}
