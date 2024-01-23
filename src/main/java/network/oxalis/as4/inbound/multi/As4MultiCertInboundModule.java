package network.oxalis.as4.inbound.multi;

import javax.servlet.http.HttpServlet;

import org.apache.cxf.wsdl.interceptors.AbstractEndpointSelectionInterceptor;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.google.inject.servlet.ServletModule;

import lombok.extern.slf4j.Slf4j;
import network.oxalis.as4.inbound.As4InboundHandler;
import network.oxalis.as4.inbound.As4Provider;
import network.oxalis.as4.inbound.multi.cert.CertificateCodeExtractor;
import network.oxalis.as4.inbound.multi.cert.PeppolNemHandelCertificateCodeExtractor;

@Slf4j
@com.mercell.nemhandel.as4.Rewritten(network.oxalis.as4.inbound.As4InboundModule.class)
public class As4MultiCertInboundModule extends ServletModule {

	private static final String OXALIS_AS4_MULTICERT = "oxalis-as4-multicert";

	public static final String PUBLISHED_ENDPOINT_PREFIX = "/as4";

	@Override
	protected void configureServlets() {
		log.info("Installing AS4 MultiCert Inbound module...");

		bind(AbstractEndpointSelectionInterceptor.class).to(As4MultiCertEndpointSelector.class);

		bind(Key.get(HttpServlet.class, Names.named(OXALIS_AS4_MULTICERT)))
				.to(As4MultiCertServlet.class)
				.asEagerSingleton();

		bind(As4Provider.class);
		bind(As4MultiCertEndpointsPublisher.class).to(As4MultiCertEndpointsPublisherImpl.class);
		bind(As4InboundHandler.class);
		bind(CertificateCodeExtractor.class).to(PeppolNemHandelCertificateCodeExtractor.class);

		serve(PUBLISHED_ENDPOINT_PREFIX + "/status").with(AS4MultiCertStatusServlet.class);
		serve(PUBLISHED_ENDPOINT_PREFIX + "*").with(Key.get(HttpServlet.class, Names.named(OXALIS_AS4_MULTICERT)));
	}

}
