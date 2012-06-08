package org.opennms.features.topology.app.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.opennms.features.topology.api.GraphContainer;
import org.opennms.features.topology.api.LayoutAlgorithm;
import org.opennms.features.topology.api.TopologyProvider;
import org.opennms.features.topology.api.VertexContainer;
import org.opennms.features.topology.app.internal.topr.SimpleTopologyProvider;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.MethodProperty;

public class SimpleGraphContainer implements GraphContainer {

    private class GVertex{
        
        private String m_key;
        private Object m_itemId;
        private Item m_item;
        private String m_groupKey;
        private Object m_groupId;
        private int m_x;
        private int m_y;
        private boolean m_selected = false;
        private int m_semanticZoomLevel;
        
        public GVertex(String key, Object itemId, Item item, String groupKey, Object groupId) {
            setKey(key);
            m_itemId = itemId;
            setItem(item);
            m_groupKey = groupKey;
            m_groupId = groupId;
                   
        }

        private Object getItemId() {
            return m_itemId;
        }

        private void setGroupId(Object groupId) {
            m_groupId = groupId;
        }

        private void setGroupKey(String groupKey) {
            m_groupKey = groupKey;
        }

        public String getKey() {
            return m_key;
        }

        private void setKey(String key) {
            m_key = key;
        }

        private Item getItem() {
            return m_item;
        }

        private void setItem(Item item) {
            m_item = item;
        }
        
        private String getGroupKey() {
            return m_groupKey;
        }

        private Object getGroupId() {
            return m_groupId;
        }

        private void setItemId(Object itemId) {
            m_itemId = itemId;
        }

        public boolean isLeaf() {
            return (Boolean) getItem().getItemProperty("leaf").getValue();
        }

        public int getX() {
            return m_x;
        }

        public void setX(int x) {
            m_x = x;
        }

        public int getY() {
            return m_y;
        }

        public void setY(int y) {
            m_y = y;
        }

        public boolean isSelected() {
            return m_selected;
        }

        public void setSelected(boolean selected) {
            m_selected = selected;
        }

        public String getIcon() {
            return (String) m_item.getItemProperty("icon").getValue();
        }

        public void setIcon(String icon) {
            m_item.getItemProperty("icon").setValue(icon);
        }

        public int getSemanticZoomLevel() {
            return m_semanticZoomLevel;
        }

        public void setSemanticZoomLevel(int semanticZoomLevel) {
            m_semanticZoomLevel = semanticZoomLevel;
        }
        
    }
    
    private class GEdge{

        private String m_key;
        private Object m_itemId;
        private Item m_item;
        private GVertex m_source;
        private GVertex m_target;

        public GEdge(String key, Object itemId, Item item, GVertex source, GVertex target) {
            m_key = key;
            m_itemId = itemId;
            m_item = item;
            m_source = source;
            m_target = target;
        }

        public String getKey() {
            return m_key;
        }

        private void setKey(String key) {
            m_key = key;
        }

        private Object getItemId() {
            return m_itemId;
        }

        private void setItemId(Object itemId) {
            m_itemId = itemId;
        }

        private Item getItem() {
            return m_item;
        }

        private void setItem(Item item) {
            m_item = item;
        }

        private GVertex getSource() {
            return m_source;
        }

        private void setSource(GVertex source) {
            m_source = source;
        }

        private GVertex getTarget() {
            return m_target;
        }

        private void setTarget(GVertex target) {
            m_target = target;
        }
        
    }
    
    private class GEdgeContainer extends BeanContainer<String, GEdge>{

        public GEdgeContainer() {
            super(GEdge.class);
            setBeanIdProperty("key");
            addAll(m_edgeHolder.getElements());
        }
        
        
    }
     
    private class GVertexContainer extends VertexContainer<Object, GVertex>{
        
        public GVertexContainer() {
            super(GVertex.class);
            setBeanIdProperty("key");
            addAll(m_vertexHolder.getElements());
        }

        @Override
        public Collection<?> getChildren(Object gItemId) {
            GVertex v = m_vertexHolder.getElementByKey(gItemId.toString());
            Collection<?> children = m_topologyProvider.getVertexContainer().getChildren(v.getItemId());
            
            return m_vertexHolder.getKeysByItemId(children);
        }
        
        @Override
        public Object getParent(Object gItemId) {
            GVertex vertex = m_vertexHolder.getElementByKey(gItemId.toString());
            return vertex.getGroupKey();
        }

        @Override
        public Collection<?> rootItemIds() {
            return m_vertexHolder.getKeysByItemId(m_topologyProvider.getVertexContainer().rootItemIds());
        }

        @Override
        public boolean setParent(Object gKey, Object gNewParentKey) throws UnsupportedOperationException {
           if(!containsId(gKey)) return false;
           
           GVertex vertex = m_vertexHolder.getElementByKey(gKey.toString());
           GVertex parentVertex = m_vertexHolder.getElementByKey(gNewParentKey.toString());
           
           if(m_topologyProvider.getVertexContainer().setParent(vertex.getItemId(), parentVertex.getItemId())) {
               vertex.setGroupId(parentVertex.getItemId());
               vertex.setGroupKey(parentVertex.getKey());
               return true;
           }
           
           return false;
           
        }

        @Override
        public boolean areChildrenAllowed(Object key) {
            GVertex vertex = m_vertexHolder.getElementByKey(key.toString());
            return !vertex.isLeaf();
        }

        @Override
        public boolean setChildrenAllowed(Object itemId, boolean areChildrenAllowed) throws UnsupportedOperationException {
            throw new UnsupportedOperationException("this operation is not allowed");
        }

