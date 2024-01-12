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
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigObject;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import network.oxalis.api.lang.OxalisLoadingException;
import network.oxalis.vefa.peppol.mode.Mode;
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
		d.multiCertConfig = config;

		d.endpointConfigData = new ArrayList<>();
		for (EndpointConfig endpointConfig : config.getEndpoints()) {
			EndpointConfigData ed = new EndpointConfigData();
			ed.endpointConfig = endpointConfig;

			EndpointKeystoreConfig keystoreConf = endpointConfig.getKeystore();
			ed.keystore = loadKeyStore(keystoreConf, confFolderPath);

			X509Certificate certificate;
			String keyAlias = keystoreConf.getKey().getAlias();
			try {
				certificate = (X509Certificate) ed.keystore.getCertificate(keyAlias);
			} catch (Exception e) {
				log.error("Cannot find certificate by alias '" + keyAlias + "' in keystore by path " + keystoreConf.getPath() + ", skip endpoint configuration for " + ed.getEndpointConfig());
				continue;
			}
			try {
				ed.mode = ModeDetector.detect(certificate);
			} catch (Exception e) {
				log.error("Cannot detect mode by certificate " + certificate.getSubjectX500Principal() + " from keystore by path " + keystoreConf.getPath() + " and alias " + keystoreConf.getKey().getAlias() + ", skip endpoint configuration for " + ed.getEndpointConfig());
				continue;
			}
			d.endpointConfigData.add(ed);
		}
		return d;
	}

	@Data
	public static class MultiCertConfig {
		private List<EndpointConfig> endpoints;

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("'endpoint config size'=");
			sb.append(endpoints == null ? -1 : endpoints.size());
			sb.append(", endpoints=[\n");
			for (int i = 0; i < this.endpoints.size(); i++) {
				sb.append("'endpoint config ");
				sb.append(i);
				sb.append("'={");
				EndpointConfig endpoint = this.endpoints.get(i);
				sb.append(endpoint);
				sb.append("}\n");
			}
			sb.append("]");
			return sb.toString();
		}
	}

	@Data
	public static class MultiCertConfigData {
		private MultiCertConfig multiCertConfig;
		private List<EndpointConfigData> endpointConfigData;
	}

	/**
	 * Config class mapped to properties
	 */
	@Data
	public static class EndpointConfig {
		private String id;
		private String name;
		private String urlPath;
		private EndpointKeystoreConfig keystore;

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (id != null) {
				sb.append("#");
				sb.append(id);
				sb.append(" ");
			}
			if (name != null) {
				sb.append("\"");
				sb.append(name);
				sb.append("\" ");
				sb.append(" ");
			}
			sb.append("urlPath=");
			sb.append(urlPath);
			sb.append(", keystore={");
			sb.append(keystore);
			sb.append("}");
			return sb.toString();
		}
	}

	/**
	 * Data class with resolved fields by config
	 */
	@Data
	public static class EndpointConfigData {
		private EndpointConfig endpointConfig;
		private KeyStore keystore;
		private Mode mode;
	}

	@Data
	public static class EndpointKeystoreConfig {
		private String path;
		private String password;
		private EndpointKeystoreKeyConfig key;

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("path=");
			sb.append(path);
			sb.append(", pass=");
			sb.append(masked(password));
			sb.append(", key:{");
			sb.append(key);
			sb.append("}");
			return sb.toString();
		}
	}

	@Data
	public static class EndpointKeystoreKeyConfig {
		private String alias;
		private String password;

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("alias=");
			sb.append(alias);
			sb.append(", pass=");
			sb.append(masked(password));
			return sb.toString();
		}
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
