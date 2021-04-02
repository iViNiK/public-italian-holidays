package it.vinicioflamini.springbootsecureapi;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HolidayDto {

	private String date;
	private String localName;
	private String name;
	@JsonIgnore
	private String countryCode;
	@JsonIgnore
	private Boolean fixed;
	@JsonIgnore
	private Boolean global;
	@JsonIgnore
	private String counties;
	@JsonIgnore
	private String launchYear;
	@JsonIgnore
	private String type;

}
