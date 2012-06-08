/**
 * 
 */
package org.opennms.features.topology.app.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.opennms.features.topology.api.VertexContainer;

public class SimpleVertexContainer extends VertexContainer<String, GVertex> {
	public SimpleVertexContainer() {
		super(GVertex.class);
		setBeanIdProperty("id");
	}
	
	public void fireLayoutChange() {
		fireItemSetChange();
	}

	public boolean areChildrenAllowed(Object itemId) {
		if (!containsId(itemId)) return false;
		GVertex v = getItem(itemId).getBean();
		return !v.isLeaf();
	}

	public Collection<?> getChildren(Object itemId) {
		if (!containsId(itemId)) return Collections.EMPTY_LIST;
		GVertex v = getItem(itemId).getBean();
		if (v.isLeaf()) {
			return Collections.EMPTY_LIST;
		}
		else {
			SimpleGroup g = (SimpleGroup)v;
			List<String> memberIds = new ArrayList<String>();
			for(GVertex member : g.getMembers()) {
				memberIds.add(member.getId());
			}
			return memberIds;
		}
	}

	public Object getParent(Object itemId) {
		if (!containsId(itemId)) return null;
		
		SimpleGroup g = getItem(itemId).getBean().getParent();
		return g == null ? null : g.getId();
	}

	public boolean hasChildren(Object itemId) {
		if (!containsId(itemId)) return false;
		GVertex v = getItem(itemId).getBean();
		return !v.isLeaf();
	}

	public boolean isRoot(Object itemId) {
		if (!containsId(itemId)) return false;
		
		return (getParent(itemId) == null);
	}

	public Collection<?> rootItemIds() {
		List<Object> rootItemIds = new ArrayList<Object>();
		
		for(Object itemId : getItemIds()) {
			if (getItem(itemId).getBean().getParent() == null) {
				rootItemIds.add(itemId);
			}
		}
		return rootItemIds;
	}

	public boolean setChildrenAllowed(Object itemId, boolean areChildrenAllowed) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("this operation is not allowed");
	}

	public boolean setParent(Object itemId, Object newParentId) throws UnsupportedOperationException {
		if (!containsId(itemId)) return false;
		
		GVertex v  = getItem(itemId).getBean();
		
		if (newParentId == null) {
			v.setParent(null);
			return true;
		}
		
		if (!containsId(newParentId)) return false;
		
		GVertex p = getItem(newParentId).getBean();
		
		if (p.isLeaf()) return false;
		
		SimpleGroup g = (SimpleGroup)p;
		
		v.setParent(g);
		return true;
		
	}
	
}