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


package org.opennms.netmgt.linkd;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.model.OnmsAtInterface;
import org.opennms.netmgt.model.OnmsStpInterface;
import org.opennms.netmgt.model.OnmsVlan;

/**
 * <p>LinkableNode class.</p>
 *
 * @author <a href="mailto:antonio@opennms.it">Antonio Russo</a>
 * @version $Id: $
 */
public class LinkableNode extends Object {

	int m_nodeId;

	String m_snmpprimaryaddr;
	
	String m_sysoid;

	List<CdpInterface> m_cdpinterfaces = new ArrayList<CdpInterface>();
	
	boolean m_hascdpinterfaces = false;

	List<RouterInterface> m_routeinterfaces = new ArrayList<RouterInterface>();
	
	boolean m_hasrouteinterfaces = false;

	List<OnmsAtInterface> m_atinterfaces = new ArrayList<OnmsAtInterface>();
	
	boolean m_hasatinterfaces = false;

	boolean isBridgeNode = false;
	
	/**
	 * the list of bridge port that are backbone bridge ports ou that are
	 * link between switches
	 */
	List<Integer> backBoneBridgePorts = new java.util.ArrayList<Integer>();

	List<OnmsVlan> vlans = new java.util.ArrayList<OnmsVlan>();

	List<String> bridgeIdentifiers = new java.util.ArrayList<String>();
	
	HashMap<String,List<OnmsStpInterface>> BridgeStpInterfaces = new HashMap<String,List<OnmsStpInterface>>();

	HashMap<String,String> vlanBridgeIdentifier = new HashMap<String,String>();

	HashMap<Integer,Set<String>> portMacs = new HashMap<Integer,Set<String>>();

	HashMap<String,String> macsVlan = new HashMap<String,String>();

	HashMap<String,String> vlanStpRoot = new HashMap<String,String>();

	HashMap<Integer,Integer> bridgePortIfindex = new HashMap<Integer,Integer>();


	LinkableNode() {
		throw new UnsupportedOperationException(
		"default constructor not supported");
	}

	/**
	 * <p>Constructor for LinkableNode.</p>
	 *
	 * @param nodeId a int.
	 * @param snmprimaryaddr a {@link java.lang.String} object.
	 * @param sysoid a {@link java.lang.String} object.
	 * @deprecated Use the InetAddress form instead.
	 */
	public LinkableNode(int nodeId, String snmprimaryaddr,String sysoid) {
		m_nodeId = nodeId;
		m_snmpprimaryaddr = snmprimaryaddr;
		m_sysoid = sysoid;
	}

	public LinkableNode(int nodeId, InetAddress snmpPrimaryAddr,String sysoid) {
		m_nodeId = nodeId;
		m_snmpprimaryaddr = InetAddressUtils.str(snmpPrimaryAddr);
		m_sysoid = sysoid;
	}
	/**
	 * <p>toString</p>
	 *
	 * @return a {@link java.lang.String} object.
	 */
	public String toString() {
		StringBuffer str = new StringBuffer("Node Id = " + m_nodeId + "\n");
		str.append("Snmp Primary Ip Address = " + m_snmpprimaryaddr + "\n");
		return str.toString();
	}

	/**
	 * <p>getNodeId</p>
	 *
	 * @return a int.
	 */
	public int getNodeId() {
		return m_nodeId;
	}

	/**
	 * <p>getSnmpPrimaryIpAddr</p>
	 *
	 * @return a {@link java.lang.String} object.
	 */
	public String getSnmpPrimaryIpAddr() {
		return m_snmpprimaryaddr;
	}

	/**
	 * <p>getCdpInterfaces</p>
	 *
	 * @return Returns the m_cdpinterfaces.
	 */
	public List<CdpInterface> getCdpInterfaces() {
		return m_cdpinterfaces;
	}
	/**
	 * <p>setCdpInterfaces</p>
	 *
	 * @param m_cdpinterfaces The m_cdpinterfaces to set.
	 */
	public void setCdpInterfaces(List<CdpInterface> m_cdpinterfaces) {
		if (m_cdpinterfaces == null || m_cdpinterfaces.isEmpty()) return;
		this.m_hascdpinterfaces = true;
		this.m_cdpinterfaces = m_cdpinterfaces;
	}
	
	/**
	 * <p>hasCdpInterfaces</p>
	 *
	 * @return Returns the m_hascdpinterfaces.
	 */
	public boolean hasCdpInterfaces() {
		return m_hascdpinterfaces;
	}

