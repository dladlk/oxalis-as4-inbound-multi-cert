package network.oxalis.as4.inbound.multi.config;

import java.security.KeyStore;
import java.security.cert.X509Certificate;

import lombok.Getter;
import network.oxalis.as4.inbound.multi.cert.CertificateOwnerExtractor;
import network.oxalis.vefa.peppol.mode.Mode;

/**
 * Data class with resolved fields by config
 */
@Getter
public class EndpointConfigData {

	protected String endpointId;
	protected String endpointName;
	protected String endpointUrlPath;
	protected String keystorePath;
	protected String keystorePassword;
	protected String keystoreKeyAlias;
	protected String keystoreKeyPassword;
	
	protected KeyStore keystore;
	protected X509Certificate keystoreCertificate;
	protected String keystoreCertificateCode;
	protected String keystoreCertificateOwner;
	protected KeyStore truststore;
	protected X509Certificate truststoreFirstCertificate;
	protected Mode mode;

	protected EndpointConfigData(EndpointConfig endpointConfig, KeyStore keystore, X509Certificate keystoreCertificate, String keystoreCertificateCode, KeyStore truststore, X509Certificate truststoreFirstCertificate, Mode mode) {
		if (endpointConfig != null) {
			this.endpointId = endpointConfig.getId();
			this.endpointName = endpointConfig.getName();
			this.endpointUrlPath = endpointConfig.getUrlPath();
			if (endpointConfig.getKeystore() != null) {
				this.keystorePath = endpointConfig.getKeystore().getPath();
				this.keystorePassword = endpointConfig.getKeystore().getPassword();
				if (endpointConfig.getKeystore().getKey() != null) {
					this.keystoreKeyAlias = endpointConfig.getKeystore().getKey().getAlias();
					this.keystoreKeyPassword = endpointConfig.getKeystore().getKey().getPassword();
				}
			}
		}
		this.keystore = keystore;
		this.keystoreCertificate = keystoreCertificate;
		if (keystoreCertificate != null) {
			this.keystoreCertificateOwner = CertificateOwnerExtractor.extract(keystoreCertificate);
		}
		this.keystoreCertificateCode = keystoreCertificateCode;
		this.truststore = truststore;
		this.truststoreFirstCertificate = truststoreFirstCertificate;
		this.mode = mode;
	}

	public static EndpointConfigData of(EndpointConfig endpointConfig, KeyStore keystore, X509Certificate keystoreCertificate, String keystoreCertificateCode, KeyStore truststore, X509Certificate truststoreFirstCertificate, Mode mode) {
		return new EndpointConfigData(endpointConfig, keystore, keystoreCertificate, keystoreCertificateCode, truststore, truststoreFirstCertificate, mode);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (mode != null) {
			sb.append(mode.getIdentifier());
		} else {
			sb.append("null");
		}
		sb.append(" for ");
		sb.append(keystoreCertificateCode);
		sb.append(" of ");
		sb.append(keystoreCertificateOwner);
		return sb.toString();
	}


}