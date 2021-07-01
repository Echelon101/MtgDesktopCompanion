package org.magic.servers.impl;

import static org.magic.tools.MTG.getEnabledPlugin;
import static org.magic.tools.MTG.listEnabledPlugins;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.security.auth.login.LoginException;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.apache.commons.lang3.StringUtils;
import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicEdition;
import org.magic.api.beans.MagicPrice;
import org.magic.api.beans.enums.MTGColor;
import org.magic.api.interfaces.MTGCardsProvider;
import org.magic.api.interfaces.MTGDao;
import org.magic.api.interfaces.MTGPictureProvider;
import org.magic.api.interfaces.MTGPricesProvider;
import org.magic.api.interfaces.abstracts.AbstractMTGServer;
import org.magic.servers.impl.NavigableEmbed.EmbedButton;
import org.magic.sorters.MagicPricesComparator;
import org.magic.tools.MTG;
import org.magic.tools.UITools;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.JDAInfo;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DiscordBotServer extends AbstractMTGServer {

	private static final String REGEX = "REGEX";
	private static final String THUMBNAIL_IMAGE = "THUMBNAIL_IMAGE";
	private static final String SHOWPRICE = "SHOWPRICE";
	private static final String AUTOSTART = "AUTOSTART";
	private static final String TOKEN = "TOKEN";
	private static final String SHOWCOLLECTIONS = "SHOW_COLLECTIONS";
	private static final String PRICE_KEYWORDS = "PRICE_KEYWORDS";
	private static final String HELP_MESSAGE = "HELP_MESSAGE";
	
	private JDA jda;
	private ListenerAdapter listener;
	

	@Override
	public String getVersion() {
		return JDAInfo.VERSION;
	}
	
	
	private void initListener()
	{
		listener = new ListenerAdapter() {
			
			
			@Override
			public void onMessageReceived(MessageReceivedEvent event)
			{
				if (event.getAuthor().isBot()) 
					return;
				
				analyseMessage(event);
			}
		};
	}
	
	private void analyseMessage(MessageReceivedEvent event) {
		logger.debug("Received message :" + event.getMessage().getContentRaw() + " from " + event.getAuthor().getName()+ " in #" + event.getChannel().getName());
		
		if(event.getMessage().getContentRaw().toLowerCase().contains("help"))
		{
			responseHelp(event);
			return;
		}
		
		
		var p = Pattern.compile(getString(REGEX));
		var m = p.matcher(event.getMessage().getContentRaw());
		if(m.find())
		{
			String name=m.group(1).trim();
			responseSearch(event,name);
		}	
	}

	
	private void responseHelp(MessageReceivedEvent event) {
		MessageChannel channel = event.getChannel();
		channel.sendTyping().queue();
		channel.sendMessage(getString(HELP_MESSAGE)).queue();
		
		if(!getString(PRICE_KEYWORDS).isEmpty())
			channel.sendMessage("Also you can type one of this keyword if you want to get prices : " + getString(PRICE_KEYWORDS)).queue();
	}


	private void responseSearch(MessageReceivedEvent event,String name) 
	{
		boolean priceask = !StringUtils.isEmpty(getString(PRICE_KEYWORDS)) && StringUtils.containsAny(event.getMessage().getContentRaw().toLowerCase(), getArray(PRICE_KEYWORDS));
		final List<MagicCard> liste = new ArrayList<>();
		MagicEdition ed = null;
		if(name.contains("|"))
		{
			ed = new MagicEdition();
			ed.setId(name.substring(name.indexOf('|')+1,name.length()).toUpperCase().trim());
			name=name.substring(0, name.indexOf('|')).trim();
		}
		
		MessageChannel channel = event.getChannel();
			channel.sendTyping().queue();

			try {
				liste.addAll(getEnabledPlugin(MTGCardsProvider.class).searchCardByName(name, ed, false));
			}
			catch(Exception e)	
			{
				logger.error(e);
			}
			
			if(liste.isEmpty())
			{
				channel.sendMessage("Sorry i can't find "+name ).queue();
				return;
			}
			
			var builder = new NavigableEmbed.Builder(event.getChannel());
			for (var x = 0; x < liste.size(); x++) {
				MagicCard result = liste.get(x);
				BiFunction<MagicCard, Integer, MessageEmbed> getEmbed = (c, resultIndex) -> {
					var embed=parseCard(result,getBoolean(SHOWPRICE)||priceask);
					var eb = new EmbedBuilder(embed);
					if (liste.size() > 1)
						eb.setFooter("Result " + (resultIndex + 1) + "/" + liste.size(), null);
					
					return eb.build();
				};
				int finalIndex = x;
				builder.addEmbed(() -> getEmbed.apply(result, finalIndex));
			}
			
			NavigableEmbed navEb = builder.build();
			
			
			if(liste.size()>1)
			{
				applyControl(EmbedButton.PREVIOUS.getIcon(), navEb.getMessage(), navEb.getWidth() > 1);
				applyControl(EmbedButton.NEXT.getIcon(), navEb.getMessage(), navEb.getWidth() > 1);
		
				var rl = new ReactionListener(jda, navEb.getMessage(), false, 30L * 1000L);
				rl.addController(event.getAuthor());
				rl.addResponse(EmbedButton.PREVIOUS.getIcon(), ev -> {
					navEb.setY(0);
					if (navEb.getX() > 0) navEb.left();
					applyControl(EmbedButton.PREVIOUS.getIcon(), navEb.getMessage(), navEb.getWidth() > 1);
				});
				rl.addResponse(EmbedButton.NEXT.getIcon(), ev -> {
					navEb.setY(0);
					if (navEb.getX() < navEb.getWidth() - 1) navEb.right();
					applyControl(EmbedButton.NEXT.getIcon(), navEb.getMessage(), navEb.getWidth() > 1);
				});

			}
		
	}


	private void applyControl(String emote, Message message, boolean enabled) {
			message.addReaction(emote).queue();
			if (!enabled) {
				message.getReactions().parallelStream().filter(r -> r.getReactionEmote().getEmote().getName().equals(emote))
								   .forEach(r -> {
									   	try {
											r.retrieveUsers().submit().get().parallelStream().forEach(u -> r.removeReaction(u).queue());
										} 
									   	catch(InterruptedException ex)
									   	{
									   		Thread.currentThread().interrupt();
									   	}catch (Exception e) {
											logger.error(e);
										}
								   	});
		}
	}
	
	
	private MessageEmbed parseCard(MagicCard mc,boolean price) {
		
		var eb = new EmbedBuilder();
		eb.setDescription("");
		eb.setTitle(mc.getName()+ " " + (mc.getCost()!=null?mc.getCost():""));
		eb.setColor(MTGColor.determine(mc.getColors()).toColor());
			
		var temp = new StringBuilder();
		temp.append(mc.getTypes()+"\n");
		temp.append(mc.getText()).append("\n");
		temp.append("**Edition:** ").append(mc.getCurrentSet().getSet()).append("\n");
		
		if(mc.getExtra()!=null)
			temp.append("**").append(mc.getExtra().toPrettyString()).append("** ").append("\n");
		
		temp.append("**Reserved:** ");
		if(mc.isReserved())
			temp.append(":white_check_mark: \n");
		else
			temp.append(":no_entry_sign:  \n");
		
		if(getBoolean(SHOWCOLLECTIONS)) {
			try {
				temp.append("**Present in:** "+getEnabledPlugin(MTGDao.class).listCollectionFromCards(mc).toString());
			} catch (SQLException e) {
				logger.error(e);
			}
		}
		eb.setDescription(temp.toString());
	
		if(getString(THUMBNAIL_IMAGE).equalsIgnoreCase("THUMBNAIL"))
			eb.setThumbnail(MTG.getEnabledPlugin(MTGPictureProvider.class).generateUrl(mc));
		else
			eb.setImage(MTG.getEnabledPlugin(MTGPictureProvider.class).generateUrl(mc));
		
		if(price) {
			listEnabledPlugins(MTGPricesProvider.class).forEach(prov->{
				List<MagicPrice> prices = null;
				
					try {
						prices = prov.getPrice(mc);
						Collections.sort(prices, new MagicPricesComparator());
						if(!prices.isEmpty())
							eb.addField(prov.getName(),UITools.formatDouble(prices.get(0).getValue())+prices.get(0).getCurrency().getCurrencyCode(),true);
					} catch (Exception e) {
						logger.error(e);
					}
					
					try {
						if(prices!=null && !prices.isEmpty()) {
							prices = prices.stream().filter(MagicPrice::isFoil).collect(Collectors.toList());
							Collections.sort(prices, new MagicPricesComparator());
							if(prices!=null && !prices.isEmpty())
								eb.addField(prov.getName() +" foil",UITools.formatDouble(prices.get(0).getValue())+" "+prices.get(0).getCurrency().getCurrencyCode(),true);
						}
					} catch (Exception e) {
						logger.error(e);
					}
					
					
				}
			);
		}
		return eb.build();
	}
	
	@Override
	public void start() throws IOException {
		try {
			initListener();
			
			
			
			jda = JDABuilder.createDefault(getString(TOKEN))
							.addEventListeners(listener)
							.build();
		
		} catch (LoginException e) {
			throw new IOException(e);
		}
		logger.info("Server " + getName() +" started");

	}

	@Override
	public void stop() throws IOException {
		if(jda!=null)
		{
			jda.shutdown();
			jda.getPresence().setPresence(OnlineStatus.OFFLINE,false);
		}
	}

	@Override
	public boolean isAlive() {
		if(jda!=null)
			return jda.getStatus().equals(JDA.Status.CONNECTED);
		return false;
	}

	@Override
	public boolean isAutostart() {
		return getBoolean(AUTOSTART);
	}

	@Override
	public String description() {
		return "Query your MTGDesktopCompanion via discord Channel";
	}

	@Override
	public String getName() {
		return "Discord Bot";
	}

	
	@Override
	public Icon getIcon() {
		return new ImageIcon(DiscordBotServer.class.getResource("/icons/plugins/discord.png"));
	}
	
	@Override
	public void initDefault() 
	{
		setProperty(TOKEN,"");
		setProperty(AUTOSTART, "false");
		setProperty(SHOWPRICE, "true");
		setProperty(THUMBNAIL_IMAGE, "THUMBNAIL");
		setProperty(REGEX,"\\{(.*?)\\}");
		setProperty(SHOWCOLLECTIONS,"true");
		setProperty(PRICE_KEYWORDS,"price,prix,how much,cost");
		setProperty(HELP_MESSAGE,":face_with_monocle: It's simple Einstein, put card name in bracket like {Black Lotus} or {Black Lotus| LEA} if you want to specify a set");
		
	}

}

