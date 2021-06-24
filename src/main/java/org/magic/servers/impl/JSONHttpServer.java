package org.magic.servers.impl;

import static org.magic.tools.MTG.getEnabledPlugin;
import static org.magic.tools.MTG.getPlugin;
import static org.magic.tools.MTG.listEnabledPlugins;
import static org.magic.tools.MTG.listPlugins;
import static spark.Spark.after;
import static spark.Spark.before;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.initExceptionHandler;
import static spark.Spark.notFound;
import static spark.Spark.options;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.put;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.magic.api.beans.Contact;
import org.magic.api.beans.GedEntry;
import org.magic.api.beans.HistoryPrice;
import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicCardAlert;
import org.magic.api.beans.MagicCardStock;
import org.magic.api.beans.MagicCollection;
import org.magic.api.beans.MagicDeck;
import org.magic.api.beans.MagicEdition;
import org.magic.api.beans.MagicFormat;
import org.magic.api.beans.MagicPrice;
import org.magic.api.beans.Transaction;
import org.magic.api.beans.Transaction.STAT;
import org.magic.api.exports.impl.JsonExport;
import org.magic.api.interfaces.MTGCache;
import org.magic.api.interfaces.MTGCardsIndexer;
import org.magic.api.interfaces.MTGCardsProvider;
import org.magic.api.interfaces.MTGDao;
import org.magic.api.interfaces.MTGDashBoard;
import org.magic.api.interfaces.MTGPictureProvider;
import org.magic.api.interfaces.MTGPricesProvider;
import org.magic.api.interfaces.MTGTrackingService;
import org.magic.api.interfaces.abstracts.AbstractEmbeddedCacheProvider;
import org.magic.api.interfaces.abstracts.AbstractMTGServer;
import org.magic.gui.models.MagicEditionsTableModel;
import org.magic.services.GedService;
import org.magic.services.MTGConstants;
import org.magic.services.MTGControler;
import org.magic.services.MTGDeckManager;
import org.magic.services.PluginRegistry;
import org.magic.services.TransactionService;
import org.magic.services.keywords.AbstractKeyWordsManager;
import org.magic.sorters.CardsEditionSorter;
import org.magic.tools.ImageTools;
import org.magic.tools.POMReader;
import org.magic.tools.URLTools;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import spark.Request;
import spark.Response;
import spark.ResponseTransformer;
import spark.Spark;
import spark.route.HttpMethod;

public class JSONHttpServer extends AbstractMTGServer {

	private static final String COLLECTION = ":collection";
	private static final String ID_SET2 = ":idSet";
	private static final String ENABLE_SSL = "ENABLE_SSL";
	private static final String NAME = ":name";
	private static final String ID_SET = ID_SET2;
	private static final String ID_ED = ":idEd";
	private static final String ID_CARDS = ":idCards";
	private static final String PASSTOKEN = "PASSWORD-TOKEN";
	private static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
	private static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
	private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
	private static final String ENABLE_GZIP = "ENABLE_GZIP";
	private static final String AUTOSTART = "AUTOSTART";
	private static final String SERVER_PORT = "SERVER-PORT";
	private static final String KEYSTORE_URI = "KEYSTORE_URI";
	private static final String KEYSTORE_PASS = "KEYSTORE_PASS";

	private ResponseTransformer transformer;
	private MTGDeckManager manager;
	private ByteArrayOutputStream baos;
	private boolean running = false;
	private static final String RETURN_OK = "{\"result\":\"OK\"}";
	private static final String CACHE_TIMEOUT = "CACHE_TIMEOUT";
	private JsonExport converter;

	private MTGCache<String, Object> cache;
	
	private String error(String msg) {
		return "{\"error\":\"" + msg + "\"}";
	}

