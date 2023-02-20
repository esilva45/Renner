package br.com.renner.adapter.recupera;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.identityconnectors.common.logging.Log;
import org.json.JSONObject;

import br.com.renner.adapter.recupera.utils.Token;

public class RecuperaAdapter {
	private Log logger = Log.getLog(getClass());
	private String conn_url;
	private String path_aut;
	private String conn_usuario;
	private String conn_senha;
	private String conn_empresa;
	
	public RecuperaAdapter(String url, String path_aut, String usuario, String senha, String empresa) {
		this.conn_url = url;
		this.path_aut = path_aut;
		this.conn_usuario = usuario;
		this.conn_senha = senha;
		this.conn_empresa = empresa;
	}
	
	public RecuperaAdapter() {}
	
	/**
	 * POST
	 * @return 200 - Success
	 * @return 400 - Bad Request
	 * @return 401 - Unauthorized
	 * @return 500 - Server Error
	 * @return 501 - Generic Error
	 * @param cod_opera: usr_key
	 * @param login: Login OIM
	 * @param nome: nome completo
	 * @param tipo_operador: 
	 * @param tipo_autenticacao: 
	 * @param tipo_discagem: 
	 * @param discador: 
	 * @param login_superior: Login do gestor
	 * @param nivel_negociacao: 
	 * @param cod_perfil: 
	 * @param cod_local: 
	 */
	public String createUser(String path, String cod_opera, String login, String senha, String nome, String tipo_operador, String tipo_autenticacao, String tipo_discagem, 
			String discador, String login_superior, String nivel_negociacao, String cod_perfil, String cod_local) {
		String ret = "501";
		
		try {
			if (nome.length() > 44)
				nome = nome.substring(0, 44);
			
			JSONObject json = new JSONObject();
			json.put("cod_opera", cod_opera);
			json.put("login", login);
			
			if (senha != null && !senha.isEmpty()) {
				json.put("senha", senha);
			}

			json.put("nome", nome);

			if (tipo_operador != null && !tipo_operador.isEmpty()) {
				json.put("tipo_operador", tipo_operador);
			}
			if (tipo_autenticacao != null && !tipo_autenticacao.isEmpty()) {
				json.put("tipo_autenticacao", tipo_autenticacao);
			}
			if (tipo_discagem != null && !tipo_discagem.isEmpty()) {
				json.put("tipo_discagem", tipo_discagem);
			}
			if (discador != null && !discador.isEmpty()) {
				json.put("discador", discador);
			}
			if (login_superior != null && !login_superior.isEmpty()) {
				json.put("login_superior", login_superior);
			}
			if (nivel_negociacao != null && !nivel_negociacao.isEmpty()) {
				json.put("nivel_negociacao", nivel_negociacao);
			}

			json.put("perfil", cod_perfil);
			json.put("cod_local", cod_local);

			String accessToken = Token.getToken(conn_url + path_aut, conn_usuario, conn_senha, conn_empresa);
			
			if (accessToken != null && !accessToken.isEmpty()) {
				ret = send(0, "POST", accessToken, conn_url + path, json.toString());
			}
		}
		catch (Exception e) {
			logger.error(e, "createUser", new Object[0]);
	    } 
		return ret;
	}
	
	/**
	 * PUT
	 * @return 200 - Success
	 * @return 400 - Bad Request
	 * @return 401 - Unauthorized
	 * @return 500 - Server Error
	 * @return 501 - Generic Error
	 * @param cod_opera: usr_key
	 * @param login: Login OIM
	 * @param nome: nome completo
	 * @param tipo_operador: 
	 * @param tipo_autenticacao: 
	 * @param tipo_discagem: 
	 * @param discador: 
	 * @param login_superior: Login do gestor
	 * @param nivel_negociacao: 
	 * @param cod_perfil: 
	 * @param cod_local: 
	 */
	public String updateUser(String path, String cod_opera, String login, String senha, String nome, String tipo_operador, String tipo_autenticacao, String tipo_discagem, 
			String discador, String login_superior, String nivel_negociacao, String cod_perfil, String cod_local) {
		String ret = "501";
		
		try {
			if (nome.length() > 44)
				nome = nome.substring(0, 44);
			
			JSONObject json = new JSONObject();
			json.put("cod_opera", cod_opera);
			
			json.put("nome", nome);
			/*
			json.put("login", login);
			json.put("senha", senha);
			json.put("tipo_operador", tipo_operador);
			json.put("tipo_autenticacao", tipo_autenticacao);
			json.put("tipo_discagem", tipo_discagem);
			json.put("discador", discador);
			json.put("login_superior", login_superior);
			json.put("nivel_negociacao", nivel_negociacao);
			json.put("cod_perfil", cod_perfil);
			json.put("cod_local", cod_local);
			*/

			String accessToken = Token.getToken(conn_url + path_aut, conn_usuario, conn_senha, conn_empresa);
			
			if (accessToken != null && !accessToken.isEmpty()) {
				ret = send(1, "PUT", accessToken, conn_url + path, json.toString());
			}
		}
		catch (Exception e) {
			logger.error(e, "updateUser", new Object[0]);
	    } 
		return ret;
	}
	
