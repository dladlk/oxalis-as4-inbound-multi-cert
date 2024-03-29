package network.oxalis.as4.inbound.multi;

import static org.apache.cxf.rt.security.SecurityConstants.ENCRYPT_CRYPTO;
import static org.apache.cxf.rt.security.SecurityConstants.ENCRYPT_USERNAME;
import static org.apache.cxf.rt.security.SecurityConstants.SIGNATURE_CRYPTO;
import static org.apache.cxf.rt.security.SecurityConstants.SIGNATURE_PASSWORD;
import static org.apache.cxf.rt.security.SecurityConstants.SIGNATURE_USERNAME;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.BusFactory;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JOutInterceptor;
import org.apache.wss4j.common.crypto.Merlin;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import network.oxalis.as4.inbound.multi.config.EndpointConfigData;
import network.oxalis.as4.inbound.multi.config.MultiCertConfigData;

@Slf4j
@Singleton

@com.mercell.nemhandel.as4.Rewritten(network.oxalis.as4.inbound.As4Servlet.class)
public class As4MultiCertServlet extends CXFNonSpringServlet {

	private static final long serialVersionUID = 6519468566215796167L;

	private As4MultiCertConfigProvider configProvider;
	private As4MultiCertEndpointsPublisher endpointsPublisher;
	private As4MultiCertMerlinProvider merlinProvider;

	@Inject
	public As4MultiCertServlet(As4MultiCertConfigProvider configProvider, As4MultiCertEndpointsPublisher endpointsPublisher, As4MultiCertMerlinProvider merlinProvider) {
		this.configProvider = configProvider;
		this.endpointsPublisher = endpointsPublisher;
		this.merlinProvider = merlinProvider;
	}

	@Override
	protected void loadBus(ServletConfig servletConfig) {
		this.bus = BusFactory.getThreadDefaultBus();

		MultiCertConfigData multiCertConfigData = configProvider.getConfigData();

		String contextPath = servletConfig.getServletContext().getContextPath();
		
		log.info("Installing {} endpoints into CXF bus with context path '{}'...", multiCertConfigData.getEndpointListSize(), contextPath);
		if ("/".equals(contextPath)) {
			contextPath = "";
		}
		
		List<EndpointConfigData> endpointList = multiCertConfigData.getEndpointList();
		for (int i = 0; i < endpointList.size(); i++) {
			EndpointConfigData endpointConfigData = endpointList.get(i);

			String urlSuffix = endpointConfigData.getEndpointUrlPath();
			String fullUri = contextPath + As4MultiCertInboundModule.PUBLISHED_ENDPOINT_PREFIX + urlSuffix;
			
			log.info("Publish endpoint on path \'{}\' in mode {}", fullUri, endpointConfigData.getMode().getIdentifier());
			EndpointImpl endpointImpl = endpointsPublisher.publish(getBus(), urlSuffix, fullUri);

			endpointImpl.getProperties().put(As4MultiCertConstants.MULTI_CERT_ENDPOINT_CONFIG_DATA, endpointConfigData);

			Merlin merlin = merlinProvider.getMerlin(endpointConfigData);

			endpointImpl.getProperties().put(SIGNATURE_CRYPTO, merlin);
			endpointImpl.getProperties().put(SIGNATURE_PASSWORD, endpointConfigData.getKeystoreKeyPassword());
			endpointImpl.getProperties().put(SIGNATURE_USERNAME, endpointConfigData.getKeystoreKeyAlias());

			endpointImpl.getProperties().put(ENCRYPT_CRYPTO, merlin);
			endpointImpl.getProperties().put(ENCRYPT_USERNAME, endpointConfigData.getKeystoreKeyAlias());

			endpointImpl.getInInterceptors().add(new PolicyBasedWSS4JInInterceptor());
			endpointImpl.getOutInterceptors().add(new PolicyBasedWSS4JOutInterceptor());

			endpointImpl.getFeatures().add(new LoggingFeature());
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		try {
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().write("Hello AS4 world\n");
		} catch (IOException e) {
			throw new ServletException("Unable to send response", e);
		}
	}
}
