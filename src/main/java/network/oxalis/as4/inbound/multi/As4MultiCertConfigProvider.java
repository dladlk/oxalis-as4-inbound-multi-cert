package network.oxalis.as4.inbound.multi;

import java.util.Collections;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigObject;

import lombok.Data;

@Singleton
public class As4MultiCertConfigProvider {

	private static final String CONFIG_PATH = "oxalis.multicert";
	private MultiCertConfig config;

	@Inject
	public As4MultiCertConfigProvider(Config conf) {
		ConfigObject prefixObject = conf.getObject(CONFIG_PATH);
		Config prefixConfig = prefixObject.toConfig();
		config = ConfigBeanFactory.create(prefixConfig, MultiCertConfig.class);
	}

	@Data
	public static class MultiCertConfig {
		private List<Endpoint> endpoints;
		
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
				Endpoint endpoint = this.endpoints.get(i);
				sb.append(endpoint);
				sb.append("}\n");
			}
			sb.append("]");
			return sb.toString();
		}
	}

	@Data
	public static class Endpoint {
		private String path;
		private EndpointKeystore keystore;
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("path=");
			sb.append(path);
			sb.append(", keystore={");
			sb.append(keystore);
			sb.append("}");
			return sb.toString();
		}			
	}

	@Data
	public static class EndpointKeystore {
		private String path;
		private String password;
		private EndpointKeystoreKey key;
		
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
	public static class EndpointKeystoreKey {
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

	public MultiCertConfig getConfig() {
		return config;
	}

	public void setConfig(MultiCertConfig config) {
		this.config = config;
	}
	
	public static String masked(String s) {
		if (s == null) {
			return "null";
		}
		return String.join("", Collections.nCopies(s.length(), "*"));
	}

}
