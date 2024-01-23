package network.oxalis.as4.inbound.multi.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MultiCertConfigData {

	private List<EndpointConfigData> endpointConfigDataList;

	private static MultiCertConfigData EMPTY;
	
	public MultiCertConfigData() {
		this.endpointConfigDataList = new ArrayList<>();
	}
	
	public void add(EndpointConfigData data) {
		this.endpointConfigDataList.add(data);
	}
	
	public static MultiCertConfigData empty() {
		if (EMPTY == null) {
			MultiCertConfigData cd = new MultiCertConfigData();
			MultiCertConfig c = new MultiCertConfig();
			c.setEndpoints(Collections.emptyList());
			EMPTY = cd;
		}
		return EMPTY;
	}

	public EndpointConfigData getEndpointConfigDataByURLPath(String urlPath) {
		if (urlPath != null && this.endpointConfigDataList != null && !this.endpointConfigDataList.isEmpty()) {
			// TODO: Instead of iterator, use hash map to get data
			for (EndpointConfigData endpointConfigData : endpointConfigDataList) {
				if (urlPath.equals(endpointConfigData.getEndpointUrlPath())) {
					return endpointConfigData;
				}
			}
		}
		return null;
	}
	
	public List<EndpointConfigData> getEndpointList() {
		return Collections.unmodifiableList(this.endpointConfigDataList);
	}
	
	public int getEndpointListSize() {
		return this.endpointConfigDataList != null ? this.endpointConfigDataList.size() : 0;
	}

	@Override
	public String toString() {
		return String.valueOf(this.endpointConfigDataList);
	}
	
}