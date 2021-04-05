package it.vinicioflamini.springbootsecureapi.validation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Collection;
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
				boolean check = false;

				for (X509Certificate cert : certs) {
					try {
						cert.checkValidity();
						verifyCertificate(cert);
					} catch (Exception ex) {
						logger.error(ex.getMessage());
						throw new CertificateException("Certificate not trusted. It was invalid or expired");
					}
					check = !check ? checkSubjectAlternativeNames(cert, httpUrl) : check;
				}
				if (!check) {
					logger.error("Certificate is not valid for the URL {}", httpUrl);
					throw new CertificateException("Certificate not trusted. It was invalid for the provided URL");
				}
			}
		} };

		SSLContext context = SSLContext.getInstance("TLS");
		context.init(null, trustAllCerts, null);
		return context.getSocketFactory();
	}

	private boolean checkSubjectAlternativeNames(X509Certificate certificate, String httpUrl) {
		boolean found = false;
		try {
			String uri = new URI(httpUrl).getHost();
			String domain = uri.substring(uri.indexOf(".") + 1);
			Collection<List<?>> altNames = certificate.getSubjectAlternativeNames();
			if (altNames == null)
				return found;
			for (List<?> item : altNames) {
				Integer type = (Integer) item.get(0);
				// otherName / dNSName
				if (type == 0 || type == 2) {
					found = !found ? (String.valueOf(item.get(1))).equalsIgnoreCase(domain) : found;
				} else {
					logger.warn("SubjectAltName of invalid type found: " + certificate);
				}
			}
		} catch (CertificateParsingException e) {
			logger.error("Error parsing SubjectAltName in certificate: " + certificate + "\r\nerror:"
					+ e.getLocalizedMessage(), e);
			return false;
		} catch (URISyntaxException e) {
			logger.error("Error parsing url: " + httpUrl + "\r\nerror:" + e.getLocalizedMessage(), e);
			return false;
		}
		return found;
	}

	private boolean verifyCertificate(X509Certificate cert) {
		String relativeCacertsPath = "/lib/security/cacerts".replace("/", File.separator);
		String filename = System.getProperty("java.home") + relativeCacertsPath;
		FileInputStream is = null;
		try {
			is = new FileInputStream(filename);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		KeyStore keystore = null;
		try {
			keystore = KeyStore.getInstance(KeyStore.getDefaultType());
		} catch (KeyStoreException e) {
			e.printStackTrace();
		}
		
		String password = "changeit";
		try {
			keystore.load(is, password.toCharArray());
		} catch (NoSuchAlgorithmException | CertificateException | IOException e) {
			e.printStackTrace();
		}

		PKIXParameters params = null;
		try {
			params = new PKIXParameters(keystore);
		} catch (KeyStoreException | InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}

		Set<TrustAnchor> trustAnchors = params.getTrustAnchors();
		List<Certificate> certificates = trustAnchors.stream().map(TrustAnchor::getTrustedCert)
				.collect(Collectors.toList());

		String sourceIssuer = getRdnO(cert);

		for (Certificate certificate : certificates) {
			X509Certificate x509Certificate = (X509Certificate) certificate;
			if (getRdnO(x509Certificate).equals(sourceIssuer)) {
				logger.info(x509Certificate.getIssuerX500Principal().getName());
				try {
					cert.verify(x509Certificate.getPublicKey());
					return true;
				} catch (InvalidKeyException | CertificateException | NoSuchAlgorithmException | NoSuchProviderException
						| SignatureException e) {
					e.printStackTrace();
					return false;
				}
			}
		}

		if (is != null) {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return false;
	}

	private String getRdnO(X509Certificate cert) {
		try {
			LdapName ln = new LdapName(cert.getIssuerDN().getName());
			for (Rdn rdn : ln.getRdns()) {
				if (rdn.getType().equalsIgnoreCase("O")) {
					logger.info("O is: " + rdn.getValue());
					return String.valueOf(rdn.getValue());
				}
			}
		} catch (InvalidNameException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return "";
	}
}
