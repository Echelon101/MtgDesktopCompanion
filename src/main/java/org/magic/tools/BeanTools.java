package org.magic.tools;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.Logger;
import org.magic.api.beans.enums.CardsPatterns;
import org.magic.api.exports.impl.JsonExport;
import org.magic.services.MTGLogger;

public class BeanTools {

	protected static Logger logger = MTGLogger.getLogger(BeanTools.class);
	public static final String TOKEN_START="{";
	public static final String TOKEN_END="}";
	
	private BeanTools() {}
	
	public static String toJson(Object o)
	{
		return new JsonExport().toJson(o);
	}

	public static String toMemory(Object o) {
		return MemoryTools.statInstanceToString(o);
	}
	
	public static String toString(Object o,String separator)
	{
		StringBuilder build = new StringBuilder();
		try {
			describe(o).entrySet().forEach(e->
				build.append(e.getKey()).append(separator).append(e.getValue()).append(System.lineSeparator())
			);
		} catch (Exception e) {
			logger.error(e);
		}
		return build.toString();
	}
	
	public static Map<String,Object> describe(Object o)
	{
		try {
			return PropertyUtils.describe(o).entrySet().stream().sorted(Map.Entry.comparingByKey())
			        .collect(LinkedHashMap::new, (m,v)->m.put(v.getKey(), v.getValue()), LinkedHashMap::putAll);
		} catch (Exception e) {
			logger.error(e);
			return new HashMap<>();
		}
	}
	
	public static Object readProperty(Object o,String k)
	{
		try {
			return PropertyUtils.getNestedProperty(o,k);
		} catch (Exception e) {
			logger.error(e);
			return null;
		}
	}

	public static String createString(Object mc, String text) {
		Pattern p = Pattern.compile("\\"+TOKEN_START+CardsPatterns.REGEX_ANY_STRING+"\\"+TOKEN_END);
		Matcher m = p.matcher(text);
		StringBuilder temp = new StringBuilder();
		
		while(m.find())
		{
			String k = m.group(1);
			m.appendReplacement(temp,readProperty(mc,k)+"");
		}
		m.appendTail(temp);
		return temp.toString();
	}

}
