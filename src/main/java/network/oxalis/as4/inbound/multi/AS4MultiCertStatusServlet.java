package network.oxalis.as4.inbound.multi;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import network.oxalis.as4.inbound.OxalisAS4Version;
import network.oxalis.as4.inbound.multi.As4MultiCertConfigProvider.EndpointConfig;
import network.oxalis.as4.inbound.multi.As4MultiCertConfigProvider.MultiCertConfig;
import network.oxalis.vefa.peppol.mode.Mode;

@com.mercell.nemhandel.as4.Rewritten(network.oxalis.as4.inbound.AS4StatusServlet.class)
@Singleton
public class AS4MultiCertStatusServlet extends HttpServlet {

	private static final long serialVersionUID = -5845973913995038750L;
	private final Mode mode;
	private MultiCertConfig multiCertConf;

	@Inject
	public AS4MultiCertStatusServlet(Mode mode, As4MultiCertConfigProvider multiCertConf) {
		this.mode = mode;
		this.multiCertConf = multiCertConf != null ? multiCertConf.getConfig() : null;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("text/plain");

		PrintWriter writer = resp.getWriter();
		writer.println("version.oxalis.as4: " + OxalisAS4Version.getVersion());
		writer.println("version.java: " + System.getProperty("java.version"));
		writer.println("mode: " + mode.getIdentifier());

		if (this.multiCertConf != null && this.multiCertConf.getEndpoints() != null) {
			writer.println("multi.cert.endpoints.size: " + this.multiCertConf.getEndpoints().size());
			List<EndpointConfig> endpoints = this.multiCertConf.getEndpoints();
			for (int i = 0; i < endpoints.size(); i++) {
				EndpointConfig endpoint = endpoints.get(i);
				writer.println("multi.cert.endpoints[" + (i + 1) + "].path: " + endpoint.getUrlPath());
			}
		}
	}

}
