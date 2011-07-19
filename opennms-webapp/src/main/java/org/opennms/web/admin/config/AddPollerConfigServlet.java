/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2006-2011 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2011 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.web.admin.config;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.ValidationException;
import org.opennms.core.utils.ConfigFileConstants;
import org.opennms.core.utils.LogUtils;
import org.opennms.netmgt.config.CapsdConfig;
import org.opennms.netmgt.config.CapsdConfigFactory;
import org.opennms.netmgt.config.PollerConfig;
import org.opennms.netmgt.config.PollerConfigFactory;
import org.opennms.netmgt.config.capsd.CapsdConfiguration;
import org.opennms.netmgt.config.capsd.ProtocolPlugin;
import org.opennms.netmgt.config.poller.Monitor;
import org.opennms.netmgt.config.poller.Package;
import org.opennms.netmgt.config.poller.PollerConfiguration;
import org.opennms.netmgt.config.poller.Service;

/**
 * A servlet that handles managing or unmanaging interfaces and services on a
 * node
 *
 * @author <A HREF="mailto:jacinta@opennms.org">Jacinta Remedios </A>
 * @author <A HREF="http://www.opennms.org/">OpenNMS </A>
 * @author <A HREF="mailto:jacinta@opennms.org">Jacinta Remedios </A>
 * @author <A HREF="http://www.opennms.org/">OpenNMS </A>
 * @version $Id: $
 * @since 1.8.1
 */
public class AddPollerConfigServlet extends HttpServlet {
    private static final long serialVersionUID = -3318323869363206074L;

    PollerConfiguration pollerConfig = null;

    CapsdConfiguration capsdConfig = null;

    protected String redirectSuccess;

    HashMap<String, Service> pollerServices = new HashMap<String, Service>();

    HashMap<String, ProtocolPlugin> capsdProtocols = new HashMap<String, ProtocolPlugin>();

    java.util.List<ProtocolPlugin> capsdColl = new ArrayList<ProtocolPlugin>();

    org.opennms.netmgt.config.poller.Package pkg = null;

    Collection<ProtocolPlugin> pluginColl = null;

    Properties props = new Properties();

    PollerConfig pollerFactory = null;

    CapsdConfig capsdFactory = null;

    boolean errorflag = false;

    /**
     * <p>init</p>
     *
     * @throws javax.servlet.ServletException if any.
     */
    public void init() throws ServletException {
        ServletConfig config = this.getServletConfig();
        try {
            props.load(new FileInputStream(ConfigFileConstants.getFile(ConfigFileConstants.POLLER_CONF_FILE_NAME)));
            PollerConfigFactory.init();
            pollerFactory = PollerConfigFactory.getInstance();
            pollerConfig = pollerFactory.getConfiguration();

            if (pollerConfig == null) {
                // response.sendRedirect( "error.jsp?error=2");
                errorflag = true;
                throw new ServletException("Poller Configuration file is empty");
            }
            CapsdConfigFactory.init();
            capsdFactory = CapsdConfigFactory.getInstance();
            capsdConfig = capsdFactory.getConfiguration();

            if (capsdConfig == null) {
                // response.sendRedirect( "error.jsp?error=3");
                errorflag = true;
                throw new ServletException("Capsd Configuration file is empty");
            }
        } catch (Throwable e) {
            throw new ServletException(e.getMessage());
        }
        initPollerServices();
        initCapsdProtocols();
        this.redirectSuccess = config.getInitParameter("redirect.success");
        if (this.redirectSuccess == null) {
            throw new ServletException("Missing required init parameter: redirect.success");
        }
    }

