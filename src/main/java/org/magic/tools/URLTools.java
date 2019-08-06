package org.magic.tools;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import javax.imageio.ImageIO;
import javax.net.ssl.SSLHandshakeException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.magic.services.MTGConstants;
import org.magic.services.MTGLogger;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

public class URLTools {

	private static Logger logger = MTGLogger.getLogger(URLTools.class);
	
	public static final String HEADER_JSON="application/json";
	public static final String REFERER = "Referer";
	public static final String HOST = "Host";
	public static final String X_REQUESTED_WITH = "X-Requested-With";
	public static final String ACCEPT_LANGUAGE = "Accept-Language";
	public static final String ACCEPT_ENCODING = "Accept-Encoding";
	public static final String ACCEPT = "Accept";
	public static final String ORIGIN = "Origin";
	public static final String UPGR_INSECURE_REQ= "Upgrade-Insecure-Requests";
	
	
	private URLTools()	{}
	
	
	
	
	public static String encode(String s)
	{
		return URLEncoder.encode(s, MTGConstants.DEFAULT_ENCODING);
	}

	public static String extractAsString(URL url,Charset enc) throws IOException
	{
		HttpURLConnection con = openConnection(url);
		String ret = IOUtils.toString(con.getInputStream(), enc);
		close(con);
		return ret;
	}
	
	public static String extractMarkDownAsString(String url) throws IOException
	{
		Parser parser = Parser.builder().build();
		Node document = parser.parse(extractAsString(url));
		HtmlRenderer renderer = HtmlRenderer.builder().build();
		return renderer.render(document); 
	}
	
	
	public static BufferedImage extractImage(URL url) throws IOException
	{
		HttpURLConnection con = openConnection(url);
		BufferedImage im = ImageIO.read(con.getInputStream());
		close(con);
		return im;
	}
	
	public static JsonElement extractJson(String url) throws IOException
	{
		HttpURLConnection con = openConnection(url);
		JsonReader reader = new JsonReader(new InputStreamReader(con.getInputStream()));
		JsonElement e= new JsonParser().parse(reader);
		reader.close();
		close(con);
		return e;
	}
	
	public static void download(String url,File to) throws IOException
	{
		HttpURLConnection con = openConnection(url);
		FileUtils.copyInputStreamToFile(con.getInputStream(),to);
		close(con);
	}
	
	public static org.w3c.dom.Document extractXML(URL url) throws IOException {
		try {
			HttpURLConnection con = openConnection(url);
			org.w3c.dom.Document doc = XMLTools.createSecureXMLFactory().newDocumentBuilder().parse(con.getInputStream());
			close(con);
			return doc;
			
		} catch (Exception e) {
			throw new IOException(e);
		} 
	}
	
	
	
	
	public static HttpURLConnection getConnection(URL url,String userAgent) throws IOException {
		
		Chrono c = new Chrono();
		c.start();

		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		try{
			
			connection.setRequestProperty("User-Agent", userAgent);
			connection.setAllowUserInteraction(true);
			connection.setInstanceFollowRedirects(true);
			connection.setRequestMethod("GET");
			connection.setReadTimeout(MTGConstants.CONNECTION_TIMEOUT);
			int status = connection.getResponseCode();
			if (!isCorrectConnection(connection) && (status == HttpURLConnection.HTTP_MOVED_TEMP|| status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER)) {
				return getConnection(connection.getHeaderField("Location"));
			}
			logger.debug("GET " + url + " : " + connection.getResponseCode() + " [" + c.stopInMillisecond() + "ms]");
		}
		catch(SSLHandshakeException e)
		{
			logger.error(url,e);
		}
		return connection;
	}
	
	
	
	public static HttpURLConnection openConnection(String url) throws IOException {
		return openConnection(new URL(url));
	}
	
	public static HttpURLConnection getConnection(String url) throws IOException {
		return getConnection(new URL(url),MTGConstants.USER_AGENT);
	}
	
	public static HttpURLConnection openConnection(URL url) throws IOException {
		HttpURLConnection con = getConnection(url,MTGConstants.USER_AGENT);
		con.connect();
		return con;
	}
	
	public static Document toHtml(String s)
	{
		return Jsoup.parse(s);
	}
	
	public static JsonElement toJson(String s)
	{
		return new JsonParser().parse(s);
	}
	
	
	public static Document extractHtml(URL uri) throws IOException
	{
		return toHtml(extractAsString(uri));
	}
	
	public static org.w3c.dom.Document extractXML(URI url)  throws IOException
	{
		return extractXML(url.toURL());
	}
	
	
	public static org.w3c.dom.Document extractXML(String url)  throws IOException
	{
		try {
			return extractXML(new URI(url));
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}
	}
	
	public static org.w3c.dom.Document extractXML(File f) throws IOException {
		try {
			return XMLTools.createSecureXMLFactory().newDocumentBuilder().parse(new FileInputStream(f));
		} catch (Exception e) {
			throw new IOException(e);
		} 
	}

	public static Document extractHtml(String url) throws IOException
	{
		return extractHtml(new URL(url));
	}
	
	
	public static String extractAsString(String url,Charset enc) throws IOException
	{
		return extractAsString(new URL(url),enc); 
	}
	
	public static String extractAsString(URL url) throws IOException
	{
		return extractAsString(url,MTGConstants.DEFAULT_ENCODING); 
	}
	
	public static String extractAsString(String url) throws IOException
	{
		return extractAsString(new URL(url),MTGConstants.DEFAULT_ENCODING); 
	}
	
	

	public static BufferedImage extractImage(String url) throws IOException
	{
		return extractImage(new URL(url));
	}
	
	

	public static void close(HttpURLConnection con)
	{
		try {
			con.getInputStream().close();
		} catch (IOException e) {
			logger.error(e);
		}
	}
	
	
	public static boolean isCorrectConnection(HttpURLConnection connection) {
			try {
				
				int resp=connection.getResponseCode();
				if(resp >= 200 && resp < 300)
				{
					return true;
				}
				else
				{
					if(connection.getErrorStream()!=null)
					{
						logger.error("Error " + connection.getURL() +": " +  connection.getRequestMethod()+":"+ IOUtils.toString(connection.getErrorStream(),MTGConstants.DEFAULT_ENCODING));
					}

					return false;
				}
			} catch (IOException e) {
				logger.error(e);
				return false;
			}
	}

	public static URLToolsClient newClient() {
		return new URLToolsClient();
	}
	
	
}
