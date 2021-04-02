package it.vinicioflamini.springbootsecureapi;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/api/holidays")
public class HolidayController {

	private static final Integer YEAR = 2021;
	private static final String COUNTRY_CODE = "IT";

	@Autowired
	private HolidayService holidayService;

	@ApiOperation(value = "Returns the public holidays for current year in Italy.", notes = "Country code must be provided as ISO 3166-1 alpha-2", response = Holiday.class, responseContainer = "List")
	@GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	public List<HolidayDto> get(@RequestParam(name = "FixedHolidays", required = false) Boolean fixedHolidays) {
		ObjectMapper mapper = new ObjectMapper();
		return holidayService.get(YEAR, COUNTRY_CODE, fixedHolidays).stream()
				.map(h -> mapper.convertValue(h, HolidayDto.class)).collect(Collectors.toList());
	}

}
