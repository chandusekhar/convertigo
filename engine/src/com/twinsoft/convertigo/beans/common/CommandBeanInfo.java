/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

package com.twinsoft.convertigo.beans.common;

import java.beans.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;

public class CommandBeanInfo extends MySimpleBeanInfo {
    
    private static final int PROPERTY_caseDependency = 0;
    private static final int PROPERTY_keywordTable = 1;

    public CommandBeanInfo() {
		try {
			beanClass =  Command.class;
			additionalBeanClass = com.twinsoft.convertigo.beans.extractionrules.JavelinMashupEventExtractionRule.class;

			iconNameC16 = "/com/twinsoft/convertigo/beans/common/images/command_color_16x16.png";
			iconNameC32 = "/com/twinsoft/convertigo/beans/common/images/command_color_32x32.png";

			resourceBundle = getResourceBundle("res/Command");
			
			displayName = getExternalizedString("display_name");
			shortDescription = getExternalizedString("short_description");

			properties = new PropertyDescriptor[2];
			
            properties[PROPERTY_caseDependency] = new PropertyDescriptor ( "caseDependency", Command.class, "isCaseDependency", "setCaseDependency" );
            properties[PROPERTY_caseDependency].setDisplayName ( getExternalizedString("property.caseDependency.display_name") );
            properties[PROPERTY_caseDependency].setShortDescription ( getExternalizedString("property.caseDependency.short_description") );

            properties[PROPERTY_keywordTable] = new PropertyDescriptor ( "keywordTable", Command.class, "getKeywordTable", "setKeywordTable" );
            properties[PROPERTY_keywordTable].setDisplayName ( getExternalizedString("property.keywordTable.display_name") );
            properties[PROPERTY_keywordTable].setShortDescription ( getExternalizedString("property.keywordTable.short_description") );
            properties[PROPERTY_keywordTable].setPropertyEditorClass (getEditorClass("KeywordEditor"));
		}
		catch(Exception e) {
			com.twinsoft.convertigo.engine.Engine.logBeans.error("Exception with bean info; beanClass=" + beanClass.toString(), e);
		}
	}

}
