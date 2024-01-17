package network.oxalis.as4.inbound.multi.config;

import java.util.List;

import lombok.Data;

@Data
public class MultiCertConfig {
	
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