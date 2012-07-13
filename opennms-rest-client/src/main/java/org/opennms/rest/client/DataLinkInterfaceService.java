package org.opennms.rest.client;

import org.opennms.rest.client.internal.model.ClientDataLinkInterface;
import org.opennms.rest.client.internal.model.ClientDataLinkInterfaceList;

public interface DataLinkInterfaceService {
    
    public ClientDataLinkInterfaceList getAll();
    
    public ClientDataLinkInterface get(Integer id);

}
