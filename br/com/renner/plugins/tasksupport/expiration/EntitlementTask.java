package br.com.renner.plugins.tasksupport.expiration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.platform.Platform;
import oracle.iam.platform.context.DuplicateContextValueException;
import oracle.iam.platform.entitymgr.vo.SearchCriteria;
import oracle.iam.provisioning.api.ProvisioningConstants;
import oracle.iam.provisioning.api.ProvisioningService;
import oracle.iam.provisioning.vo.EntitlementInstance;
import oracle.iam.scheduler.vo.TaskSupport;

public class EntitlementTask extends TaskSupport {
	private static final ODLLogger logger = ODLLogger.getODLLogger(EntitlementTask.class.getName());
	private ProvisioningService provServOps = null;
	
	@SuppressWarnings("rawtypes")
	@Override
	public void execute(HashMap arg0) throws Exception {
		logger.log(ODLLevel.NOTIFICATION, "Entering execute() method");
		Connection conn = null;
		
		try {
			provServOps = Platform.getService(ProvisioningService.class);
			
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
			
			String query = "select enta.usr_key, entl.ent_display_name, (trunc(sysdate) + lkv.lkv_decoded) data_expiracao, enta.OIU_KEY,"
					+ " usr.usr_login, lkv.lkv_decoded, enta.ent_assign_prov_mechanism, enta.ent_assign_key,"
					+ " enta.valid_from_date, enta.valid_to_date"
					+ " from ent_list entl, ent_assign enta, usr usr, lkv lkv, lku lku"
					+ " where entl.ent_list_key = enta.ent_list_key and usr.usr_key = enta.usr_key"
					+ " and lkv.lku_key = lku.lku_key and lkv.lkv_encoded = entl.ent_display_name"
					+ " and lku.lku_type_string_key = '" + lookup + "'"
					+ " and enta.ent_assign_prov_mechanism in (" + sbMechanism + ")"
					+ " and (enta.valid_to_date is null or enta.valid_to_date > (trunc(sysdate) + lkv.lkv_decoded)) and usr.usr_status != 'Deleted'";
	
			conn = Platform.getOperationalDS().getConnection();
	
			PreparedStatement prepStmt = conn.prepareStatement(query);
			ResultSet rs = prepStmt.executeQuery(query);
			
			while(rs.next()) {
				logger.log(ODLLevel.TRACE, "Update Entitlement [{0}] login [{1}] data [{2}]", new Object[] { rs.getString(2), rs.getString(5), rs.getString(3) });
				updateEntitlement(rs.getString(1), rs.getString(2), rs.getDate(3), rs.getString(4));
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
	public void updateEntitlement(String userKey, String entitlementName, Date expiracao, String accountKey) {

		try {
	        SearchCriteria criteria = new SearchCriteria(ProvisioningConstants.EntitlementSearchAttribute.ENTITLEMENT_DISPLAYNAME.getId(), entitlementName, SearchCriteria.Operator.EQUAL);
            HashMap entConfigParams = new HashMap();

            List<EntitlementInstance> userEntitlementInsts = provServOps.getEntitlementsForUser(userKey, criteria, entConfigParams);
            
            EntitlementInstance updateEntInst = userEntitlementInsts.get(0);
            updateEntInst.setValidToDate(expiracao);
            updateEntInst.setAccountKey(Long.parseLong(accountKey)); // ** OIU_KEY
            
            provServOps.updateEntitlement(updateEntInst);
		}
		catch (DuplicateContextValueException e) {
        	logger.log(ODLLevel.ERROR, "Exception DuplicateContextValueException");
		}
        catch (Exception e) {
        	logger.log(ODLLevel.ERROR, "Exception updateEntitlement {0}", new Object[]{ e });
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