    /**
     * <p>reloadFiles</p>
     *
     * @throws javax.servlet.ServletException if any.
     */
    public void reloadFiles() throws ServletException {
        ServletConfig config = this.getServletConfig();
        try {
            props.load(new FileInputStream(ConfigFileConstants.getFile(ConfigFileConstants.POLLER_CONF_FILE_NAME)));
            PollerConfigFactory.init();
            pollerFactory = PollerConfigFactory.getInstance();
            pollerConfig = pollerFactory.getConfiguration();

            if (pollerConfig == null) {
                // response.sendRedirect( "error.jsp?error=2");
                errorflag = true;
                throw new ServletException("Poller Configuration file is empty");
            }
            CapsdConfigFactory.init();
            capsdFactory = CapsdConfigFactory.getInstance();
            capsdConfig = capsdFactory.getConfiguration();

            if (capsdConfig == null) {
                errorflag = true;
                // response.sendRedirect( "error.jsp?error=3");
                throw new ServletException("Capsd Configuration file is empty");
            }
        } catch (Throwable e) {
            throw new ServletException(e.getMessage());
        }
        initPollerServices();
        initCapsdProtocols();
        this.redirectSuccess = config.getInitParameter("redirect.success");
        if (this.redirectSuccess == null) {
            throw new ServletException("Missing required init parameter: redirect.success");
        }
    }

    /**
     * <p>initCapsdProtocols</p>
     */
    public void initCapsdProtocols() {
        pluginColl = capsdConfig.getProtocolPluginCollection();
        if (pluginColl != null) {
            Iterator<ProtocolPlugin> pluginiter = pluginColl.iterator();
            while (pluginiter.hasNext()) {
                ProtocolPlugin plugin = pluginiter.next();
                capsdColl.add(plugin);
                capsdProtocols.put(plugin.getProtocol(), plugin);
            }
        }
    }

    /**
     * <p>initPollerServices</p>
     */
    public void initPollerServices() {
        List<Package> packageColl = pollerConfig.getPackageCollection();
        if (packageColl != null) {
            Iterator<Package> pkgiter = packageColl.iterator();
            if (pkgiter.hasNext()) {
                pkg = pkgiter.next();
                for (Service svcProp : pkg.getServiceCollection()) {
                    pollerServices.put(svcProp.getName(), svcProp);
                }
            }
        }
    }

    /** {@inheritDoc} */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String user_id = request.getRemoteUser();
        errorflag = false;
        reloadFiles();
        String query = request.getQueryString();
        if (query != null) {
            String check1 = request.getParameter("check1");
            String name1 = request.getParameter("name1");
            String protoArray1 = request.getParameter("protArray1");
            String port1 = request.getParameter("port1");

            java.util.List<String> checkedList = new ArrayList<String>();
            if (name1 != null && !name1.equals("")) {
                addPollerInfo(check1, name1, port1, user_id, protoArray1, response);
                if (errorflag)
                    return;
                checkedList.add(name1);
                addCapsdInfo(name1, port1, user_id, protoArray1, response);
                if (!errorflag) {
                    props.setProperty("service." + name1 + ".protocol", protoArray1);
                } else
                    return;
                LogUtils.debugf(this, "Add Capsd Info 1");
            }

            LogUtils.debugf(this, "b4 writing to files");
            props.store(new FileOutputStream(ConfigFileConstants.getFile(ConfigFileConstants.POLLER_CONF_FILE_NAME)), null);
            Writer poller_fileWriter = new OutputStreamWriter(new FileOutputStream(ConfigFileConstants.getFile(ConfigFileConstants.POLLER_CONFIG_FILE_NAME)), "UTF-8");
            Writer capsd_fileWriter = new OutputStreamWriter(new FileOutputStream(ConfigFileConstants.getFile(ConfigFileConstants.CAPSD_CONFIG_FILE_NAME)), "UTF-8");
            try {
                Marshaller.marshal(pollerConfig, poller_fileWriter);
                Marshaller.marshal(capsdConfig, capsd_fileWriter);
                LogUtils.debugf(this, "writing to files");
            } catch (MarshalException e) {
                LogUtils.errorf(this, e, "failed to marshall config files");
                throw new ServletException(e);
            } catch (ValidationException e) {
                LogUtils.warnf(this, e, "failed to validate XML");
                throw new ServletException(e);
            }
        }

