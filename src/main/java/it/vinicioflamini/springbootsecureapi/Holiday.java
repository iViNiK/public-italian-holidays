package it.vinicioflamini.springbootsecureapi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Holiday {

	private String date;
	private String localName;
	private String name;
	private String countryCode;
	private Boolean fixed;
	private Boolean global;
	private String counties;
	private String launchYear;
	private String type;

}
