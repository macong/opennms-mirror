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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opennms.core.utils.InetAddressUtils;
import org.opennms.core.utils.LogUtils;
import org.opennms.netmgt.config.LinkdConfig;
import org.opennms.netmgt.config.SnmpPeerFactory;
import org.opennms.netmgt.linkd.scheduler.ReadyRunnable;
import org.opennms.netmgt.linkd.scheduler.Scheduler;
import org.opennms.netmgt.linkd.snmp.FdbTableGet;
import org.opennms.netmgt.linkd.snmp.VlanCollectorEntry;
import org.opennms.netmgt.model.OnmsAtInterface;
import org.opennms.netmgt.model.OnmsStpInterface;
import org.opennms.netmgt.model.OnmsVlan;
import org.opennms.netmgt.snmp.SnmpAgentConfig;

/**
 * This class is designed to discover link among nodes using the collected and
 * the necessary SNMP information. When the class is initially constructed no
 * information is used.
 *
 * @author <a href="mailto:antonio@opennms.it">Antonio Russo </a>
 */
public final class DiscoveryLink implements ReadyRunnable {

	private static final int SNMP_IF_TYPE_ETHERNET = 6;

	private static final int SNMP_IF_TYPE_PROP_VIRTUAL = 53;

	private static final int SNMP_IF_TYPE_L2_VLAN = 135;

	private static final int SNMP_IF_TYPE_L3_VLAN = 136;

	private String packageName;
	
	private List<NodeToNodeLink> m_links = new ArrayList<NodeToNodeLink>();

	private List<MacToNodeLink> m_maclinks = new ArrayList<MacToNodeLink>();

	private HashMap<Integer,LinkableNode> m_bridgeNodes = new HashMap<Integer,LinkableNode>();

	private List<LinkableNode> m_routerNodes = new ArrayList<LinkableNode>();

	private List<LinkableNode> m_cdpNodes = new ArrayList<LinkableNode>();
	
	private List<LinkableNode> m_atNodes = new ArrayList<LinkableNode>();

	// this is the list of mac address just parsed by discovery process
	private List<String> m_macsParsed = new ArrayList<String>();
	
	// this is the list of mac address excluded by discovery process
	private List<String> macsExcluded = new ArrayList<String>();

	// this is the list of atinterfaces for which to be discovery link
	// here there aren't the bridge identifier because they should be discovered
	// by main processes. This is used by addlinks method.
	private Map<String,List<OnmsAtInterface>> m_macToAtinterface = new HashMap<String,List<OnmsAtInterface>>();
	
	private boolean enableDownloadDiscovery = false;
	
	private boolean discoveryUsingRoutes = true;
	
	private boolean discoveryUsingCdp = true;
	
	private boolean discoveryUsingBridge = true;

	private boolean suspendCollection = false;

	private boolean isRunned = false;

	private boolean forceIpRouteDiscoveryOnEtherNet = false;
	/**
	 * The scheduler object
	 *  
	 */

	private Scheduler m_scheduler;

	/**
	 * The interval default value 30 min
	 */

	private long snmp_poll_interval = 1800000;

	/**
	 * The interval default value 5 min It is the time in ms after snmp
	 * collection is started
	 *  
	 */

	private long discovery_interval = 300000;

	/**
	 * The initial sleep time default value 10 min
	 */

	private long initial_sleep_time = 600000;

    private Linkd m_linkd;

    /**
     * @param linkd the linkd to set
     */
    public void setLinkd(Linkd linkd) {
        this.m_linkd = linkd;
    }

    public Linkd getLinkd() {
        return m_linkd;
    }

	/**
	 * Constructs a new DiscoveryLink object . The discovery does not occur
	 * until the <code>run</code> method is invoked.
	 */
	public DiscoveryLink() {
		super();
	}
	
