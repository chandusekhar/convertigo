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

package com.twinsoft.convertigo.engine.studio.events;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.admin.util.DOMUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public abstract class AbstractEvent {

    protected String name;
    protected String data = null;

    protected String qname;
    protected Document document;

    public AbstractEvent(String name, String qname) {
        this.name = name;
        this.qname = qname;

        try {
            document = DOMUtils.createDocument();
        }
        catch (ParserConfigurationException e) {
        }
    }

    public String getName() {
        return name;
    }

    public String getData() {
        if (data == null) {
            try {
                data = XMLUtils.prettyPrintElement(toXml());
            }
            catch (Exception e) {
            }
        }

        return data;
    }

    protected abstract Element toXml() throws Exception;
}
