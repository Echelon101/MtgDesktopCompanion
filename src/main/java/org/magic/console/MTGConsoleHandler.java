package org.magic.console;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.magic.api.interfaces.MTGCommand;
import org.magic.services.MTGConstants;
import org.magic.services.MTGLogger;
import org.magic.services.PluginRegistry;

public class MTGConsoleHandler extends IoHandlerAdapter {


	private static Logger logger = MTGLogger.getLogger(MTGConsoleHandler.class);

	private List<String> history;

	public MTGConsoleHandler() {
		history = new ArrayList<>();
	}

	public List<String> getHistory() {
		return history;
	}

	@Override
	public void sessionOpened(IoSession session) throws Exception {
		session.write("Welcome to MTG Desktop Companion Server\r\n");
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		logger.debug("client disconnection : " + session.getRemoteAddress() + " is Disconnection");

	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		if (session.getCurrentWriteMessage() != null) {
			session.write(cause + "\n");
			logger.error(cause);
		}
	}

	public static MTGCommand commandFactory(String name) {
		try {
			return PluginRegistry.inst().newInstance(MTGConstants.COMMANDS_PACKAGE+StringUtils.capitalize(name));
		} catch (ClassNotFoundException e) {
			logger.error(e);
		}
		return null;
	}
	
	
	public static String[] translateCommandline(String stringCommand) 
	{
		
        if (stringCommand == null || stringCommand.isEmpty()) {
          return new String[0];
        }
        StringTokenizer tok = new StringTokenizer(stringCommand, " ", false);
        List<String> list = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        
        while (tok.hasMoreTokens()) 
        {
        	String currentTok = tok.nextToken();
        	
        	if(currentTok.startsWith("-"))
        	{
        		if(current.length()>0)
        		{
        			list.add(current.toString().trim());
        			current=new StringBuilder();
        		}
        		list.add(currentTok);
        	}
        	else
        	{
        		current.append(currentTok).append(" ");
        	}
        }
        
        if(!current.toString().isEmpty())
        	list.add(current.toString().trim());
        
        
        final String[] args = new String[list.size()];
        return list.toArray(args);
    }
	
	
	@Override
	public void messageReceived(IoSession session, Object message)throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, ParseException,InvocationTargetException, NoSuchMethodException {
		if (message == null)
			return;

		if (message.toString().equals("cls") || message.toString().equals("clear")) {
			session.write("\033[2J");
		} else {
			String line = message.toString();
			String[] commandeLine = translateCommandline(line);
			MTGCommand c = commandFactory(commandeLine[0]);
			
			if(c==null)
			{
				session.write("Command not found");
			}
			else
			{
				logger.debug("message="+line + " commandLine="+Arrays.asList(commandeLine) + " Command="+c);
				CommandResponse<?> ret = c.run(commandeLine);
				session.write(ret);
				c.quit();
				history.add(line);
				
			}
		}

	}

}