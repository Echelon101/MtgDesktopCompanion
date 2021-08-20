package org.magic.api.wallpaper.impl;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.magic.api.beans.Wallpaper;
import org.magic.api.interfaces.abstracts.AbstractWallpaperProvider;
import org.magic.tools.URLTools;
import org.magic.tools.URLToolsClient;

public class ImgUrWallPaperProvider extends AbstractWallpaperProvider {

	private static final String IMAGES_TAG = "images";
	private static final String TITLE_TAG = "title";
	private static final String CLIENTID="CLIENTID";
	
	@Override
	public List<Wallpaper> search(String search) {
		
		
		List<Wallpaper> ret = new ArrayList<>();
		URLToolsClient c = URLTools.newClient();
		Map<String,String> h = new HashMap<>();
		Map<String,String> e = new HashMap<>();

		if(getString(CLIENTID).isEmpty())
		{
			logger.error("please fill CLIENTID attribute in config panel");
			return ret;
		}
		
		
		try {
			
			String query=search.trim().replace(" ", " AND ");
	
			e.put("q", query);
			e.put("mature", "true");
			h.put("Authorization","Client-ID "+getString(CLIENTID));
			
			
			String s= c.doGet("https://api.imgur.com/3/gallery/search/"+getString("SORT").toLowerCase()+"/"+getString("WINDOW"), h,e);
			
			URLTools.toJson(s).getAsJsonObject().get("data").getAsJsonArray().forEach(je->{
				
				var defaultTitle =je.getAsJsonObject().get(TITLE_TAG).getAsString();
				
				if(je.getAsJsonObject().get(IMAGES_TAG)!=null)
				{
					je.getAsJsonObject().get(IMAGES_TAG).getAsJsonArray().forEach(im->{
						var w = new Wallpaper();
						
						if(!im.getAsJsonObject().get(TITLE_TAG).isJsonNull())
							w.setName(im.getAsJsonObject().get(TITLE_TAG).getAsString());
						else
							w.setName(defaultTitle);
						
						w.setUrl(URI.create(im.getAsJsonObject().get("link").getAsString()));
						w.setFormat(FilenameUtils.getExtension(String.valueOf(w.getUrl())));
						ret.add(w);
						notify(w);
					});
				}
				else
				{
					var w = new Wallpaper();
							w.setName(defaultTitle);
							w.setUrl(URI.create(je.getAsJsonObject().get("link").getAsString()));
							w.setFormat(FilenameUtils.getExtension(String.valueOf(w.getUrl())));
					
					ret.add(w);
					notify(w);
				}
				
				
				
				
			});
		} catch (IOException ex) {
			logger.error(ex);
		}
		
		return ret;

	}


	@Override
	public String getName() {
		return "Imgur";
	}

	@Override
	public Map<String, String> getDefaultAttributes() {
		return Map.of(CLIENTID, "",
							   "SORT", "time",
							   "WINDOW", "all");
	}
}
