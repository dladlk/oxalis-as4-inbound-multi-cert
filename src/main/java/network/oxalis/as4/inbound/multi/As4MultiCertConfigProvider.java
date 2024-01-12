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
import network.oxalis.vefa.peppol.security.ModeDetector;

@Singleton
@Slf4j
public class As4MultiCertConfigProvider {

	protected static final String CONFIG_PATH = "oxalis.multicert";
	protected MultiCertConfigData configData;
	private Path confFolderPath;

	@Inject
	public As4MultiCertConfigProvider(Config conf, @Named("conf") Path confFolderPath) {
		this.confFolderPath = confFolderPath;
		ConfigObject prefixObject = conf.getObject(CONFIG_PATH);
		Config prefixConfig = prefixObject.toConfig();
		MultiCertConfig config = ConfigBeanFactory.create(prefixConfig, MultiCertConfig.class);
		this.configData = buildConfigData(config, this.confFolderPath);
	}

	protected MultiCertConfigData buildConfigData(MultiCertConfig config, Path confFolderPath) {
		MultiCertConfigData d = new MultiCertConfigData();
		d.setMultiCertConfig(config);

		d.setEndpointConfigDataList(new ArrayList<>());
		for (EndpointConfig endpointConfig : config.getEndpoints()) {
			EndpointConfigData ed = new EndpointConfigData();
			ed.setEndpointConfig(endpointConfig);

			EndpointKeystoreConfig keystoreConf = endpointConfig.getKeystore();
			ed.setKeystore(loadKeyStore(keystoreConf, confFolderPath));

			X509Certificate certificate;
			String keyAlias = keystoreConf.getKey().getAlias();
			try {
				certificate = (X509Certificate) ed.getKeystore().getCertificate(keyAlias);
			} catch (Exception e) {
				log.error("Cannot find certificate by alias '" + keyAlias + "' in keystore by path " + keystoreConf.getPath() + ", skip endpoint configuration for " + ed.getEndpointConfig());
				continue;
			}
			try {
				ed.setMode(ModeDetector.detect(certificate));
			} catch (Exception e) {
				log.error("Cannot detect mode by certificate " + certificate.getSubjectX500Principal() + " from keystore by path " + keystoreConf.getPath() + " and alias " + keystoreConf.getKey().getAlias() + ", skip endpoint configuration for " + ed.getEndpointConfig());
				continue;
			}
			d.getEndpointConfigDataList().add(ed);
		}
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

}
