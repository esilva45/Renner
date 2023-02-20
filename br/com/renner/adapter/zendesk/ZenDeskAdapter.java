package br.com.renner.adapter.zendesk;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.identityconnectors.common.logging.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import oracle.iam.identity.rolemgmt.api.RoleManager;
import oracle.iam.platform.Platform;

public class ZenDeskAdapter {
	private Log logger = Log.getLog(getClass());
	private RoleManager roleMgrOps;
	private String url;
	private String password;
	private String user;
	private String ret = "01";
	private ArrayList<String> roles = new ArrayList<String>();
	
	public ZenDeskAdapter(String url, String password, String user) {
		this.url = url;
		this.password = password;
		this.user = user;
		this.roleMgrOps = Platform.getService(RoleManager.class);
	}
	
	public ZenDeskAdapter() {}
	
	public String execute(String upn, String userKey, String searchFilter, String userType, String endDate) {
		logger.info("Start ZenDeskAdapter UPN [{0}] UserKey [{1}] Filter [{2}] UserType [{3}] endDate [{4}]", new Object[] { upn, userKey, searchFilter, userType, endDate });
		
		try {
			if ((userType.equals("FUNCIONARIO") || userType.equals("ESTAGIARIO")) && !isNullOrEmpty(endDate)) {
				if (getGroupsAD(userKey, searchFilter)) {
					if (send(upn)) {
						for (String i : roles) {
							revokeRoleFromUser(i, userKey);
						}
					}
				} else {
					ret = "02";
				}
			} else if (!userType.equals("FUNCIONARIO") && !userType.equals("ESTAGIARIO")) {
				if (getGroupsAD(userKey, searchFilter)) {
					if (send(upn)) {
						for (String i : roles) {
							revokeRoleFromUser(i, userKey);
						}
					}
				} else {
					ret = "02";
				}
			} else {
				ret = "02";
			}
		}
		catch (Exception e) {
			logger.error(e, "Exception execute ", new Object[0]);
			ret = e.getMessage();
	    }
		
		return ret;
	}
	
	private boolean send(String upn) {
		long id = 0;
		int responseGet = 0;
		boolean sendSuccess = false;
		String user_role = "";
		
		try {
			String url_query = url + "/search.json?query=email:" + upn;
			
			String authStr = user + "/token:" + password;
		    String base64Creds = Base64.getEncoder().encodeToString(authStr.getBytes());

		    HttpGet post = new HttpGet(url_query.toString());
			post.addHeader("content-type", "application/json; charset=utf-8");
	        post.addHeader("Authorization", "Basic " + base64Creds);

	        logger.info("POST: " + post.toString());
	        
	        try (CloseableHttpClient httpClient = HttpClients.createDefault();
		        	CloseableHttpResponse response = httpClient.execute(post)) {
	        	
	        	responseGet = response.getStatusLine().getStatusCode();
	        	logger.info("Status line: " + response.getStatusLine().toString());
	        	
	        	if (responseGet == 200) {
		        	JSONObject jsonOb = new JSONObject(EntityUtils.toString(response.getEntity()));
		        	JSONArray jsonarray = jsonOb.getJSONArray("results");
		        	
		        	for (int i = 0; i < jsonarray.length(); i++) {
			            JSONObject jsonobject = jsonarray.getJSONObject(i);
			            id = jsonobject.getLong("id");
			            user_role = jsonobject.getString("role");
		        	}
	        	} else {
	        		ret = response.getStatusLine().toString();
	        		throw new Exception("HTTP Exception " + response.getStatusLine().toString());
	        	}
	        	
	        	logger.info("ID ZenDesk: " + id);
	        	logger.info("User role ZenDesk: " + user_role);
			}
	        catch (Exception e) {
	        	logger.error(e, "Exception send ", new Object[0]);
	        	ret = e.getMessage();
	        	sendSuccess = false;
	        	throw new Exception("Exception send " + e);
	        }
	        
	        if ((responseGet == 200) && (id > 0)) {
	        	if (!user_role.equals("end-user")) {
			        String url_put = url + "/users/" + id;
			        String content = "{\"user\":{ \"role\":\"end-user\"}}";
			        
			        HttpPut put = new HttpPut(url_put.toString());
			        put.addHeader("content-type", "application/json; charset=utf-8");
			        put.addHeader("Authorization", "Basic " + base64Creds);
			        put.setEntity(new StringEntity(content));
			        
			        logger.info("PUT: " + put.toString());
			        
			        try (CloseableHttpClient httpClient = HttpClients.createDefault();
				        	CloseableHttpResponse response = httpClient.execute(put)) {
		
			        	responseGet = response.getStatusLine().getStatusCode();
			        	logger.info("Status line: " + response.getStatusLine().toString());
			        	
				        if (responseGet != 200) {
			        		ret = response.getStatusLine().toString();
			        		throw new Exception("HTTP Exception " + response.getStatusLine().toString());
			        	}
			        }
			        catch (Exception e) {
			        	logger.error(e, "Exception send ", new Object[0]);
			        	ret = e.getMessage();
			        	sendSuccess = false;
			        	throw new Exception("Exception send " + e);
			        }
			        
			        sendSuccess = true;
			        ret = "00";
		        } else {
		        	sendSuccess = true;
		        	ret = "04";
		        }
	        } else if (id == 0) {
	        	sendSuccess = true;
	        	ret = "03";
	        } else {
	        	ret = "01";
	        }
		}
		catch (Exception e) {
			sendSuccess = false;
			logger.error(e, "Exception send ", new Object[0]);
			ret = e.getMessage();
	    }
		
		return sendSuccess;
	}
	
	private boolean getGroupsAD(String userKey, String searchFilter) {
		Connection conn = null;
		boolean removeRoles = false;
		
		try {
			String query = "SELECT UGP.UGP_KEY "
					+ "FROM UGP "
					+ "INNER JOIN USG ON USG.UGP_KEY = UGP.UGP_KEY "
					+ "WHERE USG.USR_KEY = " + userKey + " AND UPPER(UGP.UGP_NAME) LIKE '%" + searchFilter.toUpperCase() + "%'";

			conn = Platform.getOperationalDS().getConnection();

			PreparedStatement prepStmt = conn.prepareStatement(query);
			ResultSet rs = prepStmt.executeQuery(query);
			
			while(rs.next()) {
				removeRoles = true;
				roles.add(rs.getString(1));
			}
			
			rs.close();
			prepStmt.close();
		}
		catch (Exception e) {
			logger.error(e, "Exception getGroupsAD ", new Object[0]);
			ret = e.getMessage();
			removeRoles = false;
		}
		finally {
			try{
				if (conn != null) {
					conn.close();
				}
			}
			catch(Exception e) {
				logger.error(e, "Exception getGroupsAD ", new Object[0]);
			}
		}
		
		return removeRoles;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void revokeRoleFromUser(String roleKey, String userKey) {
		try {
			logger.info("Revoked role: Role Key = {0}, User Key = {1}", new Object[]{ roleKey, userKey });
			
	        HashSet usrKeys = new HashSet();
	        usrKeys.add(userKey);
	        
	        roleMgrOps.revokeRoleGrant(roleKey, usrKeys);
		}
		catch (Exception e) {
			logger.error(e, "Exception revokeRoleFromUser ", new Object[0]);
			ret = e.getMessage();
		}
    }
	
	private static boolean isNullOrEmpty(String str) {
		if (str != null && !str.isEmpty())
			return false;
		return true;
	}
}
