package org.magic.services;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.beta.CryptoUtils;
import org.magic.api.beans.AccountAuthenticator;
import org.magic.api.exports.impl.JsonExport;
import org.magic.api.interfaces.MTGPlugin;
import org.magic.tools.FileTools;

import com.google.gson.JsonObject;

public class AccountsManager {

	private static AccountsManager inst;
	private Map<MTGPlugin, AccountAuthenticator> keys;
	private Logger logger = MTGLogger.getLogger(this.getClass());

	public String getKey() throws IOException {
		
		String key=FileTools.readFile(new File(MTGConstants.DATA_DIR.getAbsolutePath(),"key"));
		if(key.isEmpty())
		{
			throw new IOException("Please create a keypass");
		}
		
		return key;
		
	}
	
	public void setKey(String pass) throws IOException
	{
		FileTools.saveFile(new File(MTGConstants.DATA_DIR.getAbsolutePath(),"key"), pass);
	}
	
	public static AccountsManager inst()
	{
		if(inst==null)
			inst = new AccountsManager();
		
		return inst;
	}
	
	public AccountsManager() {
		keys = new HashMap<>();
	}
	
	
	public void addAuthentication(MTGPlugin plug, AccountAuthenticator token)
	{
		keys.put(plug, token);
	}
	
	public AccountAuthenticator getAuthenticator(MTGPlugin plug)
	{
		return keys.get(plug);
	}
	
	
	public Map<MTGPlugin, AccountAuthenticator> listAuthEntries() {
		return keys;
	}
	
	public void removeEntry(MTGPlugin selectedValue) {
		keys.remove(selectedValue);
	}
	
	public MTGPlugin loadAuthenticator(String name)
	{
		return listAvailablePlugins().stream().filter(p->name.equalsIgnoreCase(p.getName())).findFirst().orElse(null);
	}
	
	public List<MTGPlugin> listAvailablePlugins()
	{
		return PluginRegistry.inst().listPlugins().stream().filter(p->!p.listAuthenticationAttributes().isEmpty()).sorted().distinct().collect(Collectors.toList());
	}
	
	public void saveConfig()
	{
		MTGControler.getInstance().saveAccounts();
	
	}
	
	public String exportConfig() {
		var p = new JsonExport();
		p.removePrettyString();
		
		try {
			return CryptoUtils.encrypt(p.toJson(AccountsManager.inst().listAuthEntries()),getKey());
		} catch (IOException e) {
			logger.error("Error getting keypass " + e);
			return "";
		}
		
	}
	
	public void loadConfig(String content) {
		
		if((content!=null) && !content.isEmpty())
			try {
				loadConfig(new JsonExport().fromJson(CryptoUtils.decrypt(content,getKey()), JsonObject.class));
			} catch (Exception e) {
				logger.error("Error while decryptions " + e);
			}
	}
	
	private void loadConfig(JsonObject o) {
		if(o!=null && !o.isJsonNull())
			o.keySet().forEach(name->{
				var tokens = o.get(name).getAsJsonObject().get("tokens").getAsJsonObject();
				var tok = new AccountAuthenticator();
				tokens.entrySet().forEach(e->tok.addToken(e.getKey(), e.getValue().getAsString()));
				keys.put(loadAuthenticator(name), tok);
			});
	}
	

	public static List<String> generateLoginPasswordsKeys() {
		return List.of(AccountAuthenticator.LOGIN,AccountAuthenticator.PASSWORD);
	}


	
}