	/**
	 * <p>
	 * Performs link discovery for the Nodes and save info in
	 * DatalinkInterface table on DataBase
	 * <p>
	 * No synchronization is performed, so if this is used in a separate thread
	 * context synchronization must be added.
	 * </p>
	 */
	public void run() {

		if (suspendCollection) {
		    LogUtils.warnf(this, "DiscoveryLink.run: Suspended!");
		} else {
			Collection<LinkableNode> linkableNodes = m_linkd.getLinkableNodesOnPackage(getPackageName());

			LogUtils.debugf(this, "DiscoveryLink.run: LinkableNodes/package found: %d/%s", linkableNodes.size(), getPackageName());
			LogUtils.debugf(this, "DiscoveryLink.run: discoveryUsingBridge/discoveryUsingCdp/discoveryUsingRoutes: %b/%b/%b", discoveryUsingBridge, discoveryUsingCdp, discoveryUsingRoutes);
			LogUtils.debugf(this, "DiscoveryLink.run: enableDownloadDiscovery: %b", enableDownloadDiscovery);

			for (final LinkableNode linkableNode : linkableNodes) {
				LogUtils.debugf(this, "DiscoveryLink.run: Iterating on LinkableNode's found node: %d", linkableNode.getNodeId());

				if (linkableNode.isBridgeNode() && discoveryUsingBridge) {
					m_bridgeNodes.put(new Integer(linkableNode.getNodeId()), linkableNode);
				}
				if (linkableNode.hasCdpInterfaces() && discoveryUsingCdp) {
					m_cdpNodes.add(linkableNode);
				}
				if (linkableNode.hasRouteInterfaces() && discoveryUsingRoutes) {
					m_routerNodes.add(linkableNode);
				}

				if (linkableNode.hasAtInterfaces()) {
					m_atNodes.add(linkableNode);
				}
			}

			populateMacToAtInterface();

			//now perform operation to complete
			if (enableDownloadDiscovery) {
			    LogUtils.infof(this, "DiscoveryLink.run: get further unknown MAC address SNMP bridge table info");
			    snmpParseBridgeNodes();
			} else {
			    LogUtils.infof(this, "DiscoveryLink.run: skipping get further unknown MAC address SNMP bridge table info");
			}

			// First of all use quick methods to get backbone ports for speeding up the link discovery
			LogUtils.debugf(this, "DiscoveryLink.run: finding links among nodes using Cisco Discovery Protocol");

			// Try Cisco Discovery Protocol to found link among all nodes
			// Add CDP info for backbones ports

			for (final LinkableNode curNode : m_cdpNodes) {
				int curCdpNodeId = curNode.getNodeId();
				final String curCdpIpAddr = curNode.getSnmpPrimaryIpAddr();

				LogUtils.debugf(this, "DiscoveryLink.run: parsing nodeid %d ip address %s with %d Cdp interfaces.", curCdpNodeId, curCdpIpAddr, curNode.getCdpInterfaces().size());

				for (final CdpInterface cdpIface : curNode.getCdpInterfaces()) {
				    final int cdpIfIndex = cdpIface.getCdpIfIndex();
					
					if (cdpIfIndex < 0) {
					    LogUtils.warnf(this, "DiscoveryLink.run: found not valid CDP IfIndex %d.  Skipping.", cdpIfIndex);
						continue;
					}

					LogUtils.debugf(this, "DiscoveryLink.run: found CDP ifindex %d", cdpIfIndex);

					final InetAddress targetIpAddr = cdpIface.getCdpTargetIpAddr();
					final String hostAddress = InetAddressUtils.str(targetIpAddr);

					if (!m_linkd.isInterfaceInPackage(hostAddress, getPackageName())) 
					{
					    LogUtils.warnf(this, "DiscoveryLink.run: ip address %s Not in package: %s.  Skipping.", hostAddress, getPackageName());
					    continue;
					}

					final int targetCdpNodeId = cdpIface.getCdpTargetNodeId();

					if (targetCdpNodeId == -1) {
					    LogUtils.warnf(this, "DiscoveryLink.run: no node id found for ip address %s.  Skipping.", hostAddress);
						continue;
					}

					LogUtils.debugf(this, "DiscoveryLink.run: found nodeid/CDP target ipaddress: %d:%s", targetCdpNodeId, targetIpAddr);

					if (targetCdpNodeId == curCdpNodeId) {
					    LogUtils.debugf(this, "DiscoveryLink.run: node id found for ip address %s is itself.  Skipping.", hostAddress);
						continue;
					}

					final int cdpDestIfindex = cdpIface.getCdpTargetIfIndex();
					
					if (cdpDestIfindex < 0) {
					    LogUtils.warnf(this, "DiscoveryLink.run: found not valid CDP destination IfIndex %d.  Skipping.", cdpDestIfindex);
						continue;
					}

					LogUtils.debugf(this, "DiscoveryLink.run: found CDP target ifindex %d", cdpDestIfindex);

					LogUtils.debugf(this, "DiscoveryLink.run: parsing CDP link: nodeid=%d ifindex=%d nodeparentid=%d parentifindex=%d", curCdpNodeId, cdpIfIndex, targetCdpNodeId, cdpDestIfindex);

					boolean add = false;
					if (curNode.isBridgeNode() && isBridgeNode(targetCdpNodeId)) {
						LinkableNode targetNode = m_bridgeNodes.get(new Integer(targetCdpNodeId));
						add = parseCdpLinkOn(curNode, cdpIfIndex,targetNode, cdpDestIfindex);
						LogUtils.debugf(this, "DiscoveryLink.run: both node are bridge nodes! Adding: %b", add);
					} else if (curNode.isBridgeNode()) {
					    LogUtils.debugf(this, "DiscoveryLink.run: source node is bridge node, target node is not bridge node! Adding: %b", add);
						add = parseCdpLinkOn(curNode,cdpIfIndex,targetCdpNodeId);
					} else if (isBridgeNode(targetCdpNodeId)) {
					    LogUtils.debugf(this, "DiscoveryLink.run: source node is not bridge node, target node is bridge node! Adding: %b", add);
						LinkableNode targetNode = m_bridgeNodes.get(new Integer(targetCdpNodeId));
						add = parseCdpLinkOn(targetNode,cdpDestIfindex,curCdpNodeId);
					} else {
					    LogUtils.debugf(this, "DiscoveryLink.run: no node is bridge node! Adding CDP link");
					    add = true;
					}

					// now add the cdp link
					if (add) {
					    final NodeToNodeLink lk = new NodeToNodeLink(targetCdpNodeId, cdpDestIfindex);
						lk.setNodeparentid(curCdpNodeId);
						lk.setParentifindex(cdpIfIndex);
						addNodetoNodeLink(lk);
						LogUtils.debugf(this, "DiscoveryLink.run: CDP link added: %s", lk.toString());
					}
				}
			}

			// try get backbone links between switches using STP info
			// and store information in Bridge class
			LogUtils.debugf(this, "DiscoveryLink.run: try to found backbone ethernet links among bridge nodes using Spanning Tree Protocol");

			for (final LinkableNode curNode : m_bridgeNodes.values()) {
			    final int curNodeId = curNode.getNodeId();
			    final String cupIpAddr = curNode.getSnmpPrimaryIpAddr();

				LogUtils.debugf(this, "DiscoveryLink.run: parsing bridge nodeid %d ip address %s", curNodeId, cupIpAddr);

				LogUtils.debugf(this, "DiscoveryLink.run: parsing %d Vlan.", curNode.getStpInterfaces().size());

				for (final Map.Entry<String,List<OnmsStpInterface>> me : curNode.getStpInterfaces().entrySet()) {
				    final String vlan = me.getKey();
				    final String curBaseBridgeAddress = curNode.getBridgeIdentifier(vlan);

					LogUtils.debugf(this, "DiscoveryLink.run: found bridge identifier %s", curBaseBridgeAddress);

					String designatedRoot = null;
					
					if (curNode.hasStpRoot(vlan)) {
						designatedRoot = curNode.getStpRoot(vlan);
					} else {
					    LogUtils.debugf(this, "DiscoveryLink.run: desigated root bridge identifier not found. Skipping %s", curBaseBridgeAddress);
						continue;
					}

					if (designatedRoot == null || designatedRoot.equals("0000000000000000")) {
					    LogUtils.warnf(this, "DiscoveryLink.run: designated root is invalid. Skipping");
						continue;
					}
					// check if designated
					// bridge is it self
					// if bridge is STP root bridge itself exiting
					// searching on linkablesnmpnodes

					if (curNode.isBridgeIdentifier(designatedRoot.substring(4))) {
					    LogUtils.debugf(this, "DiscoveryLink.run: STP designated root is the bridge itself. Skipping");
						continue;
					}

					// Now parse STP bridge port info to get designated bridge
					LogUtils.debugf(this, "DiscoveryLink.run: STP designated root is another bridge. %s Parsing Stp Interface", designatedRoot);

					for (final OnmsStpInterface stpIface : me.getValue()) {
						// the bridge port number
					    final int stpbridgeport = stpIface.getBridgePort();
						// if port is a backbone port continue
						if (curNode.isBackBoneBridgePort(stpbridgeport)) {
						    LogUtils.debugf(this, "DiscoveryLink.run: bridge port %d already found.  Skipping.", stpbridgeport);
							continue;
						}

						final String stpPortDesignatedPort = stpIface.getStpPortDesignatedPort();
						final String stpPortDesignatedBridge = stpIface.getStpPortDesignatedBridge();

						LogUtils.debugf(this, "DiscoveryLink.run: parsing bridge port %d with stp designated bridge %s and stp designated port %s", stpbridgeport, stpPortDesignatedBridge, stpPortDesignatedPort);

						if (stpPortDesignatedBridge == null || stpPortDesignatedBridge.equals("0000000000000000") || stpPortDesignatedBridge.equals("")) {
						    LogUtils.warnf(this, "DiscoveryLink.run: designated bridge is invalid: %s", stpPortDesignatedBridge);
							continue;
						}

						if (curNode.isBridgeIdentifier(stpPortDesignatedBridge.substring(4))) {
						    LogUtils.debugf(this, "DiscoveryLink.run: designated bridge for port %d is bridge itself", stpbridgeport);
							continue;
						}

						if (stpPortDesignatedPort == null || stpPortDesignatedPort.equals("0000")) {
						    LogUtils.warnf(this, "DiscoveryLink.run: designated port is invalid: %s", stpPortDesignatedPort);
							continue;
						}

                        // A Port Identifier shall be encoded as two octets,
                        // taken to represent an unsigned binary number. If
                        // two Port Identifiers are numerically compared, the
                        // lesser number denotes the Port of better priority.
                        // The more significant octet of a Port Identifier is
                        // a settable priority component that permits the
                        // relative priority of Ports on the same Bridge to be
                        // managed (17.13.7 and Clause 14). The less
                        // significant twelve bits is the Port Number
                        // expressed as an unsigned binary number. The value 0
                        // is not used as a Port Number. NOTE -- The number of
                        // bits that are considered to be part of the Port
                        // Number (12 bits) differs from the 1998 and prior
                        // versions of this standard (formerly, the priority
                        // component was 8 bits and the Port Number component
                        // also 8 bits). This change acknowledged that modern
                        // switched LAN infrastructures call for increasingly
                        // large numbers of Ports to be supported in a single
                        // Bridge. To maintain management compatibility with
                        // older implementations, the priority component is
                        // still considered, for management purposes, to be an
                        // 8-bit value, but the values that it can be set to
                        // are restricted to those where the least significant
                        // 4 bits are zero (i.e., only the most significant 4
                        // bits are settable).
                        int designatedbridgeport = Integer.parseInt(stpPortDesignatedPort.substring(1), 16);

						// try to see if designated bridge is linkable SNMP node

                        final LinkableNode designatedNode = getNodeFromMacIdentifierOfBridgeNode(stpPortDesignatedBridge.substring(4));

						if (designatedNode == null) {
						    LogUtils.warnf(this, "DiscoveryLink.run: no nodeid found for stp bridge address %s.  Nothing to save.", stpPortDesignatedBridge);
							continue; // no saving info if no nodeid
						}
						
						final int designatednodeid = designatedNode.getNodeId();

						LogUtils.debugf(this, "DiscoveryLink.run: found designated nodeid %d", designatednodeid);

						// test if there are other bridges between this link
						// USING MAC ADDRESS FORWARDING TABLE

						if (!isNearestBridgeLink(curNode, stpbridgeport, designatedNode, designatedbridgeport)) {
						    LogUtils.debugf(this, "DiscoveryLink.run: other bridge found between nodes. No links to save!");
							continue; // no saving info if no nodeid
						}

						// this is a backbone port so try adding to Bridge class
						// get the ifindex on node

						final int curIfIndex = curNode.getIfindex(stpbridgeport);

						if (curIfIndex == -1) {
						    LogUtils.warnf(this, "DiscoveryLink.run: got invalid ifindex");
							continue;
						}

						final int designatedifindex = designatedNode.getIfindex(designatedbridgeport);
						
						if (designatedifindex == -1) {
						    LogUtils.warnf(this, "DiscoveryLink.run: got invalid ifindex on designated node");
							continue;
						}

						LogUtils.debugf(this, "DiscoveryLink.run: backbone port found for node %d.  Adding to bridge %d.", curNodeId, stpbridgeport);

						curNode.addBackBoneBridgePorts(stpbridgeport);
						m_bridgeNodes.put(new Integer(curNodeId), curNode);

						LogUtils.debugf(this, "DiscoveryLink.run: backbone port found for node %d.  Adding to helper class BB port bridge port %d.", designatednodeid, designatedbridgeport);

						designatedNode.addBackBoneBridgePorts(designatedbridgeport);
						m_bridgeNodes.put(new Integer(designatednodeid), designatedNode);

						LogUtils.debugf(this, "DiscoveryLink.run: adding links on BB bridge port %d", designatedbridgeport);

						addLinks(getMacsOnBridgeLink(curNode, stpbridgeport, designatedNode, designatedbridgeport),curNodeId,curIfIndex);

						// writing to db using class
						// DbDAtaLinkInterfaceEntry
						final NodeToNodeLink lk = new NodeToNodeLink(curNodeId, curIfIndex);
						lk.setNodeparentid(designatednodeid);
						lk.setParentifindex(designatedifindex);
						addNodetoNodeLink(lk);

					}
				}
			}

			// finding links using mac address on ports
			LogUtils.debugf(this, "DiscoveryLink.run: try to found links using Mac Address Forwarding Table");

			for (final LinkableNode curNode : m_bridgeNodes.values()) {
			    final int curNodeId = curNode.getNodeId();
				LogUtils.debugf(this, "DiscoveryLink.run: parsing node bridge %d", curNodeId);

				for (final Integer curBridgePort : curNode.getPortMacs().keySet()) {
					LogUtils.debugf(this, "DiscoveryLink.run: parsing bridge port %d with mac address %s", curBridgePort, curNode.getMacAddressesOnBridgePort(curBridgePort).toString());

					if (curNode.isBackBoneBridgePort(curBridgePort)) {
					    LogUtils.debugf(this, "DiscoveryLink.run: Port %d is a backbone bridge port.  Skipping.", curBridgePort);
						continue;
					}
					
					final int curIfIndex = curNode.getIfindex(curBridgePort);
					if (curIfIndex == -1) {
					    LogUtils.warnf(this, "DiscoveryLink.run: got invalid ifIndex on bridge port %d", curBridgePort);
						continue;
					}
					// First get the mac addresses on bridge port

					final Set<String> macs = curNode.getMacAddressesOnBridgePort(curBridgePort);

					// Then find the bridges whose mac addresses are learned on bridge port
					final List<LinkableNode> bridgesOnPort = getBridgesFromMacs(macs);
					
					if (bridgesOnPort.isEmpty()) {
					    LogUtils.debugf(this, "DiscoveryLink.run: no bridge info found on port %d.  Saving MACs.", curBridgePort);
						addLinks(macs, curNodeId, curIfIndex);
					} else {
						// a bridge mac address was found on port so you should analyze what happens
					    LogUtils.debugf(this, "DiscoveryLink.run: bridge info found on port %d.  Finding nearest.", curBridgePort);
					    
                        // one among these bridges should be the node more close to the curnode, curport
					    for (final LinkableNode endNode : bridgesOnPort) {
					        final int endNodeid = endNode.getNodeId();
							
					        final int endBridgePort = getBridgePortOnEndBridge(curNode, endNode);
							// The bridge port should be valid! This control is not properly done
							if (endBridgePort == -1) {
							    LogUtils.errorf(this, "DiscoveryLink.run: no valid port found on bridge nodeid %d for node bridge identifiers nodeid %d.  Skipping.", endNodeid, curNodeId);
								continue;
							}
							
							// Try to found a new 
							final boolean isTargetNode = isNearestBridgeLink(curNode, curBridgePort, endNode, endBridgePort);
							if (!isTargetNode) continue;

							final int endIfindex = endNode.getIfindex(endBridgePort);
							if (endIfindex == -1) {
							    LogUtils.warnf(this, "DiscoveryLink.run: got invalid ifindex o designated bridge port %d", endBridgePort);
								break;
							}

							LogUtils.debugf(this, "DiscoveryLink.run: backbone port found for node %d. Adding backbone port %d to bridge", curNodeId, curBridgePort);

							curNode.addBackBoneBridgePorts(curBridgePort);
							m_bridgeNodes.put(curNodeId, curNode);

							LogUtils.debugf(this, "DiscoveryLink.run: backbone port found for node %d. Adding to helper class bb port bridge port %d", endNodeid, endBridgePort);

							endNode.addBackBoneBridgePorts(endBridgePort);
							m_bridgeNodes.put(endNodeid, endNode);

							// finding links between two backbone ports
							addLinks(getMacsOnBridgeLink(curNode, curBridgePort, endNode, endBridgePort),curNodeId,curIfIndex);

							final NodeToNodeLink lk = new NodeToNodeLink(curNodeId, curIfIndex);
							lk.setNodeparentid(endNodeid);
							lk.setParentifindex(endIfindex);
							addNodetoNodeLink(lk);
							break;
						}
					}
				}
			}

			// fourth find inter router links,
			// this part could have several special function to get inter router
			// links, but at the moment we worked much on switches.
			// In future we can try to extend this part.
			LogUtils.debugf(this, "DiscoveryLink.run: try to found not ethernet links on Router nodes");

			for (final LinkableNode curNode : m_routerNodes) {
			    final int curNodeId = curNode.getNodeId();
				String curIpAddr = curNode.getSnmpPrimaryIpAddr();
				LogUtils.debugf(this, "DiscoveryLink.run: parsing router nodeid %d ip address %s", curNodeId, curIpAddr);

				final List<RouterInterface> routeInterfaces = curNode.getRouteInterfaces();
                LogUtils.debugf(this, "DiscoveryLink.run: parsing %d route interfaces.", routeInterfaces.size());
                
                for (final RouterInterface routeIface : routeInterfaces) {
					LogUtils.debugf(this, "DiscoveryLink.run: parsing RouterInterface: " + routeIface.toString());

					if (routeIface.getMetric() == -1) {
					    LogUtils.infof(this, "DiscoveryLink.run: Router interface has invalid metric %d. Skipping", routeIface.getMetric());
						continue;
					}

					if (forceIpRouteDiscoveryOnEtherNet) {
					    LogUtils.infof(this, "DiscoveryLink.run: force ip route discovery not getting SnmpIfType");
					} else {
					    final int snmpiftype = routeIface.getSnmpiftype();
						LogUtils.infof(this, "DiscoveryLink.run: force ip route discovery getting SnmpIfType: " + snmpiftype);

						if (snmpiftype == SNMP_IF_TYPE_ETHERNET) {
						    LogUtils.infof(this, "DiscoveryLink.run: Ethernet interface for nodeid. Skipping ");
							continue;
						} else if (snmpiftype == SNMP_IF_TYPE_PROP_VIRTUAL) {
						    LogUtils.infof(this, "DiscoveryLink.run: PropVirtual interface for nodeid. Skipping ");
							continue;
						} else if (snmpiftype == SNMP_IF_TYPE_L2_VLAN) {
						    LogUtils.infof(this, "DiscoveryLink.run: Layer2 Vlan interface for nodeid. Skipping ");
							continue;
						} else if (snmpiftype == SNMP_IF_TYPE_L3_VLAN) {
						    LogUtils.infof(this, "DiscoveryLink.run: Layer3 Vlan interface for nodeid. Skipping ");
							continue;
						} else if (snmpiftype == -1) {
						    LogUtils.infof(this, "store: interface has unknown snmpiftype %d. Skipping", snmpiftype);
							continue;
						} 
					}
					
					final InetAddress nexthop = routeIface.getNextHop();
					final String hostAddress = InetAddressUtils.str(nexthop);

					if (hostAddress.equals("0.0.0.0")) {
					    LogUtils.infof(this, "DiscoveryLink.run: nexthop address is broadcast address %s. Skipping", hostAddress);
						// FIXME this should be further analyzed 
						// working on routeDestNet you can find hosts that
						// are directly connected with the destination network
						// This happens when static routing is made like this:
						// route 10.3.2.0 255.255.255.0 Serial0
						// so the router broadcasts on Serial0
						continue;
					}

					if (nexthop.isLoopbackAddress()) {
					    LogUtils.infof(this, "DiscoveryLink.run: nexthop address is localhost address %s. Skipping", hostAddress);
						continue;
					}

					if (!m_linkd.isInterfaceInPackage(hostAddress, getPackageName())) {
					    LogUtils.infof(this, "DiscoveryLink.run: nexthop address is not in package %s/%s. Skipping", hostAddress, getPackageName());
						continue;
					}

					
					final int nextHopNodeid = routeIface.getNextHopNodeid();

					if (nextHopNodeid == -1) {
					    LogUtils.infof(this, "DiscoveryLink.run: no node id found for ip next hop address %s. Skipping", hostAddress);
						continue;
					}

					if (nextHopNodeid == curNodeId) {
					    LogUtils.debugf(this, "DiscoveryLink.run: node id found for ip next hop address %s is itself. Skipping", hostAddress);
						continue;
					}

					int ifindex = routeIface.getIfindex();
					
					if (ifindex == 0) {
                        LogUtils.infof(this, "DiscoveryLink.run: route interface has ifindex %d -- trying to get ifIndex from nextHopNet: %s", ifindex, routeIface.getNextHopNet());
                        ifindex = getIfIndexFromRouter(curNode, routeIface.getNextHopNet());
						if (ifindex == -1 ) {
						    LogUtils.debugf(this, "DiscoveryLink.run: found not correct ifindex %d. Skipping", ifindex);
							continue;
						} else {
						    LogUtils.debugf(this, "DiscoveryLink.run: found correct ifindex %d.", ifindex);
						}
						
					}
					LogUtils.debugf(this, "DiscoveryLink.run: saving route link");
					
					// Saving link also when ifindex = -1 (not found)
					final NodeToNodeLink lk = new NodeToNodeLink(nextHopNodeid, routeIface.getNextHopIfindex());
					lk.setNodeparentid(curNodeId);
					lk.setParentifindex(ifindex);
					addNodetoNodeLink(lk);
				}
			}

			m_bridgeNodes.clear();
			m_routerNodes.clear();
			m_cdpNodes.clear();
			m_macsParsed.clear();
			macsExcluded.clear();
			m_macToAtinterface.clear();
			m_atNodes.clear();

			m_linkd.updateDiscoveryLinkCollection(this);

			m_links.clear();
			m_maclinks.clear();
		}
		// rescheduling activities
		isRunned = true;
		reschedule();
	}

