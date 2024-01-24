package network.oxalis.as4.inbound.multi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigObject;

import lombok.extern.slf4j.Slf4j;
import network.oxalis.api.lang.OxalisLoadingException;
import network.oxalis.as4.inbound.multi.cert.CertificateCodeExtractor;
import network.oxalis.as4.inbound.multi.config.EndpointConfig;
import network.oxalis.as4.inbound.multi.config.EndpointConfigData;
import network.oxalis.as4.inbound.multi.config.EndpointKeystoreConfig;
import network.oxalis.as4.inbound.multi.config.MultiCertConfig;
import network.oxalis.as4.inbound.multi.config.MultiCertConfigData;
import network.oxalis.commons.certvalidator.api.CrlFetcher;
import network.oxalis.pkix.ocsp.api.OcspFetcher;
import network.oxalis.vefa.peppol.common.lang.PeppolLoadingException;
import network.oxalis.vefa.peppol.mode.Mode;
import network.oxalis.vefa.peppol.security.ModeDetector;

@Singleton
@Slf4j
public class As4MultiCertConfigProvider {

	protected static final String CONFIG_PATH = "oxalis.multicert";
	protected MultiCertConfigData configData;
	protected Path confFolderPath;
	protected Config config;
	protected OcspFetcher ocspFetcher;
	protected CrlFetcher crlFetcher;
	protected CertificateCodeExtractor certificateCodeExtractor;

	@Inject
	public As4MultiCertConfigProvider(Config multiCertConfig, Config modeDetectConfig, @Named("conf") Path confFolderPath, OcspFetcher ocspFetcher, CrlFetcher crlFetcher, CertificateCodeExtractor certificateCodeExtractor) {
		this.config = modeDetectConfig;
		this.confFolderPath = confFolderPath;
		this.ocspFetcher = ocspFetcher;
		this.crlFetcher = crlFetcher;
		this.certificateCodeExtractor = certificateCodeExtractor;
		
		// Make it possible to inject As4MultiCertConfigProvider even if nothing is configured
		if (multiCertConfig != null && multiCertConfig.hasPath(CONFIG_PATH)) {
			ConfigObject prefixObject = multiCertConfig.getObject(CONFIG_PATH);
			Config prefixConfig = prefixObject.toConfig();
			MultiCertConfig multiCertConfigData = ConfigBeanFactory.create(prefixConfig, MultiCertConfig.class);
			this.configData = buildConfigData(multiCertConfigData, this.confFolderPath);
		} else {
			log.info("No MultiCertConfig is configured on key {}", CONFIG_PATH);
			this.configData = MultiCertConfigData.empty();
		}
	}

