package org.magic.api.externalshop.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.api.mkm.modele.Category;
import org.api.mkm.modele.Product;
import org.json.JSONArray;
import org.json.JSONObject;
import org.magic.api.beans.Contact;
import org.magic.api.beans.Transaction;
import org.magic.api.beans.enums.TransactionStatus;
import org.magic.api.interfaces.MTGStockItem;
import org.magic.api.interfaces.abstracts.AbstractExternalShop;
import org.magic.api.interfaces.abstracts.AbstractStockItem;
import org.magic.services.MTGConstants;
import org.magic.tools.UITools;
import org.magic.tools.WooCommerceTools;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.icoderman.woocommerce.EndpointBaseType;
import com.icoderman.woocommerce.WooCommerce;

public class WooCommerceExternalShop extends AbstractExternalShop {

	
	private static final String PER_PAGE = "PER_PAGE";
	private static final String STATUS = "status";
	private static final String DATE_PAID = "date_paid";
	private WooCommerce client;
	
	private void init()
	{
		if(client==null)
			client = WooCommerceTools.newClient(getAuthenticator());
	}
	
	@Override
	public List<String> listAuthenticationAttributes() {
		return WooCommerceTools.generateKeysForWooCommerce();
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public List<Category> listCategories() throws IOException {
		init();
		
		var params = new HashMap<String, String>();
		
		params.put("per_page", getString(PER_PAGE));
		
		
		List<JsonElement> res = client.getAll(EndpointBaseType.PRODUCTS_CATEGORIES.getValue(),params);
		 
		var ret = new ArrayList<Category>();
		 
		 res.forEach(je->{
			 
			 var objCateg = je.getAsJsonObject();
			 var c = new Category();
			 	 c.setIdCategory(objCateg.get("id").getAsInt());
			 	 c.setCategoryName(objCateg.get("name").getAsString());
			 
			 	ret.add(c);
		 });
		 
		 return ret;
		 
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	protected List<Transaction> loadTransaction() throws IOException{
		init();
		
		Map<String, String> parameters = new HashMap<>();
	    					parameters.put(STATUS, "any");
	    					parameters.put("per_page", getString(PER_PAGE));
	    List<JsonElement> res = client.getAll(EndpointBaseType.ORDERS.getValue(),parameters);
		
	    var ret = new ArrayList<Transaction>();
	   
	    for(JsonElement el : res)
	    {
	    	var obj = el.getAsJsonObject();
	    	
	    	var t = new Transaction();
	    				t.setCurrency(obj.get("currency").getAsString());
	    				t.setDateCreation(UITools.parseGMTDate(obj.get("date_created").getAsString()));
	    				t.setId(obj.get("id").getAsInt());
	    				t.setShippingPrice(obj.get("shipping_total").getAsDouble());
	    				t.setSourceShopNmae(getName());
	    				if(!obj.get(DATE_PAID).isJsonNull())
	    					t.setDatePayment(UITools.parseGMTDate(obj.get(DATE_PAID).getAsString()));
	    				
	    			
	    				switch(obj.get(STATUS).getAsString())
	    				{
	    					case "pending" : t.setStatut(TransactionStatus.NEW);break;
	    					case "processing" : t.setStatut(TransactionStatus.IN_PROGRESS);break;
	    					case "on-hold" : t.setStatut(TransactionStatus.PAYMENT_WAITING);break;
	    					case "completed": t.setStatut(TransactionStatus.CLOSED);break;
	    					case "cancelled": t.setStatut(TransactionStatus.CANCELED);break;
	    					case "failed": t.setStatut(TransactionStatus.CANCELED);break;
	    					case "lpc_transit": t.setStatut(TransactionStatus.SENT);break;
	    					case "lpc_ready_to_ship" : t.setStatut(TransactionStatus.PAID);break;
	    					default : {
	    						
	    						logger.debug(obj.get(STATUS) + " is unknow");
	    						t.setStatut(TransactionStatus.IN_PROGRESS);break;
	    					}
	    				}
	    				
	    	var c = new Contact();
	    	
	    	var contactObj = obj.get("billing").getAsJsonObject();
	    		c.setName(contactObj.get("first_name").getAsString());
	    		c.setLastName(contactObj.get("last_name").getAsString());
	    		c.setAddress(contactObj.get("address_1").getAsString());
	    		c.setZipCode(contactObj.get("postcode").getAsString());
	    		c.setCity(contactObj.get("city").getAsString());
	    		c.setCountry(contactObj.get("country").getAsString());
	    		c.setId(obj.get("customer_id").getAsInt());
	    		c.setEmail(contactObj.get("email").getAsString());
	    		c.setTelephone(contactObj.get("phone").getAsString());
	    		c.setEmailAccept(false);
	    	t.setContact(c);	
	    	
	    	
	    	var itemsArr = obj.get("line_items").getAsJsonArray();
	    	
	    	for(JsonElement item : itemsArr)
	    	{
	    		
	    		var entry = new WooCommerceItem();
	    		var objItem = item.getAsJsonObject();
	    		
	    		entry.setId(objItem.get("product_id").getAsInt());
	    		entry.setQte(objItem.get("quantity").getAsInt());
	    		entry.setPrice(objItem.get("total").getAsDouble());
	    		entry.setProductName(objItem.get("name").getAsString());
	    		entry.getTiersAppIds().put(getName(), String.valueOf(t.getId()));
	    		t.getItems().add(entry);
	    		
	    	}
	    	
	    	
	    	
	    	ret.add(t);
	    }
		return ret;
	}


	@Override
	@SuppressWarnings("unchecked")
	protected void createTransaction(Transaction t) throws IOException {
			init();
			
			Map<String,Object> content = new HashMap<>();
							   content.put("post", createOrder(t));
			
			Map<Object,Object> ret=  client.create(EndpointBaseType.ORDERS.getValue(),content);
			
			if(!ret.isEmpty() && ret.get("id") !=null)
			{
				logger.info(t + " created in " + getName() + " with id = " + ret.get("id"));
			}
			else
			{
				logger.error(ret);
			}
	}


	@Override
	@SuppressWarnings("unchecked")
	public int createProduct(Product p,Category c) throws IOException {
		init();
		
		Map<Object,Object> ret = client.create(EndpointBaseType.PRODUCTS.getValue(), toWooCommerceAttributs(p,null,c.getIdCategory()));
		
		if(!ret.isEmpty() && ret.get("id") !=null)
		{
			logger.info(p + " created in " + getName() + " with id = " + ret.get("id"));
			return Integer.parseInt(ret.get("id").toString());
		}
		else
		{
			logger.error(ret);
		}
		
		return -1;
	}

	@Override
	public String getName() {
		return WooCommerceTools.WOO_COMMERCE_NAME;
	}
	
	@Override
	public List<MTGStockItem> loadStock(int start) throws IOException {
		init();
		var ret = new ArrayList<MTGStockItem>();
		Map<String, String> parameters = new HashMap<>();
		parameters.put("per_page", getString(PER_PAGE));
		
		
		List<JsonObject> res = client.getAll(EndpointBaseType.PRODUCTS.getValue(),parameters);

		res.forEach(element->{
			Product p = new Product();
			JsonObject obj = element.getAsJsonObject();
	
			p.setIdProduct(obj.get("id").getAsInt());
			p.setEnName(obj.get("name").getAsString());
			p.setIdGame(1);
			p.setLocalization(new ArrayList<>());
			
			JsonObject objCateg = obj.get("categories").getAsJsonArray().get(0).getAsJsonObject();
			Category c = new Category();
					 c.setIdCategory(objCateg.get("id").getAsInt());
					 c.setCategoryName(objCateg.get("name").getAsString());
			p.setCategory(c);
			p.setCategoryName(c.getCategoryName());
			
			JsonObject img = obj.get("images").getAsJsonArray().get(0).getAsJsonObject();
							p.setImage(img.get("src").getAsString());
			
			
			var stockItem = new WooCommerceItem();
					stockItem.setProduct(p);
					try {
					stockItem.setPrice(obj.get("price").getAsDouble());
					}
					catch(Exception e)
					{
						stockItem.setPrice(0.0);	
					}
					
					try {
						stockItem.setQte(obj.get("stock_quantity").getAsInt());	
					}catch(Exception e)
					{
						stockItem.setQte(0);	
					}
					
				
					
					
					
				notify(stockItem);
				ret.add(stockItem);	
					
		});
		
		return ret;
		
	
	}

	@Override
	public List<Product> listProducts(String name) throws IOException {
		init();
		
		Map<String, String> productInfo = new HashMap<>();

		productInfo.put("search", name.replaceAll(" ", "%20"));
		
		@SuppressWarnings("unchecked")
		List<JsonObject> res = client.getAll(EndpointBaseType.PRODUCTS.getValue(),productInfo);
		
		List<Product> ret =  new ArrayList<>();
	
		res.forEach(element->{
			
			Product p = new Product();
			JsonObject obj = element.getAsJsonObject();
			p.setIdProduct(obj.get("id").getAsInt());
			p.setEnName(obj.get("name").getAsString());
			p.setIdGame(1);
			p.setLocalization(new ArrayList<>());
			
			JsonObject objCateg = obj.get("categories").getAsJsonArray().get(0).getAsJsonObject();
			Category c = new Category();
					 c.setIdCategory(objCateg.get("id").getAsInt());
					 c.setCategoryName(objCateg.get("name").getAsString());
			p.setCategory(c);
			p.setCategoryName(c.getCategoryName());
			
			JsonObject img = obj.get("images").getAsJsonArray().get(0).getAsJsonObject();
			p.setImage(img.get("src").getAsString());
			
			notify(p);
			ret.add(p);
		});
		return ret;
	}
	
	@Override
	public Map<String, String> getDefaultAttributes() {
		return Map.of(PER_PAGE,"50");
	}
	
	private Map<String, Object> toWooCommerceAttributs(Product product,String status, int idCategory)
	{
		Map<String, Object> productInfo = new HashMap<>();

		productInfo.put("name", product.getEnName());
		productInfo.put("type", "simple");
        productInfo.put("categories", WooCommerceTools.entryToJsonArray("id",String.valueOf(idCategory)));
        productInfo.put(STATUS, status==null?"private":status);
        productInfo.put("images", WooCommerceTools.entryToJsonArray("src",product.getImage().startsWith("//")?"https:"+product.getImage():product.getImage()));
		 
		return productInfo;
	}
	
	private JSONObject createOrder(Transaction t)
	{
		var obj = new JSONObject();
		var items = new JSONArray();
		
		var contact = new JSONObject();
				   contact.put("first_name", t.getContact().getName());
				   contact.put("last_name", t.getContact().getLastName());
				   contact.put("country", t.getContact().getCountry());
				   contact.put("email", t.getContact().getEmail());
				   contact.put("phone", t.getContact().getTelephone());
				   contact.put("address_1", t.getContact().getAddress());
				   contact.put("city", t.getContact().getCity());
				   contact.put("postcode", t.getContact().getZipCode());
				   
		obj.put("billing", contact);
		obj.put("shipping", contact);
		obj.put("line_items", items);
		obj.put("set_paid", t.getStatut().equals(TransactionStatus.PAID));
		obj.put("created_via", MTGConstants.MTG_APP_NAME);
		
		if(t.getPaymentProvider()!=null)
		{
			obj.put("payment_method_title", t.getPaymentProvider().name());
			obj.put(DATE_PAID, t.getDatePayment().getTime());
		}
		
		
		for(MTGStockItem st : t.getItems())
		{
			var line = new JSONObject();
				line.put("product_id", st.getTiersAppIds(WooCommerceTools.WOO_COMMERCE_NAME));
				line.put("quantity", st.getQte());
			items.put(line);
		}
		return obj;
	}
}


class WooCommerceItem extends AbstractStockItem<Product>
{
	private static final long serialVersionUID = 1L;
	
	public void setProduct(Product product) {
			this.id=product.getIdProduct();
			this.url=product.getImage();
			this.productName=product.getEnName();
			this.product=product;
			
	}
}

