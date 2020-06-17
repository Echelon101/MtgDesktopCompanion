import org.magic.services.*;
import org.magic.api.interfaces.*;
import org.magic.api.exports.impl.*;
import org.magic.api.beans.*;
import java.io.File;

String importerName = "XMage";
String exporterName = "Forge";
File deckFile=new File("D:\\Téléchargements\\Sliver.Overlord--Commander--XMage.dck.txt");
File deckFileExport=new File("D:\\Téléchargements\\Sliver.Overlord--Commander--XMage-Export.dck");


MTGCardsExport importer = controler.getPlugin(importerName,MTGCardsExport.class);
MTGCardsExport exporter = controler.getPlugin(exporterName,MTGCardsExport.class);

MagicDeck deck = importer.importDeckFromFile(deckFile);
println(deck.getName()+ " is loaded");

exporter.exportDeck(deck,deckFileExport);