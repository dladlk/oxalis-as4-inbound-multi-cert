# Oxalis-AS4 Inbound Multiple Certificate Endpoints

POC module to check possibility to register multiple CXF endpoints on the same Oxalis instance to work with different certificates.

Initially developed for [NemHandel eDelivery](https://rep.erst.dk/git/openebusiness/nemhandeledelivery) extension of Oxalis-AS4, but should be usable also in a plain Peppol Oxalis-AS4

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
		urlPath = "" #Path relative to /as4 - so "" means just "/as4" e.g. https://test.oes.dk/oxalis/as4
		keystore {
			path=test_certificate.jks
			password = "XXXXXXX"
			key.alias = "test systemcertifikat"
			key.password = "XXXXXXX"
		}
	},
	{
		urlPath = "/endpoint2" # means e.g. https://test.oes.dk/oxalis/as4/endpoint2
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

```
version.oxalis.as4: 1.1.0
version.java: 21
mode: NEMHANDEL_TEST
multi.cert.endpoints.size: 2
multi.cert.endpoints[1].urlPath: 
multi.cert.endpoints[2].urlPath: /endpoint2
```

# Build

`mvn11 clean install`



