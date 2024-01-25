package network.oxalis.as4.inbound.multi;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;

@com.mercell.nemhandel.as4.Rewritten(network.oxalis.as4.inbound.As4EndpointsPublisher.class)
public interface As4MultiCertEndpointsPublisher {

	EndpointImpl publish(Bus bus, String path, String fullUri);
	
}
