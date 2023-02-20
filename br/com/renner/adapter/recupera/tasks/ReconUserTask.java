package br.com.renner.adapter.recupera.tasks;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.identityconnectors.common.logging.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import br.com.renner.adapter.recupera.utils.Resource;
import br.com.renner.adapter.recupera.utils.Token;
import oracle.iam.platform.Platform;
import oracle.iam.reconciliation.api.ChangeType;
import oracle.iam.reconciliation.api.EventAttributes;
import oracle.iam.reconciliation.api.ReconOperationsService;
import oracle.iam.scheduler.vo.TaskSupport;

public class ReconUserTask extends TaskSupport {
	private static Log logger = Log.getLog(ReconUserTask.class);
	private static String RESOURCE_OBJECT_NAME = null;
	private static ReconOperationsService recService = null;
	private final Resource itOps = new Resource();
	
	@SuppressWarnings("rawtypes")
	@Override
	public void execute(HashMap arg0) throws Exception {
		logger.info("Entering execute() method");
		recService = Platform.getService(ReconOperationsService.class);
		
		String[] parts = null;
		String cod_opera = null, login = null, nome = null, tipo_autenticacao = null, tipo_discagem = null, discador = null, 
				login_superior = null, nivel_negociacao = null, cod_perfil = null, cod_local = null, tipo_operador = null;
		
		int responseCode = 501;
		String it_resource = (String)arg0.get("IT Resource");
		
		HashMap<String, String> adparameter = itOps.getParametersFromItResource(it_resource);
	    final String url = adparameter.get("Endpoint");
		final String usuario = adparameter.get("User");
		final String senha = adparameter.get("Password");
		final String empresa = adparameter.get("Company");
		final String url_aut = adparameter.get("Path autenticar");
		
		String url_api = url + (String)arg0.get("Contexto API");
		String filter = (String)arg0.get("Filtro");
		RESOURCE_OBJECT_NAME = (String)arg0.get("Recurso");
		
		String accessToken = Token.getToken(url + url_aut, usuario, senha, empresa);
		
		if (filter != null && !filter.isEmpty()) {
			url_api = url_api + "?" + filter;
		}
		
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
		        
		        logger.info("jsonarray " + jsonarray);
		        
		        for (int i = 0; i < jsonarray.length(); i++) {
		            JSONObject jsonobject = jsonarray.getJSONObject(i);
		            
		            cod_opera = jsonobject.isNull("cod_opera") ? "" : jsonobject.getString("cod_opera");
		            login = jsonobject.isNull("login") ? "" : jsonobject.getString("login");
		            nome = jsonobject.isNull("nome") ? "" : jsonobject.getString("nome");
		            tipo_autenticacao = jsonobject.isNull("tipo_autenticacao") ? "" : jsonobject.getString("tipo_autenticacao");
		            tipo_discagem = jsonobject.isNull("tipo_discagem") ? "" : jsonobject.getString("tipo_discagem");
		            discador = jsonobject.isNull("discador") ? "" : jsonobject.getString("discador");
		            login_superior = jsonobject.isNull("login_superior") ? "" : jsonobject.getString("login_superior");
		            tipo_operador = jsonobject.isNull("tipo_operador") ? "" : jsonobject.getString("tipo_operador");  
		            cod_perfil = jsonobject.isNull("perfil") ? "" : jsonobject.getString("perfil");
		            
		            nivel_negociacao = jsonobject.isNull("nivel_negociacao") ? "" : jsonobject.getString("nivel_negociacao");
		            parts = nivel_negociacao.split("-");
		            nivel_negociacao = parts[0].trim();

		            cod_local = jsonobject.isNull("local") ? "" : jsonobject.getString("local");
		            parts = cod_local.split("-");
		            cod_local = parts[0].trim();
		            
		            reconUser(it_resource,cod_opera,login,nome,tipo_autenticacao,tipo_discagem,discador,login_superior,nivel_negociacao,cod_perfil,cod_local,tipo_operador);
		        }
			} else {
				logger.info("Status line: " + response.getStatusLine().toString());
			}
        }
		
		logger.info("Exiting execute() method");
	}

	private void reconUser(String ITResourceName,String cod_opera, String login, String nome, String tipo_autenticacao, String tipo_discagem, String discador, 
			String login_superior, String nivel_negociacao, String cod_perfil, String cod_local, String tipo_operador) {
		Map<String, Object> mapKeyValue = new HashMap<String,Object>();
		String error = null;
		long reconEventKey = 0l;
		
		try {
			mapKeyValue.put("IT Resource", ITResourceName);
			mapKeyValue.put("Cod Opera", cod_opera);
			mapKeyValue.put("Login", login);
			mapKeyValue.put("Nome", nome);
			mapKeyValue.put("Tipo Autenticacao", tipo_autenticacao);
			mapKeyValue.put("Tipo Operador", tipo_operador);
			mapKeyValue.put("Tipo Discagem", tipo_discagem);
			mapKeyValue.put("Discador", discador);
			mapKeyValue.put("Login Superior", login_superior);
			mapKeyValue.put("Nivel Negociacao", nivel_negociacao);
			mapKeyValue.put("Perfil", cod_perfil);
			mapKeyValue.put("Local", cod_local);
            
            EventAttributes eventAttr = new EventAttributes();

            eventAttr.setEventFinished(false); // Child is going to be provided; Event will be in "Event Recieved" state
            eventAttr.setActionDate(null); // Processing is done instantly; no defering date
            eventAttr.setChangeType(ChangeType.CHANGELOG); // For create and modify operations with incomplete or just required dataset.
            
            reconEventKey = recService.createReconciliationEvent(RESOURCE_OBJECT_NAME, mapKeyValue, eventAttr);
            
            logger.info("Recon Event ID:  " + reconEventKey);
            
            // Marks the status of a reconciliation event as 'Data Received' which was left in status 'Event Received' to allow additional data (child table data) to be added
            recService.finishReconciliationEvent(reconEventKey);
            // Call OIM API to process reconciliation event
            recService.processReconciliationEvent(reconEventKey);
            Thread.sleep(1000);
            
            logger.info("Completed Recon Event Operation");
		}
		catch(Exception e) {
			error = e.getMessage();

			if (error.contains("No User Match Found")) {
				closeRecon(reconEventKey);
			} else {
				logger.error(e, "Error in execute", new Object[0]);
			}
        }
	}
	
	private static void closeRecon(long reconEventKey) {
		try {
			recService.closeReconciliationEvent(reconEventKey);
			logger.info("Recon Event Closed:  " + reconEventKey);
		}
		catch(Exception e) {
			logger.error(e, "Error in closeRecon", new Object[0]);
		}
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