//=================================================================EMBEDED MESSAGE
class NavigableEmbed extends ListenerAdapter {

	public enum EmbedButton {
		PREVIOUS("\u2b05"),
		NEXT("\u27a1");

		private String icon;

		EmbedButton(String icon) {
			this.icon = icon;
		}

		public String getIcon() {
			return icon;
		}
	}
	
	
	// Preferences
	private List<List<Supplier<MessageEmbed>>> embeds;
	private MessageChannel channel;

	// Internals
	private int xindex;
	private int yindex;
	private Message message;

	NavigableEmbed( List<List<Supplier<MessageEmbed>>> embeds,  MessageChannel channel) {
		this.embeds = new ArrayList<>();
		this.embeds.addAll(embeds);
		this.channel = channel;
		xindex = 0;
		yindex = 0;
		sendMessage();
	}

	public Message getMessage() {
		return message;
	}

	public int getX() {
		return xindex;
	}

	public int getY() {
		return yindex;
	}

	public int getWidth() {
		return embeds.size();
	}

	public int getHeight() {
		return embeds.parallelStream().mapToInt(List::size).max().orElse(0);
	}

	public int getHeightAt(int x) {
		if (x < 0 || x >= embeds.size()) throw new IllegalArgumentException("X is out of bounds.");
		return embeds.get(x).size();
	}

