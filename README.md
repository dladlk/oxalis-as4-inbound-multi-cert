# Oxalis-AS4 Inbound Multiple Certificate Endpoints

POC module to check possibility to register multiple CXF endpoints on the same Oxalis instance to work with different certificates

# Installation

Just place jar file into lib folder of your Oxalis installation

# Configuration

Put next part into reference.conf or oxalis.conf of your endpoint

```
# START Multi-cert Module

# Disable the standard AS4 Inbound module
oxalis.module.as4.inbound.enabled = false

# Enable multicert
oxalis.module.as4.inboundmulticert = {
    class = network.oxalis.as4.inbound.multi.As4MultiCertInboundModule
} 

# Configure multicert
oxalis.multicert.endpoints = [
	{
		path = "" #Path relative to /as4 - so "" means just "/as4" e.g. https://test.oes.dk/oxalis/as4
		keystore {
			path=test_certificate.jks
			password = "XXXXXXX"
			key.alias = "test systemcertifikat"
			key.password = "XXXXXXX"
		}
	},
	{
		path = "/endpoint2" # means e.g. https://test.oes.dk/oxalis/as4/endpoint2
		keystore {
			path="ned_mercell_test.p12"
			password = "XXXXXXX"
			key.alias = "mercell test"
			key.password = "XXXXXXX"
		}
	},
]
# END Multi-cert Module

```

# Check configuration

This implementation exposes /as4/status with overview of configured endpoints:

http://localhost:8080/as4/status

# Build

`mvn11 clean install`