    protected void populateMacToAtInterface() {
        LogUtils.debugf(this, "DiscoveryLink.run: using atNodes to populate macToAtinterface");
        for (final LinkableNode curNode : m_atNodes) {
            for (final OnmsAtInterface at : curNode.getAtInterfaces()) {
        		int nodeid = at.getNode().getId();
        		final String ipaddr = at.getIpAddress();
        		final String macAddress = at.getMacAddress();
        		LogUtils.debugf(this, "Parsing AtInterface nodeid/ipaddr/macaddr: %d/%s/%s", nodeid, ipaddr, macAddress);
        		if (!m_linkd.isInterfaceInPackage(at.getIpAddress(), getPackageName())) {
                    LogUtils.infof(this, "DiscoveryLink.run: at interface: %s does not belong to package: %s! Not adding to discoverable atinterface.", ipaddr, getPackageName());
        			macsExcluded.add(macAddress);
        			continue;
        		}
        		if (isMacIdentifierOfBridgeNode(macAddress)) {
        		    LogUtils.infof(this, "DiscoveryLink.run: at interface %s belongs to bridge node! Not adding to discoverable atinterface.", macAddress);
        			macsExcluded.add(macAddress);
        			continue;
        		}
                if (macAddress.indexOf("00000c07ac") == 0) {
                    LogUtils.infof(this, "DiscoveryLink.run: at interface %s is cisco hsrp address! Not adding to discoverable atinterface.", macAddress);
                   macsExcluded.add(macAddress); 
                   continue; 
                }
                List<OnmsAtInterface> ats = m_macToAtinterface.get(macAddress);
        		if (ats == null) ats = new ArrayList<OnmsAtInterface>();
        		LogUtils.infof(this, "parseAtNodes: Adding to discoverable atinterface.");
        		ats.add(at);
        		m_macToAtinterface.put(macAddress, ats);
        		LogUtils.debugf(this, "parseAtNodes: mac: %s now has atinterface reference: %d", macAddress, ats.size());
        	}		
        }
        LogUtils.debugf(this, "DiscoveryLink.run: end populate macToAtinterface");
    }

