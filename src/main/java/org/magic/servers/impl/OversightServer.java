package org.magic.servers.impl;

import java.awt.TrayIcon.MessageType;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import org.magic.api.beans.CardShake;
import org.magic.api.beans.MagicCardAlert;
import org.magic.api.interfaces.MTGCardsProvider.STATUT;
import org.magic.api.interfaces.abstracts.AbstractMTGServer;
import org.magic.services.MTGControler;

import com.google.common.collect.Iterables;

public class OversightServer extends AbstractMTGServer {

	@Override
	public STATUT getStatut() {
		return STATUT.STABLE;
	}

	private Timer timer;
	private TimerTask tache;
	private boolean running = false;
	private boolean enableNotify = true;
	
	public void enableGUINotify(boolean enableNotify) {
		this.enableNotify = enableNotify;
	}

	@Override
	public String description() {
		return "oversight for daily price variation";
	}

	public OversightServer() {

		super();
		timer = new Timer();
	}
	
	public void start() {
		running = true;
		tache = new TimerTask() {
			public void run() {
				try {
					List<CardShake> ret = MTGControler.getInstance().getEnabledDashBoard().getShakerFor(null);
					Collections.sort(ret, (CardShake o1, CardShake o2) -> {
						if (o1.getPriceDayChange() > o2.getPriceDayChange())
							return -1;

						if (o1.getPriceDayChange() < o2.getPriceDayChange())
							return 1;

						return 0;
					});
				
					//TODO report trends email, telegraph ? 
					
				} catch (IOException e) {
					logger.error(e);
				}

			}
		};

		timer.scheduleAtFixedRate(tache, 0, Long.parseLong(getString("TIMEOUT_MINUTE")) * 60000);
		logger.info("Server start with " + getString("TIMEOUT_MINUTE") + " min timeout");

	}

	
	public void stop() {
		tache.cancel();
		timer.purge();
		running = false;
	}

	@Override
	public boolean isAlive() {
		return running;
	}

	@Override
	public String getName() {
		return "Oversight Server";

	}

	@Override
	public boolean isAutostart() {
		return getBoolean("AUTOSTART");
	}

	@Override
	public void initDefault() {
		setProperty("AUTOSTART", "true");
		setProperty("TIMEOUT_MINUTE", "120");
		setProperty("ALERT_MIN_PERCENT","40");
		setProperty("REPORTER","email");
	}

	@Override
	public String getVersion() {
		return "1.5";
	}

}
