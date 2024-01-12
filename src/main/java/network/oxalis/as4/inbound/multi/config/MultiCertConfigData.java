package network.oxalis.as4.inbound.multi.config;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class MultiCertConfigData {

	private MultiCertConfig multiCertConfig;
	private List<EndpointConfigData> endpointConfigDataList;

	public MultiCertConfigData() {
		this.endpointConfigDataList = new ArrayList<>();
	}
}