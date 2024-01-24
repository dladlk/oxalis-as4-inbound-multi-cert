package network.oxalis.as4.inbound.multi.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiCertConfigData {

	private final List<EndpointConfigData> endpointConfigDataList;
	private final Map<String, EndpointConfigData> endpointConfigIdMap;
	private final Map<String, EndpointConfigData> endpointConfigUrlPathMap;

	private static MultiCertConfigData EMPTY;
	
	public MultiCertConfigData() {
		this.endpointConfigDataList = new ArrayList<>();
		this.endpointConfigIdMap = new HashMap<>();
		this.endpointConfigUrlPathMap = new HashMap<>();
	}
	
	public void add(EndpointConfigData data) {
		this.endpointConfigDataList.add(data);
		this.endpointConfigIdMap.put(data.getEndpointId().toLowerCase(), data);
		this.endpointConfigUrlPathMap.put(data.getEndpointUrlPath().toLowerCase(), data);
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
		if (urlPath != null) {
			return this.endpointConfigUrlPathMap.get(urlPath.toLowerCase());
		}
		return null;
	}

	public EndpointConfigData getEndpointConfigDataById(String endpointId) {
		if (endpointId != null) {
			return this.endpointConfigIdMap.get(endpointId.toLowerCase());
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