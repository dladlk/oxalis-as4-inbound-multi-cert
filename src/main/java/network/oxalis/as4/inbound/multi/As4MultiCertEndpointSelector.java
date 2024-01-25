package network.oxalis.as4.inbound.multi;

import java.util.Set;

import org.apache.cxf.binding.soap.interceptor.ReadHeadersInterceptor;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.wsdl.interceptors.AbstractEndpointSelectionInterceptor;

import lombok.extern.slf4j.Slf4j;
import network.oxalis.as4.inbound.multi.config.EndpointConfigData;

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
		log.debug("Searching for matching endpoint for message by uri {} among {} registered endpoints", messageUri, endpoints.size());
		
		log.debug("Match endpoint by uri {}", messageUri.length() == 0 ? "DEFAULT" : messageUri);
		String path = messageUri;
		for (Endpoint endpoint : endpoints) {
			String endpointPath = (String) endpoint.get(ENDPOINT_PATH);
			log.debug("Check endpoint {}", endpointPath);
			if (path.equals(endpointPath)) {
				EndpointConfigData endpointConfigData = (EndpointConfigData) endpoint.get(As4MultiCertConstants.MULTI_CERT_ENDPOINT_CONFIG_DATA);
				log.debug("Matched endpoint {}", endpointConfigData.getEndpointId());
				message.put(As4MultiCertConstants.MULTI_CERT_ENDPOINT_CONFIG_DATA, endpointConfigData);
				return endpoint;
			}
		}
		log.warn("No matching endpoint is found for message {}", message);

		return null;
	}

}
