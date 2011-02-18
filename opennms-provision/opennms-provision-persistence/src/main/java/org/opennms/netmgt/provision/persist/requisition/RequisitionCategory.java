//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.0.3-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.01.29 at 01:15:48 PM EST 
//


package org.opennms.netmgt.provision.persist.requisition;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * <p>RequisitionCategory class.</p>
 *
 * @author ranger
 * @version $Id: $
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="")
@XmlRootElement(name="category")
public class RequisitionCategory implements Comparable<RequisitionCategory> {

    @XmlAttribute(name="name", required=true)
    protected String m_name;

    /**
     * <p>Constructor for RequisitionCategory.</p>
     */
    public RequisitionCategory() {
    }

    /**
     * <p>Constructor for RequisitionCategory.</p>
     *
     * @param category a {@link java.lang.String} object.
     */
    public RequisitionCategory(String category) {
        m_name = category;
    }

    /**
     * <p>getName</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getName() {
        return m_name;
    }

    /**
     * <p>setName</p>
     *
     * @param value a {@link java.lang.String} object.
     */
    public void setName(String value) {
        m_name = value;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof RequisitionCategory) {
            return this.compareTo((RequisitionCategory)o) == 0;
        } else return false;
    }

    public int compareTo(RequisitionCategory o) {
        return m_name.compareTo(o.getName());
    }

    public String toString() {
    	return new ToStringBuilder(this)
    		.append("name", m_name)
    		.toString();
    }
}
