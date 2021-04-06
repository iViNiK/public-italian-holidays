package it.vinicioflamini.springbootsecureapi.validation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
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
			SSLSocketFactory sslSocketFactory = getFactorySimple(httpURL);
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
	private SSLSocketFactory getFactorySimple(String httpUrl) throws NoSuchAlgorithmException, KeyManagementException {
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
					String uri = new URI(httpUrl).getHost();
					if (!uri.equals(getRdnType(certs[0].getSubjectDN(), "CN"))) {
						// Certificate was not issued for the required URL
						logger.error("Certificate is not valid for the URL {}", httpUrl);
						throw new CertificateException("Certificate not trusted. It was invalid for the provided URL");
					}
				} catch (URISyntaxException e) {
					logger.error("Error parsing url: " + httpUrl + "\r\nerror:" + e.getLocalizedMessage(), e);
					throw new CertificateException("Invalid URL provided");
				}

				for (X509Certificate cert : certs) {
					try {
						if (!checkCertificate(cert, httpUrl)) {
							logger.error("Certificate is not valid for the URL {}", httpUrl);
							throw new CertificateException(
									"Certificate not trusted. It was invalid for the provided URL");
						}
					} catch (Exception ex) {
						logger.error(ex.getMessage());
						throw new CertificateException("Certificate not trusted. It was invalid or expired");
					}
				}
			}
		} };

		SSLContext context = SSLContext.getInstance("TLS");
		context.init(null, trustAllCerts, null);
		return context.getSocketFactory();
	}

	private boolean checkCertificate(X509Certificate certificate, String httpUrl)
			throws CertificateExpiredException, CertificateNotYetValidException {

		certificate.checkValidity();

		if (certificate.getKeyUsage()[5]) {
			// Root certificate
			return verifyCertificate(certificate);
		} else {
			if (!verifyCertificate(certificate)) {
				logger.info("Warning: Could not verify certificate issued by: {}", certificate.getIssuerDN().getName());
			}
			return true;
		}
	}

	/**
	 * Issuer root certificate must be found in client keystore. If missing it must
	 * be imported.
	 */
	private boolean verifyCertificate(X509Certificate cert) {
		String relativeCacertsPath = "/lib/security/cacerts".replace("/", File.separator);
		String filename = System.getProperty("java.home") + relativeCacertsPath;
		String password = "changeit";
		FileInputStream is = null;
		
		try {
			is = new FileInputStream(filename);

			KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
			keystore.load(is, password.toCharArray());

			PKIXParameters params = new PKIXParameters(keystore);

			Set<TrustAnchor> trustAnchors = params.getTrustAnchors();
			List<Certificate> certificates = trustAnchors.stream().map(TrustAnchor::getTrustedCert)
					.collect(Collectors.toList());

			String sourceIssuer = getRdnType(cert.getIssuerDN(), "O");

			for (Certificate certificate : certificates) {
				X509Certificate x509Certificate = (X509Certificate) certificate;
				if (getRdnType(x509Certificate.getIssuerDN(), "O").equals(sourceIssuer)) {
					logger.info(x509Certificate.getIssuerX500Principal().getName());
					try {
						cert.verify(x509Certificate.getPublicKey());
						return true;
					} catch (InvalidKeyException | CertificateException | NoSuchAlgorithmException
							| NoSuchProviderException | SignatureException e) {
						logger.error("Error occurred in certificate verification: {}", e.getLocalizedMessage());
						return false;
					}
				}
			}
		} catch (Exception e) {
			logger.error("Error occurred in certificate verification: {}", e.getLocalizedMessage());
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					logger.error("Error occurred in certificate verification: {}", e.getLocalizedMessage());
				}
			}
		}

		return false;
	}

	private String getRdnType(Principal principal, String type) {
		try {
			LdapName ln = new LdapName(principal.getName());
			for (Rdn rdn : ln.getRdns()) {
				if (rdn.getType().equalsIgnoreCase(type)) {
					return String.valueOf(rdn.getValue());
				}
			}
		} catch (InvalidNameException e) {
			logger.info("Could not parse {}: \rerror: {}", principal.getName(), e.getLocalizedMessage());
		}

		return "";
	}
}
