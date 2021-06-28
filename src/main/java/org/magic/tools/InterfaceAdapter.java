package org.magic.tools;

import java.lang.reflect.Type;

import org.magic.api.beans.MagicCardStock;
import org.magic.api.beans.SealedStock;
import org.magic.api.beans.enums.EnumItems;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public final class InterfaceAdapter<T> implements JsonDeserializer<T>, JsonSerializer<T> {
  
    public T deserialize(JsonElement elem, Type interfaceType, JsonDeserializationContext context) throws JsonParseException {
     	return context.deserialize(elem, typeForName(EnumItems.valueOf(elem.getAsJsonObject().get("typeStock").getAsString())));
    }

    private Type typeForName(final EnumItems t) {
    	
    	
    	if(t.equals(EnumItems.CARD))
    		return MagicCardStock.class;
    	
    	return SealedStock.class;
  
    }

	@Override
	public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
		return context.serialize(src);
	}

}