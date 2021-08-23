package org.magic.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.http.entity.ByteArrayEntity;
import org.apache.log4j.Logger;
import org.magic.api.beans.AccountAuthenticator;
import org.magic.api.exports.impl.JsonExport;
import org.magic.api.exports.impl.WooCommerceExport;
import org.magic.services.MTGConstants;
import org.magic.services.MTGLogger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.icoderman.woocommerce.ApiVersionType;
import com.icoderman.woocommerce.HttpMethod;
import com.icoderman.woocommerce.WooCommerce;
import com.icoderman.woocommerce.oauth.OAuthConfig;
import com.icoderman.woocommerce.oauth.OAuthSignature;

public class WooCommerceTools {

	protected static Logger logger = MTGLogger.getLogger(WooCommerceTools.class);

	public static final String WOO_COMMERCE_NAME = "WooCommerce";
	public static final String WOO_COMMERCE_VERSION="V3";

	public static final String WEBSITE = "WEBSITE";
	public static final String CONSUMER_KEY = "CONSUMER_KEY";
	public static final String CONSUMER_SECRET = "CONSUMER_SECRET";

	
	private WooCommerceTools() {}
	
	public static List<String> generateKeysForWooCommerce()
	{
		return List.of(WEBSITE,CONSUMER_KEY,CONSUMER_SECRET);
	}
	
	public static WooCommerce newClient(AccountAuthenticator p, String website)
	{
		return newClient(p.get("CONSUMER_KEY"), p.get("SECRET_KEY") , website, WOO_COMMERCE_VERSION);
	}
	
	public static WooCommerce newClient(Properties p)
	{
		return newClient(p.getProperty(CONSUMER_KEY), p.getProperty(CONSUMER_SECRET), p.getProperty(WEBSITE), WOO_COMMERCE_VERSION);
	}
	
	public static WooCommerce newClient(AccountAuthenticator p)
	{
		return newClient(p.get(CONSUMER_KEY), p.get(CONSUMER_SECRET), p.get(WEBSITE), WOO_COMMERCE_VERSION);
	}
	
