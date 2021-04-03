package it.vinicioflamini.springbootsecureapi.controller;

import java.util.ArrayList;
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
import it.vinicioflamini.springbootsecureapi.domain.Holiday;
import it.vinicioflamini.springbootsecureapi.domain.HolidayDto;
import it.vinicioflamini.springbootsecureapi.service.HolidayService;

@RestController
@RequestMapping("/api/holidays")
public class HolidayController {

	private static final Integer YEAR = 2021;
	private static final String COUNTRY_CODE = "IT";

	@Autowired
	private HolidayService holidayService;

	@ApiOperation(value = "Returns the public holidays for current year in Italy.", notes = "Use FixedHolidays parameter to filter", response = Holiday.class, responseContainer = "List")
	@GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	public List<HolidayDto> get(@RequestParam(name = "FixedHolidays", required = false) Boolean fixedHolidays) {
		ObjectMapper mapper = new ObjectMapper();
		List<Holiday> holidays = holidayService.get(YEAR, COUNTRY_CODE, fixedHolidays);
		if (holidays != null) {
			return holidays.stream().map(h -> mapper.convertValue(h, HolidayDto.class)).collect(Collectors.toList());
		} else {
			return new ArrayList<>();
		}
	}

}