	protected MultiCertConfigData buildConfigData(MultiCertConfig multiCertConfig, Path confFolderPath) {
		long start = System.currentTimeMillis();
		log.info("Loading data by MultiCertConfig config data {}", multiCertConfig);
		
		if (log.isDebugEnabled()) {
			Set<String> modesKeySet = config.getObject("mode").keySet();
			log.debug("Available modes in detection order({}):", modesKeySet.size());
			for (String token : modesKeySet) {
				log.debug("\t- {}", token);
			}
		}
		
		MultiCertConfigData d = new MultiCertConfigData();
		
		// This logic is copied from network.oxalis.commons.mode.ModeProvider.get()
        Map<String, Object> modeDetectionObjectStorage = new HashMap<>();
        if (ocspFetcher != null) {
        	modeDetectionObjectStorage.put("ocsp_fetcher", ocspFetcher);
        }
        if (crlFetcher != null) {
        	modeDetectionObjectStorage.put("crlFetcher", crlFetcher);
        }

        Map<String, EndpointConfig> endpointIdSet = new HashMap<>();
        Map<String, EndpointConfig> endpointUrlPathSet = new HashMap<>();
		for (EndpointConfig endpointConfig : multiCertConfig.getEndpoints()) {
			log.info("Building config data by config {}", endpointConfig);
			
			if (!isValidConfig(endpointConfig)) {
				log.warn("Skip endpoint configuration {} as invalid", endpointConfig);
				continue;
			}
			
			if (!isValidUniqueKey(endpointConfig, endpointConfig.getId(), endpointIdSet, "id")) {
				continue;
			}
			if (!isValidUniqueKey(endpointConfig, endpointConfig.getUrlPath(), endpointUrlPathSet, "urlPath")) {
				continue;
			}

			EndpointKeystoreConfig keystoreConf = endpointConfig.getKeystore();
			KeyStore keyStore = loadKeyStore(keystoreConf, confFolderPath);

			String keyAlias = keystoreConf.getKey().getAlias();
			X509Certificate keystoreCertificate;
			try {
				keystoreCertificate = (X509Certificate) keyStore.getCertificate(keyAlias);
			} catch (Exception e) {
				log.error("Cannot find certificate by alias '" + keyAlias + "' in keystore by path " + keystoreConf.getPath() + ", skip endpoint configuration for " + endpointConfig, e);
				continue;
			}
			
			String keystoreCertificateCode = null;
			if (keystoreCertificate != null) {
				keystoreCertificateCode = certificateCodeExtractor.extract(keystoreCertificate);
			}

			Mode mode;
			try {
				mode = loadMode(modeDetectionObjectStorage, keystoreCertificate);
			} catch (Exception e) {
				log.error("Cannot detect mode by certificate " + keystoreCertificate.getSubjectX500Principal() + " from keystore by path " + keystoreConf.getPath() + " and alias " + keystoreConf.getKey().getAlias() + ", skip endpoint configuration for " + endpointConfig, e);
				continue;
			}

			KeyStore truststore = loadTrustStoreApFromConf(mode, confFolderPath).orElse(null);
			X509Certificate truststoreFirstCertificate = loadTruststoreFirstCertificate(mode, truststore);
			d.add(EndpointConfigData.of(endpointConfig, keyStore, keystoreCertificate, keystoreCertificateCode, truststore, truststoreFirstCertificate, mode));
		}
		log.info("Loaded {} endpoints in {} ms", d.getEndpointListSize(), System.currentTimeMillis() - start);
		return d;
	}

	protected boolean isValidUniqueKey(EndpointConfig endpointConfig, String value, Map<String, EndpointConfig> map, String field) {
		EndpointConfig duplicate;
		String key = value.toLowerCase();
		if ((duplicate = map.get(key)) != null) {
			log.warn("Skip endpoint configuration {} because its {}={} is duplicated in {}", endpointConfig, field, key, duplicate);
			return false;
		}
		map.put(key, endpointConfig);
		return true;
	}

	protected boolean isValidConfig(EndpointConfig ec) {
		if (ec == null) {
			log.warn("Passed EndpoingConfig is null");
			return false;
		}
		if (ec.getUrlPath() == null) {
			log.warn("UrlPath is null at config {}", ec);
			return false;
		}
		if (ec.getKeystore() == null) {
			log.warn("Keystore config is null at config {}", ec);
			return false;
		}
		if (ec.getKeystore().getKey() == null) {
			log.warn("Keystore key config is null at config {}", ec);
			return false;
		}
		if (ec.getKeystore().getPassword() == null) {
			log.warn("Keystore password is null at config {}", ec);
			return false;
		}
		if (ec.getKeystore().getPath() == null) {
			log.warn("Keystore path is null at config {}", ec);
			return false;
		}
		if (ec.getKeystore().getKey().getPassword() == null) {
			log.warn("Keystore key password is null at config {}", ec);
			return false;
		}
		if (ec.getKeystore().getKey().getAlias() == null) {
			log.warn("Keystore key alias is null at config {}", ec);
			return false;
		}
		return true;
	}

	protected Mode loadMode(Map<String, Object> modeDetectionObjectStorage, X509Certificate keystoreCertificate) throws PeppolLoadingException {
		log.info("Detect mode for certificate {}", keystoreCertificate.getSubjectX500Principal());
		return ModeDetector.detect(keystoreCertificate, config, modeDetectionObjectStorage);
	}

