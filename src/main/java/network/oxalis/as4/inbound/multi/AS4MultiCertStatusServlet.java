package network.oxalis.as4.inbound.multi;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import network.oxalis.as4.inbound.OxalisAS4Version;
import network.oxalis.as4.inbound.multi.config.EndpointConfigData;
import network.oxalis.as4.inbound.multi.config.MultiCertConfigData;
import network.oxalis.vefa.peppol.mode.Mode;

@com.mercell.nemhandel.as4.Rewritten(network.oxalis.as4.inbound.AS4StatusServlet.class)
@Singleton
public class AS4MultiCertStatusServlet extends HttpServlet {

	private static final long serialVersionUID = -5845973913995038750L;
	private final Mode mode;
	private MultiCertConfigData multiCertConfigData;

	@Inject
	public AS4MultiCertStatusServlet(Mode mode, As4MultiCertConfigProvider multiCertConf) {
		this.mode = mode;
		this.multiCertConfigData = multiCertConf != null ? multiCertConf.getConfigData() : null;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("text/plain");

		boolean includeBase64 = false;
		if (req.getParameter("base64") != null) {
			includeBase64 = true;
		}

		PrintWriter writer = resp.getWriter();
		writer.println("version.oxalis.as4: " + OxalisAS4Version.getVersion());
		writer.println("version.java: " + System.getProperty("java.version"));
		writer.println("mode: " + mode.getIdentifier());

		if (this.multiCertConfigData != null && this.multiCertConfigData.getEndpointListSize() > 0) {
			writer.println("");
			writer.println("multi.cert.endpoints.size: " + this.multiCertConfigData.getEndpointListSize());
			List<EndpointConfigData> endpoints = this.multiCertConfigData.getEndpointList();
			for (int i = 0; i < endpoints.size(); i++) {
				writer.println("");
				String prefix = "multi.cert.endpoints[" + i + "].";
				EndpointConfigData endpoint = endpoints.get(i);
				writer.println(prefix + "path: " + As4MultiCertInboundModule.PUBLISHED_ENDPOINT_PREFIX + endpoint.getEndpointUrlPath());
				writer.println(prefix + "mode: " + endpoint.getMode().getIdentifier());
				if (endpoint.getEndpointId() != null) {
					writer.println(prefix + "id: " + endpoint.getEndpointId());
				}
				if (endpoint.getEndpointName() != null) {
					writer.println(prefix + "name: " + endpoint.getEndpointName());
				}
				X509Certificate endpointCert = endpoint.getKeystoreCertificate();
				if (endpointCert != null) {
					writer.println(prefix + "certificate: " + endpointCert.getSubjectX500Principal().toString());
					writer.println(prefix + "certificate.sn: " + endpointCert.getSerialNumber());
					writer.println(prefix + "certificate.code: " + endpoint.getKeystoreCertificateCode());
					writer.println(prefix + "certificate.owner: " + endpoint.getKeystoreCertificateOwner());
					if (includeBase64) {
						try {
							writer.println(prefix + "certificate.base64: " + Base64.getEncoder().encodeToString(endpointCert.getEncoded()));
						} catch (Exception e) {
						}
					}
				}
				if (endpoint.getTruststoreFirstCertificate() != null) {
					writer.println(prefix + "truststore.certificate: " + endpoint.getTruststoreFirstCertificate().getSubjectX500Principal().toString());
				}
			}
		}
	}

}