	private int getIfIndexFromRouter(LinkableNode parentnode, InetAddress nextHopNet) {

		if (!parentnode.hasRouteInterfaces())
			return -1;
		Iterator<RouterInterface> ite = parentnode.getRouteInterfaces().iterator();
		while (ite.hasNext()) {
			RouterInterface curIface = ite.next();

			if (curIface.getMetric() == -1) {
				continue;
			}

			int ifindex = curIface.getIfindex();

			if (ifindex == 0 || ifindex == -1)
				continue;

			if (curIface.getRouteNet().equals(nextHopNet)) return ifindex;
		}
		return -1;
	}

	/**
	 * 
	 * @param nodeid
	 * @return LinkableSnmpNode or null if not found
	 */

	boolean isBridgeNode(int nodeid) {
	    for (final LinkableNode curNode : m_bridgeNodes.values()) {
			if (nodeid == curNode.getNodeId())
				return true;
		}
		return false;
	}

	/**
	 * 
	 * @param nodeid
	 * @return true if found
	 */

	boolean isRouterNode(int nodeid) {
	    for (final LinkableNode curNode : m_routerNodes) {
			if (nodeid == curNode.getNodeId())
				return true;
		}
		return false;
	}

	/**
	 * 
	 * @param nodeid
	 * @return true if found
	 */

