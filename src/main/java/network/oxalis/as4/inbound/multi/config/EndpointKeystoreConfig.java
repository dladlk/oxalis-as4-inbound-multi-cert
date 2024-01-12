package network.oxalis.as4.inbound.multi.config;

import lombok.Data;
import network.oxalis.as4.inbound.multi.As4MultiCertConfigProvider;

@Data
public class EndpointKeystoreConfig {
	private String path;
	private String password;
	private EndpointKeystoreKeyConfig key;

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("path=");
		sb.append(path);
		sb.append(", pass=");
		sb.append(As4MultiCertConfigProvider.masked(password));
		sb.append(", key:{");
		sb.append(key);
		sb.append("}");
		return sb.toString();
	}
}