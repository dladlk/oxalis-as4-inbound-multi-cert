package network.oxalis.as4.inbound.multi.config;

import lombok.Data;
import network.oxalis.as4.inbound.multi.As4MultiCertConfigProvider;

@Data
public class EndpointKeystoreKeyConfig {
	private String alias;
	private String password;

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("alias=");
		sb.append(alias);
		sb.append(", pass=");
		sb.append(As4MultiCertConfigProvider.masked(password));
		return sb.toString();
	}
}