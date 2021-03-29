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

package com.twinsoft.convertigo.engine.migration;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaImport;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaObjectCollection;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.constants.Constants;
import org.apache.ws.commons.schema.resolver.DefaultURIResolver;
import org.apache.ws.commons.schema.utils.NamespaceMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.common.XmlQName;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.ISimpleTypeAffectation;
import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.beans.core.IVariableContainer;
import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Project.XsdForm;
import com.twinsoft.convertigo.beans.core.Reference;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.mobileplatforms.Android;
import com.twinsoft.convertigo.beans.mobileplatforms.IOs;
import com.twinsoft.convertigo.beans.mobileplatforms.Windows;
import com.twinsoft.convertigo.beans.mobileplatforms.WindowsPhone8;
import com.twinsoft.convertigo.beans.references.ImportXsdSchemaReference;
import com.twinsoft.convertigo.beans.references.ProjectSchemaReference;
import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.beans.steps.XMLActionStep;
import com.twinsoft.convertigo.beans.steps.XMLCopyStep;
import com.twinsoft.convertigo.beans.transactions.SqlTransaction;
import com.twinsoft.convertigo.beans.transactions.XmlHttpTransaction;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.ConvertigoError;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.SchemaMeta;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.SchemaUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaWalker;

public class Migration7_0_0 {

	public static Element migrate(Document document, Element projectNode) throws EngineException {
		try {
			NodeList mobileDevicesNodeList = XMLUtils.findElements(projectNode, "/mobiledevice");
			if (mobileDevicesNodeList != null) {
				MobileApplication mobileApplication = new MobileApplication();
				Element mobileApplicationElement = mobileApplication.toXml(document);
				projectNode.appendChild(mobileApplicationElement);
				
				Node[] mobileDeviceNodes = XMLUtils.toNodeArray(mobileDevicesNodeList);
				boolean hasAndroid = false;
				boolean hasIOs = false;
				
				for (Node mobileDeviceNode : mobileDeviceNodes) {
					Element mobileDevice = (Element) mobileDeviceNode;
					String classname = mobileDevice.getAttribute("classname");
					
					if (classname == null) {
						// may never arrived
					} else if (classname.equals("com.twinsoft.convertigo.beans.mobiledevices.Android")) {
						hasAndroid = true;
					} else if (classname.startsWith("com.twinsoft.convertigo.beans.mobiledevices.IP")) {
						hasIOs = true;
					}
					
					projectNode.removeChild(mobileDevice);
				}
				
				if (hasAndroid) {
					mobileApplicationElement.appendChild(new Android().toXml(document));
				}
				if (hasIOs) {
					mobileApplicationElement.appendChild(new IOs().toXml(document));
				}
				if (hasAndroid && hasIOs) {
					mobileApplicationElement.appendChild(new WindowsPhone8().toXml(document));
					mobileApplicationElement.appendChild(new Windows().toXml(document));
				}
				
				String projectName = "" + XMLUtils.findPropertyValue(projectNode, "name");
				File mobileFolder = new File(Engine.projectDir(projectName) + "/DisplayObjects/mobile");
				if (mobileFolder.exists()) {
					FileUtils.write(
						new File(mobileFolder, "mobile_project_migrated.txt"),
						"Your mobile project has been migrated.\n" +
						"Now, we make per platform configuration and resources.\n" +
						"You may customize your config.xml (the existing one will never used) and dispatch your existing specific resources per platform (see the 'platforms' folder).\n" +
						"You can delete this file after reading it.",
						"UTF-8"
					);
				}
			}
    	}
    	catch (Exception e) {
    		throw new EngineException("[Migration 7.0.0] Unable to migrate project",e);
    	}
		
		return projectNode;
	}

