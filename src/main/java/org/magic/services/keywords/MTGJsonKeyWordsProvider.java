package org.magic.services.keywords;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.magic.api.beans.MTGKeyWord;
import org.magic.api.beans.MTGKeyWord.TYPE;
import org.magic.api.interfaces.abstracts.AbstractMTGJsonProvider;
import org.magic.tools.URLTools;

import com.google.gson.JsonObject;


public class MTGJsonKeyWordsProvider extends AbstractKeyWordsManager {

	private List<MTGKeyWord> list;
	
	@Override
	public List<MTGKeyWord> getList() {
		
		
		if(list==null)
		{
			list = new ArrayList<>();	
			JsonObject el;
			try {
				el = URLTools.extractJson(AbstractMTGJsonProvider.MTG_JSON_KEYWORDS).getAsJsonObject().get("data").getAsJsonObject();
				el.get("abilityWords").getAsJsonArray().forEach(s->list.add(new MTGKeyWord(s.getAsString(),TYPE.WORD)));
				el.get("keywordAbilities").getAsJsonArray().forEach(s->list.add(new MTGKeyWord(s.getAsString(),TYPE.ABILITIES)));
				el.get("keywordActions").getAsJsonArray().forEach(s->list.add(new MTGKeyWord(s.getAsString(),TYPE.ACTION)));
				
				list.add(new MTGKeyWord("Flip",TYPE.ACTION));
				
			} catch (IOException e) {
				logger.error(e);
			}
		
		}
		
		return list;
	}

	@Override
	public List<MTGKeyWord> getWordsAbilities() {
		return getList().stream().filter(p->p.getType()==TYPE.WORD).collect(Collectors.toList());
	}
	

	@Override
	public List<MTGKeyWord> getStaticsAbilities() {
		return getList().stream().filter(p->p.getType()==TYPE.WORD).collect(Collectors.toList());
	}


	@Override
	public List<MTGKeyWord> getActivatedAbilities() {
		return getList().stream().filter(p->p.getType()==TYPE.ABILITIES).collect(Collectors.toList());
	}


	@Override
	public List<MTGKeyWord> getTriggeredAbilities() {
		return getActivatedAbilities();
	}


	@Override
	public List<MTGKeyWord> getKeywordActions() {
		return getList().stream().filter(p->p.getType()==TYPE.ACTION).collect(Collectors.toList());
	}

	
	
}
