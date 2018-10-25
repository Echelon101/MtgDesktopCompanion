package org.magic.servers.impl;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.Icon;

import org.apache.commons.io.FileUtils;
import org.magic.api.beans.MagicCollection;
import org.magic.api.interfaces.abstracts.AbstractMTGServer;
import org.magic.services.CollectionEvaluator;
import org.magic.services.MTGConstants;

public class CardShakeDashBoardServer extends AbstractMTGServer {

	private static final String THREAD_PAUSE = "THREAD_PAUSE";
	private static final String TIMEOUT_MINUTE = "TIMEOUT_MINUTE";
	private static final String AUTOSTART = "AUTOSTART";
	private static final String COLLECTION="COLLECTION";
	private Timer timer;
	private TimerTask tache;
	private boolean running = false;

	@Override
	public Icon getIcon() {
		return MTGConstants.ICON_DASHBOARD;
	}

	@Override
	public String description() {
		return "backup prices editions";
	}

	public CardShakeDashBoardServer() {

		super();
		timer = new Timer();
	}
	
	public void start() {
		running = true;
		tache = new TimerTask() {
			public void run() {
				
				try {
					CollectionEvaluator evaluator = new CollectionEvaluator(new MagicCollection(getString(COLLECTION)));
					logger.debug("backuping files");
					File dest = new File(evaluator.getDirectory(),new SimpleDateFormat("yyyyMMdd").format(new Date()));
					for(File f : evaluator.getDirectory().listFiles(pathname->{return !pathname.isDirectory();})){
								FileUtils.moveFileToDirectory(f, dest, true);	
					}
					
					logger.debug("updating cache");
					evaluator.initCache();
					logger.info("cache update done");
					
				} catch (IOException e) {
					logger.error(e);
				}

			}
		};

		timer.scheduleAtFixedRate(tache, 0, Long.parseLong(getString(TIMEOUT_MINUTE)) * 60000);
		logger.info("Server start with " + getString(TIMEOUT_MINUTE) + " min timeout");

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
		return "CardShake cache server";

	}

	@Override
	public boolean isAutostart() {
		return getBoolean(AUTOSTART);
	}

	@Override
	public void initDefault() {
		setProperty(AUTOSTART, "false");
		setProperty(TIMEOUT_MINUTE, "1440");
		setProperty(THREAD_PAUSE,"2000");
		setProperty(COLLECTION,"Library");
	}

	@Override
	public String getVersion() {
		return "1.5";
	}

}