        if (!errorflag)
            response.sendRedirect(this.redirectSuccess);
    }

    /**
     * <p>addCapsdInfo</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param port a {@link java.lang.String} object.
     * @param user a {@link java.lang.String} object.
     * @param protocol a {@link java.lang.String} object.
     * @param response a {@link javax.servlet.http.HttpServletResponse} object.
     * @throws javax.servlet.ServletException if any.
     * @throws java.io.IOException if any.
     */
    public void addCapsdInfo(String name, String port, String user, String protocol, HttpServletResponse response) throws ServletException, IOException {
        // Check to see if the name is duplicate of the already specified names
        // first.
        LogUtils.debugf(this, "Adding %s to capsd", name);
        for (ProtocolPlugin svc : capsdConfig.getProtocolPluginCollection()) {
            if (svc.getProtocol().equals(name)) {
                // delete from the poller configuration.
                List<Service> tmpPollers = pkg.getServiceCollection();
                Iterator<Service> polleriter = tmpPollers.iterator();
                boolean removePoller = false;
                Service pollersvc = null;
                while (polleriter.hasNext()) {
                    pollersvc = polleriter.next();
                    if (pollersvc.getName().equals(name)) {
                        removePoller = true;
                        break;
                    }
                }
                if (removePoller) {
                    List<Service> tmpPoller = pkg.getServiceCollection();
                    if (tmpPoller.contains(pollersvc) && pollersvc.getName().equals(name)) {
                        errorflag = true;
                        LogUtils.debugf(this, "Removed from poller-config");
                        tmpPoller.remove(pollersvc);
                        response.sendRedirect("error.jsp?error=1&name=" + name);
                        return;
                    }
                }
                break;
                // throw new ServletException ("ProtocolPlugin name " + name + "
                // is already defined.. Try assigning another unique name");
            }
        }
        LogUtils.debugf(this, "%s doesnt already exist in capsd", name);
        ProtocolPlugin pluginAdd = new ProtocolPlugin();
        pluginAdd.setProtocol(name);
        String className = (String) props.get("service." + protocol + ".capsd-class");
        if (className != null) {
            pluginAdd.setClassName(className);
            pluginAdd.setScan("on");
            pluginAdd.setUserDefined("true");
            org.opennms.netmgt.config.capsd.Property newprop = new org.opennms.netmgt.config.capsd.Property();
            String banner = "*";
            if (props.get("banner") != null)
                banner = (String) props.get("banner");
            newprop.setValue(banner);
            newprop.setKey("banner");
            pluginAdd.addProperty(newprop);

            newprop = new org.opennms.netmgt.config.capsd.Property();
            if (port != null && !port.equals("")) {
                newprop.setValue(port);
                if (port.indexOf(":") == -1)
                    newprop.setKey("port");
                else
                    newprop.setKey("ports");
                pluginAdd.addProperty(newprop);
            } else {
                LogUtils.debugf(this, "Port from props " + props.get("port"));
                if (props.get("service." + protocol + ".port") == null || ((String) props.get("service." + protocol + ".port")).equals("")) {
                    errorflag = true;
                    response.sendRedirect("error.jsp?error=0&name=" + "service." + protocol + ".port ");
                    return;
                    // throw new ServletException("property " + "service." +
                    // protocol + ".port " + "is missing in " +
                    // Vault.getHomeDir() +
                    // ConfigFileConstants.CAPSD_CONFIG_FILE_NAME );
                } else {
                    port = (String) props.get("service." + protocol + ".port");
                    newprop.setValue(port);
                    if (port.indexOf(":") == -1)
                        newprop.setKey("port");
                    else
                        newprop.setKey("ports");
                    pluginAdd.addProperty(newprop);
                }
            }
            LogUtils.debugf(this, "Executing this section");
            newprop = new org.opennms.netmgt.config.capsd.Property();
            String timeout = "3000";
            if (props.get("timeout") != null)
                timeout = (String) props.get("timeout");
            newprop.setValue(timeout);
            newprop.setKey("timeout");
            pluginAdd.addProperty(newprop);

            newprop = new org.opennms.netmgt.config.capsd.Property();
            String retry = "3";
            if (props.get("retry") != null)
                retry = (String) props.get("retry");
            newprop.setValue(retry);
            newprop.setKey("retry");
            pluginAdd.addProperty(newprop);
            capsdProtocols.put(name, pluginAdd);
            pluginColl = capsdProtocols.values();
            capsdColl.add(pluginAdd);
            capsdConfig.addProtocolPlugin(pluginAdd);
        } else {
            errorflag = true;
            response.sendRedirect("error.jsp?error=0&name=" + "service." + protocol + ".capsd-class ");
            return;
        }
    }

    /**
     * <p>addPollerInfo</p>
     *
     * @param bPolled a {@link java.lang.String} object.
     * @param name a {@link java.lang.String} object.
     * @param port a {@link java.lang.String} object.
     * @param user a {@link java.lang.String} object.
     * @param protocol a {@link java.lang.String} object.
     * @param response a {@link javax.servlet.http.HttpServletResponse} object.
     * @throws javax.servlet.ServletException if any.
     * @throws java.io.IOException if any.
     */
    public void addPollerInfo(String bPolled, String name, String port, String user, String protocol, HttpServletResponse response) throws ServletException, IOException {
        // Check to see if the name is duplicate of the already specified names
        // first.
        for (Service svc : pkg.getServiceCollection()) {
            if (svc.getName().equals(name)) {
                errorflag = true;
                response.sendRedirect("error.jsp?error=1&name=" + name);
                LogUtils.infof(this, "%s already exists", name);
                return;
                // throw new ServletException ("Service name " + name + " is
                // already defined.. Try assigning another unique name");
            }
        }

        if (pkg != null) {
            Service newService = new Service();
            newService.setName(name);
            if (bPolled != null)
                newService.setStatus(bPolled);
            else
                newService.setStatus("off");
            newService.setName(name);
            newService.setUserDefined("true");

            Collection<Monitor> monitorColl = pollerConfig.getMonitorCollection();
            Monitor newMonitor = new Monitor();
            String monitor = (String) props.get("service." + protocol + ".monitor");
            if (monitor != null) {
                newMonitor.setService(name);
                newMonitor.setClassName(monitor);
                if (monitorColl != null)
                    monitorColl.add(newMonitor);
            } else {
                errorflag = true;
                response.sendRedirect("error.jsp?error=0&name=" + "service." + protocol + ".monitor ");
                return;
            }

            if (props.get("interval") != null)
                newService.setInterval((new Long((String) props.get("interval"))).longValue());
            else
                newService.setInterval(300000);

            org.opennms.netmgt.config.poller.Parameter newprop = new org.opennms.netmgt.config.poller.Parameter();
            String timeout = "3000";
            if (props.get("timeout") != null) {
                timeout = (String) props.get("timeout");
            }
            newprop.setValue(timeout);
            newprop.setKey("timeout");
            newService.addParameter(newprop);

            newprop = new org.opennms.netmgt.config.poller.Parameter();
            String banner = "*";
            if (props.get("banner") != null)
                banner = (String) props.get("banner");
            newprop.setValue(banner);
            newprop.setKey("banner");
            newService.addParameter(newprop);

            newprop = new org.opennms.netmgt.config.poller.Parameter();
            String retry = "3";
            if (props.get("retry") != null)
                retry = (String) props.get("retry");
            newprop.setValue(retry);
            newprop.setKey("retry");
            newService.addParameter(newprop);

            newprop = new org.opennms.netmgt.config.poller.Parameter();
            if (port == null || port.equals("")) {
                if (props.get("service." + protocol + ".port") == null || ((String) props.get("service." + protocol + ".port")).equals("")) {
                    errorflag = true;
                    response.sendRedirect("error.jsp?error=0&name=" + "service." + protocol + ".port");
                    return;
                } else
                    port = (String) props.get("service." + protocol + ".port");

            }

            newprop.setValue(port);
            if (port.indexOf(":") != -1)
                newprop.setKey("ports");
            else
                newprop.setKey("port");
            newService.addParameter(newprop);
            pkg.addService(newService);
        }
    }
}
