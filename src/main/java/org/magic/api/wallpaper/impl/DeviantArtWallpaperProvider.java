package org.magic.api.wallpaper.impl;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.magic.api.beans.Wallpaper;
import org.magic.api.interfaces.abstracts.AbstractWallpaperProvider;
import org.magic.tools.RequestBuilder;
import org.magic.tools.RequestBuilder.METHOD;
import org.magic.tools.URLTools;

import com.google.gson.JsonObject;

public class DeviantArtWallpaperProvider extends AbstractWallpaperProvider {

	
	private static final String LIMIT = "LIMIT";
	private static final String CLIENT_ID="CLIENT_ID";
	private static final String BASE_URL = "https://www.deviantart.com";
	private RequestBuilder build;
	private String bToken;
	
	@Override
	public STATUT getStatut() {
		return STATUT.DEV;
	}
		
	@Override
	public List<Wallpaper> search(String search) {
		
		List<Wallpaper> list = new ArrayList<>();
		try {
			
			if(getString(CLIENT_ID).isEmpty())
				{
					logger.error("please fill CLIENT_ID && CLIENT_SECRET attributs in config panel");
					return list;
				}
			
			
			
			build = RequestBuilder.build();
		    bToken = build.setClient(URLTools.newClient())
								   .method(METHOD.GET)
								   .url(BASE_URL+"/oauth2/token")
								   .addContent("grant_type", "client_credentials")
								   .addContent("client_id", getString(CLIENT_ID))
								   .addContent("client_secret", getString("CLIENT_SECRET"))
								   .toJson().getAsJsonObject().get("access_token").getAsString();
		    
		    var offset = 0;
		    var ret= readOffset(offset,search);
				    while(ret.get("has_more").getAsBoolean()) 
				    {
				    	logger.trace(ret);
					    ret.get("results").getAsJsonArray().forEach(el->{
					    	
					    	logger.trace(el);
					    	try {
					    		var p = new Wallpaper();
					    		p.setFormat("png");
					    		p.setName(el.getAsJsonObject().get("title").getAsString());
					    		p.setUrl(new URI(el.getAsJsonObject().get("content").getAsJsonObject().get("src").getAsString()));
					    		list.add(p);
							} catch (Exception e) {
								logger.error("Error for " + el.getAsJsonObject().get("title"),e);
							}
					    });
					    
					    if(list.size()>=getInt(LIMIT))
			    			break;
			    		
					    ret = readOffset(ret.get("next_offset").getAsInt(), search);
				    }
		    
			} catch (Exception e) {
				logger.error("error",e);
			}
		
		return list;
	}

	private JsonObject readOffset(int offset,String search) throws IOException {
		return  build.clean()
				  .method(METHOD.GET)
				  .url(BASE_URL+"/api/v1/oauth2/browse/newest")
				  .addContent("q", search)
				  .addContent("limit", getString(LIMIT))
				  .addContent("offset", String.valueOf(offset))
				  .addContent("mature_content", getString("MATURE"))
				  .addContent("access_token", bToken)
				  .toJson().getAsJsonObject();
	}

	@Override
	public String getName() {
		return "DeviantArt";
	}

	@Override
	public void initDefault() {
		setProperty(CLIENT_ID, "");
		setProperty("CLIENT_SECRET", "");
		setProperty("MATURE","false");
		setProperty(LIMIT,"50");
	}

}
