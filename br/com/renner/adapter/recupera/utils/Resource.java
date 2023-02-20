package br.com.renner.adapter.recupera.utils;

import java.util.HashMap;

import org.identityconnectors.common.logging.Log;

import Thor.API.tcResultSet;
import Thor.API.Exceptions.tcAPIException;
import Thor.API.Exceptions.tcColumnNotFoundException;
import Thor.API.Exceptions.tcITResourceNotFoundException;
import Thor.API.Operations.tcITResourceInstanceOperationsIntf;
import oracle.iam.platform.Platform;

public class Resource {
	private static Log logger = Log.getLog(Resource.class);
	tcITResourceInstanceOperationsIntf itResourceIntf = (tcITResourceInstanceOperationsIntf)Platform.getService(tcITResourceInstanceOperationsIntf.class);

	public HashMap<String, String> getParametersFromItResource(String itResourceName) throws tcAPIException, tcColumnNotFoundException, tcITResourceNotFoundException {
		logger.info("Entering execute() method Resource");
		HashMap<String, String> itrMap = new HashMap<>();
		HashMap<String, String> itResMap = null;
		itrMap.put("IT Resources.Name", itResourceName);
		tcResultSet rsFindITResource = this.itResourceIntf.findITResourceInstances(itrMap);
		
		if (rsFindITResource != null && !rsFindITResource.isEmpty()) {
			long itResourceKey = rsFindITResource.getLongValue("IT Resources.Key");
			tcResultSet rsGetITResourceParameters = this.itResourceIntf.getITResourceInstanceParameters(itResourceKey);
			int itResCount = rsGetITResourceParameters.getRowCount();
			String type = rsFindITResource.getStringValue("IT Resource Type Definition.Server Type");
			itResMap = new HashMap<>();
			
			for (int i = 0; i < itResCount; i++) {
				rsGetITResourceParameters.goToRow(i);
				String key = rsGetITResourceParameters.getStringValue("IT Resources Type Parameter.Name");
				String value = rsGetITResourceParameters.getStringValue("IT Resources Type Parameter Value.Value");
				itResMap.put(key, value);
			}
			itResMap.put("IT Resource Type Definition.Server Type", type);
		} else {
			throw new tcITResourceNotFoundException("ITResource not found: [" + itResourceName + "]");
		}
		
		return itResMap;
	}
}
