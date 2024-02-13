package network.oxalis.as4.inbound.multi;

import java.security.cert.X509Certificate;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import lombok.extern.slf4j.Slf4j;
import network.oxalis.vefa.peppol.common.code.Service;
import network.oxalis.vefa.peppol.common.lang.PeppolLoadingException;
import network.oxalis.vefa.peppol.mode.Mode;
import network.oxalis.vefa.peppol.security.lang.PeppolSecurityException;

@Slf4j

@com.mercell.nemhandel.as4.Rewritten(network.oxalis.vefa.peppol.security.ModeDetector.class)
public class As4MultiCertModeDetector {

	public static Mode detect(X509Certificate certificate, MultiModeCertificateValidator validator) throws PeppolLoadingException {
		return detect(certificate, validator, ConfigFactory.load());
	}

	public static Mode detect(X509Certificate certificate, MultiModeCertificateValidator validator, Config config) throws PeppolLoadingException {
		for (String token : config.getObject("mode").keySet()) {
			if (!"default".equals(token)) {
				try {
					Mode mode = Mode.of(config, token);

					validator.validate(Service.ALL, mode, certificate);

					log.info("Detected mode: {}", mode.getIdentifier());
					if (mode.hasString("security.message")) {
						log.info(mode.getString("security.message"));
					}
					return mode;
				} catch (PeppolSecurityException e) {
					log.info("Detection error ({}): {}", token, e.getMessage());
				}
			}
		}

		throw new PeppolLoadingException(
				String.format("Unable to detect mode for certificate '%s'.", certificate.getSubjectDN().toString()));
	}
}
