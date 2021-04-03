# Spring Boot secure REST API with HTTPS

## Abstract

This project is about an example of secured REST API with a client certificate (X.509 certificate authentication).

In other words, a client verifies a server according to its certificate and the server identifies that client according to a client certificate (so-called mutual authentication).

In connection with Spring Security, it will be possible to perform futher authentication and authorization.

Technologies used:

* Java EE 1.8
* Spring Boot 2.4.4
* Spring Web + Security 
* Embedded Tomcat 9

The IDE used is Spring Tool Suite 4.

## About security

Following to explain how the secured context was created.

### Create certificates for server and client

a) Create folders to store all generated files (separated for client and server)

	mkdir ssl && cd ssl && mkdir client && mkdir server

b) Generate certificates and Keystores (for server and client)

Step 1: Generate server private key (CA) and self-signed certificate in one step

	openssl req -x509 -newkey rsa:4096 -keyout server/serverPrivateKey.pem -out server/server.crt -days 3650 -nodes -subj "/C=US/ST=CA/O=CAOrg, Inc./CN=cadomain.com"

Step 2: Create PKCS12 Keystore containing server private key and related self-sign certificate

	openssl pkcs12 -export -out server/keyStore.p12 -inkey server/serverPrivateKey.pem -in server/server.crt -passout pass:password

Step 3: Generate client's private key and a certificate signing request (CSR)

	openssl req -new -newkey rsa:4096 -keyout client/myPrivateKey.pem -out client/request.csr -nodes -subj "/C=US/ST=CA/O=MyOrg, Inc./CN=mydomain.com"

Step 4: Sign client's CSR with server private key and a related certificate

	openssl x509 -req -days 360 -in client/request.csr -CA server/server.crt -CAkey server/serverPrivateKey.pem -CAcreateserial -out client/vinik.crt -sha256

Step 5: Verify client's certificate (Optional)

	openssl x509 -text -noout -in client/vinik.crt

Step 6: Create PKCS12 Keystore containing client private key and related self-sign certificate 

	openssl pkcs12 -export -out client/client_vinik.p12 -inkey client/myPrivateKey.pem -in client/vinik.crt -certfile server/server.crt -passout pass:password

c) Generate Truststore

Step 7: Generate server Truststore and add server certificate 

	keytool -import -trustcacerts -alias root -file server/server.crt -keystore server/trustStore.jks


### Configure the server to serve HTTPS content

The following refers to configure an embedded Tomcat server inside Spring Boot. The configuration is quite easy, we will change the port to 8443 and configure the server key store generated in the previous steps:

* Define a custom port (instead of the default 8080): server.port=8443
* Specify the format used for the keystore: server.ssl.key-store-type=PKCS12
* Specify the path to the keystore containing the certificate: server.ssl.key-store=classpath:keyStore.p12
* Specify the password used to generate the certificate: server.ssl.key-store-password=password

### Configure the server to require a client certificate

To have server to require a client certificate (i.e. the mutual authentication) it must be configured a trust store. This way the server ensures that only clients with a valid certificate are able to call exposed REST API. Other clients will be declined by the server being unable to make correct SSL/TLS handshake (required by mutual authentication).

So the embedded Tomcat configuration seems like this:

* Trust store that holds SSL certificates: server.ssl.trust-store=classpath:trustStore.jks
* Password used to access the trust store: server.ssl.trust-store-password=password
* Type of the trust store: server.ssl.trust-store-type=JKS
* Whether client authentication is wanted ("want") or needed ("need"): server.ssl.client-auth=need

### Spring Security and further client authentication and authorization

When you successfully import client Keystore into your system and the application runs, you can visit the API URL (if you go via browser, the first access of this page displays a window to select the correct certificate to authenticate with the server). The endpoint exposed is also protected by basic authentication. To access the API, use the following credentials:

* username: admin
* password: password

## License
Copyright © 2021 by Vinicio Flamini <io@vinicioflamini.it>
