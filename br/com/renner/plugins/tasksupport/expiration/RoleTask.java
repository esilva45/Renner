package br.com.renner.plugins.tasksupport.expiration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.rolemgmt.api.RoleManager;
import oracle.iam.identity.rolemgmt.api.RoleManagerConstants;
import oracle.iam.identity.rolemgmt.vo.Role;
import oracle.iam.platform.Platform;
import oracle.iam.scheduler.vo.TaskSupport;

public class RoleTask extends TaskSupport {
	private static final ODLLogger logger = ODLLogger.getODLLogger(RoleTask.class.getName());
	private RoleManager roleMgrOps = null;
	
	@SuppressWarnings("rawtypes")
	@Override
	public void execute(HashMap arg0) throws Exception {
		logger.log(ODLLevel.NOTIFICATION, "Entering execute() method");
		Connection conn = null;
		
		try {
			roleMgrOps = Platform.getService(RoleManager.class);
			
			String lookup = (String)arg0.get("Lookup");
			String mechanism = (String)arg0.get("Mecanismo");
			
			String[] strMechanism = mechanism.split(",");
			List<String> listMechanism = Arrays.asList(strMechanism);
			StringBuilder sbMechanism = new StringBuilder();
			
			for (String s : listMechanism) {
				sbMechanism.append("'");
				sbMechanism.append(s.trim());
				sbMechanism.append("'");
				sbMechanism.append(",");
			}
			
			sbMechanism.deleteCharAt(sbMechanism.length() - 1);

			String query = "select usg.usr_key, ugp.ugp_display_name, (trunc(sysdate) + lkv.lkv_decoded) data_expiracao,"
					+ " usr.usr_login, lkv.lkv_decoded, usg.usg_prov_mechanism, usg.ugp_key, usg.usg_start_date, usg.usg_end_date"
					+ " from ugp ugp, usg usg, usr usr, lkv lkv, lku lku"
					+ " where ugp.ugp_key = usg.ugp_key and usr.usr_key = usg.usr_key and lkv.lku_key = lku.lku_key and lkv.lkv_encoded = ugp.ugp_display_name"
					+ " and lku.lku_type_string_key = '" + lookup + "'"
					+ " and usg.usg_prov_mechanism in (" + sbMechanism + ")"
					+ " and (usg.usg_end_date is null or usg.usg_end_date > (trunc(sysdate) + lkv.lkv_decoded)) and usr.usr_status != 'Deleted'";
			
			conn = Platform.getOperationalDS().getConnection();
			PreparedStatement prepStmt = conn.prepareStatement(query);
			ResultSet rs = prepStmt.executeQuery(query);
			
			while(rs.next()) {
				logger.log(ODLLevel.TRACE, "Update Role [{0}] login [{1}] data [{2}]", new Object[] { rs.getString(2), rs.getString(4), rs.getString(3) });
				updateRole(rs.getString(1),rs.getString(2), rs.getDate(3));
			}
			
			rs.close();
			prepStmt.close();
		}
		catch (Exception e) {
			logger.log(ODLLevel.ERROR, "Exception execute {0}", new Object[]{ e });
		}
		finally {
			try {
				if (conn != null) {
					conn.close();
				}
			}
			catch(Exception e) {
				logger.log(ODLLevel.ERROR, "Exception execute {0}", new Object[]{ e });
			}
		}
		
		logger.log(ODLLevel.NOTIFICATION, "Exiting execute() method");
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void updateRole(String userKey, String roleName, Date expiracao) {

		try {		
			HashSet retAttrs = new HashSet();
	        retAttrs.add(RoleManagerConstants.RoleAttributeName.KEY.getId());
	        Role role = roleMgrOps.getDetails(RoleManagerConstants.RoleAttributeName.DISPLAY_NAME.getId(), roleName, retAttrs);
	        
	        HashMap attrs = new HashMap();
	        attrs.put(RoleManagerConstants.ROLE_GRANT_END_DATE, expiracao);
	        roleMgrOps.updateRoleGrant(role.getEntityId(), userKey, attrs);
		}
        catch (Exception e) {
        	logger.log(ODLLevel.ERROR, "Exception updateRole {0}", new Object[]{ e });
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
