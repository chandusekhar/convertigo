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

package com.twinsoft.convertigo.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.hibernate.exception.JDBCConnectionException;

import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.billing.BillingException;
import com.twinsoft.convertigo.engine.billing.GoogleAnalyticsTicketManager;
import com.twinsoft.convertigo.engine.billing.HibernateTicketManager;
import com.twinsoft.convertigo.engine.billing.ITicketManager;
import com.twinsoft.convertigo.engine.billing.Ticket;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.events.PropertyChangeEvent;
import com.twinsoft.convertigo.engine.events.PropertyChangeEventListener;

public class BillingManager implements AbstractManager, PropertyChangeEventListener {
	private boolean isDestroying = true;
	
	private String customer_name = ""; 
	
	private Collection<Ticket> tickets;
	
	private Collection<ITicketManager> managers = new ArrayList<ITicketManager>(2);
	
	private Thread consumer;
	
	public BillingManager() throws EngineException {
		
	}
	
	public void init() throws EngineException {
		customer_name = Engine.isCloudMode() ? Engine.cloud_customer_name : (Engine.isStudioMode() ? "CONVERTIGO Studio" : "CONVERTIGO Server");
		Engine.theApp.eventManager.addListener(this, PropertyChangeEventListener.class);
		if (EnginePropertiesManager.getPropertyAsBoolean(PropertyName.ANALYTICS_PERSISTENCE_ENABLED) ||
			EnginePropertiesManager.getPropertyAsBoolean(PropertyName.ANALYTICS_GOOGLE_ENABLED)) {
			isDestroying = false;
			tickets = new LinkedList<Ticket>();
			consumer = new Thread(new Runnable() {
				public void run() {
					while (!isDestroying && consumer == Thread.currentThread()) {
						Ticket ticket = null;
						try {
							synchronized (tickets) {
								if (tickets.isEmpty()) {
									tickets.wait();
								} else {
									Iterator<Ticket> iTicketData = tickets.iterator();
									ticket = iTicketData.next();
									iTicketData.remove();
								}
							}
							if (ticket != null) {
								for (ITicketManager manager: managers) {
									manager.addTicket(ticket);
								}
							}
						} catch (JDBCConnectionException e) {
							Throwable cause = e.getCause();
							Engine.logBillers.info("JDBCConnectionException on ticket insertion" + (cause == null ? "" : " (cause by " + cause.getClass().getSimpleName() + ")") + ": " + ticket);
						} catch (Exception e) {
							Engine.logBillers.error("Something failed in ticket insertion : " + ticket, e);
						}
					}
				}
			});
			
			renewManager(false);
			consumer.setName("BillingManager consumer");
			consumer.setDaemon(true);
			consumer.start();
		}
	}

	public void destroy() throws EngineException {
		Engine.theApp.eventManager.removeListener(this, PropertyChangeEventListener.class);
		
		isDestroying = true;
		tickets = null;
		consumer = null;
		renewManager(true);
	}

	public void insertBilling(Context context) throws EngineException {
		insertBilling(context, null, null);
	}
	
	public synchronized void insertBilling(Context context, Long responseTime, Long score) throws EngineException {
		if (isDestroying) {
			return;
		}
		if (managers.isEmpty()) {
			return;
		}
		if (context == null) {
			return;
		}
		if (context.requestedObject == null) {
			return;
		}
		
		try {
			String username = context.getAuthenticatedUser();
			username = username == null ? (String) context.get("username") : username;
			username = username == null ? context.tasUserName : username;
			username = username == null ? "user" : username;
			
			Ticket ticket = new Ticket();
			ticket.setCreationDate(System.currentTimeMillis());
			ticket.setClientIp(context.remoteAddr);
			ticket.setCustomerName(customer_name);
			ticket.setUserName(username);
			ticket.setProjectName(context.projectName);
			ticket.setConnectorName(context.connectorName == null ? "" : context.connectorName);
			ticket.setConnectorType(context.connector == null ? "" : context.connector.getClass().getSimpleName());
			ticket.setRequestableName(context.requestedObject.getName());
			ticket.setRequestableType(context.requestedObject.getClass().getSimpleName());
			ticket.setResponseTime(responseTime == null ? context.statistics.getLatestDuration(EngineStatistics.GET_DOCUMENT) : responseTime);
			ticket.setScore(score == null ? context.requestedObject.getScore() : score);
			ticket.setSessionID(context.httpSession.getId());
			ticket.setUserAgent(context.userAgent);
			
			String uiid = (String) context.httpSession.getAttribute(Parameter.UIid.name());
			String deviceUUID = (String) context.httpSession.getAttribute(Parameter.DeviceUUID.name());
			if (uiid != null) {
				ticket.setUiID(uiid);
			}
			if (deviceUUID != null) {
				ticket.setDeviceUUID(deviceUUID);
			}
			
			synchronized (tickets) {
				tickets.add(ticket);
				tickets.notify();
			}
		} catch (Exception e) {
			throw new EngineException("Ticket create failed", e);
		}
	}

	private synchronized void renewManager(boolean justDestroy) throws EngineException {
		for (ITicketManager manager: managers) {
			try {
				manager.destroy();
			} catch (BillingException e) {
				throw new EngineException("TicketManager failed to destroy", e);
			}
		}
		managers.clear();
		
		if (!justDestroy) {
			if (EnginePropertiesManager.getPropertyAsBoolean(PropertyName.ANALYTICS_PERSISTENCE_ENABLED)) {
				try {
					managers.add(new HibernateTicketManager(Engine.logBillers));
				} catch (Throwable t) {
					throw new EngineException("TicketManager instanciation failed", t);
				}			
			}
			
			if (EnginePropertiesManager.getPropertyAsBoolean(PropertyName.ANALYTICS_GOOGLE_ENABLED)) {
				try {
					managers.add(new GoogleAnalyticsTicketManager(EnginePropertiesManager.getProperty(PropertyName.ANALYTICS_GOOGLE_ID), Engine.logBillers));
				} catch (Throwable t) {
					throw new EngineException("TicketManager instanciation failed", t);
				}
			}
		}
	}
	
	public void onEvent(PropertyChangeEvent event) {
		PropertyName name = event.getKey();
		switch (name) {
			case ANALYTICS_PERSISTENCE_ENABLED:
			case ANALYTICS_GOOGLE_ENABLED:
				try {
					destroy();
				} catch(EngineException e) {
					Engine.logBillers.error("Error on BillingManager.destroy", e);
				}
				try {
					init();
				} catch(EngineException e) {
					Engine.logBillers.error("Error on BillingManager.init", e);
				}
				break;
			case ANALYTICS_PERSISTENCE_DIALECT:
			case ANALYTICS_PERSISTENCE_JDBC_DRIVER:
			case ANALYTICS_PERSISTENCE_JDBC_PASSWORD:
			case ANALYTICS_PERSISTENCE_JDBC_URL:
			case ANALYTICS_PERSISTENCE_JDBC_USERNAME:
			case ANALYTICS_GOOGLE_ID:
				try {
					renewManager(false);
				} catch (EngineException e) {
					Engine.logBillers.error("Error on BillingManager.renewManager", e);
				}
			default: break;
		}
	}
}