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

package org.opennms.mock.snmp;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opennms.core.utils.LogUtils;
import org.opennms.netmgt.snmp.SnmpObjId;
import org.opennms.netmgt.snmp.SnmpValue;

public class MockSnmpValue implements SnmpValue {
    
    public static class NetworkAddressSnmpValue extends MockSnmpValue {

        public NetworkAddressSnmpValue(final String value) {
            super(SnmpValue.SNMP_OCTET_STRING, value);
        }

        public boolean isDisplayable() {
            return false;
        }

    }

    public static class HexStringSnmpValue extends MockSnmpValue {
    	private static final Pattern HEX_PATTERN = Pattern.compile("^[a-fA-F0-9 :]*$");
		private static final Pattern HEX_CHUNK_PATTERN = Pattern.compile("(..)[ :]?");
        private final boolean m_isRaw;

    	public HexStringSnmpValue(final byte[] bytes) {
    		super(SnmpValue.SNMP_OCTET_STRING, new String(bytes));
    		m_isRaw = true;
    	}

        public HexStringSnmpValue(final String value) {
            super(SnmpValue.SNMP_OCTET_STRING, value);
            m_isRaw = false;
        }

        public byte[] getBytes() {
        	final String string = super.toString();
//        	LogUtils.debugf(this, "string = %s", string);
            if (m_isRaw) {
        		return string.getBytes();
        	} else {
        		final Matcher hexMatcher = HEX_PATTERN.matcher(string);
        		if (hexMatcher.matches()) {
//        		    LogUtils.debugf(this, "%s matches ^[a-fA-F0-9 :]*$", string);
                    final ByteArrayOutputStream os = new ByteArrayOutputStream();
                    final Matcher m = HEX_CHUNK_PATTERN.matcher(string);
                    while (m.find()) {
//                        LogUtils.debugf(this, "matched: %s", m.group(1));
                        os.write(Integer.parseInt(m.group(1), 16));
                    }
                    return os.toByteArray();
        		} else {
    		        LogUtils.debugf(this, "Not sure how to decide what to do with %s, just returning raw bytes.", string);
    		        return string.getBytes();
        		}
        	}
        }
        
        public String toString() {
            final byte[] data = getBytes();
            
            final byte[] results = new byte[data.length];
            for (int i = 0; i < data.length; i++) {
                results[i] = Character.isISOControl((char)data[i]) ? (byte)'.' : data[i];
            }
            return new String(results);
            /*
            if (m_isRaw) {
                //
                // format the string for hex
                //

                StringBuffer b = new StringBuffer();
                // b.append("SNMP Octet String [length = " + m_data.length + ", fmt
                // = HEX] = [");
                for (int i = 0; i < data.length; ++i) {
                    int x = (int) data[i] & 0xff;
                    if (x < 16)
                        b.append('0');
                    b.append(Integer.toString(x, 16).toUpperCase());

                    if (i < data.length - 1)
                        b.append(' ');
                }
                // b.append(']');
                return b.toString();
            } else {
                //
                // raw output
                //
                // rs = "SNMP Octet String [length = " + m_data.length + ", fmt =
                // RAW] = [" + new String(m_data) + "]";
                byte[] results = new byte[data.length];
                for (int i = 0; i < data.length; i++) {
                    results[i] = Character.isISOControl((char)data[i]) ? (byte)'.' : data[i];
                }
                return new String(results);
            }
            */
        }

        public String toHexString() {
        	final byte[] data = getBytes();
            final StringBuffer b = new StringBuffer();
            for (int i = 0; i < data.length; ++i) {
                final int x = (int) data[i] & 0xff;
                if (x < 16) b.append("0");
                b.append(Integer.toString(x, 16).toLowerCase());
            }
            return b.toString();
        }
        
        public boolean isDisplayable() {
            return false;
        }

    }

    public static class IpAddressSnmpValue extends MockSnmpValue {

        public IpAddressSnmpValue(String value) {
            super(SnmpValue.SNMP_IPADDRESS, value);
        }

        public InetAddress toInetAddress() {
            try {
                return InetAddress.getByName(toString());
            } catch (Exception e) {
                return super.toInetAddress();
            }
        }
        
        public byte[] getBytes() {
            return toInetAddress().getAddress();
        }
        
        public boolean isDisplayable() {
            return true;
        }

    }

    public static class NumberSnmpValue extends MockSnmpValue {

        public NumberSnmpValue(int type, String value) {
            super(type, value);
        }
        
        public boolean isNumeric() {
            return true;
        }
        
        public int toInt() {
            return Integer.parseInt(getNumberString());
        }
        
