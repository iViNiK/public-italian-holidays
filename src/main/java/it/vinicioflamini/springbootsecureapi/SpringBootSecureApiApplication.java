package it.vinicioflamini.springbootsecureapi;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
@SpringBootApplication
public class SpringBootSecureApiApplication extends SpringBootServletInitializer {

	@Value("${server.ssl.key-store-password}")
	private String keyStorePassword;

	@Value("${server.ssl.key-store}")
	private String keyStoreFile;

	public static void main(String[] args) {
		SpringApplication.run(SpringBootSecureApiApplication.class, args);
	}

	@Bean
	RestTemplate restTemplate(ResourceLoader resourceLoader) {
		KeyStore keyStore = null;
		try {
			keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		} catch (KeyStoreException e1) {
			e1.printStackTrace();
		}
		Resource resource = resourceLoader.getResource(keyStoreFile);
		if (keyStore != null) {
			try {
				keyStore.load(resource.getInputStream(), keyStorePassword.toCharArray());
			} catch (NoSuchAlgorithmException|CertificateException|IOException e) {
				e.printStackTrace();
			}
			SSLConnectionSocketFactory socketFactory = null;
			try {
				socketFactory = new SSLConnectionSocketFactory(
						new SSLContextBuilder().loadTrustMaterial(null, new TrustSelfSignedStrategy())
								.loadKeyMaterial(keyStore, keyStorePassword.toCharArray()).build());
			} catch (KeyManagementException|UnrecoverableKeyException|NoSuchAlgorithmException|KeyStoreException e) {
				e.printStackTrace();
			}
			
			HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory).build();
			ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
			return new RestTemplate(requestFactory);
		}
		
		return new RestTemplate();
	}

	@Bean
	public ResponseEntity<List<Holiday>> response() {
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@Bean
	public Docket api() {
		return new Docket(DocumentationType.SWAGGER_2).select()
				.apis(RequestHandlerSelectors.basePackage("it.vinicioflamini.springbootsecureapi")).build()
				.apiInfo(apiInfo());
	}

	private ApiInfo apiInfo() {
		return new ApiInfoBuilder().title("Spring Boot: Secure API").description("Sample Secure Rest API")
				.version("1.0").build();
	}

}
