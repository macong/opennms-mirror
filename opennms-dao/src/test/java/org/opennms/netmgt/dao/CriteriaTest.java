/*******************************************************************************
 * This file is part of the OpenNMS(R) Application.
 *
 * OpenNMS(R) is Copyright (C) 1999-2011 The OpenNMS Group, Inc.  All rights reserved.
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 *     along with OpenNMS(R).  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information contact: 
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/
package org.opennms.netmgt.dao;

import static org.junit.Assert.assertEquals;

import java.util.Collection;

import org.hibernate.criterion.Restrictions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.netmgt.dao.db.JUnitConfigurationEnvironment;
import org.opennms.netmgt.dao.db.JUnitTemporaryDatabase;
import org.opennms.netmgt.dao.db.OpenNMSJUnit4ClassRunner;
import org.opennms.netmgt.model.OnmsCriteria;
import org.opennms.netmgt.model.OnmsNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

@RunWith(OpenNMSJUnit4ClassRunner.class)
@ContextConfiguration(locations={
        "classpath:/META-INF/opennms/applicationContext-dao.xml",
        "classpath:/META-INF/opennms/applicationContext-databasePopulator.xml",
        "classpath:/META-INF/opennms/applicationContext-setupIpLike-enabled.xml",
        "classpath*:/META-INF/opennms/component-dao.xml"
})
@JUnitConfigurationEnvironment
@JUnitTemporaryDatabase
public class CriteriaTest {

	@Autowired
    private NodeDao m_nodeDao;

	@Autowired
	private DatabasePopulator m_databasePopulator;
	
	@Before
	public void setUp() {
		m_databasePopulator.populateDatabase();
	}

	@Test
	@Transactional
	public void testSimple() {
        OnmsCriteria crit = new OnmsCriteria(OnmsNode.class);
        crit.add(Restrictions.eq("label", "node1"));
        
        Collection<OnmsNode> matching = m_nodeDao.findMatching(crit);
        
        assertEquals("Expect a single node with label node1", 1, matching.size());
        
        OnmsNode node = matching.iterator().next();
        assertEquals("node1", node.getLabel());
        assertEquals(4, node.getIpInterfaces().size());
    }
    
    @Test
	@Transactional
	public void testComplicated() {
        OnmsCriteria crit = 
            new OnmsCriteria(OnmsNode.class)
            .createAlias("ipInterfaces", "iface")
            .add(Restrictions.eq("iface.ipAddress", "192.168.2.1"));
        
        Collection<OnmsNode> matching = m_nodeDao.findMatching(crit);
        
        assertEquals("Expect a single node with an interface 192.168.2.1", 1, matching.size());
        
        OnmsNode node = matching.iterator().next();
        assertEquals("node2", node.getLabel());
        assertEquals(3, node.getIpInterfaces().size());
            
    }
}