	public void clearCache()
	{
		logger.debug("Clearing " + getName() + " cache");
		cache.clear();

	}
	
	
	public JSONHttpServer() {
		
		manager = new MTGDeckManager();
		converter = new JsonExport();
		transformer = new ResponseTransformer() {
			@Override
			public String render(Object model) throws Exception {
				return converter.toJson(model);
			}
		};
		
		cache = new AbstractEmbeddedCacheProvider<>() {
			
			Cache<String, Object> guava = CacheBuilder.newBuilder().build();
			
			
			public String getName() {
				return "";
			}
			
			@Override
			public void clear() {
				guava.invalidateAll();
				
			}

			@Override
			public Object getItem(String k) {
				return guava.getIfPresent(k);
			}

			@Override
			public void put(Object value, String key) throws IOException {
				guava.put(key, value);
				
			}
		};
	}
	
	
	


	
	private Object getCached(String k, Callable<Object> call)
	{
		if(cache.getItem(k)==null)
			try {
				cache.put(call.call(),k);
			} catch (Exception e) {
				logger.error(e);
				return new ArrayList<>();
			}
		
		return cache.getItem(k);
	}
	

	@Override
	public void start() throws IOException {
		
		if(getBoolean(ENABLE_SSL))
			Spark.secure(getString(KEYSTORE_URI), getString(KEYSTORE_PASS), null, null);
		
		initVars();
		initRoutes();
		Spark.init();
		running = true;
		logger.info("Server " + getName() +" started on port " + getInt(SERVER_PORT));
	}
	
	private void initVars() {
		
		Spark.
		
		threadPool(getInt("THREADS"));
		
		port(getInt(SERVER_PORT));
		
		initExceptionHandler(e -> {
			running = false;
			logger.error(e);
		});

		exception(Exception.class, (Exception exception, Request req, Response res) -> {
			logger.error("Error :" + req.headers(URLTools.REFERER) + ":" + exception.getMessage(), exception);
			res.status(500);
			res.body(error(exception.getMessage()));
		});

		notFound((req, res) -> {
			res.status(404);
			return error("Not Found");
		});

		after((request, response) -> {
			if (getBoolean(ENABLE_GZIP)) {
				response.header("Content-Encoding", "gzip");
			}
		});
		
		options("/*", (request, response) -> {
			String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
			if (accessControlRequestHeaders != null) {
				response.header(ACCESS_CONTROL_ALLOW_HEADERS, accessControlRequestHeaders);
			}
			String accessControlRequestMethod = request.headers(ACCESS_CONTROL_REQUEST_METHOD);
			if (accessControlRequestMethod != null) {
				response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
			}
			return RETURN_OK;
		});
		
		

	}

