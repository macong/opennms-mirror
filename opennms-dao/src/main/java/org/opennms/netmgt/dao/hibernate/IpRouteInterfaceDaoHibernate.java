//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2005 The OpenNMS Group, Inc.  All rights reserved.
// OpenNMS(R) is a derivative work, containing both original code, included code and modified
// code that was published under the GNU General Public License. Copyrights for modified 
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Modifications:
//
// 2008 Jan 26: Add getInterfacesForNodes. - dj@opennms.org
//
// Original code base Copyright (C) 1999-2001 Oculan Corp.  All rights reserved.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.                                                            
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//    
// For more information contact: 
//   OpenNMS Licensing       <license@opennms.org>
//   http://www.opennms.org/
//   http://www.opennms.com/
//
// Tab Size = 8

package org.opennms.netmgt.dao.hibernate;

import java.sql.Timestamp;

import org.hibernate.criterion.Restrictions;
import org.opennms.netmgt.dao.IpRouteInterfaceDao;
import org.opennms.netmgt.model.OnmsCriteria;
import org.opennms.netmgt.model.OnmsIpRouteInterface;

public class IpRouteInterfaceDaoHibernate extends AbstractDaoHibernate<OnmsIpRouteInterface, Integer>  implements IpRouteInterfaceDao {
    
    public IpRouteInterfaceDaoHibernate() {
        super(OnmsIpRouteInterface.class);
    }

	@Override
	public void markDeletedIfNodeDeleted() {
		final OnmsCriteria criteria = new OnmsCriteria(OnmsIpRouteInterface.class);
        criteria.createAlias("node", "node", OnmsCriteria.LEFT_JOIN);
        criteria.add(Restrictions.eq("node.type", "D"));
        
        for (final OnmsIpRouteInterface ipRouteIface : findMatching(criteria)) {
        	ipRouteIface.setStatus('D');
        	saveOrUpdate(ipRouteIface);
        }
	}

    @Override
    public void deactivateForNodeIdIfOlderThan(final int nodeid, final Timestamp scanTime) {
        final OnmsCriteria criteria = new OnmsCriteria(OnmsIpRouteInterface.class);
        criteria.createAlias("node", "node", OnmsCriteria.LEFT_JOIN);
        criteria.add(Restrictions.eq("node.id", nodeid));
        criteria.add(Restrictions.lt("lastPollTime", scanTime));
        criteria.add(Restrictions.eq("status", "A"));
        
        for (final OnmsIpRouteInterface item : findMatching(criteria)) {
            item.setStatus('N');
            saveOrUpdate(item);
        }
    }

    @Override
    public void setStatusForNode(final Integer nodeid, final Character action) {
        // UPDATE iprouteinterface set status = ? WHERE nodeid = ?

        final OnmsCriteria criteria = new OnmsCriteria(OnmsIpRouteInterface.class);
        criteria.createAlias("node", "node", OnmsCriteria.LEFT_JOIN);
        criteria.add(Restrictions.eq("node.id", nodeid));
        
        for (final OnmsIpRouteInterface item : findMatching(criteria)) {
            item.setStatus(action);
            saveOrUpdate(item);
        }
    }

    @Override
    public void setStatusForNodeAndIfIndex(final Integer nodeid, final Integer ifIndex, final Character action) {
        // UPDATE iprouteinterface set status = ? WHERE nodeid = ? AND routeifindex = ?

        final OnmsCriteria criteria = new OnmsCriteria(OnmsIpRouteInterface.class);
        criteria.createAlias("node", "node", OnmsCriteria.LEFT_JOIN);
        criteria.add(Restrictions.eq("node.id", nodeid));
        criteria.add(Restrictions.eq("routeIfIndex", ifIndex));
        
        for (final OnmsIpRouteInterface item : findMatching(criteria)) {
            item.setStatus(action);
            saveOrUpdate(item);
        }
    }

}