	public static void migrate(final String projectName) {
		try {
			Map<String, Reference> referenceMap = new HashMap<String, Reference>();
			XmlSchema projectSchema = null;

			Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName, false);
			
			// Copy all xsd files to project's xsd directory
			File destDir = new File(project.getXsdDirPath());
			copyXsdOfProject(projectName, destDir);
			
			String projectWsdlFilePath = Engine.PROJECTS_PATH + "/" + projectName + "/"+ projectName + ".wsdl";
			File wsdlFile = new File(projectWsdlFilePath);
			
			String projectXsdFilePath = Engine.PROJECTS_PATH + "/" + projectName + "/"+ projectName + ".xsd";
			File xsdFile = new File(projectXsdFilePath);
			
			if (xsdFile.exists()) {
				// Load project schema from old XSD file
				XmlSchemaCollection collection = new XmlSchemaCollection();
				collection.setSchemaResolver(new DefaultURIResolver() {
					public InputSource resolveEntity(String targetNamespace, String schemaLocation, String baseUri) {
						// Case of a c8o project location
						if (schemaLocation.startsWith("../") && schemaLocation.endsWith(".xsd")) {
							try {
								String targetProjectName = schemaLocation.substring(3, schemaLocation.indexOf("/",3));
								File pDir = new File(Engine.projectDir(targetProjectName));
								if (pDir.exists()) {
									File pFile = new File(Engine.PROJECTS_PATH + schemaLocation.substring(2));
									// Case c8o project is already migrated
									if (!pFile.exists()) {
										Document doc = Engine.theApp.schemaManager.getSchemaForProject(targetProjectName).getSchemaDocument();
										DOMSource source = new DOMSource(doc);
										StringWriter writer = new StringWriter();
										StreamResult result = new StreamResult(writer);
										TransformerFactory.newInstance().newTransformer().transform(source, result);
										StringReader reader = new StringReader(writer.toString());
										return new InputSource(reader);
									}
								}
								return null;
							} catch (Exception e) {
								Engine.logDatabaseObjectManager.warn("[Migration 7.0.0] Unable to find schema location \""+schemaLocation+"\"",e);
								return null;
							}
						}
						else if (schemaLocation.indexOf("://") == -1 && schemaLocation.endsWith(".xsd")) {
							return super.resolveEntity(targetNamespace, schemaLocation, Engine.PROJECTS_PATH + "/" + projectName);
						}
						return super.resolveEntity(targetNamespace, schemaLocation, baseUri);
					}
					
				});
				projectSchema = SchemaUtils.loadSchema(new File(projectXsdFilePath), collection);
				ConvertigoError.updateXmlSchemaObjects(projectSchema);
				SchemaMeta.setCollection(projectSchema, collection);
				
				for (Connector connector: project.getConnectorsList()) {
					for (Transaction transaction: connector.getTransactionsList()) {
						try {
							// Migrate transaction in case of a Web Service consumption project
							if (transaction instanceof XmlHttpTransaction) {
								XmlHttpTransaction xmlHttpTransaction = (XmlHttpTransaction)transaction;
								String reqn = xmlHttpTransaction.getResponseElementQName();
								if (!reqn.equals("")) {
									boolean useRef = reqn.indexOf(";") == -1;
									// Doc/Literal case
									if (useRef) {
										try {
											String[] qn = reqn.split(":");
											QName refName = new QName(projectSchema.getNamespaceContext().getNamespaceURI(qn[0]), qn[1]);
											xmlHttpTransaction.setXmlElementRefAffectation(new XmlQName(refName));
										}
										catch (Exception e) {}
									}
									// RPC case
									else {
							    		int index, index2;
							    		try {
								    		index = reqn.indexOf(";");
							    			String opName = reqn.substring(0, index);
							    			if ((index2 = reqn.indexOf(";", index+1)) != -1) {
							        			String eltName = reqn.substring(index+1,index2);
							        			String eltType = reqn.substring(index2+1);
							        			String[] qn = eltType.split(":");
							        			
							        			QName typeName = new QName(projectSchema.getNamespaceContext().getNamespaceURI(qn[0]), qn[1]);
							        			String responseElementQName = opName + ";" + eltName +";" + "{"+typeName.getNamespaceURI()+"}"+typeName.getLocalPart();
							        			xmlHttpTransaction.setResponseElementQName(responseElementQName);
							    			}
							    		}
							    		catch (Exception e) {}
									}
								}
							}
							
							// Retrieve required XmlSchemaObjects for transaction
							QName requestQName = new QName(project.getTargetNamespace(), transaction.getXsdRequestElementName());
							QName responseQName = new QName(project.getTargetNamespace(), transaction.getXsdResponseElementName());
							LinkedHashMap<QName, XmlSchemaObject> map = new LinkedHashMap<QName, XmlSchemaObject>();
							XmlSchemaWalker dw = XmlSchemaWalker.newDependencyWalker(map, true, false);
							dw.walkByElementRef(projectSchema, requestQName);
							dw.walkByElementRef(projectSchema, responseQName);
							
							// Create transaction schema
							String targetNamespace = projectSchema.getTargetNamespace();
							String prefix = projectSchema.getNamespaceContext().getPrefix(targetNamespace);
							XmlSchema transactionSchema = SchemaUtils.createSchema(prefix, targetNamespace, XsdForm.unqualified.name(), XsdForm.unqualified.name());
							
							// Add required prefix declarations
							List<String> nsList = new LinkedList<String>();
							for (QName qname: map.keySet()) {
								String nsURI = qname.getNamespaceURI();
								if (!nsURI.equals(Constants.URI_2001_SCHEMA_XSD)) {
									if (!nsList.contains(nsURI)) {
										nsList.add(nsURI);
									}
								}
								String nsPrefix = qname.getPrefix();
								if (!nsURI.equals(targetNamespace)) {
									NamespaceMap nsMap = SchemaUtils.getNamespaceMap(transactionSchema);
									if (nsMap.getNamespaceURI(nsPrefix) == null) {
										nsMap.add(nsPrefix, nsURI);
										transactionSchema.setNamespaceContext(nsMap);
									}
								}
							}
							
							// Add required imports
							for (String namespaceURI: nsList) {
								XmlSchemaObjectCollection includes = projectSchema.getIncludes();
								for (int i=0; i < includes.getCount(); i++) {
									XmlSchemaObject xmlSchemaObject = includes.getItem(i);
									if (xmlSchemaObject instanceof XmlSchemaImport) {
										if (((XmlSchemaImport) xmlSchemaObject).getNamespace().equals(namespaceURI)) {
											
											// do not allow import with same ns !
											if (namespaceURI.equals(project.getTargetNamespace()))
												continue;
											
											String location = ((XmlSchemaImport) xmlSchemaObject).getSchemaLocation();
											
											// This is a convertigo project reference
											if (location.startsWith("../")) {
												// Copy all xsd files to xsd directory
												String targetProjectName = location.substring(3, location.indexOf("/",3));
												copyXsdOfProject(targetProjectName, destDir);
											}
											
											// Add reference
											addReferenceToMap(referenceMap, namespaceURI, location);
											
											// Add import
											addImport(transactionSchema, namespaceURI, location);
										}
									}
								}
							}
							
							QName responseTypeQName = new QName(project.getTargetNamespace(), transaction.getXsdResponseTypeName());
							
							// Add required schema objects
							for (QName qname: map.keySet()) {
								if (qname.getNamespaceURI().equals(targetNamespace)) {
									XmlSchemaObject ob = map.get(qname);
									
									if (qname.getLocalPart().startsWith("ConvertigoError"))
										continue;
									transactionSchema.getItems().add(ob);
									
									// Add missing response error element and attributes
									if (qname.equals(responseTypeQName)) {
										Transaction.addSchemaResponseObjects(transactionSchema, (XmlSchemaComplexType) ob);
									}
								}
							}
							
							// Add missing ResponseType (with document)
							if (map.containsKey(responseTypeQName)) {
								Transaction.addSchemaResponseType(transactionSchema, transaction);
							}
							
							// Add everything
							if (map.isEmpty()) {
								Transaction.addSchemaObjects(transactionSchema, transaction);
							}
							
							// Add c8o error objects (for internal xsd edition only)
							ConvertigoError.updateXmlSchemaObjects(transactionSchema);
							
							// Save schema to file
							String transactionXsdFilePath = transaction.getSchemaFilePath();
							new File(transaction.getSchemaFileDirPath()).mkdirs();
							SchemaUtils.saveSchema(transactionXsdFilePath, transactionSchema);
						}
						catch (Exception e) {
							Engine.logDatabaseObjectManager.error("[Migration 7.0.0] An error occured while migrating transaction \""+transaction.getName()+"\"",e);
						}
						
						if (transaction instanceof TransactionWithVariables) {
							TransactionWithVariables transactionVars = (TransactionWithVariables) transaction;
							handleRequestableVariable(transactionVars.getVariablesList());
							
							// Change SQLQuery variables : i.e. {id} --> {{id}}
							if (transaction instanceof SqlTransaction){
								String sqlQuery = ((SqlTransaction) transaction).getSqlQuery();
								
								sqlQuery = sqlQuery.replaceAll("\\{([a-zA-Z0-9_]+)\\}", "{{$1}}");
								((SqlTransaction) transaction).setSqlQuery(sqlQuery);
							}
							
						}
					}
				}
			}
			else {// Should only happen for projects which version <= 4.6.0 
				XmlSchemaCollection collection = new XmlSchemaCollection();
				String prefix = project.getName()+"_ns";
				projectSchema = SchemaUtils.createSchema(prefix, project.getNamespaceUri(), XsdForm.unqualified.name(), XsdForm.unqualified.name());
				ConvertigoError.addXmlSchemaObjects(projectSchema);
				SchemaMeta.setCollection(projectSchema, collection);
				
				for (Connector connector: project.getConnectorsList()) {
					for (Transaction transaction: connector.getTransactionsList()) {
						if (transaction instanceof TransactionWithVariables) {
							TransactionWithVariables transactionVars = (TransactionWithVariables) transaction;
							handleRequestableVariable(transactionVars.getVariablesList());
						}
					}
				}
			}
			
			// Handle sequence objects
			for (Sequence sequence: project.getSequencesList()) {
				handleSteps(projectSchema,referenceMap, sequence.getSteps());
				handleRequestableVariable(sequence.getVariablesList());
			}
			
			// Add all references to project
			if (!referenceMap.isEmpty()) {
				for (Reference reference: referenceMap.values())
					project.add(reference);
			}
			
			// Delete XSD file
			if (xsdFile.exists())
				xsdFile.delete();
			
			// Delete WSDL file
			if (wsdlFile.exists())
				wsdlFile.delete();
			
		}
		catch (Exception e) {
			Engine.logDatabaseObjectManager.error("[Migration 7.0.0] An error occured while migrating project \""+projectName+"\"",e);
		}
	}
	
	private static void copyXsdOfProject(String projectName, File destDir) throws IOException {
		File srcDir = new File(Engine.projectDir(projectName));
		if (srcDir.exists()) {
			Collection<File> xsdFiles = GenericUtils.cast(FileUtils.listFiles(srcDir, new String[] { "xsd" }, false));
			for (File file: xsdFiles) {
				FileUtils.copyFileToDirectory(file, destDir);
			}
		}
	}
	
	private static void addImport(XmlSchema transactionSchema, String namespaceURI, String location) {
		XmlSchemaImport xmlSchemaImport = new XmlSchemaImport();
		// c8o project reference
		if (location.startsWith("../")) {
			// Should not happen 
		}
		// other reference
		else
			xmlSchemaImport.setSchemaLocation("../../" + location);
		xmlSchemaImport.setNamespace(namespaceURI);
		transactionSchema.getIncludes().add(xmlSchemaImport);
		transactionSchema.getItems().add(xmlSchemaImport);
	}
	
	private static void addReferenceToMap(Map<String, Reference> referenceMap, String namespaceURI, String location) throws EngineException {
		if (!referenceMap.containsKey(namespaceURI)) {
			// c8o project reference
			if (location.startsWith("../")) {
				String targetProjectName = location.substring(3, location.indexOf("/",3));
				ProjectSchemaReference reference = new ProjectSchemaReference();
				reference.setProjectName(targetProjectName);
				reference.setName(targetProjectName + "_reference");
				referenceMap.put(namespaceURI, reference);
			}
			// other reference
			else {
				ImportXsdSchemaReference reference = new ImportXsdSchemaReference();
				reference.setName(StringUtils.normalize(location));
				reference.setFilepath(".//xsd/"+location);
				referenceMap.put(namespaceURI, reference);
			}
		}
	}
	
	private static void handleRequestableVariable(List<RequestableVariable> variables) {
		for (RequestableVariable variable : variables) {
			QName qName = XmlSchemaUtils.getSchemaDataTypeName(variable.getSchemaType());
			variable.setXmlTypeAffectation(new XmlQName(qName));
		}
	}
	
	private static void handleSourceDefinition(XMLVector<String> definition) {
		if (!definition.isEmpty()) {
			String xpath = definition.get(1);
			definition.set(1, xpath = xpath.replaceAll("\\./sequence|\\./transaction", "."));
		}
	}
	
	private static void handleSteps(XmlSchema projectSchema, Map<String, Reference> referenceMap, List<Step> stepList) {
		for (Step step: stepList) {
			if (step instanceof XMLActionStep) {
				XMLVector<XMLVector<Object>> sourcesDefinition = ((XMLActionStep) step).getSourcesDefinition();
				for (XMLVector<Object> row : sourcesDefinition) {
					if (row.size() > 1) {
						XMLVector<String> definition = GenericUtils.cast(row.get(1));
						handleSourceDefinition(definition);
					}
				}
			}
			
			if (step instanceof TransactionStep) {
				XMLVector<String> definition = ((TransactionStep)step).getConnectionStringDefinition();
				handleSourceDefinition(definition);
			}
			
			if (step instanceof IStepSourceContainer) {
				/** Case step's xpath has not been migrated when project has been deployed
				 ** on a 5.0 server from a Studio with an older version **/
				IStepSourceContainer stepSourceContainer = (IStepSourceContainer) step;
				XMLVector<String> definition = stepSourceContainer.getSourceDefinition();
				handleSourceDefinition(definition);
			}
			
			if (step instanceof IVariableContainer) {
				IVariableContainer variableContainer = (IVariableContainer) step;
				for (Variable variable : variableContainer.getVariables()) {
					if (variable instanceof IStepSourceContainer) {
						IStepSourceContainer stepSourceContainer = (IStepSourceContainer) variable;
						XMLVector<String> definition = stepSourceContainer.getSourceDefinition();
						handleSourceDefinition(definition);
					}
				}
			}
			
			String targetProjectName = null;
			String typeLocalName = null;
			if (step instanceof TransactionStep) {
				targetProjectName = ((TransactionStep)step).getProjectName();
				typeLocalName = ((TransactionStep)step).getConnectorName()+ "__"	+
							((TransactionStep)step).getTransactionName() 	+
							"ResponseType";
			}
			else if (step instanceof SequenceStep) {
				targetProjectName = ((SequenceStep)step).getProjectName();
				typeLocalName = ((SequenceStep)step).getSequenceName() +
							"ResponseType";
			}
			
			String namespaceURI = null;
			
			// Case of Requestable steps
			if (targetProjectName != null) {
				try {
					namespaceURI = Project.CONVERTIGO_PROJECTS_NAMESPACEURI + targetProjectName;
					if (!targetProjectName.equals(step.getProject().getName())) {
						try {
							namespaceURI = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(targetProjectName).getTargetNamespace();
						}
						catch (Exception e) {}
						
						// Add reference
						String location = "../"+ targetProjectName +"/"+ targetProjectName + ".xsd";
						addReferenceToMap(referenceMap, namespaceURI, location);
					}
						
					// Set step's typeQName
					step.setXmlComplexTypeAffectation(new XmlQName(new QName(namespaceURI,typeLocalName)));
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			// Other steps
			else {
				try {
					String targetNamespace = projectSchema.getTargetNamespace();
					String targetPrefix = projectSchema.getNamespaceContext().getPrefix(targetNamespace);
					
					String s = null;
					try {
						if (step instanceof XMLCopyStep) {
							XmlSchemaCollection collection = SchemaMeta.getCollection(projectSchema);
							XmlSchemaObject ob = step.getXmlSchemaObject(collection, projectSchema);
							if (ob != null) {
								if (ob instanceof XmlSchemaSequence) {
									ob = ((XmlSchemaSequence)ob).getItems().getItem(0);
								}
								if (ob instanceof XmlSchemaElement || ob instanceof XmlSchemaAttribute) {
									QName schemaTypeName = ob instanceof XmlSchemaElement ? ((XmlSchemaElement)ob).getSchemaTypeName():((XmlSchemaAttribute)ob).getSchemaTypeName();
									String schemaTypePrefix = projectSchema.getNamespaceContext().getPrefix(schemaTypeName.getNamespaceURI());
									String schemaTypeLocalName = schemaTypeName.getLocalPart();
									s = schemaTypePrefix + ":" + schemaTypeLocalName;
								}
							}
						}
						else {
							String schemaType = step.getSchemaDataType();
							s = schemaType.equals("")?"xsd:string":schemaType;
						}
					} catch (Exception e) {
						s = "xsd:string";
					}
					
					if ((s != null) && (!s.equals("")) && (!s.startsWith("xsd:"))) {
						String prefix = s.split(":")[0];
						typeLocalName = s.split(":")[1];
						if (prefix.equals(targetPrefix)) {
							// ignore
						}
						else {
							// Retrieve namespace uri
							namespaceURI = projectSchema.getNamespaceContext().getNamespaceURI(prefix);
							
							// Set step's typeQName
							QName qname = new QName(namespaceURI,typeLocalName);
							XmlSchemaType schemaType = projectSchema.getTypeByName(qname);
							if (schemaType instanceof XmlSchemaComplexType)
								step.setXmlComplexTypeAffectation(new XmlQName(qname));
							if (schemaType instanceof XmlSchemaSimpleType)
								step.setXmlSimpleTypeAffectation(new XmlQName(qname));
						}
					}
				} catch (Exception e) {
					
				}
			}
			
			if (step instanceof ISimpleTypeAffectation) {
				QName qName = XmlSchemaUtils.getSchemaDataTypeName(step.getSchemaDataType());
				step.setXmlSimpleTypeAffectation(new XmlQName(qName));
			}
			
			if (step instanceof StepWithExpressions) {
				handleSteps(projectSchema,referenceMap, ((StepWithExpressions)step).getSteps());
			}
		}
	}
}
