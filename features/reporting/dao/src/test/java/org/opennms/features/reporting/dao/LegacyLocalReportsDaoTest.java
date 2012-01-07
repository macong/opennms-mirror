/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2007-2011 The OpenNMS Group, Inc.
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

package org.opennms.features.reporting.dao;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.features.reporting.model.basicreport.BasicReportDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.Assert.*;

/**
 * <p>LegacyLocalReportsDaoTest class.</p>
 *
 * @author Ronny Trommer <ronny@opennms.org>
 * @version $Id: $
 * @since 1.8.1
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:reportingDaoTest-context.xml"})
public class LegacyLocalReportsDaoTest {

    /**
     * Local report data access object to test
     */
    @Autowired
    private LegacyLocalReportsDao m_legacyLocalReportsDao;

    /**
     * List with all configured online reports
     */
    private List<BasicReportDefinition> m_onlineReports;

    /**
     * List with all configured reports
     */
    private List<BasicReportDefinition> m_reports;

    /**
     * Name of the configuration file
     */
    private String m_configFile;

    /**
     * Initialize the configuration file. Check if the configuration file exist. Try retrieve the reports from
     * configuration.
     *
     * @throws Exception
     */
    @Before
    public void sanityCheck() throws Exception {
        // Injected configuration
        assertNotNull("Inject legacy local report data access.", m_legacyLocalReportsDao);

        // Configuration file tests
        assertNotNull("Config file not null", m_legacyLocalReportsDao.getConfigResource());

        m_configFile = m_legacyLocalReportsDao.getConfigResource().getFile().getAbsolutePath();
        assertTrue("Config file " + m_configFile + " exist", m_legacyLocalReportsDao.getConfigResource().exists());
        assertTrue("Config file " + m_configFile + " is readable", m_legacyLocalReportsDao.getConfigResource().isReadable());

        // Unmarshal with JAXB to load XML into POJO's
        m_legacyLocalReportsDao.afterPropertiesSet();

        // Read reports which are configured as online reports
        m_onlineReports = m_legacyLocalReportsDao.getOnlineReports();
        assertNotNull("Test to retrieve 2 online reports from " + m_configFile, m_onlineReports);
        assertEquals("Test 2 configured online reports.", 2, m_onlineReports.size());
        assertFalse("Online reports from " + m_configFile + " is empty.", m_legacyLocalReportsDao.getReports().isEmpty());

        // Read all configured reports
        m_reports = m_legacyLocalReportsDao.getReports();
        assertNotNull("Test to retrieve 3 online reports from " + m_configFile, m_reports);
        assertFalse("Reports from " + m_configFile + " is empty.", m_legacyLocalReportsDao.getReports().isEmpty());
        assertEquals("Test 3 configured online reports.", 3, m_reports.size());
    }

    /**
     * Tests to retrieve all descriptions from report configuration file
     *
     * @throws Exception
     */
    @Test
    public void testReportDescription() throws Exception {
        assertEquals("First report description test", "sample Jasper report using jdbc datasource", m_reports.get(0).getDescription());
        assertEquals("Second report description test", "online sample Jasper report using jdbc datasource", m_reports.get(1).getDescription());
        assertEquals("Third report description test", "NOT online sample Jasper report using jdbc datasource", m_reports.get(2).getDescription());
    }

    /**
     * Tests to retrieve all IDs from report file
     *
     * @throws Exception
     */
    @Test
    public void testReportIds() throws Exception {
        assertEquals("First report id test", "sample-report", m_reports.get(0).getId());
        assertEquals("Second report id test", "online-sample-report", m_reports.get(1).getId());
        assertEquals("Third report id test", "not-online-sample-report", m_reports.get(2).getId());
    }

    /**
     * Test to retrieve all DisplayNames from report configuration file
     *
     * @throws Exception
     */
    @Test
    public void testReportDisplayName() throws Exception {
        assertEquals("First report display name test", "sample JasperReport", m_reports.get(0).getDisplayName());
        assertEquals("Second report display name test", "online sample JasperReport", m_reports.get(1).getDisplayName());
        assertEquals("Third report display name test", "NOT online sample JasperReport", m_reports.get(2).getDisplayName());
    }

    /**
     * Test to retrieve all report services from report configuration file
     *
     * @throws Exception
     */
    @Test
    public void testReportService() throws Exception {
        assertEquals("First report report-service test", "jasperReportService", m_reports.get(0).getReportService());
        assertEquals("Second report report-service test", "jasperReportService", m_reports.get(1).getReportService());
        assertEquals("Third report report-service test", "jasperReportService", m_reports.get(2).getReportService());
    }

    /**
     * Test to retrieve repository IDs. The configuration is read in a repository. The repository will set his
     * repository id. For this reason the repository ID should be NULL.
     *
     * @throws Exception
     */
    @Test
    public void testReportRepositoryId() throws Exception {
        assertNull("First report repository id test", m_reports.get(0).getRepositoryId());
        assertNull("Second report repository id test", m_reports.get(1).getRepositoryId());
        assertNull("Third report repository id test", m_reports.get(2).getRepositoryId());
    }

    /**
     * Test to retrieve the allow access configuration. The implementation of a repository configure this property. By
     * default the allowAccess property is set to "FALSE"
     *
     * @throws Exception
     */
    @Test
    public void testReportAllowAccess() throws Exception {
        assertFalse("First report allow access test", m_reports.get(0).getAllowAccess());
        assertFalse("Second report allow access test", m_reports.get(1).getAllowAccess());
        assertFalse("Third report allow access test", m_reports.get(2).getAllowAccess());
    }

    /**
     * Test to retrieve is online property.
     *
     * @throws Exception
     */
    @Test
    public void testReportIsOnline() throws Exception {
        assertTrue("First report is online test", m_reports.get(0).getOnline());
        assertTrue("Second report is online test", m_reports.get(1).getOnline());
        assertFalse("Third report is online test", m_reports.get(2).getOnline());
    }

    /**
     * Set the configured data access object with injection 
     *
     * @param legacyLocalReportsDao a {@link org.opennms.features.reporting.dao.LegacyLocalReportsDao} object
     */
    public void setLegacyLocalReportsDao(LegacyLocalReportsDao legacyLocalReportsDao) {
        this.m_legacyLocalReportsDao = legacyLocalReportsDao;
    }

    /**
     * Get the configured data access object injected by Spring
     *
     * @return a {@link org.opennms.features.reporting.dao.LegacyLocalReportsDao} object
     */
    public LegacyLocalReportsDao getLegacyLocalReportsDao() {
        return this.m_legacyLocalReportsDao;
    }
}