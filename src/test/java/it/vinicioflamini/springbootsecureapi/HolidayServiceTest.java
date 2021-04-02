package it.vinicioflamini.springbootsecureapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class HolidayServiceTest {
	@Value("${client.url}")
	private String clientUrl;

	@Mock
	private CertificateValidator certificateValidator;
	
	@Mock
	private RestTemplate restTemplate;
	
	@Mock
	private ResponseEntity<List<Holiday>> responseEntity;

	@InjectMocks
	@Resource
	HolidayService holidayService;

	@BeforeEach
	public void setUp() throws Exception {
		when(restTemplate.exchange(
				String.format("%s/PublicHolidays/%s/%s", clientUrl, "2020", "IT"), HttpMethod.GET,
				null, new ParameterizedTypeReference<List<Holiday>>() {
				})).thenReturn(responseEntity);

		lenient().when(responseEntity.getBody()).thenReturn(new ArrayList<>());
	}

	@Test
	void testGetCertificateOK() {
		when(certificateValidator.validate(clientUrl)).thenReturn(Boolean.TRUE);
		List<Holiday> list = holidayService.get(2020, "IT", null);
		assertEquals(new ArrayList<>(), list);
	}

	@Test
	void testGetCertificateKO() {
		when(certificateValidator.validate(clientUrl)).thenReturn(Boolean.FALSE);
		List<Holiday> list = holidayService.get(2020, "IT", null);
		assertEquals(null, list);
	}

}
