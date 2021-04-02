package it.vinicioflamini.springbootsecureapi;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class HolidayService {

	@Value("${server.ssl.key-store-password}")
	private String keyStorePassword;

	@Value("${server.ssl.key-store}")
	private String keyStoreFile;

	@Value("${client.url}")
	private String clientUrl;

	@Autowired
	ResourceLoader resourceLoader;

	@Autowired
	private CertificateValidator certificateValidator;

	/**
	 * get elements.
	 * 
	 * @param year          the year list holidays of
	 * @param countryCode   ISO 3166-1 alpha-2 code of the country
	 * @param fixedHolidays
	 *
	 * @return elements
	 */
	public List<Holiday> get(Integer year, String countryCode, Boolean fixedHolidays) {
		List<Holiday> holidays = new ArrayList<>();

		try {
			holidays = certificateValidator.validate(clientUrl) ? getHolidays(year, countryCode) : new ArrayList<>();
			if (holidays != null && fixedHolidays != null) {
				holidays = holidays.stream()
						.filter(h -> h.getFixed().equals(fixedHolidays))
						.collect(Collectors.toList());
			}
		} catch (KeyManagementException | UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException
				| CertificateException | IOException e) {
			e.printStackTrace();
		}

		return holidays;
	}

	private List<Holiday> getHolidays(Integer year, String countryCode)
			throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException,
			KeyManagementException, UnrecoverableKeyException {
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		Resource resource = resourceLoader.getResource(keyStoreFile);
		keyStore.load(resource.getInputStream(), keyStorePassword.toCharArray());
		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
				new SSLContextBuilder().loadTrustMaterial(null, new TrustSelfSignedStrategy())
						.loadKeyMaterial(keyStore, keyStorePassword.toCharArray()).build());
		HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory).build();
		ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
		RestTemplate restTemplate = new RestTemplate(requestFactory);
		ResponseEntity<List<Holiday>> response = restTemplate.exchange(
				String.format("%s/PublicHolidays/%s/%s", clientUrl, String.valueOf(year), countryCode), HttpMethod.GET,
				null, new ParameterizedTypeReference<List<Holiday>>() {
				});
		return response.getBody();
	}

}
