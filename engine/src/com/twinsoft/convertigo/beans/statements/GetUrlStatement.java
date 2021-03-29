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

import org.apache.commons.lang3.StringUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.engine.EngineException;

public class GetUrlStatement extends Statement {

	private static final long serialVersionUID = 5555147220832481093L;

	private String variableName = "//todo";
	
	public GetUrlStatement() {
		super();
	}

    @Override		
	public String toString() {
		return StringUtils.abbreviate(variableName, 20);
	}

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}

    @Override	
	public boolean execute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnabled()) {
			if (super.execute(javascriptContext, scope)) {
				String referer = null;
				
				try {
					HtmlConnector connector = (HtmlConnector)getConnector();
					com.twinsoft.convertigo.engine.Context ctx = getParentTransaction().context;
					referer = connector.getHtmlParser().getReferer(ctx);
				} catch (ClassCastException e) {
					return false;
				}
				
				addToScope(scope, referer);
				return true;
			}
		}
		return false;
	}

	public void addToScope(Scriptable scope, String chaine) {
		if (chaine != null)
			scope.put(this.variableName, scope, chaine);
	}

    @Override	
	public String toJsString() {
		return variableName;
	}

}