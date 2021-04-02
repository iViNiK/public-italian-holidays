package it.vinicioflamini.springbootsecureapi;

public interface CertificateValidator {
	public boolean validate(String httpURL);
}
