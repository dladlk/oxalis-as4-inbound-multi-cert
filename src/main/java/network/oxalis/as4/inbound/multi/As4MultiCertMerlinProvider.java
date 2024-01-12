package network.oxalis.as4.inbound.multi;

import org.apache.wss4j.common.crypto.Merlin;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import network.oxalis.as4.common.MerlinProvider;
import network.oxalis.as4.inbound.multi.config.EndpointConfigData;

@Singleton
@com.mercell.nemhandel.as4.Rewritten(network.oxalis.as4.common.MerlinProvider.class)
public class As4MultiCertMerlinProvider {

	protected Merlin defaultMerlin;

	@Inject
	public As4MultiCertMerlinProvider(MerlinProvider merlinProvider) {
		this.defaultMerlin = merlinProvider.getMerlin();
	}

	public Merlin getMerlin(EndpointConfigData endpointConfigData) {
		Merlin merlin = new Merlin();
		merlin.setCryptoProvider(BouncyCastleProvider.PROVIDER_NAME);
		merlin.setKeyStore(endpointConfigData.getKeystore());
		merlin.setTrustStore(this.defaultMerlin.getTrustStore());
		return merlin;
	}

}