	/**
	 * <p>getRouteInterfaces</p>
	 *
	 * @return Returns the m_routeinterfaces.
	 */
	public List<RouterInterface> getRouteInterfaces() {
		return m_routeinterfaces;
	}
	/**
	 * <p>setRouteInterfaces</p>
	 *
	 * @param m_routeinterfaces a {@link java.util.List} object.
	 */
	public void setRouteInterfaces(List<RouterInterface> m_routeinterfaces) {
		if (m_routeinterfaces == null || m_routeinterfaces.isEmpty()) return;
		this.m_hasrouteinterfaces = true;
		this.m_routeinterfaces = m_routeinterfaces;
	}
	
	/**
	 * <p>hasAtInterfaces</p>
	 *
	 * @return Returns the m_hasatinterfaces.
	 */
	public boolean hasAtInterfaces() {
		return m_hasatinterfaces;
	}

	/**
	 * <p>getAtInterfaces</p>
	 *
	 * @return Returns the m_routeinterfaces.
	 */
	public List<OnmsAtInterface> getAtInterfaces() {
		return m_atinterfaces;
	}
	/**
	 * <p>setAtInterfaces</p>
	 *
	 * @param m_atinterfaces a {@link java.util.List} object.
	 */
	public void setAtInterfaces(List<OnmsAtInterface> m_atinterfaces) {
		if (m_atinterfaces == null || m_atinterfaces.isEmpty()) return;
		this.m_hasatinterfaces = true;
		this.m_atinterfaces = m_atinterfaces;
	}
	
	/**
	 * <p>hasRouteInterfaces</p>
	 *
	 * @return Returns the m_hascdpinterfaces.
	 */
	public boolean hasRouteInterfaces() {
		return m_hasrouteinterfaces;
	}


	/**
	 * <p>isBridgeNode</p>
	 *
	 * @return Returns the isBridgeNode.
	 */
	public boolean isBridgeNode() {
		return isBridgeNode;
	}

	/**
	 * @return Returns the backBoneBridgePorts.
	 */
	List<Integer> getBackBoneBridgePorts() {
		return backBoneBridgePorts;
	}

	/**
	 * @param backBoneBridgePorts
	 *            The backBoneBridgePorts to set.
	 */
	void setBackBoneBridgePorts(List<Integer> backBoneBridgePorts) {
		this.backBoneBridgePorts = backBoneBridgePorts;
	}

	/**
	 * return true if bridgeport is a backbone port
	 * @param bridgeport
	 * @return
	 */
	boolean isBackBoneBridgePort(int bridgeport) {
		return backBoneBridgePorts.contains(bridgeport);
	}

	/**
	 * add bridgeport to backbone ports
	 * @param bridgeport
	 */
	void addBackBoneBridgePorts(final int bridgeport) {
		if (backBoneBridgePorts.contains(bridgeport))
			return;
		backBoneBridgePorts.add(bridgeport);
	}

	/**
	 * @return Returns the bridgeIdentifiers.
	 */
	List<String> getBridgeIdentifiers() {
		return bridgeIdentifiers;
	}

	/**
	 * @param bridgeIdentifiers
	 *            The bridgeIdentifiers to set.
	 */
	void setBridgeIdentifiers(List<String> bridgeIdentifiers) {
		if (bridgeIdentifiers == null || bridgeIdentifiers.isEmpty() ) return;
		this.bridgeIdentifiers = bridgeIdentifiers;
		isBridgeNode = true;
	}

	void addBridgeIdentifier(String bridge, String vlan) {
		vlanBridgeIdentifier.put(vlan, bridge);
		if (bridgeIdentifiers.contains(bridge))
			return;
		bridgeIdentifiers.add(bridge);
		isBridgeNode = true;
	}

	boolean isBridgeIdentifier(String bridge) {
		return bridgeIdentifiers.contains(bridge);
	}

	void addBridgeIdentifier(String bridge) {
		if (bridgeIdentifiers.contains(bridge))
			return;
		bridgeIdentifiers.add(bridge);
		isBridgeNode = true;
	}

	String getBridgeIdentifier(String vlan) {
		return vlanBridgeIdentifier.get(vlan);
	}

	void addMacAddress(int bridgeport, String macAddress, String vlan) {

		Set<String> macs = new HashSet<String>();
		if (portMacs.containsKey(bridgeport)) {
			macs = portMacs.get(bridgeport);
		}
		macs.add(macAddress);

		portMacs.put(bridgeport, macs);
		macsVlan.put(macAddress, vlan);
	}

	boolean hasMacAddress(String macAddress) {
	    for (final Set<String> macs : portMacs.values()) {
	        if (macs.contains(macAddress)) {
	            return true;
	        }
	    }
		return false;
	}

	boolean hasMacAddresses() {
		return !portMacs.isEmpty();
	}
	String getVlan(String macAddress) {
		return macsVlan.get(macAddress);
	}

