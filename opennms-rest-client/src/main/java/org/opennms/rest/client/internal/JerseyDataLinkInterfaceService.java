package org.opennms.rest.client.internal;

import org.opennms.rest.client.DataLinkInterfaceService;
import org.opennms.rest.model.ClientDataLinkInterface;
import org.opennms.rest.model.ClientDataLinkInterfaceList;

public class JerseyDataLinkInterfaceService implements DataLinkInterfaceService  {

    private static String REST_PATH = "links/";
    
    private JerseyClientImpl m_jerseyClient;
        
    public JerseyClientImpl getJerseyClient() {
        return m_jerseyClient;
    }

    public void setJerseyClient(JerseyClientImpl jerseyClient) {
        m_jerseyClient = jerseyClient;
    }

    public ClientDataLinkInterfaceList getAll() {
        return getJerseyClient().get(ClientDataLinkInterfaceList.class, REST_PATH);                
    }
 
    public ClientDataLinkInterface get(Integer id) {
        return getJerseyClient().get(ClientDataLinkInterface.class, REST_PATH+id);
    }
 
    public String getXml(String relativePath) {
        return getJerseyClient().getXml(REST_PATH+relativePath);
    }
}
