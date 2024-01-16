# Oxalis-AS4 Inbound Multiple Certificate Endpoints

POC module to check possibility to register multiple CXF endpoints on the same Oxalis instance to work with different certificates.

Initially developed for [NemHandel eDelivery](https://rep.erst.dk/git/openebusiness/nemhandeledelivery) extension of Oxalis-AS4, but should be usable also in a plain Peppol Oxalis-AS4

# Installation

Just place jar file into lib folder of your Oxalis installation and activate it, but disable standard Oxalis AS4 Inbound module.

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
		id = "default"
		name = "Default endpoint"	
		urlPath = "" #Path relative to /as4 - so "" means just "/as4" e.g. https://ap.io/oxalis/as4
		keystore {
			path=test_certificate.jks
			password = "XXXXXXX"
			key.alias = "test systemcertifikat"
			key.password = "XXXXXXX"
		}
	},
	{
		id = "e2"
		name = "Endpoint 2"
		urlPath = "/endpoint2" # means e.g. https://ap.io/oxalis/as4/endpoint2
		keystore {
			path="test_certificate2.p12"
			password = "XXXXXXX"
			key.alias = "test2"
			key.password = "XXXXXXX"
		}
	},
	{
		id = "e3"
		name = "Endpoint 3"
		urlPath = "/endpoint3" # means e.g. https://ap.io/oxalis/as4/endpoint3
		keystore {
			path="test_certificate3.p12"
			password = "XXXXXXX"
			key.alias = "test3"
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

multi.cert.endpoints.size: 5

multi.cert.endpoints[0].path: /as4
multi.cert.endpoints[0].mode: NEMHANDEL_TEST
multi.cert.endpoints[0].id: default
multi.cert.endpoints[0].name: Default URL - eDelivery test system certificate
multi.cert.endpoints[0].certificate: C=DK, OID.2.5.4.97=NTRDK-97281536, O=Testorganisation nr. 97281536, SERIALNUMBER=UI:DK-O:G:b60fee55-6d77-41b1-a210-445658a727a6, CN=Test systemcertifikat
multi.cert.endpoints[0].certificate.sn: 113690715833268132126522438224254720921509723043
multi.cert.endpoints[0].truststore.certificate: C=DK, O=Den Danske Stat, OU=Test - cti, CN=Den Danske Stat OCES rod-CA

multi.cert.endpoints[1].path: /as4/nemhandel/mercell/test
multi.cert.endpoints[1].mode: NEMHANDEL_TEST
multi.cert.endpoints[1].id: mercellTest
multi.cert.endpoints[1].name: Mercell eDelivery test system certificate
multi.cert.endpoints[1].certificate: C=DK, OID.2.5.4.97=NTRDK-93885472, O=Testorganisation nr. 93885472, SERIALNUMBER=UI:DK-O:G:71a0c9aa-c0ed-4db9-9af1-dd04a0283b0c, CN=Mercell Test
multi.cert.endpoints[1].certificate.sn: 562579981342602225838220567588156577519286405222
multi.cert.endpoints[1].truststore.certificate: C=DK, O=Den Danske Stat, OU=Test - cti, CN=Den Danske Stat OCES rod-CA

multi.cert.endpoints[2].path: /as4/nemhandel/mercell/prod
multi.cert.endpoints[2].mode: NEMHANDEL_PRODUCTION
multi.cert.endpoints[2].id: mercellNemhandelPROD
multi.cert.endpoints[2].name: Mercell eDelivery PROD system certificate
multi.cert.endpoints[2].certificate: C=DK, OID.2.5.4.97=NTRDK-31261430, O=Mercell A/S, SERIALNUMBER=UI:DK-O:G:cf9f7073-612d-476f-94fc-ebcf7bcef16a, CN=Mercell Nemhandel System
multi.cert.endpoints[2].certificate.sn: 705382596647272500499801755376584413325554430627
multi.cert.endpoints[2].truststore.certificate: C=DK, O=Den Danske Stat, CN=Den Danske Stat OCES rod-CA

multi.cert.endpoints[3].path: /as4/peppol/test
multi.cert.endpoints[3].mode: TEST
multi.cert.endpoints[3].id: mercellPeppolTest
multi.cert.endpoints[3].name: Mercell Peppol test certificate
multi.cert.endpoints[3].certificate: CN=PDK000253, OU=PEPPOL TEST AP, O=Mercell A/S, C=DK
multi.cert.endpoints[3].certificate.sn: 128459785080809891633823616432474086109
multi.cert.endpoints[3].truststore.certificate: CN=PEPPOL ACCESS POINT TEST CA - G2, OU=FOR TEST ONLY, O=OpenPEPPOL AISBL, C=BE

multi.cert.endpoints[4].path: /as4/peppol/prod
multi.cert.endpoints[4].mode: PRODUCTION
multi.cert.endpoints[4].id: mercellPeppolPROD
multi.cert.endpoints[4].name: Mercell Peppol PROD certificate
multi.cert.endpoints[4].certificate: CN=PDK000253, OU=PEPPOL PRODUCTION AP, O=Mercell A/S, C=DK
multi.cert.endpoints[4].certificate.sn: 88491489482158613593813270928022585014
multi.cert.endpoints[4].truststore.certificate: CN=PEPPOL ACCESS POINT CA - G2, O=OpenPEPPOL AISBL, C=BE
```

# Build

`mvn11 clean install`



