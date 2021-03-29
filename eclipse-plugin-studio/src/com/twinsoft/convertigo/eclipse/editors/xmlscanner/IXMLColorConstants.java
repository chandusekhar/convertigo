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

package com.twinsoft.convertigo.eclipse.editors.xmlscanner;

import org.eclipse.swt.graphics.RGB;

public interface IXMLColorConstants {
	RGB XML_COMMENT = new RGB(128, 0, 0);//98,98,98
	RGB PROC_INSTR = new RGB(128, 128, 128);//254,224,146
	RGB STRING = new RGB(0, 128, 0);//23,198,163
	RGB DEFAULT = new RGB(0, 0, 0);//255,255,225
	RGB TAG = new RGB(0, 0, 128);//12,168,216
	RGB ATTRIBUTE = new RGB(0, 0, 180);//135,236,32
}
