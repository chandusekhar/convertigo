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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.engine.EngineException;

public class DoWhileStatement extends BlockStatement {

	private static final long serialVersionUID = 9066925199710227152L;

	public DoWhileStatement() {
		super(true);
	}
	
	public DoWhileStatement(String condition) {
		super(condition,true);
	}

	public String toString() {
		return "do...while(" + getCondition() + ")";
	}
	
	public String toJsString() {
		String code = "";
		String condition = getCondition();
		if (!condition.equals("")) {
			code += " do {\n";
			code += super.toString();
			code += " \n} while ("+ condition +") ;\n";
		}
		return code;
	}
	
	public boolean execute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnabled()) {
			if (super.execute(javascriptContext, scope)) {
				return executeNextStatement(javascriptContext, scope);
			}
		}
		return false;
	}

	public boolean doLoop(Context javascriptContext, Scriptable scope) throws EngineException {
		evaluate(javascriptContext, scope, getCondition(), "condition",true);
		if (evaluated instanceof Boolean) {
			if (evaluated.equals(Boolean.TRUE)) {
				return execute(javascriptContext, scope);
			}
			return true;
		}
		else {
			EngineException ee = new EngineException(
					"Invalid statement condition.\n" +
					"WhileStatement: \"" + getName()+ "\"");
			throw ee;
		}
	}
	
}