	@SuppressWarnings("unchecked")
	private void initRoutes() {

		
		
		before("/*", (request, response) -> {
			response.type(URLTools.HEADER_JSON);
			response.header(ACCESS_CONTROL_ALLOW_ORIGIN, getWhiteHeader(request));
			response.header(ACCESS_CONTROL_REQUEST_METHOD, getString(ACCESS_CONTROL_REQUEST_METHOD));
			response.header(ACCESS_CONTROL_ALLOW_HEADERS, getString(ACCESS_CONTROL_ALLOW_HEADERS));
		});
	
		get("/cards/search/:att/:val", URLTools.HEADER_JSON,
				(request, response) -> getEnabledPlugin(MTGCardsProvider.class)
						.searchCardByCriteria(request.params(":att"), request.params(":val"), null, false),
				transformer);
		
		get("/cards/search/:att/:val/:exact", URLTools.HEADER_JSON,
				(request, response) -> getEnabledPlugin(MTGCardsProvider.class)
						.searchCardByCriteria(request.params(":att"), request.params(":val"), null, Boolean.parseBoolean(request.params(":exact"))),
				transformer);
		
		get("/cards/suggest/:val", URLTools.HEADER_JSON,
				(request, response) -> getEnabledPlugin(MTGCardsIndexer.class).suggestCardName(request.params(":val")),
				transformer);
		
		get("/cards/light/:name", URLTools.HEADER_JSON,(request, response) -> {
			List<MagicCard> list= getEnabledPlugin(MTGCardsProvider.class).searchCardByName(request.params(NAME), null, true);
			var arr = new JsonArray();
			
			for(MagicCard mc : list)
			{
				List<MagicCollection> cols = getEnabledPlugin(MTGDao.class).listCollectionFromCards(mc);
				
				var obj = new JsonObject();
							obj.addProperty("name", mc.getName());
							obj.addProperty("cost", mc.getCost());
							obj.addProperty("type", mc.getFullType());
							obj.addProperty("set", mc.getCurrentSet().getSet());
							obj.addProperty("multiverse", mc.getCurrentSet().getMultiverseid());
							obj.add("collections", converter.toJsonElement(cols));
				arr.add(obj);			
			}
			return arr;
			
		},transformer);
		
		
		get("/ged/:class/:id", URLTools.HEADER_JSON,(request, response) -> {
			
			var arr = new JsonArray();
			GedService.inst().list(request.params(":class")+"/"+request.params(":id")).forEach(p->{
				try {
					var e = GedService.inst().read(p);
					if(e.isImage()) {
						var obj = new JsonObject();
						    obj.addProperty("name", e.getName());
						    obj.addProperty("ext",e.getExt());
						    obj.addProperty("obj",e.getObject().toString());
						    obj.addProperty("data",ImageTools.toBase64(e.getContent()));
						    arr.add(obj);
					}
				} catch (IOException e) {
					logger.error(e);
				}
				
				
			});
			
			
			return arr;
			
		},transformer);
		
		
		get("/orders/list", URLTools.HEADER_JSON, (request, response) -> getEnabledPlugin(MTGDao.class).listOrders(), transformer);
		
		get("/keywords", URLTools.HEADER_JSON, (request, response) -> AbstractKeyWordsManager.getInstance().toJson(), transformer);
		
		
		get("/cards/name/:idEd/:cName", URLTools.HEADER_JSON, (request, response) -> {
			MagicEdition ed = getEnabledPlugin(MTGCardsProvider.class).getSetById(request.params(ID_ED));
			return getEnabledPlugin(MTGCardsProvider.class).searchCardByName(
					request.params(":cName"), ed, true);
		}, transformer);
		
		get("/cards/number/:idEd/:cNumber", URLTools.HEADER_JSON, (request, response) -> getEnabledPlugin(MTGCardsProvider.class).getCardByNumber(request.params(":cNumber"), request.params(ID_ED)), transformer);

		put("/cards/move/:from/:to/:id", URLTools.HEADER_JSON, (request, response) -> {
			var from = new MagicCollection(request.params(":from"));
			var to = new MagicCollection(request.params(":to"));
			MagicCard mc = getEnabledPlugin(MTGCardsProvider.class).getCardById(request.params(":id"));
			getEnabledPlugin(MTGDao.class).moveCard(mc, from,to);
			return RETURN_OK;
		}, transformer);

		put("/cards/add/:id", URLTools.HEADER_JSON, (request, response) -> {
			var from = new MagicCollection(MTGControler.getInstance().get("default-library"));
			MagicCard mc = getEnabledPlugin(MTGCardsProvider.class).getCardById(request.params(":id"));
			MTGControler.getInstance().saveCard(mc, from,null);
			return RETURN_OK;
		}, transformer);

		put("/cards/add/:to/:id", URLTools.HEADER_JSON, (request, response) -> {
			MagicCard mc = getEnabledPlugin(MTGCardsProvider.class).getCardById(request.params(":id"));
			MTGControler.getInstance().saveCard(mc, new MagicCollection(request.params(":to")),null);
			return RETURN_OK;
		}, transformer);

		get("/cards/list/:col/:idEd", URLTools.HEADER_JSON, (request, response) -> {
			var col = new MagicCollection(request.params(":col"));
			MagicEdition ed = getEnabledPlugin(MTGCardsProvider.class).getSetById(request.params(ID_ED));
			return getEnabledPlugin(MTGDao.class).listCardsFromCollection(col, ed);
		}, transformer);

		get("/cards/:id", URLTools.HEADER_JSON, (request, response) -> getEnabledPlugin(MTGCardsProvider.class)
				.getCardById(request.params(":id")), transformer);
		
		get("/cards/:idSet/cards", URLTools.HEADER_JSON, (request, response) -> {
			MagicEdition ed = getEnabledPlugin(MTGCardsProvider.class).getSetById(request.params(ID_SET));
			List<MagicCard> ret = getEnabledPlugin(MTGCardsProvider.class).searchCardByEdition(ed);
			Collections.sort(ret, new CardsEditionSorter());

			return ret;
		}, transformer);

		get("/collections/:name/count", URLTools.HEADER_JSON, (request, response) -> getEnabledPlugin(MTGDao.class).getCardsCountGlobal(new MagicCollection(request.params(NAME))), transformer);

		get("/collections/list", URLTools.HEADER_JSON,
				(request, response) -> getEnabledPlugin(MTGDao.class).listCollections(), transformer);

		get("/collections/cards/:idcards", URLTools.HEADER_JSON, (request, response) -> {
			MagicCard mc = getEnabledPlugin(MTGCardsProvider.class).getCardById(request.params(":idcards"));
			return getEnabledPlugin(MTGDao.class).listCollectionFromCards(mc);
		}, transformer);

		get("/collections/:name", URLTools.HEADER_JSON, (request, response) -> getEnabledPlugin(MTGDao.class)
				.getCollection(request.params(NAME)), transformer);

		put("/collections/add/:name", URLTools.HEADER_JSON, (request, response) -> {
			getEnabledPlugin(MTGDao.class).saveCollection(request.params(NAME));
			return RETURN_OK;
		});

		get("/editions/list", URLTools.HEADER_JSON,
				(request, response) -> getEnabledPlugin(MTGCardsProvider.class).listEditions(), transformer);

		get("/editions/:idSet", URLTools.HEADER_JSON, (request, response) -> getEnabledPlugin(MTGCardsProvider.class).getSetById(request.params(ID_SET)), transformer);

		
		get("/editions/list/:colName", URLTools.HEADER_JSON, (request, response) -> {
			List<MagicEdition> eds = new ArrayList<>();
			List<String> list = getEnabledPlugin(MTGDao.class)
					.listEditionsIDFromCollection(new MagicCollection(request.params(":colName")));
			for (String s : list)
				eds.add(getEnabledPlugin(MTGCardsProvider.class).getSetById(s));

			Collections.sort(eds);
			return eds;

		}, transformer);

		get("/prices/:idSet/:name", URLTools.HEADER_JSON, (request, response) -> {
			MagicEdition ed = getEnabledPlugin(MTGCardsProvider.class).getSetById(request.params(ID_SET));
			MagicCard mc = getEnabledPlugin(MTGCardsProvider.class).searchCardByName( request.params(NAME), ed, false).get(0);
			List<MagicPrice> pricesret = new ArrayList<>();
			for (MTGPricesProvider prices : listEnabledPlugins(MTGPricesProvider.class))
			{
				try {
					pricesret.addAll(prices.getPrice(mc));
				}
				catch(Exception e)
				{
					logger.error(e);
				}
			}

			return pricesret;

		}, transformer);
		
	
		get("/alerts/list", URLTools.HEADER_JSON,
				(request, response) -> getEnabledPlugin(MTGDao.class).listAlerts(), transformer);

		get("/alerts/:idCards", URLTools.HEADER_JSON, (request, response) -> {
			MagicCard mc = getEnabledPlugin(MTGCardsProvider.class).getCardById(request.params(ID_CARDS));
			return getEnabledPlugin(MTGDao.class).hasAlert(mc);

		}, transformer);

		put("/alerts/add/:idCards", (request, response) -> {
			MagicCard mc = getEnabledPlugin(MTGCardsProvider.class).getCardById(request.params(ID_CARDS));
			var alert = new MagicCardAlert();
			alert.setCard(mc);
			alert.setPrice(0.0);
			getEnabledPlugin(MTGDao.class).saveAlert(alert);
			return RETURN_OK;
		});

		put("/stock/add/:idCards", (request, response) -> {
			MagicCard mc = getEnabledPlugin(MTGCardsProvider.class).getCardById(request.params(ID_CARDS));
			MagicCardStock stock = MTGControler.getInstance().getDefaultStock();
			stock.setQte(1);
			stock.setProduct(mc);

			getEnabledPlugin(MTGDao.class).saveOrUpdateStock(stock);
			return RETURN_OK;
		});

		
		get("/sealed/list", URLTools.HEADER_JSON,(request, response) ->
				getCached(request.pathInfo(), new Callable<Object>() {
		
					@Override
					public Object call() throws Exception {
						return getEnabledPlugin(MTGDao.class).listSealedStocks();
					}
				})
		, transformer);
		
		get("/sealed/get/:id", URLTools.HEADER_JSON,
				(request, response) -> getEnabledPlugin(MTGDao.class).getSealedStockById(Integer.parseInt(request.params(":id"))), transformer);
		
		
		

		get("/stock/list", URLTools.HEADER_JSON,(request, response) -> { 
			
			if(cache.getItem(request.pathInfo())==null)
				cache.put(getEnabledPlugin(MTGDao.class).listStocks(),request.pathInfo());
			
			return cache.getItem(request.pathInfo());
			
		}, transformer);
		
		
		
		get("/stock/get/:idStock", URLTools.HEADER_JSON,
				(request, response) -> getEnabledPlugin(MTGDao.class).getStockById(Integer.parseInt(request.params(":idStock"))), transformer);
		
		
	
		
		get("/stock/list/:collection", URLTools.HEADER_JSON,(request, response) ->
			 getCached(request.pathInfo(), new Callable<Object>() {

				@Override
				public Object call() throws Exception {
					return getEnabledPlugin(MTGDao.class).listStocks(List.of(new MagicCollection(request.params(COLLECTION))));
				}
			})

		, transformer);
		
		get("/stock/virtual/:collection", URLTools.HEADER_JSON,(request, response) ->
			getCached(request.pathInfo(), new Callable<Object>() {

				@Override
				public Object call() throws Exception {
					return getEnabledPlugin(MTGDao.class).listCardsFromCollection(new MagicCollection(request.params(COLLECTION))).stream().map(e->{
						var mcs =  MTGControler.getInstance().getDefaultStock();
						mcs.setProduct(e);
						return mcs;
						
					}).collect(Collectors.toList());
				}
			})

		, transformer);
		
		
		get("/stock/sets/:collection", URLTools.HEADER_JSON,(request, response) ->
			
			 getCached(request.pathInfo(), new Callable<Object>() {
				@Override
				public List<Object> call() throws Exception {
					return getEnabledPlugin(MTGDao.class).listStocks(List.of(new MagicCollection(request.params(COLLECTION)))).stream().map(mcs->mcs.getProduct().getCurrentSet()).distinct().sorted().collect(Collectors.toList());
				}
			})
		, transformer);
	
		
		get("/stock/list/:collection/:idSet", URLTools.HEADER_JSON, (request, response) ->
			getCached(request.pathInfo(), new Callable<Object>() {
				@Override
				public List<Object> call() throws Exception {
					return getEnabledPlugin(MTGDao.class).listStocks(List.of(new MagicCollection(request.params(COLLECTION)))).stream().filter(mcs->mcs.getProduct().getCurrentSet().getId().equalsIgnoreCase(request.params(ID_SET2))).collect(Collectors.toList());
				}
			})
		, transformer);
		
		get("/stock/searchCard/:collection/:cardName", URLTools.HEADER_JSON,
				(request, response) -> getEnabledPlugin(MTGDao.class).listStocks(request.params(":cardName"),List.of(new MagicCollection(request.params(COLLECTION)))), transformer);
		
		get("/dash/collection", URLTools.HEADER_JSON, (request, response) -> {
			List<MagicEdition> eds = getEnabledPlugin(MTGCardsProvider.class).listEditions();
			var model = new MagicEditionsTableModel();
			model.init(eds);

			var arr = new JsonArray();
			double pc = 0;
			for (MagicEdition ed : eds) {
				var obj = new JsonObject();
				obj.add("edition", converter.toJsonElement(ed));
				obj.addProperty("set", ed.getId());
				obj.addProperty("name", ed.getSet());
				obj.addProperty("release", ed.getReleaseDate());
				obj.add("qty", new JsonPrimitive(model.getMapCount().get(ed)));
				obj.add("cardNumber", new JsonPrimitive(ed.getCardCount()));
				obj.addProperty("defaultLibrary", MTGControler.getInstance().get("default-library"));
				pc = 0;
				if (ed.getCardCount() > 0)
					pc = (double) model.getMapCount().get(ed) / ed.getCardCount();
				else
					pc = (double) model.getMapCount().get(ed) / 1;

				obj.add("pc", new JsonPrimitive(pc));

				arr.add(obj);
			}
			return arr;
		}, transformer);

		get("/dash/card/:idCards/:foil", URLTools.HEADER_JSON, (request, response) -> {
			MagicCard mc = getEnabledPlugin(MTGCardsProvider.class).getCardById(request.params(ID_CARDS));

			var arr = new JsonArray();
			HistoryPrice<MagicCard> res = getEnabledPlugin(MTGDashBoard.class).getPriceVariation(mc,request.params(":foil").equals("true"));

			for (Entry<Date, Double> val : res) {
				var obj = new JsonObject();
				obj.add("date", new JsonPrimitive(val.getKey().getTime()));
				obj.add("value", new JsonPrimitive(val.getValue()));

				arr.add(obj);
			}

			return arr;
		});

		get("/dash/edition/:idEd", URLTools.HEADER_JSON, (request, response) -> {
			var ed = new MagicEdition();
			ed.setId(request.params(ID_ED));
			return getEnabledPlugin(MTGDashBoard.class).getShakesForEdition(ed);
		}, transformer);

		get("/dash/format/:format", URLTools.HEADER_JSON, (request, response) -> getEnabledPlugin(MTGDashBoard.class).getShakerFor(MagicFormat.FORMATS.valueOf(request.params(":format"))), transformer);

		get("/pics/cards/:idEd/:name", URLTools.HEADER_JSON, (request, response) -> {

			baos = new ByteArrayOutputStream();

			MagicEdition ed = getEnabledPlugin(MTGCardsProvider.class).getSetById(request.params(ID_ED));
			MagicCard mc = getEnabledPlugin(MTGCardsProvider.class)
					.searchCardByName( request.params(NAME), ed, true).get(0);
			BufferedImage im = getEnabledPlugin(MTGPictureProvider.class).getPicture(mc);
			ImageTools.write(im, "png", baos);
			
			baos.flush();
			byte[] imageInByte = baos.toByteArray();
			baos.close();
			response.type("image/png");

			return imageInByte;
		});

		get("/pics/cardname/:name", URLTools.HEADER_JSON, (request, response) -> {

			baos = new ByteArrayOutputStream();
			MagicCard mc = getEnabledPlugin(MTGCardsProvider.class)
					.searchCardByName( request.params(NAME), null, true).get(0);
			BufferedImage im = getEnabledPlugin(MTGPictureProvider.class).getPicture(mc);
			ImageTools.write(im, "png", baos);
			baos.flush();
			byte[] imageInByte = baos.toByteArray();
			baos.close();
			response.type("image/png");

			return imageInByte;
		});
		
		
		get("/decks/list", URLTools.HEADER_JSON, (request, response) -> {

			var arr = new JsonArray();
			var exp = new JsonExport();

			for (MagicDeck d : manager.listDecks()) {
				JsonElement el = exp.toJsonDeck(d);
				arr.add(el);
			}
			return arr;
		}, transformer);

		get("/deck/:name", URLTools.HEADER_JSON,(request, response) -> {
			
				MagicDeck d = manager.getDeck(request.params(NAME));
				JsonElement el= new JsonExport().toJsonDeck(d);
				el.getAsJsonObject().addProperty("colors", d.getColors());
				
				return el;
		},transformer);

		get("/deck/stats/:name", URLTools.HEADER_JSON, (request, response) -> {

			MagicDeck d = manager.getDeck(request.params(NAME));

			var obj = new JsonObject();

			obj.add("cmc", converter.toJsonElement(manager.analyseCMC(d.getMainAsList())));
			obj.add("types", converter.toJsonElement(manager.analyseTypes(d.getMainAsList())));
			obj.add("rarity", converter.toJsonElement(manager.analyseRarities(d.getMainAsList())));
			obj.add("colors", converter.toJsonElement(manager.analyseColors(d.getMainAsList())));
			obj.add("legalities", converter.toJsonElement(manager.analyseLegalities(d)));
			obj.add("drawing", converter.toJsonElement(manager.analyseDrawing(d)));
			return obj;

		}, transformer);

		get("/admin/plugins/list", URLTools.HEADER_JSON, (request, response) -> {
			var obj = new JsonObject();
			PluginRegistry.inst().entrySet().forEach(entry->obj.add(entry.getValue().getType().name(), converter.convert(listPlugins(entry.getKey()))));
			return obj;
		}, transformer);
		
		
		get("/events", URLTools.HEADER_JSON, (request, response) -> {
			MTGControler.getInstance().getEventsManager().load();
			return MTGControler.getInstance().getEventsManager().getEvents();
		}, transformer);
		
		
		get("/events/:id", URLTools.HEADER_JSON, (request, response) -> {
			MTGControler.getInstance().getEventsManager().load();
			return MTGControler.getInstance().getEventsManager().getEventById(Integer.parseInt(request.params(":id")));
		}, transformer);
		
		get("/webshop/config", URLTools.HEADER_JSON, (request, response) -> 
			
			 getCached(request.pathInfo(), new Callable<Object>() {

				@Override
				public Object call() throws Exception {
					return MTGControler.getInstance().getWebConfig();
				}
			})
			
		, transformer);
		
		get("/webshop/transaction/:id", URLTools.HEADER_JSON, (request, response) -> 
			getEnabledPlugin(MTGDao.class).getTransaction(Integer.parseInt(request.params(":id")))
		, transformer);

		get("/track/:provider/:number", URLTools.HEADER_JSON, (request, response) -> 
			getPlugin(request.params(":provider"),MTGTrackingService.class).track(request.params(":number"))
		, transformer);
		
		
		post("/webshop/user/connect", URLTools.HEADER_JSON, (request, response) -> getEnabledPlugin(MTGDao.class).getContactByLogin(request.queryParams("email"),request.queryParams("password")), transformer);
		
		post("/webshop/transaction/cancel/:id", URLTools.HEADER_JSON, (request, response) -> {
			
			Contact c=new Gson().fromJson(request.queryParams("user"), Contact.class);
			
			var t = getEnabledPlugin(MTGDao.class).getTransaction(Integer.parseInt(request.params(":id")));
			
			if(t.getContact().getId()==c.getId())
			{
				t.setStatut(STAT.CANCELATION_ASK);
				getEnabledPlugin(MTGDao.class).saveOrUpdateTransaction(t);
				return "OK";
			}
			else
			{
				return "Wrong User";	
			}
			
		}, transformer);
		
		
		post("/transaction/add", URLTools.HEADER_JSON, (request, response) -> {
			
			Transaction t=new Gson().fromJson(new InputStreamReader(request.raw().getInputStream()), Transaction.class);
			
			return TransactionService.newTransaction(t);
		});
	
		post("/transaction/paid/:provider", URLTools.HEADER_JSON, (request, response) -> {
			
			Transaction t=new Gson().fromJson(new InputStreamReader(request.raw().getInputStream()), Transaction.class);
			TransactionService.payingTransaction(t,request.params(":provider"));
			
			return "ok";
		}, transformer);
		
		post("/transactions/contact", URLTools.HEADER_JSON, (request, response) -> {
			
			Contact c=new Gson().fromJson(new InputStreamReader(request.raw().getInputStream()), Contact.class);
			return getEnabledPlugin(MTGDao.class).listTransactions(c);
		}, transformer);

		post("/contact/save", URLTools.HEADER_JSON, (request, response) -> {
			
			Contact t=new Gson().fromJson(new InputStreamReader(request.raw().getInputStream()), Contact.class);
			
			
			if(t.getId()<=0)
			{
				try{
					TransactionService.createContact(t);
					return t;
				}
				catch(SQLIntegrityConstraintViolationException e)
				{
					response.status(500);
					return "Email already exist";
				}
			}
			else
			{
				try{
					getEnabledPlugin(MTGDao.class).saveOrUpdateContact(t);
					return t;
				}
				catch(SQLIntegrityConstraintViolationException e)
				{
					response.status(500);
					return e.getMessage();
				}
			}
		}, transformer);
		
		
		get("/contact/validation/:token", URLTools.HEADER_JSON, (request, response) -> {
			return getEnabledPlugin(MTGDao.class).enableContact(request.params(":token"));
		}, transformer);
		
		
		
		get("/",URLTools.HEADER_HTML,(request,response) -> {
			
			var temp = new StringBuilder();
			response.type(URLTools.HEADER_HTML);
			
			Spark.routes().stream().filter(rm->rm.getHttpMethod()!=HttpMethod.after && rm.getHttpMethod()!=HttpMethod.before && rm.getHttpMethod()!=HttpMethod.options).forEach(rm->{
				temp.append(rm.getHttpMethod());
				temp.append("&nbsp;");
				temp.append("<a href='").append(rm.getMatchUri()).append("'>").append(rm.getMatchUri()).append("</a>");
				temp.append("<br/>");
			});
			
			return temp.toString();
		});		
	}

