package network.oxalis.as4.inbound.multi.holder;

import network.oxalis.as4.inbound.multi.config.EndpointConfigData;

/**
 * Somehow the info about endpoint config, which processes inbound request, should be passed
 * 
 * to persister. But Oxalis AS4 InboundPersister.persist is quite limited, so let's use
 * 
 * ThreadLocal for it.<br/>
 * 
 * @As4MultiCertEndpointDataPreInterceptor should put data to this container,<br/> 
 * 
 * @As4MultiCertEndpointDataPostInterceptor should clean in.
 */
public class As4MultiCertEndpointDataThreadLocal {

	private static final ThreadLocal<EndpointConfigData> HOLDER = new ThreadLocal<>();

	public static void setData(EndpointConfigData data) {
		HOLDER.set(data);
	}

	public static void clearData() {
		HOLDER.remove();
	}

	public static EndpointConfigData getData() {
		return HOLDER.get();
	}

}
