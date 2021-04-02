package it.vinicioflamini.springbootsecureapi;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class HolidayService {

	@Value("${client.url}")
	private String clientUrl;

	@Autowired
	private CertificateValidator certificateValidator;

	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private ResponseEntity<List<Holiday>> response;

	/**
	 * get elements.
	 * 
	 * @param year          the year list holidays of
	 * @param countryCode   ISO 3166-1 alpha-2 code of the country
	 * @param fixedHolidays get only fixed (false)/only non fixed (true)/all (null)
	 *                      holidays
	 *
	 * @return elements
	 */
	public List<Holiday> get(Integer year, String countryCode, Boolean fixedHolidays) {
		response = restTemplate.exchange(
				String.format("%s/PublicHolidays/%s/%s", clientUrl, String.valueOf(year), countryCode), HttpMethod.GET,
				null, new ParameterizedTypeReference<List<Holiday>>() {
				});
		List<Holiday> holidays = certificateValidator.validate(clientUrl) ? response.getBody() : null;
		if (holidays != null && fixedHolidays != null) {
			holidays = holidays.stream().filter(h -> h.getFixed().equals(fixedHolidays)).collect(Collectors.toList());
		}

		return holidays;
	}

}
