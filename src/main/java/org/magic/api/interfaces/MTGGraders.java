package org.magic.api.interfaces;

import java.io.IOException;
import java.net.URL;

import org.magic.api.beans.Grading;

public interface MTGGraders extends MTGPlugin {
	
	public Grading loadGrading(String identifier) throws IOException;
	public String getWebSite();
	
	
	
}
