package network.oxalis.as4.inbound.multi.holder;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import network.oxalis.as4.inbound.multi.As4MultiCertConstants;
import network.oxalis.as4.inbound.multi.config.EndpointConfigData;

@Singleton
public class As4MultiCertEndpointDataPreInterceptor extends AbstractSoapInterceptor {

	@Inject
	public As4MultiCertEndpointDataPreInterceptor() {
		super(Phase.PRE_INVOKE);
	}

	@Override
	public void handleMessage(SoapMessage message) throws Fault {
		EndpointConfigData configData = (EndpointConfigData) message.get(As4MultiCertConstants.MULTI_CERT_ENDPOINT_CONFIG_DATA);
		As4MultiCertEndpointDataThreadLocal.setData(configData);
	}

}
