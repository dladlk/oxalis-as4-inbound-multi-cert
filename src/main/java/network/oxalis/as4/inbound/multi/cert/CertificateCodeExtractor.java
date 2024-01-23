package network.oxalis.as4.inbound.multi.cert;

import java.security.cert.X509Certificate;

public interface CertificateCodeExtractor {

	public String extract(X509Certificate certificate);
	
}
