package it.vinicioflamini.springbootsecureapi;

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
}
