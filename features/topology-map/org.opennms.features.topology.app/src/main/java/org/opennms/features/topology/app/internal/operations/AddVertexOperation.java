package org.opennms.features.topology.app.internal.operations;

import java.util.List;

import org.opennms.features.topology.api.DisplayState;
import org.opennms.features.topology.api.Operation;
import org.opennms.features.topology.api.OperationContext;
import org.opennms.features.topology.app.internal.Constants;
import org.opennms.features.topology.app.internal.topr.SimpleTopologyProvider;

public class AddVertexOperation implements Operation{
    
    private SimpleTopologyProvider m_topologyProvider = new SimpleTopologyProvider();
    
    private String m_icon;
    public AddVertexOperation(String icon) {
        m_icon = icon;
    }
    
    @Override
    public boolean display(List<Object> targets, OperationContext operationContext) {
        return false;
    }

    @Override
    public boolean enabled(List<Object> targets,OperationContext operationContext) {
        if(targets.size() > 1) return false;
        
        Object itemId = targets.size() == 1 ? targets.get(0) : null;
        
        return itemId == null || operationContext.getGraphContainer().getVertexContainer().containsId(itemId);
    }

    @Override
    public String getId() {
        return null;
    }

    void connectNewVertex(String vertexId, String icon, DisplayState graphContainer) {
        Object vertId1 = m_topologyProvider.addVertex(0, 0, icon);
        m_topologyProvider.setParent(vertId1, Constants.ROOT_GROUP_ID);
        m_topologyProvider.connectVertices(vertexId, vertId1);
        
    }

    public String getIcon() {
        return m_icon;
    }

    public Undoer execute(List<Object> targets, OperationContext operationContext) {
        Object vertexId = targets.isEmpty() ? null : targets.get(0);
        String icon = getIcon();
        if (vertexId == null) {
            if (operationContext.getGraphContainer().getVertexContainer().containsId(Constants.CENTER_VERTEX_ID)) {
            	connectNewVertex(Constants.CENTER_VERTEX_ID, Constants.SERVER_ICON, operationContext.getGraphContainer());
            }
            else {
                Object vertId = m_topologyProvider.addVertex(50, 50, Constants.SERVER_ICON);
                m_topologyProvider.setParent(vertId, Constants.ROOT_GROUP_ID);
                
            }
        } else {
            
            connectNewVertex(vertexId.toString(), icon, operationContext.getGraphContainer());
        }
        operationContext.getGraphContainer().redoLayout();
        
        return null;
    }
    
}