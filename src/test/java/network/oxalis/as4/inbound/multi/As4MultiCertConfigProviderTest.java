package network.oxalis.as4.inbound.multi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import lombok.extern.slf4j.Slf4j;
import network.oxalis.as4.inbound.multi.cert.CertificateCodeExtractor;
import network.oxalis.as4.inbound.multi.cert.PeppolNemHandelCertificateCodeExtractor;
import network.oxalis.as4.inbound.multi.config.EndpointConfigData;
import network.oxalis.as4.inbound.multi.config.MultiCertConfigData;

@Slf4j
public class As4MultiCertConfigProviderTest {

	@Test
	public void testGetConfig() throws URISyntaxException {
		Config referenceConfig = ConfigFactory.defaultReference();

		Path confPath = Paths.get(this.getClass().getResource("/oxalis_home").toURI());

		Config localConfig = ConfigFactory.load();
		// Exclude FRTEST and DUMMY modes to spead-up detection and omit DUMMY mode detected before PRODUCTION
		localConfig = localConfig.withoutPath("mode.FRTEST").withoutPath("mode.DUMMY");

		CertificateCodeExtractor extractor = new PeppolNemHandelCertificateCodeExtractor();
		As4MultiCertConfigProvider configProvider = new As4MultiCertConfigProvider(referenceConfig, localConfig, confPath, null, null, extractor);
		assertNotNull(configProvider);
		MultiCertConfigData config = configProvider.getConfigData();
		log.info("MultiCert Config: {}", config);
		assertNotNull(config);
		List<EndpointConfigData> endpoints = config.getEndpointList();
		assertNotNull(endpoints);
		assertEquals(3, endpoints.size());
		for (EndpointConfigData endpoint : endpoints) {
			assertNotNull(endpoint.getEndpointUrlPath());
			assertNotNull(endpoint.getKeystore());
			assertNotNull(endpoint.getKeystorePath());
			assertNotNull(endpoint.getKeystorePassword());
			assertNotNull(endpoint.getKeystoreKeyAlias());
			assertNotNull(endpoint.getKeystoreKeyPassword());
		}
	}

}
