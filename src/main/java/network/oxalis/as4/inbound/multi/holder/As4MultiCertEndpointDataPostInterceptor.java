package network.oxalis.as4.inbound.multi.holder;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class As4MultiCertEndpointDataPostInterceptor extends AbstractSoapInterceptor {

	@Inject
	public As4MultiCertEndpointDataPostInterceptor() {
		// TODO: Ensure that it is called in case of Fault too!
		super(Phase.POST_INVOKE);
	}

	@Override
	public void handleMessage(SoapMessage message) throws Fault {
		As4MultiCertEndpointDataThreadLocal.clearData();
	}

}
