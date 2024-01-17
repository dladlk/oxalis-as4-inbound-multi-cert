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
import java.util.ArrayList;
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
import network.oxalis.as4.inbound.multi.config.EndpointConfig;
import network.oxalis.as4.inbound.multi.config.EndpointConfigData;
import network.oxalis.as4.inbound.multi.config.EndpointKeystoreConfig;
import network.oxalis.as4.inbound.multi.config.MultiCertConfig;
import network.oxalis.as4.inbound.multi.config.MultiCertConfigData;
import network.oxalis.commons.certvalidator.api.CrlFetcher;
import network.oxalis.pkix.ocsp.api.OcspFetcher;
import network.oxalis.vefa.peppol.mode.Mode;
import network.oxalis.vefa.peppol.security.ModeDetector;

@Singleton
@Slf4j
public class As4MultiCertConfigProvider {

	protected static final String CONFIG_PATH = "oxalis.multicert";
	protected MultiCertConfigData configData;
	private Path confFolderPath;
	private Config config;
	private OcspFetcher ocspFetcher;
	private CrlFetcher crlFetcher;

	@Inject
	public As4MultiCertConfigProvider(Config multiCertConfig, Config modeDetectConfig, @Named("conf") Path confFolderPath, OcspFetcher ocspFetcher, CrlFetcher crlFetcher) {
		this.config = modeDetectConfig;
		this.confFolderPath = confFolderPath;
		this.ocspFetcher = ocspFetcher;
		this.crlFetcher = crlFetcher;
		
		// Make it possible to inject As4MultiCertConfigProvider even if nothing is configured
		if (multiCertConfig.hasPath(CONFIG_PATH)) {
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
		d.setMultiCertConfig(multiCertConfig);
		
		// This logic is copied from network.oxalis.commons.mode.ModeProvider.get()
        Map<String, Object> modeDetectionObjectStorage = new HashMap<>();
        if (ocspFetcher != null) {
        	modeDetectionObjectStorage.put("ocsp_fetcher", ocspFetcher);
        }
        if (crlFetcher != null) {
        	modeDetectionObjectStorage.put("crlFetcher", crlFetcher);
        }

		d.setEndpointConfigDataList(new ArrayList<>());
		for (EndpointConfig endpointConfig : multiCertConfig.getEndpoints()) {
			EndpointConfigData ed = new EndpointConfigData();
			ed.setEndpointConfig(endpointConfig);

			log.info("Building config data by config {}", endpointConfig);

			EndpointKeystoreConfig keystoreConf = endpointConfig.getKeystore();
			ed.setKeystore(loadKeyStore(keystoreConf, confFolderPath));

			String keyAlias = keystoreConf.getKey().getAlias();
			try {
				X509Certificate certificate = (X509Certificate) ed.getKeystore().getCertificate(keyAlias);
				ed.setKeystoreCertificate(certificate);
			} catch (Exception e) {
				log.error("Cannot find certificate by alias '" + keyAlias + "' in keystore by path " + keystoreConf.getPath() + ", skip endpoint configuration for " + ed.getEndpointConfig(), e);
				continue;
			}

			log.info("Detect mode for certificate {}", ed.getKeystoreCertificate().getSubjectX500Principal());
			Mode mode;
			try {
				mode = ModeDetector.detect(ed.getKeystoreCertificate(), config, modeDetectionObjectStorage);
				ed.setMode(mode);
			} catch (Exception e) {
				log.error("Cannot detect mode by certificate " + ed.getKeystoreCertificate().getSubjectX500Principal() + " from keystore by path " + keystoreConf.getPath() + " and alias " + keystoreConf.getKey().getAlias() + ", skip endpoint configuration for " + ed.getEndpointConfig(), e);
				continue;
			}

			ed.setTruststore(loadTrustStoreApFromConf(ed.getMode(), confFolderPath).orElse(null));
			if (ed.getTruststore() != null) {
				try {
					StringBuilder sb = new StringBuilder();
					Enumeration<String> aliases = ed.getTruststore().aliases();
					while (aliases.hasMoreElements()) {
						String anyAlias = aliases.nextElement();
						if (sb.length() > 0) {
							sb.append(", ");
						}
						sb.append(anyAlias);
						X509Certificate certificate = (X509Certificate) ed.getTruststore().getCertificate(anyAlias);
						if (certificate != null) {
							ed.setTruststoreFirstCertificate(certificate);
							break;
						}
					}
					if (ed.getTruststoreFirstCertificate() == null) {
						log.warn("No certificate was loaded in truststore of mode " + mode.getIdentifier() + ", scanned next aliases: " + sb.toString());
					}
				} catch (Exception e) {
					log.warn("Failed to extract any certificate from truststore in mode " + mode.getIdentifier(), e);
				}
			}
			d.getEndpointConfigDataList().add(ed);
		}
		log.info("Loaded {} endpoints in {} ms", d.getEndpointConfigDataList().size(), System.currentTimeMillis() - start);
		return d;
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
