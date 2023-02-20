package br.com.renner.adapter.recupera.tasks;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.identityconnectors.common.logging.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import Thor.API.Operations.tcLookupOperationsIntf;
import br.com.renner.adapter.recupera.utils.Resource;
import br.com.renner.adapter.recupera.utils.Token;
import br.com.renner.adapter.recupera.utils.Util;
import oracle.iam.platform.Platform;
import oracle.iam.scheduler.vo.TaskSupport;

public class ReconLookupTask extends TaskSupport {
	private static Log logger = Log.getLog(ReconLookupTask.class);
	private final Resource itOps = new Resource();
	tcLookupOperationsIntf lookupAPI = null;
	
	@SuppressWarnings("rawtypes")
	@Override
	public void execute(HashMap arg0) throws Exception {
		logger.info("Entering execute() method");
		
		lookupAPI = Platform.getService(tcLookupOperationsIntf.class);

		int responseCode = 501;
		
		String it_resource = (String)arg0.get("IT Resource");

		HashMap<String, String> adparameter = itOps.getParametersFromItResource(it_resource);
	    final String url = adparameter.get("Endpoint");
		final String usuario = adparameter.get("User");
		final String senha = adparameter.get("Password");
		final String empresa = adparameter.get("Company");
		final String url_aut = adparameter.get("Path autenticar");
		
	    String url_api = url + (String)arg0.get("Contexto API");
		String lookup = (String)arg0.get("Lookup");
		String accessToken = Token.getToken(url + url_aut, usuario, senha, empresa);

		URL primaryUrl = new URL(url_api);
		HttpURLConnection urlConnection = (HttpURLConnection) primaryUrl.openConnection();
		urlConnection.setInstanceFollowRedirects(false);
		URL secondURL = new URL(urlConnection.getHeaderField("Location"));
		
		HttpGet post = new HttpGet(secondURL.toString());
		post.addHeader("content-type", "application/json; charset=utf-8");
        post.addHeader("Authorization", "Bearer " + accessToken);
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
	        	CloseableHttpResponse response = httpClient.execute(post)) {
        	
        	responseCode = response.getStatusLine().getStatusCode();
        	
        	if (responseCode == 200) {
		        JSONArray jsonarray = new JSONArray(EntityUtils.toString(response.getEntity()));
		        
		        for (int i = 0; i < jsonarray.length(); i++) {
		            JSONObject jsonobject = jsonarray.getJSONObject(i);
		            
		            try {
		            	if (lookup.equals("Lookup.Recupera.Local")) {
		            		logger.info("read values codigo=" + jsonobject.getString("codigo") + ", descricao=" + jsonobject.getString("razao_social"));
		            		lookupAPI.addLookupValue(lookup, Util.sanitizeString(jsonobject.getString("codigo")), jsonobject.getString("razao_social"), "pt", "BR");
		            	} else {
		            		logger.info("read values codigo=" + jsonobject.getString("codigo") +", descricao=" + jsonobject.getString("descricao"));
		            		lookupAPI.addLookupValue(lookup, Util.sanitizeString(jsonobject.getString("codigo")), jsonobject.getString("descricao"), "pt", "BR");
		            	}
		            }
		            catch (Thor.API.Exceptions.tcInvalidValueException e) {
		            	logger.error("RecuperaReconLookupTask Exception: " + e.getMessage());
		            }
		        }
			} else {
				logger.info("Status line: " + response.getStatusLine().toString());
			}
        }
        
		logger.info("Exiting execute() method");
	}

	@Override
	public boolean stop() {
		return true;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public HashMap getAttributes() {
		return null;
	}

	@Override
	public void setAttributes() { }

}
