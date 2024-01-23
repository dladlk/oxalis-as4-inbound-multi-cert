package network.oxalis.as4.inbound.multi.cert;

import java.security.cert.X509Certificate;

import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;

/**
 * Combines functionality of Peppol filling of PartyInfo/From,To and NemHandel CVR extraction from certificate.
 * 
 * At first try to use NemHandel as more specific, otherwise - use Peppol common way.
 */
public class PeppolNemHandelCertificateCodeExtractor implements CertificateCodeExtractor {

	@Override
	public String extract(X509Certificate cert) {
		if (cert == null) {
			return null;
		}
		// Both peppol and nemhandel are based on subject princiapl X500name
		X500Name x500name = new X500Name(cert.getSubjectX500Principal().getName());
		// At first try nemhandel
		String nemhandelCVR = extractNemhandelCVR(x500name);
		if (nemhandelCVR != null) {
			return nemhandelCVR;
		}
		// Ottherwise - just return common name as Peppol does
		return extractCommonName(x500name);
	}

	protected String extractNemhandelCVR(X500Name x500name) {
		// NemHandel has 2.5.4.97 (org identifier) of specific structure
		RDN[] rdns = x500name.getRDNs(BCStyle.ORGANIZATION_IDENTIFIER);
		if (rdns.length > 0) {
			RDN oi = rdns[0];
			String firstValue = IETFUtils.valueToString(oi.getFirst().getValue());
			String cvr = extractCvr(firstValue);
			if (cvr != null) {
				return cvr;
			}
		}
		return null;
	}

	/*
	 * See network.oxalis.as4.outbound.MessagingProvider.createPartyInfo(TransmissionRequest) invokation
	 * 
	 * of network.oxalis.commons.security.CertificateUtils.extractCommonName(X509Certificate)
	 */
	protected String extractCommonName(X500Name x500name) {
		RDN cn = x500name.getRDNs(BCStyle.CN)[0];
		return IETFUtils.valueToString(cn.getFirst().getValue());
	}

	/*
	 * See dk.erst.oxalis.as4.util.CertificateUtil.extractCvr(String)
	 */
	protected String extractCvr(String organizationIdentifier) {
		if (organizationIdentifier == null) {
			return null;
		}
		// MitID certificates have organization identifier = NTRDK-<cvr>
		if (organizationIdentifier.startsWith("NTRDK-")) {
			organizationIdentifier = organizationIdentifier.replace("NTRDK-", "");
		}
		return organizationIdentifier.matches("\\d{8}") ? organizationIdentifier : null;
	}

}
