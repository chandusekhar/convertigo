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

package com.twinsoft.convertigo.engine.requesters;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.mozilla.javascript.NativeJavaObject;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EngineStatistics;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;
import com.twinsoft.convertigo.engine.translators.DefaultInternalTranslator;
import com.twinsoft.convertigo.engine.translators.Translator;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class InternalRequester extends GenericRequester {
	
	private HttpServletRequest httpServletRequest;
	
	boolean bStrictMode = false;

    protected String subPath = null;
	
    public InternalRequester(Map<String, Object> request) throws EngineException {
    	this(request, null);
    }
    
    public InternalRequester(Map<String, Object> request, HttpServletRequest httpServletRequest) throws EngineException {
    	String projectName = ((String[]) request.get(Parameter.Project.getName()))[0];
    	bStrictMode = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName).isStrictMode();
    	inputData = request;
    	this.httpServletRequest = httpServletRequest == null ? new InternalHttpServletRequest() : httpServletRequest;
    	
    	if (this.httpServletRequest instanceof InternalHttpServletRequest) {
    		((InternalHttpServletRequest) this.httpServletRequest).setInternalRequester(this);
    	}
    }
    
    public Object processRequest() throws Exception {
    	try {
    		return processRequest(inputData);
    	} finally {
    		Map<String, Object> request = GenericUtils.cast(inputData);
            processRequestEnd(request);
			onFinally(request);
    	}
    }
    
    void processRequestEnd(Map<String, Object> request) {
		request.put("convertigo.cookies", context.getCookieStrings());
		
		String trSessionId = context.getSequenceTransactionSessionId();
		if (trSessionId != null) {
			request.put("sequence.transaction.sessionid", trSessionId);
		}
		
		boolean isNew = true;
		HttpSession session = httpServletRequest.getSession();
		if (session != null) {
			if (SessionAttribute.isNew.has(session)) {
				isNew = false;
			} else {
				SessionAttribute.isNew.set(session, true);
			}
		}
		
		if (context.requireEndOfContext || (isNew && context.isErrorDocument) || context.project == null) {
			request.put("convertigo.requireEndOfContext", Boolean.TRUE);
		}

		if (request.get("convertigo.contentType") == null) { // if
			// contentType
			// set by
			// webclipper
			// servlet
			// (#320)
			request.put("convertigo.contentType", context.contentType);
		}
		
		request.put("convertigo.cacheControl", context.cacheControl);
		request.put("convertigo.context.contextID", context.contextID);
		request.put("convertigo.isErrorDocument", Boolean.valueOf(context.isErrorDocument));
		request.put("convertigo.context.removalRequired", Boolean.valueOf(context.removalRequired()));
		if (request.get("convertigo.charset") == null) {
			request.put("convertigo.charset", "UTF-8");
		}
    	
    }
    
	void onFinally(Map<String, Object> request) {
		// Removes context when finished
		// Note: case of context.requireEndOfContext has been set in scope
		if (getParameterValue(request.get("convertigo.requireEndOfContext")) != null) {
			removeContext();
		}

		// Removes context when finished
		String removeContextParam = getParameterValue(request.get(Parameter.RemoveContext.getName()));
		if (removeContextParam == null) {
			// case of a mother sequence (context is removed by default)
			Boolean removeContextAttr = Boolean.valueOf(getParameterValue(request.get("convertigo.context.removalRequired")));
			if ((removeContextAttr != null) && removeContextAttr.booleanValue()) {
				removeContext();
			}
		} else {
			// other cases (remove context if exist __removeContext
			// or __removeContext=true/false)
			if (!("false".equals(removeContextParam))) {
				removeContext();
			}
		}
	}

	protected void removeContext() {
		if (Engine.isEngineMode()) {
			if (context != null) {
				Engine.logContext.debug("(InternalRequester) End of context " + context.contextID + " required => removing context");
				Engine.theApp.contextManager.remove(context);
			}
		}
	}
	
    public String getName() {
        return "InternalRequester";
    }
    
    protected void initInternalVariables() throws EngineException {
		Map<String, Object> request = GenericUtils.cast(inputData);

		// Find the project name
		projectName = getParameterValue(request.get(Parameter.Project.getName()));
		if (projectName != null) {
			Engine.logContext.debug("(InternalRequester) project name: " + projectName);
		}

		// Find the pool name
		poolName = getParameterValue(request.get(Parameter.Pool.getName()));
		if (poolName != null) {
			Engine.logContext.debug("(InternalRequester) pool name: " + poolName);
		}

		// Find the sequence name
		sequenceName = getParameterValue(request.get(Parameter.Sequence.getName()));
		if (sequenceName != null) {
			Engine.logContext.debug("(InternalRequester) sequence name: " + sequenceName);
		}

		// Find the connector name
		connectorName = getParameterValue(request.get(Parameter.Connector.getName()));
		if (connectorName != null) {
			Engine.logContext.debug("(InternalRequester) connector name: " + connectorName);
		}
    }
    
	public Context getContext() throws Exception {
		Map<String, String[]> request = GenericUtils.cast(inputData);

		String contextName = getContextName();

		initInternalVariables();
		
		String sessionID = getParameterValue(request.get(Parameter.SessionId.getName()));
		if (sessionID == null) {
			sessionID = httpServletRequest.getSession().getId();
		}
		Engine.logContext.debug("(ServletRequester) Requested execution sessionID: " + sessionID);
		
		context = Engine.theApp.contextManager.get(this, contextName, sessionID, poolName, projectName, connectorName, sequenceName);

		if (context.remoteAddr == null) {
			context.remoteAddr = httpServletRequest.getRemoteAddr();
		}
		return context;
	}

	public String getContextName() throws Exception {
		Map<String, String[]> request = GenericUtils.cast(inputData);

		// Find the context name
		String contextName = getParameterValue(request.get(Parameter.Context.getName()));
		
		if (StringUtils.isBlank(contextName)) { 
			contextName = "default";
		} else if (contextName.equals("*")) {
			contextName = "default*";
		}
		
		Engine.logContext.debug("(InternalRequester) Context name: " + contextName);
		return contextName;
	}
	
	@Override
    public void initContext(Context context) throws Exception {
    	if (httpServletRequest != null) {
    		context.setRequest(httpServletRequest);
    	}
    	
    	super.initContext(context);
    	
    	Map<String, Object> request = GenericUtils.cast(inputData);

		// We transform the HTTP post data into XML data.
		Set<String> parameterNames = request.keySet();
		boolean bConnectorGivenByUser = false;

		for (String parameterName : parameterNames) {
			String parameterValue;

			// Handle only convertigo parameters
			if (parameterName.startsWith("__")) {
				Object parameterObjectValue = request.get(parameterName);
				parameterValue = getParameterValue(parameterObjectValue);
				
				handleParameter(context, parameterName, parameterValue);
				
				if (parameterName.equals(Parameter.Connector.getName())) {
					bConnectorGivenByUser = true;
				}
			}
		}

		if (!bConnectorGivenByUser) {
			if (context.project != null && context.project.getName().equals(context.projectName)) {
				String defaultConnectorName = context.project.getDefaultConnector().getName();
				if (!defaultConnectorName.equals(context.connectorName)) {
					context.connectorName = defaultConnectorName;
				}
			}
		}
		
		Engine.logContext.debug("Context initialized!");
	}

	public static String getParameterValue(Object parameterObjectValue) {
		if (parameterObjectValue == null) {
			return null;
		}
		
		if (parameterObjectValue instanceof NativeJavaObject) {
			parameterObjectValue = ((NativeJavaObject) parameterObjectValue).unwrap();
		}
		
		if (parameterObjectValue.getClass().isArray()) {
			String[] parameterValues = (String[]) parameterObjectValue;
			if (parameterValues.length > 0) {
				return parameterValues[0];
			} else {
				return null;
			}
		} else if (parameterObjectValue instanceof Node) {
			Node node = (Node) parameterObjectValue;
			return node instanceof Element ? ((Element) node).getTextContent() : node.getNodeValue();
		} else if (parameterObjectValue instanceof NodeList) {
			NodeList nl = (NodeList) parameterObjectValue;
			if (nl.getLength() > 0) {
				Node node = nl.item(0);
				return node instanceof Element ? ((Element) node).getTextContent() : node.getNodeValue();
			} else {
				return null;
			}
		} else if (parameterObjectValue instanceof XMLVector) {
			XMLVector<Object> parameterValues = GenericUtils.cast(parameterObjectValue);
			if (parameterValues.size() > 0) {
				return getParameterValue(parameterValues.get(0));
			} else {
				return null;
			}
		} else {
			return parameterObjectValue.toString();
		}
	}

	public Translator getTranslator() {
		DefaultInternalTranslator defaultInternalTranslator = new DefaultInternalTranslator();
		defaultInternalTranslator.setStrictMode(bStrictMode);
		return defaultInternalTranslator;
	}

	public void preGetDocument() {
		String contextID = context.contextID;
		Engine.logContext.debug("Context ID: " + contextID);

		context.servletPath = null;
		if (context.userAgent == null) {
			context.userAgent = "Convertigo engine internal requester";
		}
		if (context.remoteAddr == null) {
			context.remoteAddr = "127.0.0.1";
		}
		if (context.remoteHost == null) {
			context.remoteHost = "localhost";
		}
	}

	public void setStyleSheet(Document document) {
		// Do nothing
	}
	
	protected Object addStatisticsAsData(Object result) { 
		return EngineStatistics.addStatisticsAsXML(context, result); 
	} 
	
	protected Object addStatisticsAsText(String stats, Object result) throws UnsupportedEncodingException{ 
		if (result != null) { 
                if (stats == null) stats = context.statistics.printStatistics(); 
                if (result instanceof Document) { 
                        Document document = (Document) result; 
                        Comment comment = document.createComment("\n" + stats); 
                        document.appendChild(comment); 
                } 
                else if (result instanceof byte[]) { 
                        String encodingCharSet = "UTF-8"; 
                        if (context.requestedObject != null) 
                                encodingCharSet = context.requestedObject.getEncodingCharSet(); 
                        String sResult = new String((byte[]) result, encodingCharSet); 
                        sResult += "<!--\n" + stats + "\n-->"; 
                        result = sResult.getBytes(encodingCharSet); 
                } 
        } 
        return result;
	}

	public HttpServletRequest getHttpServletRequest() {
		return httpServletRequest;
	} 
}