        public long toLong() {
            return Long.parseLong(getNumberString());
        }
        
        public BigInteger toBigInteger() {
            return new BigInteger(getNumberString());
        }
        
        public String getNumberString() {
            return toString();
        }
        
        public byte[] getBytes() {
            return toBigInteger().toByteArray();
        }

        public boolean isDisplayable() {
            return true;
        }
    }
    
    public static class Integer32SnmpValue extends NumberSnmpValue {
        public Integer32SnmpValue(int value) {
            this(Integer.toString(value));
        }
        public Integer32SnmpValue(String value) {
            super(SnmpValue.SNMP_INT32, value);
        }
    }
    
    public static class Gauge32SnmpValue extends NumberSnmpValue {
        public Gauge32SnmpValue(int value) {
            this(Integer.toString(value));
        }
        public Gauge32SnmpValue(String value) {
            super(SnmpValue.SNMP_GAUGE32, value);
        }
    }
   
    public static class Counter32SnmpValue extends NumberSnmpValue {
        public Counter32SnmpValue(int value) {
            this(Integer.toString(value));
        }
        public Counter32SnmpValue(String value) {
            super(SnmpValue.SNMP_COUNTER32, value);
        }
    }
    
    public static class Counter64SnmpValue extends NumberSnmpValue {
        public Counter64SnmpValue(long value) {
            this(Long.toString(value));
        }
        public Counter64SnmpValue(String value) {
            super(SnmpValue.SNMP_COUNTER64, value);
        }
    }
    
    static enum UnitOfTime {
        DAYS(4), HOURS(3), MINUTES(2), SECONDS(1), MILLIS(0);
        
        private int m_index;
        
        private UnitOfTime(int index) {
            m_index = index;
        }
        
        private static final long[] s_millisPerUnit = {
            1L,                         // millis
            1000L,                      // seconds
            1000L * 60L,                // minutes
            1000L * 60L * 60L,          // hours
            1000L * 60L * 60L * 24L     // days
        };
        
        private static final String[] s_unitName = {
            "ms",   // millis
            "s",    // seconds
            "m",    // minutes
            "h",    // hours
            "d"     // days
        };
        
        public long wholeUnits(long millis) {
            return millis / s_millisPerUnit[m_index];
        }
        
        public long remainingMillis(long millis) {
            return millis % s_millisPerUnit[m_index];
        }
        
        public String unit() {
            return s_unitName[m_index];
        }
        
        
    }
    
    public static class TimeticksSnmpValue extends NumberSnmpValue {

        // Format of string is '(numTicks) HH:mm:ss.hh'
        public TimeticksSnmpValue(String value) {
            super(SnmpValue.SNMP_TIMETICKS, value);
        }

        public String getNumberString() {
            String str = getValue();
            int end = str.indexOf(')');
            return (end < 0 ? str : str.substring(1, end));
        }
        
        public String toString() {
        	return String.valueOf(toLong());
        }
        
        public String toDisplayString() {
        	return toString();
        	/*
            long millis = toLong()*10L;
            
            StringBuilder buf = new StringBuilder();

            boolean first = true;
            for(UnitOfTime unit : UnitOfTime.values()) {

                if (first) {
                    first = false; 
                 } else {
                     buf.append(' ');
                 }

                buf.append(unit.wholeUnits(millis)).append(unit.unit());
                millis = unit.remainingMillis(millis);
            }
            
            return buf.toString();
            */
        }

    }



    public static class StringSnmpValue extends MockSnmpValue {
        public StringSnmpValue(String value) {
            super(SnmpValue.SNMP_OCTET_STRING, value);
        }
        
        public int toInt() {
            try {
                return Integer.parseInt(toString());
            } catch (NumberFormatException e) {
                return super.toInt();
            }
            
        }

        public boolean isDisplayable() {
            return true;
        }
        
        @Override
        public long toLong() {
            return Long.parseLong(toString());
        }

        @Override
        public String toHexString() {
            StringBuffer buff = new StringBuffer();

            for (byte b : toString().getBytes()) {
                buff.append(Integer.toHexString(b));
            }
            
            return buff.toString();
        }
    }

    public static class OidSnmpValue extends MockSnmpValue {

        public OidSnmpValue(String value) {
            super(SnmpValue.SNMP_OBJECT_IDENTIFIER, value);
        }

        public SnmpObjId toSnmpObjId() {
            return SnmpObjId.get(toString());
        }

        public boolean isDisplayable() {
            return true;
        }


    }

