/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2010-2011 The OpenNMS Group, Inc.
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


package org.opennms.mock.snmp;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Properties;

import junit.framework.TestCase;

import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.snmp.SnmpAgentConfig;
import org.opennms.netmgt.snmp.SnmpObjId;
import org.opennms.netmgt.snmp.SnmpUtils;
import org.opennms.netmgt.snmp.SnmpValue;
import org.opennms.test.mock.MockLogAppender;
import org.opennms.test.mock.MockUtil;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.ScopedPDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.UserTarget;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.AuthMD5;
import org.snmp4j.security.PrivDES;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.SMIConstants;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.springframework.core.io.ClassPathResource;

public class MockSnmpAgentTest extends TestCase {

    private MockSnmpAgent m_agent;
    private USM m_usm;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MockUtil.println("------------ Begin Test "+getName()+" --------------------------");
        final Properties p = new Properties();
        p.setProperty("log4j.logger.org.snmp4j", "DEBUG");
        p.setProperty("log4j.logger.org.snmp4j.agent", "DEBUG");
        MockLogAppender.setupLogging(true, p);

        // Create a global USM that all client calls will use
        MPv3.setEnterpriseID(5813);
        m_usm = new USM(SecurityProtocols.getInstance(), new OctetString(MPv3.createLocalEngineID()), 0);
        SecurityModels.getInstance().addSecurityModel(m_usm);

