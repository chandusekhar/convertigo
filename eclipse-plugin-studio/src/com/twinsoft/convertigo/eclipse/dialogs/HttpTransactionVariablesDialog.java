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

package com.twinsoft.convertigo.eclipse.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.transactions.AbstractHttpTransaction;
import com.twinsoft.convertigo.beans.variables.RequestableHttpVariable;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.DynamicHttpVariable;

public class HttpTransactionVariablesDialog extends Dialog {
	private AbstractHttpTransaction httpTransaction = null;
	private List<Button> checkboxes = new ArrayList<>(DynamicHttpVariable.values().length);
	
	private int nWidth = 650;
	private int nHeight = 670;
	
	public HttpTransactionVariablesDialog(Shell parentShell, AbstractHttpTransaction httpTransaction) {
		super(parentShell);		
		this.httpTransaction = httpTransaction;
	}
	
	@Override
	public void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Parameters availables");
		newShell.setSize(nWidth, nHeight);

		int nLeft = 0;
		int nTop = 0;

		Display display = newShell.getDisplay();

		Point pt = display.getCursorLocation();
		Monitor[] monitors = display.getMonitors();

		for (int i = 0; i < monitors.length; i++) {
			if (monitors[i].getBounds().contains(pt)) {
				Rectangle rect = monitors[i].getClientArea();

				if (rect.x < 0)
					nLeft = ((rect.width - nWidth) / 2) + rect.x;
				else
					nLeft = (rect.width - nWidth) / 2;

				if (rect.y < 0)
					nTop = ((rect.height - nHeight) / 2) + rect.y;
				else
					nTop = (rect.height - nHeight) / 2;

				break;
			}
		}

		newShell.setBounds(nLeft, nTop, nWidth, nHeight);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		ScrolledComposite scrolledComposite = new ScrolledComposite(parent, SWT.V_SCROLL);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		scrolledComposite.setLayoutData(gd);
		Composite composite = new Composite(scrolledComposite, SWT.NONE);
		scrolledComposite.setContent(composite);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);
		GridLayout gl = new GridLayout(3, false);
		gl.marginLeft = 10;
		gl.marginRight = 10;
		gl.horizontalSpacing = 20;
		composite.setLayout(gl);
		
		Label title = new Label(composite, SWT.NONE);
		title.setText("Add or remove dynamic variables for this '" + httpTransaction.getName() + "'HTTP Transaction.");
		title.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
		
		for (DynamicHttpVariable v: DynamicHttpVariable.values()) {
			if (!v.can(httpTransaction)) {
				continue;
			}
			Button checkBtn = new Button(composite, SWT.CHECK);
			checkboxes.add(checkBtn);
			checkBtn.setSelection(httpTransaction.getVariable(v.name()) != null);
			checkBtn.setData(DynamicHttpVariable.class.getName(), v);
			checkBtn.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
			Label labelName = new Label(composite, SWT.NONE);
			labelName.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
			FontData fontData = labelName.getFont().getFontData()[0];
			Font font = new Font(parent.getDisplay(), new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
			labelName.setFont(font);
			labelName.setText(v.display());
			C8oBrowser browserDescription = new C8oBrowser(composite, SWT.MULTI | SWT.WRAP);
			gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			gd.heightHint = 60;
			browserDescription.setLayoutData(gd);
			browserDescription.setUseExternalBrowser(true);
			if (SwtUtils.isDark()) {
				browserDescription.setBackground(composite.getBackground());
			}
			browserDescription.setText("<html>" +
					"<head>" +
					"<script type=\"text/javascript\">" +
						"document.oncontextmenu = new Function(\"return false\");" +
					"</script>" +
					"<style type=\"text/css\">" +
						  "body {" +
						    "margin: auto;" +
						    "height: 60px;" +
						    "display: table-cell;" +
						    "vertical-align: middle;" +
						    "font-family: Tahoma new, sans-serif;" +
						    "font-size: 0.7em;" +
						    "overflow-y: auto;" +
						    "color: $foreground$;" +
						    "background-color: $background$ } \n" +
						  "a { color: $link$; }" +
					"</style></head><body>" + v.description() + "</body></html>");
		}
		return scrolledComposite;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {	
		/* APPLY ACTION */
		Button buttonApply = createButton(parent, IDialogConstants.PROCEED_ID, "Apply", true);
		buttonApply.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				applyProceed();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		buttonApply.setEnabled(true);
	}
	
	private void applyProceed() {
		for (Button check: checkboxes) {
			DynamicHttpVariable v = (DynamicHttpVariable) check.getData(DynamicHttpVariable.class.getName());
			Variable dboVar = httpTransaction.getVariable(v.name());
			if (check.getSelection() && dboVar == null) {
				try {
					RequestableHttpVariable newDboVar = new RequestableHttpVariable();
					newDboVar.setName(v.value());
					httpTransaction.addVariable(newDboVar);
				} catch (EngineException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (!check.getSelection() && dboVar != null) {
				httpTransaction.removeVariable((RequestableVariable) dboVar);
			}
		}
		
		close();
	}
	
}
