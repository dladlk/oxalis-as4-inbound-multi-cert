package network.oxalis.as4.inbound.multi;

import static org.testng.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.EnumSet;
import java.util.UUID;

import javax.servlet.DispatcherType;

import org.apache.commons.io.IOUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.feature.Feature;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.util.Modules;
import com.typesafe.config.Config;

import lombok.extern.slf4j.Slf4j;
import network.oxalis.api.lang.OxalisTransmissionException;
import network.oxalis.api.outbound.MessageSender;
import network.oxalis.api.outbound.TransmissionRequest;
import network.oxalis.api.outbound.TransmissionResponse;
import network.oxalis.api.tag.Tag;
import network.oxalis.as4.api.MessageIdGenerator;
import network.oxalis.as4.common.DefaultMessageIdGenerator;
import network.oxalis.as4.util.OxalisAlgorithmSuiteLoader;
import network.oxalis.as4.util.PeppolConfiguration;
import network.oxalis.commons.guice.GuiceModuleLoader;
import network.oxalis.vefa.peppol.common.model.DocumentTypeIdentifier;
import network.oxalis.vefa.peppol.common.model.Endpoint;
import network.oxalis.vefa.peppol.common.model.Header;
import network.oxalis.vefa.peppol.common.model.ParticipantIdentifier;
import network.oxalis.vefa.peppol.common.model.ProcessIdentifier;
import network.oxalis.vefa.peppol.common.model.TransportProfile;
import network.oxalis.vefa.peppol.mode.Mode;

@Slf4j
public class SendLocalTest {

	protected Injector injector;
	protected Server server;

	private static boolean START_SERVER = true;

	private static final boolean LOG_INSTALLED_MODULES = false;

	private static final int TEST_INVOKATION_COUNT = 1;
	private static final int TEST_THREAD_POOL_SIZE = 1;
	// Activated when running test in multi thread - TestNG threads do not work properly with As4CommonModule cipher suites configuration
	// Otherwise fails with "Algorithm suite "Basic128GCMSha256MgfSha256" is not registered"
	private static final boolean REINITIALIZE_BUS_FOR_MULTITHREAD = TEST_THREAD_POOL_SIZE > 1;

	private final byte[] original;
	private MessageSender messageSender;

	public SendLocalTest() throws Exception {
		this.original = IOUtils.resourceToByteArray("/sbd-test-file.xml");
	}