        m_agent = MockSnmpAgent.createAgentAndRun(new ClassPathResource("loadSnmpDataTest.properties"), "127.0.0.1/1691");	// Homage to Empire
    }

    @Override
    public void runTest() throws Throwable {
        super.runTest();
        MockLogAppender.assertNoWarningsOrGreater();
    }

    @Override
    protected void tearDown() throws Exception {
        m_agent.shutDownAndWait();
        super.tearDown();
        MockUtil.println("------------ End Test "+getName()+" --------------------------");
    }

    /**
     * Make sure that we can setUp() and tearDown() the agent.
     * @throws InterruptedException 
     */
    public void testAgentSetup() {
        assertNotNull("agent should be non-null", m_agent);
    }

    /**
     * Test that we can setUp() and tearDown() twice to ensure that the
     * MockSnmpAgent tears itself down properly. In particular, we want to make
     * sure that the UDP listener gets torn down so listening port is free for
     * later instances of the agent.
     * 
     * @throws Exception
     */
    public void testSetUpTearDownTwice() throws Exception {
        // don't need the first setUp(), since it's already been done by JUnit
        tearDown();
        setUp();
        // don't need the second tearDown(), since it will be done by JUnit
    }

    public void testGetNext() throws Exception {
        assertResultFromGetNext("1.3.5.1.1.3", "1.3.5.1.1.3.0", SMIConstants.SYNTAX_INTEGER, new Integer32(42));
    }

    public void testGet() throws Exception {
        assertResultFromGet("1.3.5.1.1.3.0", SMIConstants.SYNTAX_INTEGER, new Integer32(42));

        m_agent.updateValue("1.3.5.1.1.3.0", new Integer32(77));

        assertResultFromGet("1.3.5.1.1.3.0", SMIConstants.SYNTAX_INTEGER, new Integer32(77));

    }
    
    public void testUpdateFromFile() throws Exception {
        assertResultFromGet("1.3.5.1.1.3.0", SMIConstants.SYNTAX_INTEGER, new Integer32(42));
        
        m_agent.updateValuesFromResource(new ClassPathResource("differentSnmpData.properties"));
        
        assertResultFromGet("1.3.5.1.1.3.0", SMIConstants.SYNTAX_INTEGER, new Integer32(77));
    }

    public void testGetNextMultipleVarbinds() throws UnknownHostException {
        SnmpAgentConfig agentConfig = getAgentConfig();
        SnmpObjId[] oids = new SnmpObjId[] { SnmpObjId.get("1.3.5.1.1.3") };
        SnmpValue[] vals = SnmpUtils.getNext(agentConfig, oids);
        assertNotNull(vals);
        assertEquals(1, vals.length);

        m_agent.getUsm().setEngineBoots(15);

        oids = new SnmpObjId[] { SnmpObjId.get("1.3.5.1.1.3") };
        vals = SnmpUtils.getNext(agentConfig, oids);
        assertNotNull(vals);
        assertEquals(1, vals.length);

        oids = new SnmpObjId[] { SnmpObjId.get("1.3.5.1.1.3") };
        vals = SnmpUtils.getNext(agentConfig, oids);
        assertNotNull(vals);
        assertEquals(1, vals.length);

        // This statement breaks the internal state of the SNMP4J agent
        // m_agent.getUsm().setLocalEngine(m_agent.getUsm().getLocalEngineID(), 15, 200);
        m_agent.getUsm().removeEngineTime(m_usm.getLocalEngineID());
        m_usm.removeEngineTime(m_agent.getUsm().getLocalEngineID());

        oids = new SnmpObjId[] { SnmpObjId.get("1.3.5.1.1.3") };
        vals = SnmpUtils.getNext(agentConfig, oids);
        assertNotNull(vals);
        assertEquals(1, vals.length);
    }
    
    public void testSet() throws Exception {
        final String oid = "1.3.5.1.1.3.0";
        assertResultFromGet(oid, SMIConstants.SYNTAX_INTEGER32, new Integer32(42));
		assertResultFromSet(oid, new Integer32(17), oid, SMIConstants.SYNTAX_INTEGER32, new Integer32(17));
        assertResultFromGet(oid, SMIConstants.SYNTAX_INTEGER32, new Integer32(17));
    }

    public void testUpdateFromFileWithUSMTimeReset() throws Exception {
        assertResultFromGet("1.3.5.1.1.3.0", SMIConstants.SYNTAX_INTEGER, new Integer32(42));

        m_agent.getUsm().setEngineBoots(15);

        assertResultFromGet("1.3.5.1.1.3.0", SMIConstants.SYNTAX_INTEGER, new Integer32(42));
        assertResultFromGet("1.3.5.1.1.3.0", SMIConstants.SYNTAX_INTEGER, new Integer32(42));

        // This statement breaks the internal state of the SNMP4J agent
        // m_agent.getUsm().setLocalEngine(m_agent.getUsm().getLocalEngineID(), 15, 200);
        m_agent.getUsm().removeEngineTime(m_usm.getLocalEngineID());
        m_usm.removeEngineTime(m_agent.getUsm().getLocalEngineID());

        assertResultFromGet("1.3.5.1.1.3.0", SMIConstants.SYNTAX_INTEGER, new Integer32(42));
        assertResultFromGet("1.3.5.1.1.3.0", SMIConstants.SYNTAX_INTEGER, new Integer32(42));

        m_usm.removeEngineTime(m_agent.getUsm().getLocalEngineID());

        assertResultFromGet("1.3.5.1.1.3.0", SMIConstants.SYNTAX_INTEGER, new Integer32(42));
        assertResultFromGet("1.3.5.1.1.3.0", SMIConstants.SYNTAX_INTEGER, new Integer32(42));
    }

    private void assertResultFromGet(String oidStr, int expectedSyntax, Variable expected) throws Exception {
        assertResult(PDU.GET, oidStr, oidStr, expectedSyntax, expected);
    }

    private void assertResultFromGetNext(String oidStr, String expectedOid, int expectedSyntax, Variable expected) throws UnknownHostException, IOException {
        assertResult(PDU.GETNEXT, oidStr, expectedOid, expectedSyntax, expected);
    }

    private void assertResultFromSet(String oidStr, Variable sendVariable, String expectedOid, int expectedSyntax, Variable expected) throws UnknownHostException, IOException {
        assertV3Result(PDU.SET, oidStr, sendVariable, expectedOid, expectedSyntax, expected);
    }
    
    private void assertResult(int pduType, String oidStr, String expectedOid, int expectedSyntax, Variable expected) throws UnknownHostException, IOException {
        assertV3Result(pduType, oidStr, null, expectedOid, expectedSyntax, expected);
    }

    @SuppressWarnings("unused")
    private void assertV1Result(int pduType, String oidStr, String expectedOid, int expectedSyntax, Variable expected) throws UnknownHostException, IOException {
        PDU pdu = new PDU();
        OID oid = new OID(oidStr);
        pdu.add(new VariableBinding(oid));
        pdu.setType(pduType);

        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString("public"));
        target.setAddress(new UdpAddress(InetAddressUtils.addr("127.0.0.1"), 1691));
        target.setVersion(SnmpConstants.version1);

        TransportMapping transport = null;
        try {
            transport = new DefaultUdpTransportMapping();
            Snmp snmp = new Snmp(transport);
            transport.listen();

            ResponseEvent e = snmp.send(pdu, target);
            PDU response = e.getResponse();
            assertNotNull("request timed out", response);

            VariableBinding vb = response.get(0);
            assertNotNull(vb);
            assertNotNull(vb.getVariable());
            assertEquals(new OID(expectedOid), vb.getOid());
            assertEquals(expectedSyntax, vb.getSyntax());
            Variable val = vb.getVariable();
            assertNotNull(val);
            assertEquals(expected, val);

        } finally { 
            if (transport != null) {
                transport.close();
            }
        }
    }

    @SuppressWarnings("unused")
    private void assertV2Result(int pduType, String oidStr, String expectedOid, int expectedSyntax, Integer32 expected) throws UnknownHostException, IOException {
        PDU pdu = new PDU();
        OID oid = new OID(oidStr);
        pdu.add(new VariableBinding(oid));
        pdu.setType(pduType);

        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString("public"));
        target.setAddress(new UdpAddress(InetAddressUtils.addr("127.0.0.1"), 1691));
        target.setVersion(SnmpConstants.version2c);

        TransportMapping transport = null;
        try {
            transport = new DefaultUdpTransportMapping();
            Snmp snmp = new Snmp(transport);
            transport.listen();

            ResponseEvent e = snmp.send(pdu, target);
            PDU response = e.getResponse();
            assertNotNull("request timed out", response);

            VariableBinding vb = response.get(0);
            assertNotNull(vb);
            assertNotNull(vb.getVariable());
            assertEquals(new OID(expectedOid), vb.getOid());
            assertEquals(expectedSyntax, vb.getSyntax());
            Variable val = vb.getVariable();
            assertNotNull(val);
            assertEquals(expected, val);

        } finally { 
            if (transport != null) {
                transport.close();
            }
        }
    }

    private void assertV3Result(final int pduType, final String oidStr, final Variable sendVariable, final String expectedOid, final int expectedSyntax, final Variable expected) throws UnknownHostException, IOException {
        PDU pdu = new ScopedPDU();
        OID oid = new OID(oidStr);
        if (sendVariable != null) {
        	pdu.add(new VariableBinding(oid, sendVariable));
        } else {
        	pdu.add(new VariableBinding(oid));
        }
        pdu.setType(pduType);

        OctetString userId = new OctetString("opennmsUser");
        OctetString pw = new OctetString("0p3nNMSv3");

        UserTarget target = new UserTarget();
        target.setSecurityLevel(SecurityLevel.AUTH_PRIV);
        target.setSecurityName(userId);
        target.setAddress(new UdpAddress(InetAddressUtils.addr("127.0.0.1"), 1691));
        target.setVersion(SnmpConstants.version3);
        target.setTimeout(5000);
        
        TransportMapping transport = null;
        try {
            USM usm = new USM(SecurityProtocols.getInstance(), new OctetString(MPv3.createLocalEngineID()), 0);
            SecurityModels.getInstance().addSecurityModel(usm);
            transport = new DefaultUdpTransportMapping();
            Snmp snmp = new Snmp(transport);

            UsmUser user = new UsmUser(userId, AuthMD5.ID, pw, PrivDES.ID, pw);
            snmp.getUSM().addUser(userId, user);

            transport.listen();
            
            ResponseEvent e = snmp.send(pdu, target);
            PDU response = e.getResponse();
            assertNotNull("request timed out", response);
            MockUtil.println("Response is: "+response);
            assertTrue("unexpected report pdu: " + ((VariableBinding)response.getVariableBindings().get(0)).getOid(), response.getType() != PDU.REPORT);
            
            VariableBinding vb = response.get(0);
            assertNotNull("variable binding should not be null", vb);
            Variable val = vb.getVariable();
            assertNotNull("variable should not be null", val);
            assertEquals("OID (value: " + val + ")", new OID(expectedOid), vb.getOid());
            assertEquals("syntax", expectedSyntax, vb.getSyntax());
            assertEquals("value", expected, val);

        } finally { 
            if (transport != null) {
                transport.close();
            }
        }
    }

    private static SnmpAgentConfig getAgentConfig() throws UnknownHostException {
        SnmpAgentConfig config = new SnmpAgentConfig(InetAddressUtils.addr("127.0.0.1"));
        config.setPort(1691);
        config.setVersion(SnmpAgentConfig.VERSION3);
        return config;
    }
}
