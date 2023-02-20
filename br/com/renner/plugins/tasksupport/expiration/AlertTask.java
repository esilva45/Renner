package br.com.renner.plugins.tasksupport.expiration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.sun.mail.smtp.SMTPAddressFailedException;

import br.com.renner.utils.Resource;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.notification.api.NotificationService;
import oracle.iam.notification.exception.MultipleTemplateException;
import oracle.iam.notification.exception.NotificationManagementException;
import oracle.iam.notification.exception.TemplateNotFoundException;
import oracle.iam.notification.vo.LocalTemplate;
import oracle.iam.notification.vo.NotificationTemplate;
import oracle.iam.platform.Platform;
import oracle.iam.scheduler.vo.TaskSupport;

public class AlertTask extends TaskSupport {
	private static final ODLLogger logger = ODLLogger.getODLLogger(AlertTask.class.getName());
	private final Resource itOps = new Resource();
	private String host, port, template;
	private Long dias = 5L;
	private boolean recorrente = false;
	
	@SuppressWarnings("rawtypes")
	@Override
	public void execute(HashMap arg0) throws Exception {
		logger.log(ODLLevel.NOTIFICATION, "Entering execute() method");
		
		String it_resource = (String)arg0.get("IT Resource");
		HashMap<String, String> adparameter = itOps.getParametersFromItResource(it_resource);
	    host = adparameter.get("Server Name");
		port = adparameter.get("Port");
		
		if (isNullOrEmpty(port)) {
			port = "25";
		}
		
		template = (String)arg0.get("Email Template");
		dias = (Long)arg0.get("Dias");
		recorrente = (Boolean)arg0.get("Recorrente");

		alertEntitlement();
		alertRole();
		
		logger.log(ODLLevel.NOTIFICATION, "Exiting execute() method");
	}
	
	private void alertEntitlement() {
		logger.log(ODLLevel.NOTIFICATION, "Send Entitlement");
		Connection conn = null;
		
		try {
			String query = "SELECT entl.ent_display_name, usr.usr_login, usr.USR_EMAIL, enta.valid_from_date, TO_CHAR(enta.valid_to_date,'DD/MM/YYYY') valid_to_date,"
					+ " trunc(enta.valid_to_date - sysdate) dias"
					+ " FROM ent_list entl, ent_assign enta, usr usr"
					+ " WHERE entl.ent_list_key = enta.ent_list_key and usr.usr_key = enta.usr_key"
					+ " AND trunc(enta.valid_to_date - sysdate) <= " + dias
					+ " AND trunc(enta.valid_to_date - sysdate) >= 1";
			
			conn = Platform.getOperationalDS().getConnection();
			PreparedStatement prepStmt = conn.prepareStatement(query);
			ResultSet rs = prepStmt.executeQuery(query);
			
			while(rs.next()) {
				logger.log(ODLLevel.TRACE, "Entitlement Send to [{0}] login [{1}] data [{2}]", new Object[] { rs.getString(3), rs.getString(1), rs.getString(5) });
				
				if (recorrente) {
					sendNotification(rs.getString(3), rs.getString(1), rs.getString(5));
				} else {
					if (rs.getInt(6) == dias) {
						sendNotification(rs.getString(3), rs.getString(1), rs.getString(5));
					}
				}
			}
		}
		catch (Exception e) {
			logger.log(ODLLevel.ERROR, "Exception alertEntitlement {0}", new Object[]{ e });
		}
		finally {
			try {
				if (conn != null) {
					conn.close();
				}
			}
			catch(Exception e) {
				logger.log(ODLLevel.ERROR, "Exception alertEntitlement {0}", new Object[]{ e });
			}
		}
	}
	
