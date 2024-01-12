package network.oxalis.as4.inbound.multi.config;

import lombok.Data;

/**
 * Config class mapped to properties
 */
@Data
public class EndpointConfig {
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