	boolean isCdpNode(int nodeid) {

		Iterator<LinkableNode> ite = m_cdpNodes.iterator();
		while (ite.hasNext()) {
			LinkableNode curNode = ite.next();
			if (nodeid == curNode.getNodeId())
				return true;
		}
		return false;
	}

	private boolean isEndBridgePort(LinkableNode bridge, int bridgeport){

		Set<String> macsOnBridge = bridge.getMacAddressesOnBridgePort(bridgeport);

		if (macsOnBridge == null || macsOnBridge.isEmpty())
			return true;

		for (final String macaddr : macsOnBridge) {
			if (isMacIdentifierOfBridgeNode(macaddr)) return false;
		}

		return true;
	}
	
	private boolean isNearestBridgeLink(LinkableNode bridge1, int bp1,
			LinkableNode bridge2, int bp2) {

		boolean hasbridge2forwardingRule = false;
		Set<String> macsOnBridge2 = bridge2.getMacAddressesOnBridgePort(bp2);

		Set<String> macsOnBridge1 = bridge1.getMacAddressesOnBridgePort(bp1);

		if (macsOnBridge2 == null || macsOnBridge1 == null)
			return false;

		if (macsOnBridge2.isEmpty() || macsOnBridge1.isEmpty())
			return false;

		for (final String curMacOnBridge1 : macsOnBridge1) {
			// if mac address is bridge identifier of bridge 2 continue
			
			if (bridge2.isBridgeIdentifier(curMacOnBridge1)) {
				hasbridge2forwardingRule = true;
				continue;
			}
			// if mac address is itself identifier of bridge1 continue
			if (bridge1.isBridgeIdentifier(curMacOnBridge1))
				continue;
			// then no identifier of bridge one no identifier of bridge 2
			// bridge 2 contains  
			if (macsOnBridge2.contains(curMacOnBridge1)
					&& isMacIdentifierOfBridgeNode(curMacOnBridge1))
				return false;
		}

		return hasbridge2forwardingRule;
	}

	private Set<String> getMacsOnBridgeLink(LinkableNode bridge1, int bp1,
			LinkableNode bridge2, int bp2) {

		Set<String> macsOnLink = new HashSet<String>();

    	Set<String> macsOnBridge1 = bridge1.getMacAddressesOnBridgePort(bp1);

		Set<String> macsOnBridge2 = bridge2.getMacAddressesOnBridgePort(bp2);

		if (macsOnBridge2 == null || macsOnBridge1 == null)
			return null;

		if (macsOnBridge2.isEmpty() || macsOnBridge1.isEmpty())
			return null;

		for (final String curMacOnBridge1 : macsOnBridge1) {
			if (bridge2.isBridgeIdentifier(curMacOnBridge1))
				continue;
			if (macsOnBridge2.contains(curMacOnBridge1))
				macsOnLink.add(curMacOnBridge1);
		}
		return macsOnLink;
	}

	private boolean isMacIdentifierOfBridgeNode(String macAddress) {
	    for (final LinkableNode curNode : m_bridgeNodes.values()) {
			if (curNode.isBridgeIdentifier(macAddress))
				return true;
		}
		return false;
	}

	/**
	 * 
	 * @param stpportdesignatedbridge
	 * @return Bridge Bridge Node if found else null
	 */

	private LinkableNode getNodeFromMacIdentifierOfBridgeNode(final String macAddress) {
	    for (final LinkableNode curNode : m_bridgeNodes.values()) {
			if (curNode.isBridgeIdentifier(macAddress))
				return curNode;
		}
		return null;
	}

	private List<LinkableNode> getBridgesFromMacs(final Set<String> macs) {
		List<LinkableNode> bridges = new ArrayList<LinkableNode>();
		for (final LinkableNode curNode : m_bridgeNodes.values()) {
		    for (final String curBridgeIdentifier : curNode.getBridgeIdentifiers()) {
				if (macs.contains((curBridgeIdentifier)))
					bridges.add(curNode);
			}
		}
		return bridges;
	}