	@BeforeClass
	public void beforeClass() throws Exception {
		this.injector = this.buildInjector();
		if (START_SERVER) {
			this.server = new Server(8080);
			ServletContextHandler handler = new ServletContextHandler(this.server, "/");
			handler.addFilter(GuiceFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
			handler.addEventListener(new GuiceServletContextListener() {
				protected Injector getInjector() {
					return SendLocalTest.this.injector;
				}
			});
			handler.addServlet(DefaultServlet.class, "/");
			this.server.start();
		}

		messageSender = injector.getInstance(Key.get(MessageSender.class, Names.named("oxalis-as4")));
	}

	@AfterClass
	public void afterClass() throws Exception {
		if (START_SERVER) {
			this.server.stop();
		}
	}

	public Injector buildInjector() {
		return Guice.createInjector(
				Modules.override(new GuiceModuleLoader() {
					@Override
					protected void configure() {
						getModules().forEach(e -> {
							long start = System.currentTimeMillis();
							binder().install(e);
							if (LOG_INSTALLED_MODULES) {
								log.info("Installed module {} in {} ms", e.getClass().getName(), (System.currentTimeMillis() - start));
							}
						});
					}

				}).with(new AbstractModule() {
					@Override
					protected void configure() {
						bind(MessageIdGenerator.class).toInstance(new DefaultMessageIdGenerator("test.com"));
					}
				}));
	}

	@BeforeClass
	public void addLoggingInterceptors() {
		Collection<Feature> features = BusFactory.getDefaultBus().getFeatures();
		LoggingFeature loggingFeature = new LoggingFeature();
		loggingFeature.setLogBinary(false);
		features.add(loggingFeature);
		/*
		 * setting features to getDefaultBus() did not help - as As4CommonModule creates a new Bus and sets it to BusFactory.setThreadDefaultBus(bus);
		 * 
		 * so LoggingFeature configuration changes above are ignored.
		 */
		BusFactory.getThreadDefaultBus().setFeatures(features);
	}

	@Test(invocationCount = TEST_INVOKATION_COUNT, threadPoolSize = TEST_THREAD_POOL_SIZE)
	public void send() throws Exception {
		if (REINITIALIZE_BUS_FOR_MULTITHREAD) {
			Bus bus = BusFactory.getThreadDefaultBus(true);
			new OxalisAlgorithmSuiteLoader(bus);
		}

		String defaultUrl = "http://localhost:8080/as4";
		String secondUrl = "http://localhost:8080/as4/endpoint2";
		String thirdUrl = "http://localhost:8080/as4/endpoint3";
		log.info("Endpoint status: " + loadStatus(defaultUrl));

		send(defaultUrl, "oxalis.keystore");
		send(secondUrl, "oxalis.keystore2");
		send(thirdUrl, "oxalis.keystore3");
	}

	public void send(String serverUrl, String keyStoreKey) throws Exception {
		X509Certificate serverCertificate = loadCertificate(keyStoreKey);

		byte[] payload = original;
		payload = replaceUUID(payload);

		send(serverUrl, serverCertificate, payload);
	}

	private byte[] replaceUUID(byte[] original) {
		String oldId = "bd376dea-9207-4cde-8168-b437e4cbb7cd";
		String newId = UUID.randomUUID().toString();

		log.info("Replace hardcoded GUID with random: " + newId);

		String data = new String(original, StandardCharsets.UTF_8).replaceAll(oldId, newId);
		// TODO: Avoid such CRLF replacement, it hides the real problem - but should be solved
		// when c14n is in place
		data = data.replaceAll("\r\n", "\n");
		return data.getBytes(StandardCharsets.UTF_8);
	}

	protected X509Certificate loadCertificate(String keyStoreKey) throws Exception {
		Config conf = injector.getInstance(Mode.class).getConfig();
		KeyStore secondKeystore = loadKeystore(conf, keyStoreKey);
		X509Certificate serverCertificate = (X509Certificate) secondKeystore.getCertificate(conf.getString(keyStoreKey + ".key.alias"));
		assertNotNull(serverCertificate);
		return serverCertificate;
	}

	public static KeyStore loadKeystore(Config conf, String prefix) throws Exception {
		// note: this keystore is just a contains a sample MitID FOCES3 certificate which is not used for anything other than
		// unit tests
		KeyStore keystore;
		String secondKeystorePassword = conf.getString(prefix + ".password");
		try (InputStream is = SendLocalTest.class.getResourceAsStream("/oxalis_home/" + conf.getString(prefix + ".path"))) {
			keystore = KeyStore.getInstance("PKCS12");
			keystore.load(is, secondKeystorePassword.toCharArray());
		}
		return keystore;
	}

	protected void send(String serverUrl, X509Certificate serverCertificate, byte[] payload) throws OxalisTransmissionException {
		log.info("Start sending to URL " + serverUrl + " with certificate: " + serverCertificate.getSubjectX500Principal());
		long startSend = System.currentTimeMillis();

		Endpoint endpoint = Endpoint.of(TransportProfile.AS4, URI.create(serverUrl), serverCertificate);

		final byte[] finalPayload = payload;
		TransmissionResponse response = messageSender.send(new TestTransmissionRequest(endpoint, finalPayload));

		Assert.assertNotNull(response);
		Assert.assertEquals(TransportProfile.AS4, response.getProtocol());

		log.info("Received response in {} ms", (System.currentTimeMillis() - startSend));
	}

	private String loadStatus(String serverUrl) throws Exception {
		String statusUrl = serverUrl + "/status";

		log.info("Requesting status by " + statusUrl);

		URL url = new URL(statusUrl);
		HttpURLConnection c = (HttpURLConnection) url.openConnection();
		c.setDoInput(true);
		c.connect();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (InputStream in = c.getInputStream()) {
			IOUtils.copy(in, baos);
		}
		return new String(baos.toByteArray(), StandardCharsets.UTF_8);
	}

	protected static final class TestTransmissionRequest implements TransmissionRequest {
		private final Endpoint endpoint;
		private final byte[] finalPayload;

		protected TestTransmissionRequest(Endpoint endpoint, byte[] finalPayload) {
			this.endpoint = endpoint;
			this.finalPayload = finalPayload;
		}

		@Override
		public Endpoint getEndpoint() {
			return endpoint;
		}

		@Override
		public Header getHeader() {
			return Header.newInstance()
					.sender(ParticipantIdentifier.of("0007:5567125082"))
					.receiver(ParticipantIdentifier.of("0007:4455454480"))
					.documentType(DocumentTypeIdentifier.of("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##OIOUBL-2.1::2.1"))
					.process(ProcessIdentifier.of("urn:www.nesubl.eu:profiles:profile5:ver2.0"));
		}

		@Override
		public InputStream getPayload() {
			return new ByteArrayInputStream(finalPayload);
		}

		@Override
		public Tag getTag() {
			return new PeppolConfiguration();
		}
	}
}
