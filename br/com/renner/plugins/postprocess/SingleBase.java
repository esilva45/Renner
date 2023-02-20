package br.com.renner.plugins.postprocess;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;

import br.com.renner.plugins.CustomConstants;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.identity.vo.Identity;
import oracle.iam.platform.Platform;
import oracle.iam.platform.kernel.spi.ConditionalEventHandler;
import oracle.iam.platform.kernel.spi.PostProcessHandler;
import oracle.iam.platform.kernel.vo.AbstractGenericOrchestration;
import oracle.iam.platform.kernel.vo.BulkEventResult;
import oracle.iam.platform.kernel.vo.BulkOrchestration;
import oracle.iam.platform.kernel.vo.EventResult;
import oracle.iam.platform.kernel.vo.Orchestration;

public class SingleBase implements ConditionalEventHandler, PostProcessHandler {
	private static final ODLLogger logger = ODLLogger.getODLLogger(SingleBase.class.getName());
	private final String VERSION = "v1.0.0";
	private String[] types = {"ESTRANGEIRO","FUNCIONARIO_ESTRANGEIRO","PRESTADOR_SERVICO","ESTAGIARIO","FUNCIONARIO","TEMPORARIO"};
	private boolean isLogin = false, isFirstName = false, isMiddleName = false, isLastName = false, isRG = false, isDoc = false, isCPF = false, isNascimento = false, isEmp = false;
	private int unique_id = 0;
	
	@Override
    public boolean isApplicable(AbstractGenericOrchestration ago) {
		boolean isApplicable = false;
		
		HashMap<String, String> params = ago.getParameters();
		logger.log(ODLLevel.TRACE, "isApplicable params [{0}]", new Object[] { params });
		
		if (ago.getOperation().equals("CREATE")) {
			isLogin = true; isFirstName = true; isMiddleName = true; isLastName = true; isRG = true; isDoc = true; isCPF = true; isNascimento = true; isEmp = true;
			isApplicable = true;
		} else if (ago.getOperation().equals("MODIFY")) {
			isLogin = params.containsKey("User Login") ? true : false;
			isFirstName = params.containsKey("First Name") ? true : false;
			isMiddleName = params.containsKey("Middle Name") ? true : false;
			isLastName = params.containsKey("Last Name") ? true : false;
			isCPF = params.containsKey("cpf") ? true : false;
			isRG = params.containsKey("rg_funcionario") ? true : false;
			isDoc = params.containsKey("doc_estrangeiro") ? true : false;
			isNascimento = params.containsKey("DatadeNascimento") ? true : false;
			isEmp = params.containsKey("Employee Number") ? true : false;
			
			if (isLogin || isFirstName || isMiddleName || isLastName || isRG || isDoc || isCPF || isNascimento || isEmp) {
				isApplicable = true;
			}
		}
		
		logger.log(ODLLevel.TRACE, "isApplicable Login [{0}] FirstName [{1}] MiddleName [{2}] LastName [{3}] CPF [{4}] RG [{5}] Doc [{6}] Nascimento [{7}] EmployeeNumber [{8}]", 
				new Object[] { isLogin, isFirstName, isMiddleName, isLastName, isRG, isDoc, isCPF, isNascimento, isEmp });

		return isApplicable;
	}
	
	@Override
	public EventResult execute(long processId, long eventId, Orchestration orchestration) {
		logger.log(ODLLevel.NOTIFICATION, "Start orchestration processId [{0}] eventId [{1}] operation [{2}]", new Object[] { processId, eventId, orchestration.getOperation() });

		User newUserState = (User) orchestration.getInterEventData().get("NEW_USER_STATE");
		User oldUserState = (User) orchestration.getInterEventData().get("CURRENT_USER");
		
		try {
			String emptype = (String) newUserState.getAttribute(UserManagerConstants.AttributeName.EMPTYPE.getId());
			
			if (ArrayUtils.contains(types, emptype)) {
		        String userKey = orchestration.getTarget().getEntityId();
		        
		        if (orchestration.getOperation().equals("MODIFY")) {
		        	toCheck("MODIFY", oldUserState, newUserState, userKey);
		        } else if (orchestration.getOperation().equals("CREATE")) {
		        	toCheck("CREATE",newUserState, oldUserState, userKey);
		        	modifyUser((String) newUserState.getAttribute(UserManagerConstants.AttributeName.USER_LOGIN.getId()), String.valueOf(unique_id));
		        }
			}
		}
		catch (Exception e) {
			logger.log(ODLLevel.ERROR, "Exception EventResult {0}", new Object[]{ e });
		}
		
		return null;
	}
	