	private int getBridgePortOnEndBridge(final LinkableNode startBridge, final LinkableNode endBridge) {

		int port = -1;
		for (final String curBridgeIdentifier : startBridge.getBridgeIdentifiers()) {
		    LogUtils.debugf(this, "getBridgePortOnEndBridge: parsing bridge identifier "
								+ curBridgeIdentifier);
			
			if (endBridge.hasMacAddress(curBridgeIdentifier)) {
			    for (final Integer p : endBridge.getBridgePortsFromMac(curBridgeIdentifier)) {
			        port = p;
					if (endBridge.isBackBoneBridgePort(port)) {
					    LogUtils.debugf(this, "getBridgePortOnEndBridge: found backbone bridge port "
											+ port
											+ " .... Skipping");
						continue;
					}
					if (port == -1) {
					    LogUtils.debugf(this, "run: no port found on bridge nodeid "
											+ endBridge.getNodeId()
											+ " for node bridge identifiers nodeid "
											+ startBridge.getNodeId()
											+ " . .....Skipping");
						continue;
					}
					LogUtils.debugf(this, "run: using mac address table found bridge port "
										+ port
										+ " on node "
										+ endBridge.getNodeId());
					return port;
				}
					
			} else {
			    LogUtils.debugf(this, "run: bridge identifier not found on node "
									+ endBridge.getNodeId());
			}
		}
		return -1;
	}

	
	/**
	 * Return the Scheduler
	 *
	 * @return a {@link org.opennms.netmgt.linkd.scheduler.Scheduler} object.
	 */
	public Scheduler getScheduler() {
		return m_scheduler;
	}

	/**
	 * Set the Scheduler
	 *
	 * @param scheduler a {@link org.opennms.netmgt.linkd.scheduler.Scheduler} object.
	 */
	public void setScheduler(Scheduler scheduler) {
		m_scheduler = scheduler;
	}

	/**
	 * This Method is called when DiscoveryLink is initialized
	 */
	public void schedule() {
		if (m_scheduler == null)
			throw new IllegalStateException(
					"schedule: Cannot schedule a service whose scheduler is set to null");

		m_scheduler.schedule(snmp_poll_interval + discovery_interval
				+ initial_sleep_time, this);
	}

	/**
	 * Schedule again the job
	 * 
	 * @return
	 */

	private void reschedule() {
		if (m_scheduler == null)
			throw new IllegalStateException(
					"rescedule: Cannot schedule a service whose scheduler is set to null");
		m_scheduler.schedule(snmp_poll_interval, this);
	}

	/**
	 * <p>getInitialSleepTime</p>
	 *
	 * @return Returns the initial_sleep_time.
	 */
	public long getInitialSleepTime() {
		return initial_sleep_time;
	}

	/**
	 * <p>setInitialSleepTime</p>
	 *
	 * @param initial_sleep_time
	 *            The initial_sleep_timeto set.
	 */
	public void setInitialSleepTime(long initial_sleep_time) {
		this.initial_sleep_time = initial_sleep_time;
	}

	/**
	 * <p>isReady</p>
	 *
	 * @return a boolean.
	 */
	public boolean isReady() {
		return true;
	}

	/**
	 * <p>getDiscoveryInterval</p>
	 *
	 * @return Returns the discovery_link_interval.
	 */
	public long getDiscoveryInterval() {
		return discovery_interval;
	}

	/**
	 * <p>setSnmpPollInterval</p>
	 *
	 * @param interval
	 *            The discovery_link_interval to set.
	 */
	public void setSnmpPollInterval(long interval) {
		this.snmp_poll_interval = interval;
	}

	/**
	 * <p>getSnmpPollInterval</p>
	 *
	 * @return Returns the discovery_link_interval.
	 */
	public long getSnmpPollInterval() {
		return snmp_poll_interval;
	}

	/**
	 * <p>setDiscoveryInterval</p>
	 *
	 * @param interval
	 *            The discovery_link_interval to set.
	 */
	public void setDiscoveryInterval(long interval) {
		this.discovery_interval = interval;
	}

	/**
	 * <p>Getter for the field <code>links</code>.</p>
	 *
	 * @return an array of {@link org.opennms.netmgt.linkd.NodeToNodeLink} objects.
	 */
	public NodeToNodeLink[] getLinks() {
		return m_links.toArray(new NodeToNodeLink[0]);
	}

	/**
	 * <p>getMacLinks</p>
	 *
	 * @return an array of {@link org.opennms.netmgt.linkd.MacToNodeLink} objects.
	 */
	public MacToNodeLink[] getMacLinks() {
		return m_maclinks.toArray(new MacToNodeLink[0]);
	}

	/**
	 * <p>isSuspended</p>
	 *
	 * @return Returns the suspendCollection.
	 */
	public boolean isSuspended() {
		return suspendCollection;
	}

	/**
	 * <p>suspend</p>
	 */
	public void suspend() {
		this.suspendCollection = true;
	}

	/**
	 * <p>wakeUp</p>
	 */
	public void wakeUp() {
		this.suspendCollection = false;
	}

	/**
	 * <p>unschedule</p>
	 */
	public void unschedule() {
		if (m_scheduler == null)
			throw new IllegalStateException(
					"rescedule: Cannot schedule a service whose scheduler is set to null");
		if (isRunned) {
			m_scheduler.unschedule(this, snmp_poll_interval);
		} else {
			m_scheduler.unschedule(this, snmp_poll_interval
					+ initial_sleep_time + discovery_interval);
		}
	}
	
	private boolean parseCdpLinkOn(LinkableNode node1,int ifindex1,
								int nodeid2) {

		int bridgeport = node1.getBridgePort(ifindex1);

		if (node1.isBackBoneBridgePort(bridgeport)) {
		    LogUtils.debugf(this, "parseCdpLinkOn: node/backbone bridge port "
						+ node1.getNodeId() +"/" +bridgeport
						+ " already parsed. Skipping");
			return false;
		}

		if (isEndBridgePort(node1, bridgeport)) {

			node1.addBackBoneBridgePorts(bridgeport);
			m_bridgeNodes.put(node1.getNodeId(), node1);
			
			Set<String> macs = node1.getMacAddressesOnBridgePort(bridgeport);
			addLinks(macs,node1.getNodeId(),ifindex1);
		} else {
		    LogUtils.warnf(this, "parseCdpLinkOn: link cannot be saved. Skipping");
			return false;
		}


		return true;
	}

