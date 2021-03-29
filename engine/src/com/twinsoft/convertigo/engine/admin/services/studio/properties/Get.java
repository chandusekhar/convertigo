/*
 * Copyright (c) 2001-2021 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.admin.services.studio.properties;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

@ServiceDefinition(
		name = "Get",
		roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG, Role.PROJECT_DBO_VIEW },
		parameters = {},
		returnValue = ""
	)
public class Get extends XmlService {
	private Map<String, String> getInformationProperties(DatabaseObject dbo) {
		Map<String, String> informationProperties = new HashMap<String, String>();
		informationProperties.put("depth", dbo instanceof ScreenClass ? Integer.toString(((ScreenClass) dbo).getDepth()) : "n/a");
		informationProperties.put("exported", (dbo instanceof Project) ? ((Project) dbo).getInfoForProperty("exported") : "n/a");
		informationProperties.put("java_class", dbo.getClass().getName());

		return informationProperties;
	}

	@Override
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		String qname = request.getParameter("qname");
		DatabaseObject dbo = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
		Element elt = com.twinsoft.convertigo.engine.admin.services.database_objects.Get.getProperties(dbo, document, qname);

		// Add information properties
		Map<String, String> infoProperties = getInformationProperties(dbo);
		for (Map.Entry<String, String> infoProperty : infoProperties.entrySet()) {
			elt.setAttribute(infoProperty.getKey(), infoProperty.getValue());
		}
        elt.setAttribute("isExtractionRule", Boolean.toString(dbo instanceof com.twinsoft.convertigo.beans.core.ExtractionRule));

		document.getDocumentElement().appendChild(elt);
	}
}