	Set<String> getMacAddressesOnBridgePort(int bridgeport) {
		return portMacs.get(bridgeport);
	}

	boolean hasMacAddressesOnBridgePort(int bridgeport) {
		return (portMacs.containsKey(bridgeport) && portMacs.get(bridgeport) != null );
	}

	List<Integer> getBridgePortsFromMac(String macAddress) {
		List<Integer> ports = new ArrayList<Integer>();
		Iterator<Integer> ite = portMacs.keySet().iterator();
		while (ite.hasNext()) {
			Integer intePort = ite.next();
			Set<String> macs = portMacs.get(intePort);
			if (macs.contains(macAddress)) {
				ports.add(intePort);
			}
		}
		return ports;
	}

	int getIfindex(int bridgeport) {
		if (bridgePortIfindex.containsKey(bridgeport)) {
		    final Integer ifindex = bridgePortIfindex.get(bridgeport);
			return ifindex.intValue();
		}
		return -1;
	}

	int getBridgePort(int ifindex) {
		Iterator<Integer> ite = bridgePortIfindex.keySet().iterator();
		while (ite.hasNext() ) {
			Integer curBridgePort = ite.next();
			Integer curIfIndex = bridgePortIfindex.get(curBridgePort);
			if (curIfIndex.intValue() == ifindex) return curBridgePort.intValue();
		}
		return -1;
	}

	void setIfIndexBridgePort(Integer ifindex, Integer bridgeport) {
        
        if ( ifindex == null ) {
            throw new NullPointerException("ifindex is null");
        } else if (bridgeport == null) {
            throw new NullPointerException("bridgeport is null");
        }
		bridgePortIfindex.put(bridgeport, ifindex);
	}

	/**
	 * @return Returns the portMacs.
	 */
	HashMap<Integer, Set<String>> getPortMacs() {
		return portMacs;
	}

	/**
	 * @param portMacs
	 *            The portMacs to set.
	 */
	void setPortMacs(HashMap<Integer,Set<String>> portMacs) {
		this.portMacs = portMacs;
	}

	void setVlanStpRoot(String vlan, String stproot) {
		if (stproot != null) vlanStpRoot.put(vlan, stproot);
	}

	boolean hasStpRoot(String vlan) {
		return vlanStpRoot.containsKey(vlan);
	}

	String getStpRoot(String vlan) {
		if (vlanStpRoot.containsKey(vlan)) {
			return vlanStpRoot.get(vlan);
		}
		return null;
	}

	/**
	 * <p>getStpInterfaces</p>
	 *
	 * @return Returns the stpInterfaces.
	 */
	public HashMap<String,List<OnmsStpInterface>> getStpInterfaces() {
		return BridgeStpInterfaces;
	}
	/**
	 * <p>setStpInterfaces</p>
	 *
	 * @param stpInterfaces The stpInterfaces to set.
	 */
	public void setStpInterfaces(HashMap<String,List<OnmsStpInterface>> stpInterfaces) {
		BridgeStpInterfaces = stpInterfaces;
	}
	
	/**
	 * <p>addStpInterface</p>
	 *
	 * @param stpIface a {@link org.opennms.netmgt.model.OnmsStpInterface} object.
	 */
	public void addStpInterface(OnmsStpInterface stpIface) {
		final String vlanindex = stpIface.getVlan() == null? "0" : stpIface.getVlan().toString();
		List<OnmsStpInterface> stpifs = new ArrayList<OnmsStpInterface>();;
		if (BridgeStpInterfaces.containsKey(vlanindex)) {
			stpifs = BridgeStpInterfaces.get(vlanindex);
		}
		stpifs.add(stpIface);
		BridgeStpInterfaces.put(vlanindex, stpifs);
	}

	/**
	 * <p>getSysoid</p>
	 *
	 * @return a {@link java.lang.String} object.
	 */
	public String getSysoid() {
		return m_sysoid;
	}

	/**
	 * <p>setSysoid</p>
	 *
	 * @param m_sysoid a {@link java.lang.String} object.
	 */
	public void setSysoid(String m_sysoid) {
		this.m_sysoid = m_sysoid;
	}

	/**
	 * <p>Getter for the field <code>vlans</code>.</p>
	 *
	 * @return a {@link java.util.List} object.
	 */
	public List<OnmsVlan> getVlans() {
		return vlans;
	}

	/**
	 * <p>Setter for the field <code>vlans</code>.</p>
	 *
	 * @param vlans a {@link java.util.List} object.
	 */
	public void setVlans(List<OnmsVlan> vlans) {
		this.vlans = vlans;
	}
}
