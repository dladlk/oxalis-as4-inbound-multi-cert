package network.oxalis.as4.inbound.multi.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Data;

@Data
public class MultiCertConfigData {

	private MultiCertConfig multiCertConfig;
	private List<EndpointConfigData> endpointConfigDataList;

	private static MultiCertConfigData EMPTY;
	
	public MultiCertConfigData() {
		this.endpointConfigDataList = new ArrayList<>();
	}
	
	public static MultiCertConfigData empty() {
		if (EMPTY == null) {
			MultiCertConfigData cd = new MultiCertConfigData();
			MultiCertConfig c = new MultiCertConfig();
			c.setEndpoints(Collections.emptyList());
			cd.setMultiCertConfig(c);
			
			EMPTY = cd;
		}
		return EMPTY;
	}
}