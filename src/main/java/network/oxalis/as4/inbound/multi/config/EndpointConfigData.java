package network.oxalis.as4.inbound.multi.config;

import java.security.KeyStore;
import java.security.cert.X509Certificate;

import lombok.Data;
import network.oxalis.vefa.peppol.mode.Mode;

/**
 * Data class with resolved fields by config
 */
@Data
public class EndpointConfigData {
	private EndpointConfig endpointConfig;
	private KeyStore keystore;
	private X509Certificate keystoreCertificate;
	private KeyStore truststore;
	private X509Certificate truststoreFirstCertificate;
	private Mode mode;

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (mode != null) {
			sb.append(mode.getIdentifier());
		} else {
			sb.append("null");
		}
		sb.append(" by ");
		sb.append(this.endpointConfig);
		sb.append(" on ");
		if (keystoreCertificate != null) {
			sb.append(keystoreCertificate.getSubjectX500Principal());
		}
		return sb.toString();
	}
}