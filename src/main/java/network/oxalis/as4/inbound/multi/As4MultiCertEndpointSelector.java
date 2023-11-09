package network.oxalis.as4.inbound.multi;

import java.util.Set;

import org.apache.cxf.binding.soap.interceptor.ReadHeadersInterceptor;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.wsdl.interceptors.AbstractEndpointSelectionInterceptor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@com.mercell.nemhandel.as4.Rewritten(network.oxalis.as4.inbound.As4EndpointSelector.class)
public class As4MultiCertEndpointSelector extends AbstractEndpointSelectionInterceptor {

	public static final String ENDPOINT_PATH = "Endpoint-path";

	public As4MultiCertEndpointSelector() {
		super(Phase.READ);
		getAfter().add(ReadHeadersInterceptor.class.getName());
	}

	@Override
	protected Endpoint selectEndpoint(Message message, Set<Endpoint> endpoints) {
		String messageUri = (String) message.get("org.apache.cxf.request.uri");
		log.info("Searching for matching endpoint for message by uri {} among {} registered endpoints", messageUri, endpoints.size());
		
		log.info("Message class: {}", message.getClass());
		log.info("Full uri: {}", messageUri);
		if (messageUri.startsWith("/as4")) {
			messageUri = messageUri.substring("/as4".length());
		}
		log.info("Match endpoint by uri {}", messageUri.length() == 0 ? "DEFAULT" : messageUri);
		String path = messageUri;
		for (Endpoint endpoint : endpoints) {
			String endpointPath = (String) endpoint.get(ENDPOINT_PATH);
			log.info("Check endpoint {}", endpointPath);
			if (path.equals(endpointPath)) {
				log.info("Matched endpoint {}", endpoint);
				return endpoint;
			}
		}
		log.warn("No matching endpoint is found for message {}", message);

		return null;
	}

}