	@Override
	public BulkEventResult execute(long arg0, long arg1, BulkOrchestration bulkOrchestration) {
		try {
			String[] entityIds = bulkOrchestration.getTarget().getAllEntityId();
			HashMap<String, Serializable> interParameters = bulkOrchestration.getInterEventData();
			
			Object newUsersObj = interParameters.get("NEW_USER_STATE");
	        Identity[] newUsersState  = (Identity[]) newUsersObj;
	        
	        Object oldUsersObj = interParameters.get("CURRENT_USER");
	        Identity[] oldUsersState  = (Identity[]) oldUsersObj;
	        
	        for (int i = 0; i < entityIds.length; i++) {
	        	String userKey = entityIds[i];
	        	
	        	User newUserState = (User) newUsersState[i];
	        	User oldUserState = (User) oldUsersState[i];
	        	
	        	String emptype = (String) newUserState.getAttribute(UserManagerConstants.AttributeName.EMPTYPE.getId());

	        	if (ArrayUtils.contains(types, emptype)) {
			        if (bulkOrchestration.getOperation().equals("MODIFY")) {
			        	toCheck("MODIFY", oldUserState, newUserState, userKey);
			        } else if (bulkOrchestration.getOperation().equals("CREATE")) {
			        	toCheck("CREATE", newUserState, oldUserState, userKey);
			        	modifyUser((String) newUserState.getAttribute(UserManagerConstants.AttributeName.USER_LOGIN.getId()), String.valueOf(unique_id));
			        }
				}
	        }
		}
		catch (Exception e) {
			logger.log(ODLLevel.ERROR, "Exception BulkEventResult {0}", new Object[]{ e });
		}
        
        return null;
	}
	
	@Override
	public void initialize(HashMap<String, String> arg0) {
		logger.log(ODLLevel.NOTIFICATION, "initialize {0}", new Object[] { VERSION });
	}

	@Override
	public void compensate(long arg0, long arg1, AbstractGenericOrchestration arg2) {
		logger.log(ODLLevel.NOTIFICATION, "Compensate not implemented");
	}

	@Override
	public boolean cancel(long arg0, long arg1, AbstractGenericOrchestration arg2) {
		logger.log(ODLLevel.NOTIFICATION, "Cancel not implemented");
		return false;
	}

	public void modifyUser(String userId, String value1) {
		UserManager usrMgrOps = Platform.getService(UserManager.class);
		Set<String> resAttrs = new HashSet<String>();
		
		try {
			User searchUser = usrMgrOps.getDetails(userId, resAttrs, true);
			
			HashMap<String, Object> userAttributeValueMap = new HashMap<String, Object>();
			userAttributeValueMap.put("IDUnicoRenner", value1);
			User retrievedUser = searchUser;
			User user = new User(retrievedUser.getEntityId(), userAttributeValueMap);

			usrMgrOps.modify(user);
		}
		catch (Exception e) {
			logger.log(ODLLevel.ERROR, "Exception modifyUser {0}", new Object[]{ e });
		}
	}
	