	@Override
	public void stop() throws IOException {
		Spark.stop();
		logger.info("Server stop");
		running = false;
	}

	@Override
	public boolean isAlive() {
		return running;
	}

	@Override
	public boolean isAutostart() {
		return getBoolean(AUTOSTART);
	}

	@Override
	public String description() {
		return "Rest backend server";
	}

	@Override
	public String getName() {
		return "Json Http Server";
	}

	@Override
	public Icon getIcon() {
		return new ImageIcon(DiscordBotServer.class.getResource("/icons/plugins/json.png"));
	}
	
	

	@Override
	public void initDefault() {
		setProperty(SERVER_PORT, "8080");
		setProperty(AUTOSTART, FALSE);
		setProperty(ENABLE_GZIP, FALSE);
		setProperty(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
		setProperty(ACCESS_CONTROL_REQUEST_METHOD, "GET,PUT,POST,DELETE,OPTIONS");
		setProperty(ACCESS_CONTROL_ALLOW_HEADERS,"Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin");
		setProperty(PASSTOKEN, "");
		setProperty("THREADS","8");
		setProperty(ENABLE_SSL,FALSE);
		setProperty(KEYSTORE_URI, new File(MTGConstants.DATA_DIR,"jetty.jks").getAbsolutePath());
		setProperty(KEYSTORE_PASS, "changeit");
		setProperty(CACHE_TIMEOUT, "-1");
	}

	@Override
	public String getVersion() {
		return POMReader.readVersionFromPom(Spark.class, "/META-INF/maven/com.sparkjava/spark-core/pom.properties");
	}

	private String getWhiteHeader(Request request) {
		logger.debug("request :" + request.pathInfo() + " from " + request.ip());

		for (String k : request.headers())
			logger.trace("---" + k + "=" + request.headers(k));

		return getString(ACCESS_CONTROL_ALLOW_ORIGIN);
	}



}
