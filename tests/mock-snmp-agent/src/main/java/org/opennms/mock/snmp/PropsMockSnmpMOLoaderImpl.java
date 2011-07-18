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

import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.opennms.netmgt.snmp.SnmpUtils;
import org.snmp4j.agent.MOAccess;
import org.snmp4j.agent.ManagedObject;
import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.agent.mo.MOScalar;
import org.snmp4j.smi.Counter32;
import org.snmp4j.smi.Counter64;
import org.snmp4j.smi.Gauge32;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.Null;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.Variable;
import org.springframework.core.io.Resource;

/**
 * <p>PropsMockSnmpMOLoaderImpl class.</p>
 *
 * @author ranger
 * @version $Id: $
 */
public class PropsMockSnmpMOLoaderImpl implements MockSnmpMOLoader {

	/** {@inheritDoc} */
	public List<ManagedObject> loadMOs(Resource moFile) {
	    ArrayList<ManagedObject> moList = new ArrayList<ManagedObject>();
		
        Properties moProps = SnmpUtils.loadProperties(moFile);
        if (moProps == null) return null;

		Enumeration<Object> moKeys = moProps.keys();
		while ( moKeys.hasMoreElements() ) {
			String oidStr = moKeys.nextElement().toString();
			ManagedObject newMo = getMOFromPropString(oidStr, moProps.getProperty(oidStr));
			moList.add(newMo);
		}
		return moList;
	}

    /**
	 * <p>loadProperties</p>
	 *
	 * @param propertiesFile a {@link org.springframework.core.io.Resource} object.
	 * @return a {@link java.util.Properties} object.
	 * @deprecated Use {@link SnmpUtils#loadProperties(Resource)} instead
	 */
	public static  Properties loadProperties(final Resource propertiesFile) {
		return SnmpUtils.loadProperties(propertiesFile);
	}
    
    private static class UpdatableScalar extends MOScalar implements Updatable {

        public UpdatableScalar(OID id, MOAccess access, Variable value) {
            super(id, access, value);
        }

        public void updateValue(OID oid, Variable value) {
            if (!getScope().covers(oid)) {
                throw new IllegalArgumentException("attempt to set value of oid not defined in this scalar: oid = "+oid+", scalar = "+this);
            }
            setValue(value);
        }
        
    }
	
	/**
	 * <p>getMOFromPropString</p>
	 *
	 * @param oidStr a {@link java.lang.String} object.
	 * @param valStr a {@link java.lang.String} object.
	 * @return a {@link org.snmp4j.agent.ManagedObject} object.
	 */
	protected static ManagedObject getMOFromPropString(String oidStr, String valStr) {
	    OID moOID = new OID(oidStr);

	    MOScalar newMO;
	    Variable newVar = getVariableFromValueString(oidStr, valStr);
	    newMO = new UpdatableScalar(moOID, MOAccessImpl.ACCESS_READ_ONLY, newVar);
	    newMO.setVolatile(true);
	    return newMO;
	}

    /**
     * <p>getVariableFromValueString</p>
     *
     * @param oidStr a {@link java.lang.String} object.
     * @param valStr a {@link java.lang.String} object.
     * @return a {@link org.snmp4j.smi.Variable} object.
     */
    public static Variable getVariableFromValueString(String oidStr, String valStr) {
        Variable newVar;

	    if ("\"\"".equals(valStr)) {
	        newVar = new Null();
	    }
	    else {
	        String moTypeStr = valStr.substring(0, valStr.indexOf(":"));
	        String moValStr = valStr.substring(valStr.indexOf(":") + 2);

	        try {

	            if (moTypeStr.equals("STRING")) {
	                newVar = new OctetString(moValStr);
	            } else if (moTypeStr.equals("Hex-STRING")) {
	                newVar = OctetString.fromHexString(moValStr.trim().replace(' ', ':'));
	            } else if (moTypeStr.equals("INTEGER")) {
	                newVar = new Integer32(Integer.parseInt(moValStr));
	            } else if (moTypeStr.equals("Gauge32")) {
	                newVar = new Gauge32(Long.parseLong(moValStr));
	            } else if (moTypeStr.equals("Counter32")) {
	                newVar = new Counter32(Long.parseLong(moValStr)); // a 32 bit counter can be > 2 ^ 31, which is > INTEGER_MAX
	            } else if (moTypeStr.equals("Counter64")) {
	                newVar = new Counter64(Long.parseLong(moValStr));
	            } else if (moTypeStr.equals("Timeticks")) {
	                Integer ticksInt = Integer.parseInt( moValStr.substring( moValStr.indexOf("(") + 1, moValStr.indexOf(")") ) );
	                newVar = new TimeTicks(ticksInt);
	            } else if (moTypeStr.equals("OID")) {
	                newVar = new OID(moValStr);
                } else if (moTypeStr.equals("IpAddress")) {
                    newVar = new IpAddress(moValStr.trim());
                } else if (moTypeStr.equals("Network Address")) {
                    newVar = OctetString.fromHexString(moValStr.trim());
	            } else {
	                // Punt, assume it's a String
	                //newVar = new OctetString(moValStr);
                    throw new IllegalArgumentException("Unrecognized Snmp Type "+moTypeStr);
	            }
	        } catch (Throwable t) {
	            throw new UndeclaredThrowableException(t, "Could not convert value '" + moValStr + "' of type '" + moTypeStr + "' to SNMP object for OID " + oidStr);
	        }
	    }
        return newVar;
    }
    
}