	private void toCheck(String operation, User userState, User updateState, String userKey) {
		Connection conn = null;
		int ret = 0;
		
		try {
			conn = Platform.getOperationalDS().getConnection();
	        
	        String first = userState != null ? (String) userState.getAttribute(UserManagerConstants.AttributeName.FIRSTNAME.getId()) : null;
	        String last = userState != null ? (String) userState.getAttribute(UserManagerConstants.AttributeName.LASTNAME.getId()) : null;
	        String middle = userState != null ? (String) userState.getAttribute(UserManagerConstants.AttributeName.MIDDLENAME.getId()) : null;
	        String rg = userState != null ? (String) userState.getAttribute(CustomConstants.Field.RG_FUNCIONARIO.getId().replaceAll("[^A-Za-z0-9]", "")) : null;
	        String doc = userState != null ? (String) userState.getAttribute(CustomConstants.Field.DOC_ESTRANGEIRO.getId().replaceAll("[^A-Za-z0-9]", "")) : null;
	        String cpf = userState != null ? (String) userState.getAttribute(CustomConstants.Field.CPF.getId().replaceAll("[^A-Za-z0-9]", "")) : null;
	        Date data = userState != null ? (Date) userState.getAttribute(CustomConstants.Field.DATADENASCIMENTO.getId()) : null;
	        String nome = first + " " + middle + " " + last;
	        
	        String query1 = "SELECT ID FROM SINGLE_BASE WHERE regexp_replace(upper(FIRST_NAME || ' ' || MIDDLE_NAME || ' ' || LAST_NAME),'\\s+',' ') = ? AND DATA_NASCIMENTO = ? AND STATUS = 'Active'";
	        String query2 = "SELECT ID FROM SINGLE_BASE WHERE CPF = ? OR RG = ? OR DOC_ESTRANGEIRO = ? AND STATUS = 'Active'";
	        
	        PreparedStatement ps = conn.prepareStatement(query2);
	        ps.setString(1, cpf);
	        ps.setString(2, rg);
	        ps.setString(3, doc);
	        ResultSet rs = ps.executeQuery();

	        if (rs != null && rs.next()) {
				ret = rs.getInt(1);
			} else {
				ps = conn.prepareStatement(query1);
				ps.setString(1, nome.toUpperCase());
		        ps.setDate(2, data == null ? null : new java.sql.Date(data.getTime()));
		        rs = ps.executeQuery();
		        
		        if (rs != null && rs.next()) {
					ret = rs.getInt(1);
		        }
			}
	        
	        if (ret == 0) {
	        	insertRecord(userState, conn);
	        } else {
	        	unique_id = ret;
	        	
	        	if (operation.equals("CREATE")) {
	        		updateRecord(userState, conn, ret);
	        	} else {
	        		updateRecord(updateState, conn, ret);
	        	}
	        }
	        
	        rs.close();
	        ps.close();
		}
		catch (Exception e) {
			logger.log(ODLLevel.ERROR, "Exception toCheck {0}", new Object[]{ e });
		}
		finally {
			try {
				if (conn != null) {
					conn.close();
				}
			}
			catch(Exception e) {
				logger.log(ODLLevel.ERROR, "Exception toCheck {0}", new Object[]{ e });
			}
		}
    }
	
	private void insertRecord(User userState, Connection conn) {
		int key = 0;
		
		try {
			String nextval = "Select SINGLE_BASE_SEQ.nextval from dual";
			PreparedStatement ps = conn.prepareStatement(nextval);
			
			synchronized(this) {
				ResultSet rs = ps.executeQuery();
				rs.next();
				key = rs.getInt(1);
				rs.close();
				unique_id = key;
			}

			String query = "INSERT INTO SINGLE_BASE (ID,LOGIN,FIRST_NAME,MIDDLE_NAME,LAST_NAME,DATA_NASCIMENTO,CPF,RG,DOC_ESTRANGEIRO,EMP_NO,STATUS,CREATE_DATE) "
					+ "values (?,?,?,?,?,?,?,?,?,?,'Active',sysdate)";
			
			Date data = userState != null ? (Date) userState.getAttribute(CustomConstants.Field.DATADENASCIMENTO.getId()) : null;
			
			ps = conn.prepareStatement(query);
			ps.setInt(1, key);
			ps.setString(2, (String) userState.getAttribute(UserManagerConstants.AttributeName.USER_LOGIN.getId()));
			ps.setString(3, (String) userState.getAttribute(UserManagerConstants.AttributeName.FIRSTNAME.getId()));
			ps.setString(4, (String) userState.getAttribute(UserManagerConstants.AttributeName.MIDDLENAME.getId()));
			ps.setString(5, (String) userState.getAttribute(UserManagerConstants.AttributeName.LASTNAME.getId()));
			ps.setDate(6, data == null ? null : new java.sql.Date(data.getTime()));
			ps.setString(7, (String) userState.getAttribute(CustomConstants.Field.CPF.getId().replaceAll("[^A-Za-z0-9]", "")));
			ps.setString(8, (String) userState.getAttribute(CustomConstants.Field.RG_FUNCIONARIO.getId().replaceAll("[^A-Za-z0-9]", "")));
			ps.setString(9, (String) userState.getAttribute(CustomConstants.Field.DOC_ESTRANGEIRO.getId().replaceAll("[^A-Za-z0-9]", "")));
			ps.setString(10, (String) userState.getAttribute(UserManagerConstants.AttributeName.EMPLOYEE_NUMBER.getId()));
			ps.execute();
			ps.close();
		}
		catch (Exception e) {
			logger.log(ODLLevel.ERROR, "Exception insertRecord {0}", new Object[]{ e });
		}
	}
	
