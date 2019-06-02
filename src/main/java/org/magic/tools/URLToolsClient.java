package org.magic.tools;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.magic.services.MTGConstants;
import org.magic.services.MTGLogger;
import org.magic.tools.RequestBuilder.METHOD;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

public class URLToolsClient {

	private HttpClient httpclient;
	private HttpClientContext httpContext;
	private BasicCookieStore cookieStore;
	private Logger logger = MTGLogger.getLogger(this.getClass());
	private HttpResponse response;

	public HttpResponse getResponse() {
		return response;
	}
	
	public HttpClient getHttpclient() {
		return httpclient;
	}
	
	public HttpClientContext getHttpContext() {
		return httpContext;
	}
	
	
	public URLToolsClient() {
		httpclient = HttpClients.custom().setUserAgent(MTGConstants.USER_AGENT).setRedirectStrategy(new LaxRedirectStrategy()).build();
		httpContext = new HttpClientContext();
		cookieStore = new BasicCookieStore();
		httpContext.setCookieStore(cookieStore);
	}
	
	public String extractAndClose(HttpResponse response) throws IOException
	{
		String ret = EntityUtils.toString(response.getEntity());
		EntityUtils.consume(response.getEntity());
		return ret;
	}
	
	public HttpResponse execute(HttpRequestBase req) throws IOException
	{
		logger.trace("execute " + req);
		HttpResponse resp = httpclient.execute(req,httpContext);
		
		logger.trace("reponse " + resp);
		
		return resp;
	}
	


	public String execute(RequestBuilder builder) throws IOException
	{
		
		if(builder.getMethod()== METHOD.GET)
			return doGet(builder.getUrl(),builder.getHeaders(),builder.getContent());

		if(builder.getMethod()== METHOD.POST)
			return doPost(builder.getUrl(), builder.getContent(), builder.getHeaders());
			
		throw new IOException("choose a method");
		
	}
	
	
	public String doPost(String url, Map<String,String> entities, Map<String,String> headers) throws IOException
	{
		return doPost(url,new UrlEncodedFormEntity(entities.entrySet().stream().map(e-> new BasicNameValuePair(e.getKey(), e.getValue())).collect(Collectors.toList())),headers);
	}

	
	public String doPost(String url, HttpEntity entities, Map<String,String> headers) throws IOException
	{
			HttpPost postReq = new HttpPost(url);
			try {
				if(entities!=null)
					postReq.setEntity(entities);
				
				if(headers!=null)
					headers.entrySet().forEach(e->postReq.addHeader(e.getKey(), e.getValue()));
				
				response  = execute(postReq);
				return extractAndClose(response);
			} catch (UnsupportedEncodingException e1) {
				throw new IOException(e1);
			}

	}
	
	public String doGet(String url, Map<String,String> headers,Map<String,String> entities) throws IOException
	{
		HttpGet getReq = new HttpGet(url);
		
		if(entities!=null && !entities.isEmpty()) 
		{
			try {
				URIBuilder builder = new URIBuilder(url);
				entities.entrySet().forEach(e->builder.addParameter(e.getKey(),e.getValue()));
				getReq = new HttpGet(builder.build());
			} catch (URISyntaxException e1) {
				throw new IOException(e1);
			}
		}
		
		
		if(headers!=null && !headers.isEmpty())
		{
			for(Entry<String, String> e : headers.entrySet())
				getReq.addHeader(e.getKey(), e.getValue());
			
		}
		
		
		response  = execute(getReq);
		return extractAndClose(response);
	}

	public String doGet(String url) throws IOException
	{
		return doGet(url,null,null);
	}

	public String getCookieValue(String cookieName) {
		String value = null;
		for (Cookie cookie : cookieStore.getCookies()) {
			if (cookie.getName().equals(cookieName)) {
				value = cookie.getValue();
				break;
			}
		}
		return value;
		
	}

	public List<Cookie> getCookies() {
		return cookieStore.getCookies();
	}



	public Builder<String, String> buildMap() {
		return new ImmutableMap.Builder<>();
	}
	
	public RequestBuilder build()
	{
		return RequestBuilder.build();
	}
	
	
	
	
}