	/**
	 * POST
	 * @return 200 - Success
	 * @return 400 - Bad Request
	 * @return 401 - Unauthorized
	 * @return 500 - Server Error
	 * @return 501 - Generic Error
	 * @param cod_opera: usr_key
	 */
	public String disableUser(String path, String cod_opera) {
		String ret = "501";
		
		try {
			JSONObject json = new JSONObject();
			json.put("cod_opera", cod_opera);
			
			String accessToken = Token.getToken(conn_url + path_aut, conn_usuario, conn_senha, conn_empresa);
			
			if (accessToken != null && !accessToken.isEmpty()) {
				ret = send(0, "POST", accessToken, conn_url + path, json.toString());
			}
		}
		catch (Exception e) {
			logger.error(e, "disableUser", new Object[0]);
	    } 
		return ret;
	}
	
	/**
	 * DELETE
	 * @return 200 - Success
	 * @return 400 - Bad Request
	 * @return 401 - Unauthorized
	 * @return 500 - Server Error
	 * @return 501 - Generic Error
	 * @param cod_opera: usr_key
	 */
	public String enableUser(String path, String cod_opera) {
		String ret = "501";
		
		try {
			JSONObject json = new JSONObject();
			json.put("cod_opera", cod_opera);
			
			String accessToken = Token.getToken(conn_url + path_aut, conn_usuario, conn_senha, conn_empresa);
			
			if (accessToken != null && !accessToken.isEmpty()) {
				ret = send(2, "DELETE", accessToken, conn_url + path, json.toString());
			}
		}
		catch (Exception e) {
			logger.error(e, "enableUser", new Object[0]);
	    } 
		return ret;
	}

	/**
	 * DELETE
	 * @return 200 - Success
	 * @return 400 - Bad Request
	 * @return 401 - Unauthorized
	 * @return 500 - Server Error
	 * @return 501 - Generic Error
	 * @param cod_opera: usr_key
	 */
	public String deleteUser(String path, String cod_opera) {
		String ret = "501";
		
		try {
			JSONObject json = new JSONObject();
			json.put("cod_opera", cod_opera);
			
			String accessToken = Token.getToken(conn_url + path_aut, conn_usuario, conn_senha, conn_empresa);
			
			if (accessToken != null && !accessToken.isEmpty()) {
				ret = send(2, "DELETE", accessToken, conn_url + path, json.toString());
			}
		}
		catch (Exception e) {
			logger.error(e, "deleteUser", new Object[0]);
	    } 
		return ret;
	}
	
	/**
	 * PUT
	 * @return 200 - Success
	 * @return 400 - Bad Request
	 * @return 401 - Unauthorized
	 * @return 500 - Server Error
	 * @return 501 - Generic Error
	 * @param cod_opera: usr_key
	 */
	public String updateProfile(String path, String cod_opera, String cod_perfil) {
		String ret = "501";
		
		try {
			JSONObject json = new JSONObject();
			json.put("cod_opera", cod_opera);
			json.put("perfil", cod_perfil);
			
			String accessToken = Token.getToken(conn_url + path_aut, conn_usuario, conn_senha, conn_empresa);
			
			if (accessToken != null && !accessToken.isEmpty()) {
				ret = send(1, "PUT", accessToken, conn_url + path, json.toString());
			}
		}
		catch (Exception e) {
			logger.error(e, "updateProfile", new Object[0]);
	    } 
		return ret;
	}
	
	/**
	 * PUT
	 * @return 200 - Success
	 * @return 400 - Bad Request
	 * @return 401 - Unauthorized
	 * @return 500 - Server Error
	 * @return 501 - Generic Error
	 * @param cod_opera: usr_key
	 */
	public String updateNivelNegociacao(String path, String cod_opera, String nivel_negociacao) {
		String ret = "501";
		
		try {
			JSONObject json = new JSONObject();
			json.put("cod_opera", cod_opera);
			json.put("nivel_negociacao", nivel_negociacao);
			
			String accessToken = Token.getToken(conn_url + path_aut, conn_usuario, conn_senha, conn_empresa);
			
			if (accessToken != null && !accessToken.isEmpty()) {
				ret = send(1, "PUT", accessToken, conn_url + path, json.toString());
			}
		}
		catch (Exception e) {
			logger.error(e, "updateNivelNegociacao", new Object[0]);
	    } 
		return ret;
	}
	
