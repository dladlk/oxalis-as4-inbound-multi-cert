package network.oxalis.as4.inbound.multi;

import static org.apache.cxf.ws.security.SecurityConstants.ENABLE_STREAMING_SECURITY;

import java.util.Arrays;

import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.attachment.As4AttachmentInInterceptor;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.interceptor.CheckFaultInterceptor;
import org.apache.cxf.binding.soap.interceptor.ReadHeadersInterceptor;
import org.apache.cxf.binding.soap.interceptor.StartBodyInterceptor;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.interceptor.StaxInEndingInterceptor;
import org.apache.cxf.interceptor.StaxInInterceptor;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.handler.soap.SOAPHandlerFaultInInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.MultipleEndpointObserver;
import org.apache.cxf.ws.policy.WSPolicyFeature;
import org.apache.cxf.wsdl.interceptors.AbstractEndpointSelectionInterceptor;

import com.google.inject.Inject;

import network.oxalis.as4.inbound.As4FaultInHandler;
import network.oxalis.as4.inbound.As4Interceptor;
import network.oxalis.as4.inbound.As4Provider;
import network.oxalis.as4.inbound.AttachmentCleanupInterceptor;
import network.oxalis.as4.inbound.MessagingHandler;
import network.oxalis.as4.inbound.SetPolicyInInterceptor;
import network.oxalis.as4.inbound.SetPolicyOutInterceptor;
import network.oxalis.as4.inbound.multi.holder.As4MultiCertEndpointDataPostInterceptor;
import network.oxalis.as4.inbound.multi.holder.As4MultiCertEndpointDataPreInterceptor;
import network.oxalis.as4.inbound.multi.listener.EndInterceptor;
import network.oxalis.as4.inbound.multi.listener.StartInterceptor;

@com.mercell.nemhandel.as4.Rewritten(network.oxalis.as4.inbound.As4EndpointsPublisherImpl.class)
public class As4MultiCertEndpointsPublisherImpl implements As4MultiCertEndpointsPublisher {

    @Inject
    private As4Provider as4Provider;

    @Inject
    private AbstractEndpointSelectionInterceptor endpointSelector;

    @Inject
    private As4FaultInHandler as4FaultInHandler;

    @Inject
    private As4Interceptor oxalisAs4Interceptor;

    @Inject
    private SetPolicyInInterceptor setPolicyInInterceptor;

    @Inject
    private SetPolicyOutInterceptor setPolicyOutInterceptor;
    
    @Inject
    private As4MultiCertEndpointDataPreInterceptor endpointDataPreInterceptor;

    @Inject
    private As4MultiCertEndpointDataPostInterceptor endpointDataPostInterceptor;

    @Inject
    private StartInterceptor startListener;

    @Inject
    private EndInterceptor endListener;
    
    @Override
    public EndpointImpl publish(Bus bus, String path, String fullUri) {
        EndpointImpl endpoint = (EndpointImpl) Endpoint.publish(path, as4Provider,
                new LoggingFeature(),
                new WSPolicyFeature());

        endpoint.getServer().getEndpoint().put("allow-multiplex-endpoint", Boolean.TRUE);
        endpoint.getServer().getEndpoint().put(ENABLE_STREAMING_SECURITY, false);
//        endpoint.getServer().getEndpoint()
//                .put(As4EndpointSelector.ENDPOINT_NAME, As4EndpointSelector.OXALIS_AS4_ENDPOINT_NAME);

        endpoint.getServer().getEndpoint()
        		.put(As4MultiCertEndpointSelector.ENDPOINT_PATH, fullUri);

        
        endpoint.getBinding().setHandlerChain(Arrays.asList(as4FaultInHandler, new MessagingHandler()));
        endpoint.getInInterceptors().add(oxalisAs4Interceptor);
        endpoint.getInInterceptors().add(setPolicyInInterceptor);
        endpoint.getInInterceptors().add(new AttachmentCleanupInterceptor());

        endpoint.getOutInterceptors().add(setPolicyOutInterceptor);
        endpoint.getInFaultInterceptors().add(setPolicyInInterceptor);
        endpoint.getOutFaultInterceptors().add(setPolicyOutInterceptor);
        
        endpoint.getInInterceptors().add(endpointDataPreInterceptor);
        endpoint.getInInterceptors().add(endpointDataPostInterceptor);

        endpoint.getInInterceptors().add(startListener);
        endpoint.getOutInterceptors().add(endListener);
        endpoint.getOutFaultInterceptors().add(endListener);
        
        MultipleEndpointObserver newMO = new MultipleEndpointObserver(bus) {
            @Override
            protected Message createMessage(Message message) {
                return new SoapMessage(message);
            }
        };

        newMO.getBindingInterceptors().add(new As4AttachmentInInterceptor());
        newMO.getBindingInterceptors().add(new StaxInInterceptor());
        newMO.getBindingInterceptors().add(new StaxInEndingInterceptor());
        newMO.getBindingInterceptors().add(new SOAPHandlerFaultInInterceptor(endpoint.getBinding()));
        newMO.getBindingInterceptors().add(new ReadHeadersInterceptor(bus, (SoapVersion) null));
        newMO.getBindingInterceptors().add(new StartBodyInterceptor());
        newMO.getBindingInterceptors().add(new CheckFaultInterceptor());


        // Add in a default selection interceptor
        newMO.getRoutingInterceptors().add(endpointSelector);
        newMO.getEndpoints().add(endpoint.getServer().getEndpoint());

        endpoint.getServer().getDestination().setMessageObserver(newMO);

        return endpoint;
    }
}
