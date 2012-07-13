/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2011 The OpenNMS Group, Inc.
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

package org.opennms.rest.client;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.test.MockLogAppender;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.core.test.http.annotations.JUnitHttpServer;
import org.opennms.core.test.http.annotations.Webapp;
import org.opennms.netmgt.dao.DatabasePopulator;
import org.opennms.netmgt.dao.db.JUnitConfigurationEnvironment;
import org.opennms.netmgt.dao.db.JUnitTemporaryDatabase;
import org.opennms.rest.client.internal.JerseyClientImpl;
import org.opennms.rest.client.internal.JerseyDataLinkInterfaceService;
import org.opennms.rest.client.internal.model.ClientDataLinkInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@RunWith(OpenNMSJUnit4ClassRunner.class)
@ContextConfiguration(locations= {
        "classpath:/META-INF/opennms/applicationContext-dao.xml",
        "classpath:/META-INF/opennms/applicationContext-commonConfigs.xml",
        "classpath:/META-INF/opennms/applicationContext-daemon.xml",
        "classpath:/META-INF/opennms/mockEventIpcManager.xml",
        "classpath:/META-INF/opennms/applicationContext-databasePopulator.xml",
        "classpath:/META-INF/opennms/applicationContext-setupIpLike-enabled.xml",
        "classpath:/META-INF/opennms/applicationContext-minimal-conf.xml"
})
@JUnitConfigurationEnvironment
@JUnitTemporaryDatabase
@JUnitHttpServer(port = 10342, webapps = @Webapp(context = "/opennms", path = "src/test/resources/opennmsRestWebServices"))
public class ClientDataLinkInterfaceServiceTest {
    @Autowired
    private DatabasePopulator m_databasePopulator;
    
    private JerseyDataLinkInterfaceService m_datalinkinterfaceservice;
    
    @Before
    public void setUp() throws Exception {
        MockLogAppender.setupLogging(true, "DEBUG");
        m_datalinkinterfaceservice = new JerseyDataLinkInterfaceService();
        JerseyClientImpl jerseyClient = new JerseyClientImpl(
                                                         "http://127.0.0.1:10342/opennms/rest/","demo","demo");
        m_datalinkinterfaceservice.setJerseyClient(jerseyClient);
        m_databasePopulator.populateDatabase();        
    }

    @After
    public void tearDown() throws Exception {
        MockLogAppender.assertNoWarningsOrGreater();
    }
    
    @Test
    public void testLinks() throws Exception {
        
        
        
        List<ClientDataLinkInterface> datalinkinterfacelist = m_datalinkinterfaceservice.getAll();
        assertTrue(3 == datalinkinterfacelist.size());
        
        ClientDataLinkInterface datalinkinterface = m_datalinkinterfaceservice.get(64);
        assertTrue(64 == Integer.parseInt(datalinkinterface.getId()));

        datalinkinterface = m_datalinkinterfaceservice.get(65);
        assertTrue(65 == Integer.parseInt(datalinkinterface.getId()));

        datalinkinterface = m_datalinkinterfaceservice.get(66);
        assertTrue(66 == Integer.parseInt(datalinkinterface.getId()));

        String xml = m_datalinkinterfaceservice.getXml("");
        assertTrue(xml.contains("count=\"3\""));
 
        xml = m_datalinkinterfaceservice.getXml("64");
        assertTrue(xml.contains("id=\"64\""));
 
    }

}
