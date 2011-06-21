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
package org.opennms.web.map.config;

/**
 * <p>Filter class.</p>
 *
 * @author <a href="mailto:antonio@opennms.it">Antonio Russo</a>
 * @version $Id: $
 * @since 1.8.1
 */
public class Filter{
	String table;
	String condition;
	/**
	 * <p>Constructor for Filter.</p>
	 *
	 * @param table a {@link java.lang.String} object.
	 * @param condition a {@link java.lang.String} object.
	 */
	public Filter(String table, String condition) {
		super();
		this.table = table;
		this.condition = condition;
	}
	/**
	 * <p>Getter for the field <code>condition</code>.</p>
	 *
	 * @return a {@link java.lang.String} object.
	 */
	protected String getCondition() {
		return condition;
	}
	/**
	 * <p>Setter for the field <code>condition</code>.</p>
	 *
	 * @param condition a {@link java.lang.String} object.
	 */
	protected void setCondition(String condition) {
		this.condition = condition;
	}
	/**
	 * <p>Getter for the field <code>table</code>.</p>
	 *
	 * @return a {@link java.lang.String} object.
	 */
	protected String getTable() {
		return table;
	}
	/**
	 * <p>Setter for the field <code>table</code>.</p>
	 *
	 * @param table a {@link java.lang.String} object.
	 */
	protected void setTable(String table) {
		this.table = table;
	}
	
}