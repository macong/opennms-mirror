package org.opennms.rest.client;

public class DataLinkInterfaceRestClient extends JerseyClientImpl {

    public DataLinkInterfaceRestClient(String url, String username, String password) {
        super(url,username,password);
    }
    
    public DataLinkInterfaceList getAll() {
        return get(DataLinkInterfaceList.class, "links");                
    }
 
    public DataLinkInterface get(Integer id) {
        return get(DataLinkInterface.class, "links/"+id);
    }
 
}
