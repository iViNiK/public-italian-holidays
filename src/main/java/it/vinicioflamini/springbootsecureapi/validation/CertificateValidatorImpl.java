package it.vinicioflamini.springbootsecureapi.validation;

import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CertificateValidatorImpl implements CertificateValidator {

	private static final Logger logger = LoggerFactory.getLogger(CertificateValidatorImpl.class);

	public boolean validate(String httpURL) {
		HttpsURLConnection connection = null;
		boolean isValid = true;
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

					// Get subject
					Principal principal = x509cert.getSubjectDN();
					logger.info("Certificate Subject DN {}", principal.getName());

					// Get issuer
					principal = x509cert.getIssuerDN();
					logger.info("Certificate Issuer DN {}", principal.getName());
				}
			}

		} catch (Exception e) {
			isValid = false;
			logger.error(e.getMessage());
		} finally {
			// Close Connection
			if (connection != null) {
				connection.disconnect();
			}
		}

		return isValid;
	}

	/**
	 * 
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws CertificateException
	 * @throws KeyManagementException
	 * @throws Exception
	 */
	private static SSLSocketFactory getFactorySimple() throws NoSuchAlgorithmException, KeyManagementException {
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			@Override
			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}

			@Override
			public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
				try {
					for (X509Certificate cert : certs) {
						cert.checkValidity();
					}
				} catch (Exception ex) {
					logger.error(ex.getMessage());
					throw new CertificateException("Certificate not trusted. It was invalid or expired");
				}
			}
		} };

		SSLContext context = SSLContext.getInstance("TLS");
		context.init(null, trustAllCerts, null);
		return context.getSocketFactory();
	}
}