	private boolean parseCdpLinkOn(LinkableNode node1,int ifindex1,
								LinkableNode node2,int ifindex2) {
		
		int bridgeport1 = node1.getBridgePort(ifindex1);

		if (node1.isBackBoneBridgePort(bridgeport1)) {
		    LogUtils.debugf(this, "parseCdpLinkOn: backbone bridge port "
						+ bridgeport1
						+ " already parsed. Skipping");
			return false;
		}
		
		int bridgeport2 = node2
				.getBridgePort(ifindex2);
		if (node2.isBackBoneBridgePort(bridgeport2)) {
		    LogUtils.debugf(this, "parseCdpLinkOn: backbone bridge port "
						+ bridgeport2
						+ " already parsed. Skipping");
			return false;
		}

		if (isNearestBridgeLink(node1, bridgeport1,
				node2, bridgeport2)) {

			node1.addBackBoneBridgePorts(bridgeport1);
			m_bridgeNodes.put(node1.getNodeId(), node1);

			node2.addBackBoneBridgePorts(bridgeport2);
			m_bridgeNodes.put(node2.getNodeId(),node2);

			
			LogUtils.debugf(this, "parseCdpLinkOn: Adding node on links.");
			addLinks(getMacsOnBridgeLink(node1,
					bridgeport1, node2, bridgeport2),node1.getNodeId(),ifindex1);
		} else {
		    LogUtils.debugf(this, "parseCdpLinkOn: link found not on nearest. Skipping");
			return false;
		}
		return true;
	} 	

	private void addNodetoNodeLink(NodeToNodeLink nnlink) {
		if (nnlink == null)
		{
				LogUtils.warnf(this, "addNodetoNodeLink: node link is null.");
				return;
		}
		if (!m_links.isEmpty()) {
			Iterator<NodeToNodeLink> ite = m_links.iterator();
			while (ite.hasNext()) {
				NodeToNodeLink curNnLink = ite.next();
				if (curNnLink.equals(nnlink)) {
				    LogUtils.infof(this, "addNodetoNodeLink: link %s exists, not adding", nnlink.toString());
					return;
				}
			}
		}
		
		LogUtils.debugf(this, "addNodetoNodeLink: adding link %s", nnlink.toString());
		m_links.add(nnlink);
	}

	private void addLinks(Set<String> macs,int nodeid,int ifindex) { 
		if (macs == null || macs.isEmpty()) {
		    LogUtils.debugf(this, "addLinks: mac's list on link is empty.");
		} else {
			Iterator<String> mac_ite = macs.iterator();

			while (mac_ite.hasNext()) {
				String curMacAddress = mac_ite
						.next();
				if (m_macsParsed.contains(curMacAddress)) {
				    LogUtils.warnf(this, "addLinks: mac address "
									+ curMacAddress
									+ " just found on other bridge port! Skipping...");
					continue;
				}
				
				if (macsExcluded.contains(curMacAddress)) {
				    LogUtils.warnf(this, "addLinks: mac address "
									+ curMacAddress
									+ " is excluded from discovery package! Skipping...");
					continue;
				}
				
				if (m_macToAtinterface.containsKey(curMacAddress)) {
					List<OnmsAtInterface> ats = m_macToAtinterface.get(curMacAddress);
					Iterator<OnmsAtInterface> ite = ats.iterator();
					while (ite.hasNext()) {
						OnmsAtInterface at = ite.next();
						NodeToNodeLink lNode = new NodeToNodeLink(at.getNode().getId(),at.getIfIndex());
						lNode.setNodeparentid(nodeid);
						lNode.setParentifindex(ifindex);
						addNodetoNodeLink(lNode);
					}
				} else {
				    LogUtils.debugf(this, "addLinks: not find nodeid for ethernet mac address %s found on node/ifindex %d/%d", curMacAddress, nodeid, ifindex);
					MacToNodeLink lMac = new MacToNodeLink(curMacAddress);
					lMac.setNodeparentid(nodeid);
					lMac.setParentifindex(ifindex);
					m_maclinks.add(lMac);
				}
				m_macsParsed.add(curMacAddress);
			}
		}
	}
	
	/** {@inheritDoc} */
	public boolean equals(ReadyRunnable r) {
		return (r instanceof DiscoveryLink && this.getPackageName().equals(r.getPackageName()));
	}
	
	/**
	 * <p>getInfo</p>
	 *
	 * @return a {@link java.lang.String} object.
	 */
	public String getInfo() {
		return " Ready Runnable Discovery Link discoveryUsingBridge/discoveryUsingCdp/discoveryUsingRoutes/package: "
		+ discoveryUsingBridge() + "/"
		+ discoveryUsingCdp() + "/"
		+ discoveryUsingRoutes() + "/"+ getPackageName();
	}

	/**
	 * <p>discoveryUsingBridge</p>
	 *
	 * @return a boolean.
	 */
	public boolean discoveryUsingBridge() {
		return discoveryUsingBridge;
	}

	/**
	 * <p>Setter for the field <code>discoveryUsingBridge</code>.</p>
	 *
	 * @param discoveryUsingBridge a boolean.
	 */
	public void setDiscoveryUsingBridge(boolean discoveryUsingBridge) {
		this.discoveryUsingBridge = discoveryUsingBridge;
	}

	/**
	 * <p>discoveryUsingCdp</p>
	 *
	 * @return a boolean.
	 */
	public boolean discoveryUsingCdp() {
		return discoveryUsingCdp;
	}

	/**
	 * <p>Setter for the field <code>discoveryUsingCdp</code>.</p>
	 *
	 * @param discoveryUsingCdp a boolean.
	 */
	public void setDiscoveryUsingCdp(boolean discoveryUsingCdp) {
		this.discoveryUsingCdp = discoveryUsingCdp;
	}

	/**
	 * <p>discoveryUsingRoutes</p>
	 *
	 * @return a boolean.
	 */
	public boolean discoveryUsingRoutes() {
		return discoveryUsingRoutes;
	}

	/**
	 * <p>Setter for the field <code>discoveryUsingRoutes</code>.</p>
	 *
	 * @param discoveryUsingRoutes a boolean.
	 */
	public void setDiscoveryUsingRoutes(boolean discoveryUsingRoutes) {
		this.discoveryUsingRoutes = discoveryUsingRoutes;
	}

	/**
	 * <p>Getter for the field <code>packageName</code>.</p>
	 *
	 * @return a {@link java.lang.String} object.
	 */
	public String getPackageName() {
		return packageName;
	}