	public static WooCommerce newClient(String key, String secret, String website,String version)
	{
		return new WooCommerce() {
			
			private OAuthConfig config = new OAuthConfig(website,key,secret);
			private ApiVersionType apiVersion = ApiVersionType.valueOf(version);
			private static final String API_URL_FORMAT = "%s/wp-json/wc/%s/%s";
		    private static final String API_URL_BATCH_FORMAT = "%s/wp-json/wc/%s/%s/batch";
		    private static final String API_URL_ONE_ENTITY_FORMAT = "%s/wp-json/wc/%s/%s/%d";
		    private static final String URL_SECURED_FORMAT = "%s?%s";
			private final String contentType=URLTools.HEADER_JSON +"; charset="+MTGConstants.DEFAULT_ENCODING.name();
		    
		    
			@Override
			public Map<String,JsonElement> update(String endpointBase, int id, Map<String, Object> object) {
				Map<String,JsonElement> map = new HashMap<>();
				try {
					var url = String.format(API_URL_ONE_ENTITY_FORMAT, config.getUrl(), apiVersion, endpointBase,id);
					URLToolsClient c = URLTools.newClient();
					Map<String,String> header = new HashMap<>();
									   header.put(URLTools.CONTENT_TYPE, contentType);
									   
					String ret = c.doPut(url+"?"+OAuthSignature.getAsQueryString(config, url, HttpMethod.PUT), new ByteArrayEntity(new JsonExport().toJson(object).getBytes(MTGConstants.DEFAULT_ENCODING)), header);
					
					var obj = URLTools.toJson(ret).getAsJsonObject();
					obj.entrySet().forEach(e->map.put(e.getKey(), e.getValue()));
				} catch (IOException e) {
					logger.error(e);
				}
				
				
				return map;
			}
		
			@Override
			public Map<String,JsonElement> create(String endpointBase, Map<String, Object> object) {
				
				Map<String,JsonElement> map = new HashMap<>();
				try {
					var url = String.format(API_URL_FORMAT, config.getUrl(), apiVersion, endpointBase);
					var c = URLTools.newClient();
					Map<String,String> header = new HashMap<>();
									   header.put(URLTools.CONTENT_TYPE, contentType);
									   
					var ret = "";	
									   
					if(object.get("post")==null)				   
					{
						ret = c.doPost(url+"?"+OAuthSignature.getAsQueryString(config, url, HttpMethod.POST), new ByteArrayEntity(new JsonExport().toJson(object).getBytes(MTGConstants.DEFAULT_ENCODING)), header);
					}
					else
					{
						ret = c.doPost(url+"?"+OAuthSignature.getAsQueryString(config, url, HttpMethod.POST), new ByteArrayEntity(object.get("post").toString().getBytes(MTGConstants.DEFAULT_ENCODING)), header);
					}
					
					var obj = URLTools.toJson(ret).getAsJsonObject();
					obj.entrySet().forEach(e->map.put(e.getKey(), e.getValue()));
				} catch (Exception e) {
					logger.error(e);
				}
				
				
				return map;
			}
			
			
			@Override
			public List<JsonElement> getAll(String endpointBase, Map<String, String> params) {
			
				var url = String.format(API_URL_FORMAT, config.getUrl(), apiVersion, endpointBase);
				var signature = OAuthSignature.getAsQueryString(config, url, HttpMethod.GET, params);
				var securedUrl = String.format(URL_SECURED_FORMAT, url, signature);
			    List<JsonElement> ret = new ArrayList<>();
		        try 
		        {
		        	
		        	for(JsonElement e : URLTools.extractJson(securedUrl).getAsJsonArray())
		        		ret.add(e);
		        	
				} catch (Exception e) {
					logger.error(e);
				}
		        return ret;
			}
			
			@Override
			public Map<String,JsonElement> get(String endpointBase, int id) {
				var url = String.format(API_URL_ONE_ENTITY_FORMAT, config.getUrl(), apiVersion, endpointBase, id);
				var signature = OAuthSignature.getAsQueryString(config, url, HttpMethod.GET);
				var securedUrl = String.format(URL_SECURED_FORMAT, url, signature);
		        Map<String,JsonElement> map = new HashMap<>();
				try {
					var el = URLTools.extractJson(securedUrl).getAsJsonObject();
					el.entrySet().forEach(e->map.put(e.getKey(), e.getValue()));
					return map;
				       
				} catch (IOException e) {
					logger.error(e);
				}
		        return map;
			}
			
			@Override
			public Map delete(String endpointBase, int id) {
				return null;
			}
			
			
			@Override
			public Map<String,JsonElement> batch(String endpointBase, Map<String, Object> object) {
				var url = String.format(API_URL_BATCH_FORMAT, config.getUrl(), apiVersion, endpointBase);
				URLToolsClient c = URLTools.newClient();
				Map<String,String> header = new HashMap<>();
				  				   header.put(URLTools.CONTENT_TYPE, contentType);
					 
				Map<String,JsonElement> ret = new HashMap<>();
				try {
					String str = c.doPost(url+"?"+OAuthSignature.getAsQueryString(config, url, HttpMethod.POST), new ByteArrayEntity(new JsonExport().toJson(object).getBytes(MTGConstants.DEFAULT_ENCODING)), header);
					var obj = URLTools.toJson(str).getAsJsonObject();
					obj.entrySet().forEach(e->ret.put(e.getKey(), e.getValue()));
				} catch (IOException e) {
					logger.error("Error in batch",e);
				}    
			
				return ret;
			}
		};
	}

	public static JsonArray entryToJsonArray(String string, String value) {

		var obj = new JsonObject();
		    obj.addProperty(string, value);
				   
		var arr = new JsonArray();
		    arr.add(obj);
		
		return arr;
	}

	
}
