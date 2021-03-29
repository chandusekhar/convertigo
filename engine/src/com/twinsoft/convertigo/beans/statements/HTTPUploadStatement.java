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

package com.twinsoft.convertigo.beans.statements;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.HttpMethodType;

public class HTTPUploadStatement extends HTTPStatement {
	private static final long serialVersionUID = 3095426964527946257L;
	
	private String filename =  "\"filename\"";
	private String httpFilename = "";
	
	transient List<Part> parts = new LinkedList<Part>();

	public HTTPUploadStatement() {
		super();
		
		setHttpVerb(HttpMethodType.POST);
	}
	
	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getHttpFilename() {
		return httpFilename;
	}

	public void setHttpFilename(String httpFilename) {
		this.httpFilename = httpFilename;
	}
	
    @Override
	public boolean execute(org.mozilla.javascript.Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnabled()) {
			parts.clear();
			evaluate(javascriptContext, scope, filename, "filename", true);
			
			if (evaluated != null) {
				String filepath = Engine.theApp.filePropertyManager.getFilepathFromProperty(evaluated.toString(), getProject().getName());
				File fileupload = new File(filepath);
				if (!fileupload.exists()) {
					throw new EngineException("(HTTPUploadStatement) The file '" + fileupload.getAbsolutePath() + "' doesn't exist.");
				}
				if (!fileupload.isFile()) {
					throw new EngineException("(HTTPUploadStatement) The file '" + fileupload.getAbsolutePath() + "' isn't a file.");
				}
				
				String sHttpFilename = fileupload.getName();
				if (httpFilename.length() > 0) {
					evaluate(javascriptContext, scope, httpFilename, "httpFilename", true);
					if (evaluated != null) {
						sHttpFilename = evaluated.toString();
					}
				}
				
				try {
					parts.add(new FilePart(sHttpFilename, fileupload));
				} catch (FileNotFoundException e) {
					throw new EngineException("(HTTPUploadStatement) The file is not found.", e);
				}
			} else {
				throw new EngineException("(HTTPUploadStatement) The filename expresion must return the file path in string.");
			}
			
			setHttpVerb(HttpMethodType.POST);
			return super.execute(javascriptContext, scope);
		}
		return false;
	}
	
	@Override
	protected String addVariableToQuery(String methodToAnalyse, String httpVariable, String httpVariableValue, String query, String urlEncodingCharset, boolean firstParam) throws UnsupportedEncodingException {
		if (methodToAnalyse.equalsIgnoreCase("POST")) {
			parts.add(new StringPart(httpVariable, httpVariableValue));
			return "";
		} else {
			return super.addVariableToQuery(methodToAnalyse, httpVariable, httpVariableValue, query, urlEncodingCharset, firstParam);
		}
	}
	
	public void handleUpload(HttpMethod method, Context context) throws EngineException {
		if (method instanceof PostMethod) {
			try {
				PostMethod uploadMethod = (PostMethod) method;
				MultipartRequestEntity mp = new MultipartRequestEntity(parts.toArray(new Part[parts.size()]), uploadMethod.getParams());
				HeaderName.ContentType.setRequestHeader(method, mp.getContentType());
				uploadMethod.setRequestEntity(mp);
			} catch (Exception e) {
				throw new EngineException("(HTTPUploadStatement) failed to handleUpload", e);
			}
		}
	}
}