	public List<List<Supplier<MessageEmbed>>> getEmbeds() {
		return new ArrayList<>(embeds);
	}

	public void sendMessage() {
		MessageEmbed embed = embeds.get(xindex).get(yindex).get();
		try {
			if (message == null)
				message = channel.sendMessage(embed).submit().get();
			else {
				message = message.editMessage(embed).submit().get();
			}
		} catch (InterruptedException | ExecutionException e) {
			Thread.currentThread().interrupt();
		}
	}

	public void setX(int x) {
		int newX = Math.min(Math.max(x, 0), getWidth() - 1);
		if (newX != xindex) {
			xindex = newX;
			sendMessage();
		}
	}

	public void setY(int y) {
		int newY = Math.min(Math.max(y, 0), embeds.get(xindex).size() - 1);
		if (newY != yindex) {
			yindex = newY;
			sendMessage();
		}
	}

	public void modX(int mod) {
		setX(getX() + mod);
	}

	public void modY(int mod) {
		setY(getY() + mod);
	}

	public void right() {
		modX(1);
	}

	public void left() {
		modX(-1);
	}

	public void up() {
		modY(-1);
	}

	public void down() {
		modY(1);
	}

	public static class Builder {
		private List<List<Supplier<MessageEmbed>>> embeds;
		private MessageChannel channel;

