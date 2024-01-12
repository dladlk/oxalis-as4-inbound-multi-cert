package network.oxalis.as4.inbound.multi;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import lombok.extern.slf4j.Slf4j;
import network.oxalis.as4.inbound.multi.config.EndpointConfig;
import network.oxalis.as4.inbound.multi.config.EndpointConfigData;
import network.oxalis.as4.inbound.multi.config.MultiCertConfigData;

@Slf4j
public class As4MultiCertConfigProviderTest {

	@Test
	public void testGetConfig() throws URISyntaxException {
		Config referenceConfig = ConfigFactory.defaultReference();
		
		Path confPath = Paths.get(this.getClass().getResource("/oxalis_home").toURI());
		
		As4MultiCertConfigProvider configProvider = new As4MultiCertConfigProvider(referenceConfig, confPath);
		assertNotNull(configProvider);
		MultiCertConfigData config = configProvider.getConfigData();
		log.info("MultiCert Config: {}", config.getMultiCertConfig());
		assertNotNull(config);
		List<EndpointConfigData> endpoints = config.getEndpointConfigDataList();
		assertNotNull(endpoints);
		assertTrue(config.getEndpointConfigDataList().size() > 0);
		for (EndpointConfigData endpointData : endpoints) {
			EndpointConfig endpoint = endpointData.getEndpointConfig();
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
