package network.oxalis.as4.inbound.multi;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import lombok.extern.slf4j.Slf4j;
import network.oxalis.as4.inbound.multi.As4MultiCertConfigProvider.EndpointConfig;
import network.oxalis.as4.inbound.multi.As4MultiCertConfigProvider.MultiCertConfig;

@Slf4j
public class As4MultiCertConfigProviderTest {

	@Test
	public void testGetConfig() {
		Config referenceConfig = ConfigFactory.defaultReference();
		As4MultiCertConfigProvider configProvider = new As4MultiCertConfigProvider(referenceConfig);
		assertNotNull(configProvider);
		MultiCertConfig config = configProvider.getConfig();
		log.info("MultiCert Config: {}", config);
		assertNotNull(config);
		List<EndpointConfig> endpoints = config.getEndpoints();
		assertNotNull(endpoints);
		assertTrue(config.getEndpoints().size() > 0);
		for (EndpointConfig endpoint : endpoints) {
			assertNotNull(endpoint.getUrlPath());
			assertNotNull(endpoint.getKeystore());
			assertNotNull(endpoint.getKeystore().getPath());
			assertNotNull(endpoint.getKeystore().getPassword());
			assertNotNull(endpoint.getKeystore().getKey());
			assertNotNull(endpoint.getKeystore().getKey().getAlias());
			assertNotNull(endpoint.getKeystore().getKey().getPassword());
		}
	}

}
