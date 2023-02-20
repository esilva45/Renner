package br.com.renner.adapter.recupera.utils;

import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import com.thortech.util.logging.Logger;

public class Token {
	private static Logger logger = Logger.getLogger("BR.COM.RENNER.ADAPTERS.RECUPERA");
	
	public static String getToken(String url, String usuario, String senha, String empresa) {
		logger.info("Entering execute() method getToken");
		String ret = "";
		URL secondURL = null;
		
		try {
			JSONObject json = new JSONObject();
			json.put("usuario", usuario);
			json.put("senha", senha);
			json.put("empresa", empresa);
			
			logger.info("usuario " + usuario);
			logger.info("senha " + senha);
			logger.info("empresa " + empresa);
			
			URL primaryUrl = new URL(url);
			HttpURLConnection urlConnection = (HttpURLConnection) primaryUrl.openConnection();
			urlConnection.setInstanceFollowRedirects(false);
			
			int testCode = urlConnection.getResponseCode();
			
			if (testCode == 307) {
				secondURL = new URL(urlConnection.getHeaderField("Location"));
			} else {
				secondURL = new URL(url);
			}
			
			HttpPost post = new HttpPost(secondURL.toString());
			post.addHeader("content-type", "application/json; charset=utf-8");
			post.setEntity(new StringEntity(json.toString()));
			
			try (CloseableHttpClient httpClient = HttpClients.createDefault();
		        	CloseableHttpResponse response = httpClient.execute(post)) {

	        	logger.info("URL: " + post.getURI());
	        	logger.info("Method: " + post.getMethod());
	        	logger.info("Status line: " + response.getStatusLine().toString());
	        	
	        	if (response.getEntity() != null) {
	        		String result = EntityUtils.toString(response.getEntity());
	        		JSONObject jsonObject = new JSONObject(result);
	        		ret = jsonObject.getString("access_token");
			        logger.info("token: " + ret); 
				} else {
					JSONObject content = new JSONObject(EntityUtils.toString(response.getEntity()));
					logger.error("getToken " + content.getString("mensagem"));
				}
			}
		}
		catch (Exception e) {
			logger.error("getToken ", e);
	    }
		
		return ret;
	}
}