    private int m_type;
    private String m_value;
    public static final SnmpValue NULL_VALUE = new MockSnmpValue(SnmpValue.SNMP_NULL, null) {
        public boolean isNull() {
            return true;
        }
    };
    public static final SnmpValue NO_SUCH_INSTANCE = new MockSnmpValue(SnmpValue.SNMP_NO_SUCH_INSTANCE, "noSuchInstance");
    public static final SnmpValue NO_SUCH_OBJECT = new MockSnmpValue(SnmpValue.SNMP_NO_SUCH_OBJECT, "noSuchObject");
    public static final SnmpValue END_OF_MIB = new MockSnmpValue(SnmpValue.SNMP_END_OF_MIB, "endOfMibView");

    public MockSnmpValue(int type, String value) {
        m_type = type;
        m_value = value;
    }

    public boolean isEndOfMib() {
        return getType() == SnmpValue.SNMP_END_OF_MIB;
    }
    
    public int getType() {
        return m_type;
    }
    
    public String toDisplayString() { return toString(); }
    
    public String toString() { return m_value; }
    
    public String getValue() { return m_value; }

    public boolean equals(Object obj) {
        if (obj instanceof MockSnmpValue ) {
            MockSnmpValue val = (MockSnmpValue)obj;
            return (m_value == null ? val.m_value == null : m_value.equals(val.m_value));
        }
        return false;
    }

    public int hashCode() {
        if (m_value == null) return 0;
        return m_value.hashCode();
    }

    public boolean isNumeric() {
        return false;
    }
    
    public boolean isError() {
        switch (getType()) {
        case SnmpValue.SNMP_NO_SUCH_INSTANCE:
        case SnmpValue.SNMP_NO_SUCH_OBJECT:
            return true;
        default:
            return false;
        }
        
    }

    public static SnmpValue parseMibValue(final String mibVal) {
        if (mibVal.startsWith("OID:"))
            return new OidSnmpValue(mibVal.substring("OID:".length()).trim());
        else if (mibVal.startsWith("Timeticks:"))
            return new TimeticksSnmpValue(mibVal.substring("Timeticks:".length()).trim());
        else if (mibVal.startsWith("STRING:"))
            return new StringSnmpValue(mibVal.substring("STRING:".length()).trim());
        else if (mibVal.startsWith("INTEGER:"))
            return new NumberSnmpValue(SnmpValue.SNMP_INT32, mibVal.substring("INTEGER:".length()).trim());
        else if (mibVal.startsWith("Gauge32:"))
            return new NumberSnmpValue(SnmpValue.SNMP_GAUGE32, mibVal.substring("Gauge32:".length()).trim());
        else if (mibVal.startsWith("Counter32:"))
            return new NumberSnmpValue(SnmpValue.SNMP_COUNTER32, mibVal.substring("Counter32:".length()).trim());
        else if (mibVal.startsWith("Counter64:"))
            return new NumberSnmpValue(SnmpValue.SNMP_COUNTER64, mibVal.substring("Counter64:".length()).trim());
        else if (mibVal.startsWith("IpAddress:"))
            return new IpAddressSnmpValue(mibVal.substring("IpAddress:".length()).trim());
        else if (mibVal.startsWith("Hex-STRING:"))
            return new HexStringSnmpValue(mibVal.substring("Hex-STRING:".length()).trim());
        else if (mibVal.startsWith("Network Address:"))
            return new NetworkAddressSnmpValue(mibVal.substring("Network Address:".length()).trim());
        else if (mibVal.startsWith("BITS:"))
            return new HexStringSnmpValue(mibVal.substring("BITS:".length()).trim());
        else if (mibVal.equals("\"\""))
            return NULL_VALUE;

        throw new IllegalArgumentException("Unknown Snmp Type: "+mibVal);
    }

    public int toInt() {
        throw new IllegalArgumentException("Unable to convert "+this+" to an int");
    }

    public InetAddress toInetAddress() {
        throw new IllegalArgumentException("Unable to convert "+this+" to an InetAddress");
    }

    public long toLong() {
        throw new IllegalArgumentException("Unable to convert "+this+" to a long");
    }

    public String toHexString() {
        throw new IllegalArgumentException("Unable to convert "+this+" to a hex string");
    }

    public BigInteger toBigInteger() {
        throw new IllegalArgumentException("Unable to convert "+this+" to a hex string");
    }

    public SnmpObjId toSnmpObjId() {
        throw new IllegalArgumentException("Unable to convert "+this+" to an SNMP object ID");
    }

    public byte[] getBytes() {
        return toString().getBytes();
    }

    public boolean isDisplayable() {
        return false;
    }

    public boolean isNull() {
        return false;
    }

 
}
