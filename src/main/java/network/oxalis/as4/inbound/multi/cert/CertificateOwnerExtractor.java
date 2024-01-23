package network.oxalis.as4.inbound.multi.cert;

import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;

public class CertificateOwnerExtractor {

	public static String extract(X509Certificate cert) {
		if (cert == null) {
			return null;
		}
		X500Principal x500Principal = cert.getSubjectX500Principal();
		X500Name x500name = new X500Name(x500Principal.getName());
		RDN[] rdns = x500name.getRDNs(BCStyle.O);
		if (rdns.length > 0) {
			RDN oi = rdns[0];
			return IETFUtils.valueToString(oi.getFirst().getValue());
		}
		return null;
	}
}