	private void updateRecord(User userState, Connection conn, int id) {
		boolean next = false;
		int i = 1;
		
		try {
			String login = userState != null ? (String) userState.getAttribute(UserManagerConstants.AttributeName.USER_LOGIN.getId()) : null;
			String first = userState != null ? (String) userState.getAttribute(UserManagerConstants.AttributeName.FIRSTNAME.getId()) : null;
			String last = userState != null ? (String) userState.getAttribute(UserManagerConstants.AttributeName.LASTNAME.getId()) : null;
			String middle = userState != null ? (String) userState.getAttribute(UserManagerConstants.AttributeName.MIDDLENAME.getId()) : null;
			String emp = userState != null ? (String) userState.getAttribute(UserManagerConstants.AttributeName.EMPLOYEE_NUMBER.getId()) : null;
			String rg = userState != null ? (String) userState.getAttribute(CustomConstants.Field.RG_FUNCIONARIO.getId().replaceAll("[^A-Za-z0-9]", "")) : null;
			String doc = userState != null ? (String) userState.getAttribute(CustomConstants.Field.DOC_ESTRANGEIRO.getId().replaceAll("[^A-Za-z0-9]", "")) : null;
			String cpf = userState != null ? (String) userState.getAttribute(CustomConstants.Field.CPF.getId().replaceAll("[^A-Za-z0-9]", "")) : null;
			Date data = userState != null ? (Date) userState.getAttribute(CustomConstants.Field.DATADENASCIMENTO.getId()) : null;

			StringBuffer stringbuffer_sql = new StringBuffer("UPDATE SINGLE_BASE SET ");
			
			if (isLogin) {
				next = true;
				stringbuffer_sql.append("LOGIN = ?");
			}
			
			if (isFirstName) {
				if (next) {
					stringbuffer_sql.append(",FIRST_NAME=?");
				} else {
					stringbuffer_sql.append("FIRST_NAME=?");
					next = true;
				}
			}
						
			if (isMiddleName) {
				if (next) {
					stringbuffer_sql.append(",MIDDLE_NAME=?");
				} else {
					stringbuffer_sql.append("MIDDLE_NAME=?");
					next = true;
				}
			}
			
			if (isLastName) {
				if (next) {
					stringbuffer_sql.append(",LAST_NAME=?");
				} else {
					stringbuffer_sql.append("LAST_NAME=?");
					next = true;
				}
			}
			
			if (isNascimento) {
				if (next) {
					stringbuffer_sql.append(",DATA_NASCIMENTO=?");
				} else {
					stringbuffer_sql.append("DATA_NASCIMENTO=?");
					next = true;
				}
			}
			
			if (isCPF) {
				if (next) {
					stringbuffer_sql.append(",CPF=?");
				} else {
					stringbuffer_sql.append("CPF=?");
					next = true;
				}
			}
			
			if (isRG) {
				if (next) {
					stringbuffer_sql.append(",RG=?");
				} else {
					stringbuffer_sql.append("RG=?");
					next = true;
				}
			}
			
			if (isDoc) {
				if (next) {
					stringbuffer_sql.append(",DOC_ESTRANGEIRO=?");
				} else {
					stringbuffer_sql.append("DOC_ESTRANGEIRO=?");
					next = true;
				}
			}
			
			if (isEmp) {
				if (next) {
					stringbuffer_sql.append(",EMP_NO=?");
				} else {
					stringbuffer_sql.append("EMP_NO=?");
				}
			}
			
			stringbuffer_sql.append(", UPDATE_DATE=sysdate WHERE ID = ?");
			
			logger.log(ODLLevel.TRACE, "Update query [{0}]", new Object[] { stringbuffer_sql.toString() });
			
			PreparedStatement ps = conn.prepareStatement(stringbuffer_sql.toString());

			if (isLogin) { ps.setString(i, login); i++; }
			
			if (isFirstName) { ps.setString(i, first); i++; }
						
			if (isMiddleName) { ps.setString(i, middle); i++; }
			
			if (isLastName) { ps.setString(i, last); i++; }
			
			if (isNascimento) { ps.setDate(i, data == null ? null : new java.sql.Date(data.getTime())); i++; }
			
			if (isCPF) { ps.setString(i, cpf); i++; }
			
			if (isRG) { ps.setString(i, rg); i++; }
			
			if (isDoc) { ps.setString(i, doc); i++; }
			
			if (isEmp) { ps.setString(i, emp); i++; }
			
			ps.setInt(i, id);
			
			ps.executeUpdate();
			ps.close();
		}
		catch (Exception e) {
			logger.log(ODLLevel.ERROR, "Exception updateRecord {0}", new Object[]{ e });
		}
	}
}
