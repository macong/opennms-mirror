package org.opennms.features.topology.plugins.topo.vmware.internal;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="vertex")
public class SimpleLeafVertex extends SimpleVertex {

	public SimpleLeafVertex() {}
	
	public SimpleLeafVertex(String id, int x, int y) {
		super(id, x, y);
	}

	@Override
	public boolean isLeaf() {
		return true;
	}
	

}