		public Builder( MessageChannel channel) {
			embeds = new ArrayList<>();
			this.channel = channel;
		}

		public Builder addEmbed( Supplier<MessageEmbed> embedSupplier) {
			
			ArrayList<Supplier<MessageEmbed>> list = new ArrayList<>();
			list.add(embedSupplier);
			embeds.add(list);
			return this;
		}

		public Builder addEmbed(Supplier<MessageEmbed> embedSupplier, int xIndex) {
			if (xIndex >= embeds.size())
				throw new IllegalArgumentException("xIndex is not within current bounds of the navigatable embed. " + xIndex + " >= " + embeds.size());
			List<Supplier<MessageEmbed>> xList = embeds.get(xIndex);
			xList.add(embedSupplier);
			return this;
		}

		public NavigableEmbed build() {
			return new NavigableEmbed(embeds, channel);
		}

	}
}

//=================================================================LISTENER
class ReactionListener extends ListenerAdapter {

	private static final long MAX_LIFE = 150000;

	private JDA jda;

	private Map<String, ReactionCallback> actionMap = new HashMap<>();
	private boolean oneTimeUse = false;
	private long expireTimeout = 0;
	private long startTime;
	private Timer expireTimer;
	private Message message;
	private Set<String> controllers = new HashSet<>();

	public ReactionListener(JDA jda, Message message, boolean oneTimeUse, long expireTimeout) {
		this.jda = jda;
		this.message = message;
		this.oneTimeUse = oneTimeUse;
		this.actionMap = new HashMap<>();
		this.expireTimeout = expireTimeout;
		this.expireTimer = new Timer();
		this.startTime = System.currentTimeMillis();
		enable();

		// Force disable after max life expiry
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				disable();
			}
		}, MAX_LIFE);
	}

	public void addResponse(String reaction, ReactionCallback cb) {
		actionMap.put(reaction, cb);
	}

	@Override
	public void onGenericMessageReaction(GenericMessageReactionEvent event) {
		
		if (message == null || event.getMessageIdLong() != message.getIdLong() || event.getUser()==null|| !controllers.contains(event.getUser().getId()))
			return;
		
		
		ReactionCallback cb = actionMap.getOrDefault(event.getReactionEmote().getName(), null);
	
		if (cb != null) {
			cb.exec(event);
			if (oneTimeUse) disable();
			else resetTimer();
		}
	}

	private void enable() {
		this.jda.addEventListener(this);
		if (this.expireTimeout > 0) resetTimer();
	}

	public void disable() {
		this.jda.removeEventListener(this);
		this.expireTimer.cancel();
	}

	private void resetTimer() {
		if (System.currentTimeMillis() - startTime >= MAX_LIFE) return;
		if (this.expireTimeout > 0) {
			this.expireTimer.cancel();
			this.expireTimer = new Timer();
			this.expireTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					disable();
				}
			}, expireTimeout);
		}
	}

	public void addController(User author) {
		controllers.add(author.getId());
	}

	public interface ReactionCallback {

		void exec(GenericMessageReactionEvent event);
	}
}