	private void alertRole() {
		logger.log(ODLLevel.NOTIFICATION, "Send Role");
		Connection conn = null;
		
		try {
			String query = "SELECT ugp.ugp_display_name, usr.usr_login, usr.USR_EMAIL, TO_CHAR(usg.usg_end_date,'DD/MM/YYYY') usg_end_date,"
					+ " trunc(usg.usg_end_date - sysdate) dias"
					+ " FROM ugp ugp, usg usg, usr usr"
					+ " WHERE ugp.ugp_key = usg.ugp_key and usr.usr_key = usg.usr_key"
					+ " AND trunc(usg.usg_end_date - sysdate) <= " + dias
					+ " AND trunc(usg.usg_end_date - sysdate) >= 1";
			
			conn = Platform.getOperationalDS().getConnection();
			PreparedStatement prepStmt = conn.prepareStatement(query);
			ResultSet rs = prepStmt.executeQuery(query);
			
			while(rs.next()) {
				logger.log(ODLLevel.TRACE, "Role Send to [{0}] login [{1}] data [{2}]", new Object[] { rs.getString(3), rs.getString(1), rs.getString(4) });
				
				if (recorrente) {
					sendNotification(rs.getString(3), rs.getString(1), rs.getString(4));
				} else {
					if (rs.getInt(5) == dias) {
						sendNotification(rs.getString(3), rs.getString(1), rs.getString(4));
					}
				}
			}
		}
		catch (Exception e) {
			logger.log(ODLLevel.ERROR, "Exception alertRole {0}", new Object[]{ e });
		}
		finally {
			try {
				if (conn != null) {
					conn.close();
				}
			}
			catch(Exception e) {
				logger.log(ODLLevel.ERROR, "Exception alertRole {0}", new Object[]{ e });
			}
		}
	}

	private void sendNotification(String email, String value1, String value2) {
		Session session = null;
		
		try {
			logger.log(ODLLevel.TRACE, "Host [{0}]", new Object[] { host });
			logger.log(ODLLevel.TRACE, "Send User [{0}]", new Object[] { email });
			
			if (!isNullOrEmpty(email)) {
				String from = "giahml@lojasrenner.com.br";
				Properties properties = System.getProperties();
				properties.setProperty("mail.smtp.host", host);
				properties.setProperty("mail.smtp.port", port);
				session = Session.getDefaultInstance(properties);
	
				MimeMessage message = new MimeMessage(session);
				message.setFrom((Address) new InternetAddress(from));
				message.addRecipient(Message.RecipientType.TO, (Address) new InternetAddress(email));
				LocalTemplate msg = findMsgByNotificationTemplate(template);
				String body = msg.getLongmessage();
				String subject = msg.getSubject();
				message.setSubject(subject);
				message.setContent(body.replace("$ValueOne", value1).replace("$ValueTwo", value2), "text/html; charset=utf-8");
				Transport.send((Message) message);
			} else {
				logger.log(ODLLevel.ERROR, "Without email address");
			}
		}
		catch (SMTPAddressFailedException e1) {
			logger.log(ODLLevel.ERROR, "Invalid Address {0}", new Object[]{ e1 });
		}
		catch (AddressException e2) {
			logger.log(ODLLevel.ERROR, "Illegal address {0}", new Object[]{ e2 });
		}
		catch (Exception e3) {
			logger.log(ODLLevel.ERROR, "Exception Send Resource Notification {0}", new Object[]{ e3 });
		}
	}
	
	private LocalTemplate findMsgByNotificationTemplate(String templateName) throws TemplateNotFoundException, MultipleTemplateException, NotificationManagementException {
		LocalTemplate msg = null;
		
		try {
			NotificationService notificationService = (NotificationService) Platform.getService(NotificationService.class);
			NotificationTemplate notificationTemplate = notificationService.lookupTemplate(templateName, null);
			Map<String, LocalTemplate> hashLocalTemplate = new HashMap<>();
			hashLocalTemplate = notificationTemplate.getLocaltemplateCollection();
			
			for (LocalTemplate element : hashLocalTemplate.values()) {
				if (element.getLocale().equals("pt_BR")) {
					msg = element;
					break;
				}
			}
		}
		catch (Exception e) {
			logger.log(ODLLevel.ERROR, "Illegal findMsgByNotificationTemplate {0}", new Object[]{ e });
		}
		
		return msg;
	}
	
	private static boolean isNullOrEmpty(String str) {
		if (str != null && !str.isEmpty())
			return false;
		return true;
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
