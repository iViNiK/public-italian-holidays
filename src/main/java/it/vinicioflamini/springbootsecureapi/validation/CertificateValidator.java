package it.vinicioflamini.springbootsecureapi.validation;

public interface CertificateValidator {
	public boolean validate(String httpURL);
}
