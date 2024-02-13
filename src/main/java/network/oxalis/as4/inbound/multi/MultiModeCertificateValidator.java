package network.oxalis.as4.inbound.multi;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import network.oxalis.commons.certvalidator.api.CrlFetcher;
import network.oxalis.pkix.ocsp.api.OcspFetcher;
import network.oxalis.vefa.peppol.common.code.Service;
import network.oxalis.vefa.peppol.common.lang.PeppolLoadingException;
import network.oxalis.vefa.peppol.mode.Mode;
import network.oxalis.vefa.peppol.security.api.CertificateValidator;
import network.oxalis.vefa.peppol.security.lang.PeppolSecurityException;

@Singleton
@Slf4j
public class MultiModeCertificateValidator {

	protected OcspFetcher ocspFetcher;
	protected CrlFetcher crlFetcher;
	protected Map<String, Object> objectStorage;
	protected Map<String, CertificateValidator> validatorMap;

	@Inject
	public MultiModeCertificateValidator(OcspFetcher ocspFetcher, CrlFetcher crlFetcher) {
		this.ocspFetcher = ocspFetcher;
		this.crlFetcher = crlFetcher;

		objectStorage = new HashMap<>();
		if (ocspFetcher != null) {
			objectStorage.put("ocsp_fetcher", ocspFetcher);
		}
		if (crlFetcher != null) {
			objectStorage.put("crlFetcher", crlFetcher);
		}
		validatorMap = new ConcurrentHashMap<>();
	}

	public void validate(Service service, Mode mode, X509Certificate certificate) throws PeppolSecurityException, PeppolLoadingException {
		CertificateValidator certificateValidator = getCertificateValidatorByMode(mode);
		certificateValidator.validate(service, certificate);
	}

	protected CertificateValidator getCertificateValidatorByMode(Mode mode) throws PeppolLoadingException {
		if (mode == null) {
			throw new PeppolLoadingException("MultiModeCertificateValidator.getCertificateValidatorByMode with null mode");
		}
		CertificateValidator certificateValidator = validatorMap.get(mode.getIdentifier());
		if (certificateValidator == null) {
			long start = System.currentTimeMillis();
			certificateValidator = mode.initiate("security.validator.class", CertificateValidator.class, objectStorage);
			log.info("Built new certificate validator for mode {} in {}", mode.getIdentifier(), System.currentTimeMillis() - start);
			validatorMap.put(mode.getIdentifier(), certificateValidator);
		}
		return certificateValidator;
	}

}
