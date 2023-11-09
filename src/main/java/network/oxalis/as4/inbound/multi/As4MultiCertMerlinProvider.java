package network.oxalis.as4.inbound.multi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.apache.wss4j.common.crypto.Merlin;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import lombok.extern.slf4j.Slf4j;
import network.oxalis.api.lang.OxalisLoadingException;
import network.oxalis.as4.common.MerlinProvider;
import network.oxalis.as4.inbound.multi.As4MultiCertConfigProvider.EndpointKeystore;

@Slf4j
@Singleton
@com.mercell.nemhandel.as4.Rewritten(network.oxalis.as4.common.MerlinProvider.class)
public class As4MultiCertMerlinProvider {

	private Path confFolder;
	private Merlin defaultMerlin;

	@Inject
	public As4MultiCertMerlinProvider(@Named("conf") Path confFolder, MerlinProvider merlinProvider) {
		this.confFolder = confFolder;
		this.defaultMerlin = merlinProvider.getMerlin();
	}

	public Merlin getMerlin(EndpointKeystore endpointKeystoreConf) {
		Merlin merlin = new Merlin();
		merlin.setCryptoProvider(BouncyCastleProvider.PROVIDER_NAME);
		merlin.setKeyStore(this.loadKeyStore(endpointKeystoreConf, confFolder));
		merlin.setTrustStore(this.defaultMerlin.getTrustStore());
		return merlin;
	}

	protected KeyStore loadKeyStore(EndpointKeystore endpointKeystoreConf, Path confFolder) {
		if (endpointKeystoreConf == null) {
			return null;
		}

		Path path = confFolder.resolve(endpointKeystoreConf.getPath());

		try {
			KeyStore keystore = KeyStore.getInstance("JKS");
			if (!path.toFile().exists())
				return null;

			log.info("Loading KEYSTORE: {}", path);

			try (InputStream inputStream = Files.newInputStream(path)) {
				keystore.load(inputStream, endpointKeystoreConf.getPassword().toCharArray());
			}
			return keystore;
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
			throw new OxalisLoadingException("Something went wrong during handling of key store.", e);
		} catch (IOException e) {
			throw new OxalisLoadingException(String.format("Error during reading of '%s'.", path), e);
		}
	}
}