	/** {@inheritDoc} */
	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}
	
	/* 
	 * This method is useful to get forwarding table
	 * for switch failed
	 */

	private void snmpParseBridgeNodes() {
	    LogUtils.debugf(this, "parseBridgeNodes: searching bridge port for bridge identifier not yet already found. Iterating on bridge nodes.");
		
		List<LinkableNode> bridgenodeschanged = new ArrayList<LinkableNode>();
		Iterator<LinkableNode> ite = m_bridgeNodes.values().iterator();
		while (ite.hasNext()) {
			LinkableNode curNode = ite.next();
			LogUtils.debugf(this, "parseBridgeNodes: parsing bridge: %d/%s", curNode.getNodeId(), curNode.getSnmpPrimaryIpAddr());

			// get macs
			
			final List<String> macs = getNotAlreadyFoundMacsOnNode(curNode);

			if (macs.isEmpty()) continue;

			SnmpAgentConfig agentConfig = null;

			String className = null;
			
			final LinkdConfig linkdConfig = m_linkd.getLinkdConfig();
			linkdConfig.getReadLock().lock();

			try {
                boolean useVlan = linkdConfig.isVlanDiscoveryEnabled();
    			if (linkdConfig.getPackage(getPackageName()).hasEnableVlanDiscovery()) {
    				useVlan = linkdConfig.getPackage(getPackageName()).getEnableVlanDiscovery();
    			}
    			
    			if (useVlan && linkdConfig.hasClassName(curNode.getSysoid())) {
    				className = linkdConfig.getVlanClassName(curNode.getSysoid());
    			}
    
				final InetAddress addr = InetAddressUtils.addr(curNode.getSnmpPrimaryIpAddr());
				if (addr == null) {
    			    LogUtils.errorf(this, "parseBridgeNodes: Failed to load SNMP parameter from SNMP configuration file.");
    				return;
				}
				agentConfig = SnmpPeerFactory.getInstance().getAgentConfig(addr);
    			
    			String community = agentConfig.getReadCommunity();
    			
    			for (final String mac : macs) {
    				LogUtils.debugf(this, "parseBridgeNodes: parsing mac: %s", mac);
    
    				if (className != null && (className.equals("org.opennms.netmgt.linkd.snmp.CiscoVlanTable") 
    						|| className.equals("org.opennms.netmgt.linkd.snmp.IntelVlanTable"))){
    					Iterator<OnmsVlan> vlan_ite = curNode.getVlans().iterator();
    					while (vlan_ite.hasNext()) {
    						OnmsVlan vlan = vlan_ite.next();
    						if (vlan.getVlanStatus() != VlanCollectorEntry.VLAN_STATUS_OPERATIONAL || vlan.getVlanType() != VlanCollectorEntry.VLAN_TYPE_ETHERNET) {
    						    LogUtils.debugf(this, "parseBridgeNodes: skipping vlan: %s", vlan.getVlanName());
    							continue;
    						}
    						agentConfig.setReadCommunity(community+"@"+vlan.getVlanId());
    						curNode = collectMacAddress(agentConfig, curNode, mac, vlan.getVlanId());
    						agentConfig.setReadCommunity(community);
    					}
    				} else {
    					int vlan = SnmpCollection.DEFAULT_VLAN_INDEX;
    					if (useVlan) vlan = SnmpCollection.TRUNK_VLAN_INDEX;
    					curNode = collectMacAddress(agentConfig, curNode, mac, vlan);
    				}
    			}
    			bridgenodeschanged.add(curNode);
			} finally {
			    linkdConfig.getReadLock().unlock();
			}
		}
		
		ite = bridgenodeschanged.iterator();
		while (ite.hasNext()) {
			LinkableNode node = ite.next();
			m_bridgeNodes.put(node.getNodeId(), node);
		}

	}
	
	private LinkableNode collectMacAddress(SnmpAgentConfig agentConfig, LinkableNode node,String mac,int vlan) {
		FdbTableGet coll = new FdbTableGet(agentConfig,mac);
		LogUtils.infof(this, "collectMacAddress: finding entry in bridge forwarding table for mac on node: %s/%d", mac, node.getNodeId());
		int bridgeport = coll.getBridgePort();
		if (bridgeport > 0 && coll.getBridgePortStatus() == QueryManager.SNMP_DOT1D_FDB_STATUS_LEARNED) {
			node.addMacAddress(bridgeport, mac, Integer.toString(vlan));
			LogUtils.infof(this, "collectMacAddress: found mac on bridge port: %d", bridgeport);
		} else {
			bridgeport = coll.getQBridgePort();
			if (bridgeport > 0 && coll.getQBridgePortStatus() == QueryManager.SNMP_DOT1D_FDB_STATUS_LEARNED) {
				node.addMacAddress(bridgeport, mac, Integer.toString(vlan));
				LogUtils.infof(this, "collectMacAddress: found mac on bridge port: %d", bridgeport);
			} else {
			    LogUtils.infof(this, "collectMacAddress: mac not found: %d", bridgeport);
			}
		}
		return node;
	}
	
	private List<String> getNotAlreadyFoundMacsOnNode(LinkableNode node){
	    LogUtils.debugf(this, "Searching Not Yet Found Bridge Identifier Occurrence on Node: %d", node.getNodeId());
		List<String> macs = new ArrayList<String>();
		Iterator<LinkableNode> ite = m_bridgeNodes.values().iterator();
		while (ite.hasNext()) {
			LinkableNode curNode = ite.next();
			if (node.getNodeId() == curNode.getNodeId()) continue;
			Iterator<String> mac_ite =curNode.getBridgeIdentifiers().iterator();
			while (mac_ite.hasNext()) {
				String curMac = mac_ite.next();
				if (node.hasMacAddress(curMac)) continue;
				if (macs.contains(curMac)) continue;
				LogUtils.debugf(this, "Found a node/Bridge Identifier %d/%s that was not found in bridge forwarding table for bridge node: %d", curNode.getNodeId(), curMac, node.getNodeId());
				macs.add(curMac);
			}
		}

		LogUtils.debugf(this, "Searching Not Yet Found Mac Address Occurrence on Node: %d", node.getNodeId());

		Iterator<String> mac_ite = m_macToAtinterface.keySet().iterator();
		while (mac_ite.hasNext()) {
			String curMac = mac_ite.next();
			if (node.hasMacAddress(curMac)) continue;
			if (macs.contains(curMac)) continue;
			LogUtils.debugf(this, "Found a Mac Address %s that was not found in bridge forwarding table for bridge node: %d", curMac, node.getNodeId());
			macs.add(curMac);
		}
		
		return macs;
	}

	/**
	 * <p>isEnableDownloadDiscovery</p>
	 *
	 * @return a boolean.
	 */
	public boolean isEnableDownloadDiscovery() {
		return enableDownloadDiscovery;
	}

	/**
	 * <p>Setter for the field <code>enableDownloadDiscovery</code>.</p>
	 *
	 * @param enableDownloaddiscovery a boolean.
	 */
	public void setEnableDownloadDiscovery(boolean enableDownloaddiscovery) {
		this.enableDownloadDiscovery = enableDownloaddiscovery;
	}

	/**
	 * <p>isForceIpRouteDiscoveryOnEtherNet</p>
	 *
	 * @return a boolean.
	 */
	public boolean isForceIpRouteDiscoveryOnEtherNet() {
		return forceIpRouteDiscoveryOnEtherNet;
	}

	/**
	 * <p>Setter for the field <code>forceIpRouteDiscoveryOnEtherNet</code>.</p>
	 *
	 * @param forceIpRouteDiscoveryOnEtherNet a boolean.
	 */
	public void setForceIpRouteDiscoveryOnEtherNet(
			boolean forceIpRouteDiscoveryOnEtherNet) {
		this.forceIpRouteDiscoveryOnEtherNet = forceIpRouteDiscoveryOnEtherNet;
	}

}
