package org.magic.gui.components.browser;

import java.awt.BorderLayout;
import java.io.IOException;

import org.cef.browser.CefBrowser;
import org.cef.handler.CefLoadHandlerAdapter;
import org.magic.gui.abstracts.MTGUIBrowserComponent;
import org.magic.services.MTGConstants;
import org.magic.tools.UITools;
import org.panda_lang.pandomium.Pandomium;
import org.panda_lang.pandomium.settings.PandomiumSettings;
import org.panda_lang.pandomium.util.os.PandomiumOS;
import org.panda_lang.pandomium.wrapper.PandomiumBrowser;
import org.panda_lang.pandomium.wrapper.PandomiumClient;

public class ChromiumBrowserComponent extends MTGUIBrowserComponent {

	private static final long serialVersionUID = 1L;
	private transient PandomiumClient client;
	private transient PandomiumBrowser browser;
	private String currentUrl;
	
	
	public ChromiumBrowserComponent() throws IOException {
		setLayout(new BorderLayout());
		
		
		try {
			client = UITools.getPandomiumInstance().createClient();
			browser = client.loadURL("about:blank");
			add(browser.toAWTComponent(),BorderLayout.CENTER);
			
			client.getCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
				
				@Override
				public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
					if(!isLoading)
					{
						observable.setChanged();
						observable.notifyObservers(browser.getURL());
					}
				}
					
			});
			
			
		} catch (UnsatisfiedLinkError e) {
			logger.error("maybe add : -Djava.library.path=\""+MTGConstants.NATIVE_DIR+"\" at jvm startup args");
			throw new IOException(e);
		} 
			
	}
	
	
	

	@Override
	public String getCurrentURL() {
		return currentUrl;
				
	}

	@Override
	public void loadURL(String url) {
		logger.debug("browse to " + url);
		browser.getCefBrowser().loadURL(url);
	}

	

}