	/**
	 * Executes the request
	 * @param action
	 * @param method
	 * @param token
	 * @param url
	 * @param content
	 * 0 POST
	 * 1 PUT
	 * 2 DELETE
	 */
	private String send(int action, String method, String accessToken, String url, String content) {
		logger.info("entering send method={0}, accessToken={1}, url={2}, content={3}", new Object[] {method, accessToken, url, content});
		String ret = "501";
		int responseCode = 501;
		String jsonResult = null;
		URL secondURL = null;
		
		try {
			URL primaryUrl = new URL(url);
			HttpURLConnection urlConnection = (HttpURLConnection) primaryUrl.openConnection();
			urlConnection.setInstanceFollowRedirects(false);
			
			int testCode = urlConnection.getResponseCode();
			
			if (testCode == 307) {
				secondURL = new URL(urlConnection.getHeaderField("Location"));
			} else {
				secondURL = new URL(url);
			}
			
			if (action == 0) { // POST
				HttpPost post = new HttpPost(secondURL.toString());
				post.addHeader("content-type", "application/json; charset=utf-8");
		        post.addHeader("Authorization", "Bearer " + accessToken);
		        post.setEntity(new StringEntity(content));
		        
		        try (CloseableHttpClient httpClient = HttpClients.createDefault();
			        	CloseableHttpResponse response = httpClient.execute(post)) {
		        	
					responseCode = response.getStatusLine().getStatusCode();
	
		        	logger.info("URL: " + post.getURI());
		        	logger.info("Method: " + post.getMethod());
		        	logger.info("Status line: " + response.getStatusLine().toString());
	
					if (response.getEntity() != null) {
				        jsonResult = EntityUtils.toString(response.getEntity());
				        logger.info("Result: " + jsonResult);
					}
		        } 
			} else if (action == 1) { // PUT
				HttpPut post = new HttpPut(secondURL.toString());
				post.addHeader("content-type", "application/json; charset=utf-8");
				post.addHeader("Authorization", "Bearer " + accessToken);
		        post.setEntity(new StringEntity(content));
		        
		        try (CloseableHttpClient httpClient = HttpClients.createDefault();
			        	CloseableHttpResponse response = httpClient.execute(post)) {
		        	
					responseCode = response.getStatusLine().getStatusCode();

					logger.info("URL: " + post.getURI());
					logger.info("Method: " + post.getMethod());
					logger.info("Status line: " + response.getStatusLine().toString());
		        	
					if (response.getEntity() != null) {
				        jsonResult = EntityUtils.toString(response.getEntity());
				        logger.info("Result: " + jsonResult);
					}
		        }
	        } else if (action == 2) { // DELETE
	        	CloseableHttpClient httpclient = HttpClients.createDefault();
		        HttpDeleteWithBody post = new HttpDeleteWithBody(secondURL.toString());
		        
		        post.addHeader("content-type", "application/json; charset=utf-8");
		        post.addHeader("Authorization", "Bearer " + accessToken);
		        post.setEntity(new StringEntity(content));
		        
		        CloseableHttpResponse response = httpclient.execute(post);
		        responseCode = response.getStatusLine().getStatusCode();
		        
		        logger.info("URL: " + post.getURI());
				logger.info("Method: " + post.getMethod());
				logger.info("Status line: " + response.getStatusLine().toString());
	        	
				if (response.getEntity() != null) {
			        jsonResult = EntityUtils.toString(response.getEntity());
			        logger.info("Result: " + jsonResult);
				}
	        }
	        
	        ret = Integer.toString(responseCode);
		}
		catch (Exception e) {
			logger.error(e, "send", new Object[0]);
	    }
		
		return ret;
	}
	
	class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {
	    public static final String METHOD_NAME = "DELETE";

	    public String getMethod() {
	        return METHOD_NAME;
	    }

	    public HttpDeleteWithBody(final String uri) {
	        super();
	        setURI(URI.create(uri));
	    }

	    public HttpDeleteWithBody(final URI uri) {
	        super();
	        setURI(uri);
	    }

	    public HttpDeleteWithBody() {
	        super();
	    }
	}
}