        @Override
        public boolean isRoot(Object key) {
            GVertex vertex = m_vertexHolder.getElementByKey(key.toString());
            return m_topologyProvider.getVertexContainer().isRoot(vertex.getItemId());
        }

        @Override
        public boolean hasChildren(Object key) {
            GVertex vertex = m_vertexHolder.getElementByKey(key.toString());
            return m_topologyProvider.getVertexContainer().hasChildren(vertex.getItemId());
        }
        
    }
    
	private LayoutAlgorithm m_layoutAlgorithm;
	private double m_scale = 1;
	private int m_semanticZoomLevel;
	private MethodProperty<Integer> m_zoomLevelProperty;
	private MethodProperty<Double> m_scaleProperty;
	
    private GVertexContainer m_vertexContainer;
    
    private ElementHolder<GVertex> m_vertexHolder;
    private ElementHolder<GEdge> m_edgeHolder;
    private TopologyProvider m_topologyProvider;
    private BeanContainer<?, ?> m_edgeContainer;

	
	public SimpleGraphContainer() {
		m_zoomLevelProperty = new MethodProperty<Integer>(Integer.class, this, "getSemanticZoomLevel", "setSemanticZoomLevel");
		m_scaleProperty = new MethodProperty<Double>(Double.class, this, "getScale", "setScale");
		
		setDataSource(new SimpleTopologyProvider());
		
		m_vertexContainer = new GVertexContainer();
		m_edgeContainer = new GEdgeContainer();
		
	}
	
	
	private void setDataSource(TopologyProvider topologyProvider) {
	    if (m_topologyProvider == topologyProvider) return;
	    
	    m_topologyProvider = topologyProvider;
	    
        m_vertexHolder = new ElementHolder<GVertex>(m_topologyProvider.getVertexContainer()) {

            @Override
            protected GVertex update(GVertex element) {
                Object groupId = m_topologyProvider.getVertexContainer().getParent(element.getItemId());
                String groupKey = groupId == null ? null : getKeyForItemId(groupId);
                
                element.setGroupId(groupId);
                element.setGroupKey(groupKey);
                return element;
            }

            @Override
            protected GVertex make(String key, Object itemId, Item item) {
                Object groupId = m_topologyProvider.getVertexContainer().getParent(itemId);
                String groupKey = groupId == null ? null : getKeyForItemId(groupId);
                System.out.println("Parent of itemId: " + itemId + " groupId: " + groupId);
                return new GVertex(key, itemId, item, groupKey, groupId);
            }

        };
        
        m_edgeHolder = new ElementHolder<GEdge>(m_topologyProvider.getEdgeContainer()) {

            @Override
            protected GEdge make(String key, Object itemId, Item item) {

                List<Object> endPoints = new ArrayList<Object>(m_topologyProvider.getEndPointIdsForEdge(itemId));

                Object sourceId = endPoints.get(0);
                Object targetId = endPoints.get(1);
                
                GVertex source = m_vertexHolder.getElementByItemId(sourceId);
                GVertex target = m_vertexHolder.getElementByItemId(targetId);

                return new GEdge(key, itemId, item, source, target);
            }

        };
	    
	}

	public VertexContainer<?,?> getVertexContainer() {
		return m_vertexContainer;
	}

	public BeanContainer<?, ?> getEdgeContainer() {
		return m_edgeContainer;
	}

	public Collection<?> getVertexIds() {
		return m_vertexContainer.getItemIds();
	}

	public Collection<?> getEdgeIds() {
		return m_edgeContainer.getItemIds();
	}

	public Item getVertexItem(Object vertexId) {
		return m_vertexContainer.getItem(vertexId);
	}

	public Item getEdgeItem(Object edgeId) {
		return m_edgeContainer.getItem(edgeId); 
	}
	
	public Collection<?> getEndPointIdsForEdge(Object key) {
		GEdge edge = m_edgeHolder.getElementByKey(key.toString());
		return Arrays.asList(edge.getSource().getKey(), edge.getTarget().getKey());
	}

	public Collection<?> getEdgeIdsForVertex(Object vertexKey) {
	    GVertex vertex = m_vertexHolder.getElementByKey(vertexKey.toString());
	    return m_edgeHolder.getKeysByItemId(m_topologyProvider.getEdgeIdsForVertex(vertex.getItemId()));
	}
	
	public Collection<?> getPropertyIds() {
	    return Arrays.asList(new String[] {"semanticZoomLevel", "scale"});
	}

	public Property getProperty(String propertyId) {
	    if(propertyId.equals("semanticZoomLevel")) {
	        return m_zoomLevelProperty;
	    }else if(propertyId.equals("scale")) {
	        return m_scaleProperty;
	    }
		return null;
	}


    @Override
    public Integer getSemanticZoomLevel() {
        return m_semanticZoomLevel;
    }


    @Override
    public void setSemanticZoomLevel(Integer level) {
        m_semanticZoomLevel = level;
    }


    @Override
    public void setLayoutAlgorithm(LayoutAlgorithm layoutAlgorithm) {
        m_layoutAlgorithm = layoutAlgorithm;
        redoLayout();
    }


    @Override
    public LayoutAlgorithm getLayoutAlgorithm() {
        return m_layoutAlgorithm;
    }
    
    public Double getScale() {
        return m_scale;
    }
    
    public void setScale(Double scale) {
        m_scale = scale;
    }
    @Override
    public void redoLayout() {
        if(m_layoutAlgorithm != null) {
            m_layoutAlgorithm.updateLayout(this);
        }
    }

}