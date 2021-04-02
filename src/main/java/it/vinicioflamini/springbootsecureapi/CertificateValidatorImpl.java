package it.vinicioflamini.springbootsecureapi;

import java.net.URL;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CertificateValidatorImpl implements CertificateValidator {

	private static final Logger logger = LoggerFactory.getLogger(CertificateValidatorImpl.class);

	public boolean validate(String httpURL) {
		HttpsURLConnection connection = null;
		boolean result = false;
		try {
			// Create connection
			logger.info(String.format("Try to connect to the URL %s ...", httpURL));
			URL url = new URL(httpURL);
			connection = (HttpsURLConnection) url.openConnection();

			// Prepare a GET request Action
			connection.setRequestMethod("GET");
			connection.setRequestProperty("User-Agent", "Demo App Client");
			connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml");
			connection.setRequestProperty("Accept-Language", "it-IT,it");

			connection.setUseCaches(false);
			connection.setDoOutput(true);

			// Create a SSL SocketFactory
			SSLSocketFactory sslSocketFactory = getFactorySimple();
			connection.setSSLSocketFactory(sslSocketFactory);

			logger.info("HTTP Response Code {}", connection.getResponseCode());
			logger.info("HTTP Response Message {}", connection.getResponseMessage());
			logger.info("HTTP Content Length {}", connection.getContentLength());
			logger.info("HTTP Content Type {}", connection.getContentType());
			logger.info("HTTP Cipher Suite {}", connection.getCipherSuite());

			Certificate[] serverCertificate = connection.getServerCertificates();

			for (Certificate certificate : serverCertificate) {
				logger.info("Certificate Type {}", certificate.getType());

				if (certificate instanceof X509Certificate) {
					X509Certificate x509cert = (X509Certificate) certificate;

					// Get subject and validate
					Principal principal = x509cert.getSubjectDN();
					result = !result
							? principal.getName().equals("CN=Cloudflare Inc ECC CA-3, O=\"Cloudflare, Inc.\", C=US")
							: result;
					if (result) {
						logger.info("Certificate Subject DN {} recognized!", principal.getName());
					}

					// Get issuer
					// principal = x509cert.getIssuerDN();
					// logger.info("Certificate IssuerDn {}", principal.getName());
				}
			}

			// Close Connection
			connection.disconnect();

		} catch (Exception e) {
			if (connection != null) {
				connection.disconnect();
			}
			logger.error(e.getMessage());
		}

		if (!result) {
			logger.info("SSL Certificate not recognized!");
		}

		return result;
	}

	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	private static SSLSocketFactory getFactorySimple() throws Exception {
		SSLContext context = SSLContext.getInstance("TLS");
		context.init(null, null, null);
		return context.getSocketFactory();
	}
}
