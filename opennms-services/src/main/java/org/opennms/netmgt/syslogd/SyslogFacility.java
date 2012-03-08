package org.opennms.netmgt.syslogd;

public enum SyslogFacility {
    KERNEL(0, "kernel messages"),
    USER(1, "user-level messages"),
    MAIL(2, "mail system"),
    SYSTEM(3, "system daemons"),
    AUTH(4, "security/authorization messages"),
    SYSLOG(5, "messages generated internally by syslogd"),
    LPD(6, "line printer subsystem"),
    NEWS(7, "network news subsystem"),
    UUCP(8, "UUCP subsystem"),
    CLOCK(9, "clock daemon"),
    AUTHPRIV(10, "privileged security/authorization messages"),
    FTP(11, "FTP daemon"),
    NTP(12, "NTP subsystem"),
    AUDIT(13, "log audit"),
    ALERT(14, "log alert"),
    CRON(15, "cron daemon"),
    LOCAL0(16, "local use 0"),
    LOCAL1(17, "local use 1"),
    LOCAL2(18, "local use 2"),
    LOCAL3(19, "local use 3"),
    LOCAL4(20, "local use 4"),
    LOCAL5(21, "local use 5"),
    LOCAL6(22, "local use 6"),
    LOCAL7(23, "local use 7"),
    UNKNOWN(99, "unknown");

    public static final int MASK = 0x03F8;
    
    private final int m_facility;
    private final String m_name;
    private final String m_description;

    SyslogFacility(final int fac, final String description) {
        m_facility = fac;
        m_name = name().toLowerCase().intern();
        m_description = description.intern();
    }
    
    public int getFacilityNumber() {
        return m_facility;
    }
    
    public String getDescription() {
        return m_description;
    }

    public int getPriority(final SyslogSeverity severity) {
        if (severity == null) {
            return m_facility & MASK;
        }
        return ((m_facility & MASK) | severity.getSeverityNumber());
    }

    public String toString() {
        return m_name;
    }

    public static SyslogFacility getFacility(final int fac) {
        final SyslogFacility[] facilities = SyslogFacility.values();
        if (facilities.length < fac) {
            return null;
        }
        return facilities[fac];
    }
    
    public static SyslogFacility getFacilityForCode(final int code) {
        return getFacility((code & MASK) >> 3);
    }
}