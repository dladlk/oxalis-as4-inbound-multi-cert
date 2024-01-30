package network.oxalis.as4.inbound.multi.listener;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class StartInterceptor extends AbstractSoapInterceptor {

	private ConcurrencyCounter counter;

	@Inject
	public StartInterceptor(ConcurrencyCounter counter) {
		super(Phase.READ);
		this.counter = counter;
	}

	@Override
	public void handleMessage(SoapMessage message) throws Fault {
		counter.notifyStart();
	}

}