	protected X509Certificate loadTruststoreFirstCertificate(Mode mode, KeyStore truststore) {
		X509Certificate truststoreFirstCertificate = null;
		if (truststore != null) {
			try {
				StringBuilder sb = new StringBuilder();
				Enumeration<String> aliases = truststore.aliases();
				while (aliases.hasMoreElements()) {
					String anyAlias = aliases.nextElement();
					if (sb.length() > 0) {
						sb.append(", ");
					}
					sb.append(anyAlias);
					X509Certificate certificate = (X509Certificate) truststore.getCertificate(anyAlias);
					if (certificate != null) {
						truststoreFirstCertificate = certificate;
						break;
					}
				}
				if (truststoreFirstCertificate == null) {
					log.warn("No certificate was loaded in truststore of mode " + mode.getIdentifier() + ", scanned next aliases: " + sb.toString());
				}
			} catch (Exception e) {
				log.warn("Failed to extract any certificate from truststore in mode " + mode.getIdentifier(), e);
			}
		}
		return truststoreFirstCertificate;
	}

	public MultiCertConfigData getConfigData() {
		return configData;
	}

	public static String masked(String s) {
		if (s == null) {
			return "null";
		}
		return String.join("", Collections.nCopies(s.length(), "*"));
	}

	protected KeyStore loadKeyStore(EndpointKeystoreConfig endpointKeystoreConf, Path confFolder) {
		if (endpointKeystoreConf == null) {
			return null;
		}

		Path path = confFolder.resolve(endpointKeystoreConf.getPath());

		try {
			KeyStore keystore = KeyStore.getInstance("JKS");
			if (!path.toFile().exists())
				return null;

			log.info("Loading KEYSTORE: {}", path);

			try (InputStream inputStream = Files.newInputStream(path)) {
				keystore.load(inputStream, endpointKeystoreConf.getPassword().toCharArray());
			}
			return keystore;
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
			throw new OxalisLoadingException("Something went wrong during handling of key store.", e);
		} catch (IOException e) {
			throw new OxalisLoadingException(String.format("Error during reading of '%s'.", path), e);
		}
	}

	protected Optional<KeyStore> loadTrustStoreApFromConf(Mode mode, Path confFolder) {
		String truststoreAp = null;
		String truststoreKey = "security.truststore.ap";
		if (mode.hasString(truststoreKey)) {
			truststoreAp = mode.getConfig().getString(truststoreKey);
		}

		if (truststoreAp == null) {
			log.warn("No truststore config is found for mode " + mode.getIdentifier() + " by path " + truststoreKey);
			return Optional.empty();
		}

		log.info("Truststore path: " + truststoreAp);

		Path path = confFolder.resolve(truststoreAp);

		String truststorePasswordKey = mode.getString("security.truststore.password");
		try (InputStream inputStream = getClass().getResourceAsStream(truststoreAp)) {
			if (inputStream != null) {
				KeyStore keystore = KeyStore.getInstance("JKS");

				log.info("Loading TRUSTSTORE from resource: {}", truststoreAp);

				keystore.load(inputStream, truststorePasswordKey.toCharArray());
				return Optional.of(keystore);
			}
		} catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException e) {
			throw new OxalisLoadingException("Unable to load truststore for AP.", e);
		}

		try {
			KeyStore keystore = KeyStore.getInstance("JKS");
			if (!path.toFile().exists()) {
				log.warn("Configured truststore path " + path + " does not exist, skip it!");
				return Optional.empty();
			}

			log.info("Loading TRUSTSTORE from file: {}", path);

			try (InputStream inputStream = Files.newInputStream(path)) {
				keystore.load(inputStream, truststorePasswordKey.toCharArray());
			}
			return Optional.of(keystore);
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
			throw new OxalisLoadingException("Something went wrong during handling of key store.", e);
		} catch (IOException e) {
			throw new OxalisLoadingException(String.format("Error during reading of '%s'.", path), e);
		}
	}
}
