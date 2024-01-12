package network.oxalis.as4.inbound.multi.config;

import java.security.KeyStore;

import lombok.Data;
import network.oxalis.vefa.peppol.mode.Mode;

/**
 * Data class with resolved fields by config
 */
@Data
public class EndpointConfigData {
	private EndpointConfig endpointConfig;
	private KeyStore keystore;
	private Mode mode;
}