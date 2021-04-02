 # Spring Boot secure REST API with HTTPS
TODO: DESCRIBE APP

To let the application run in HTTPS, we need to configure a certificate.

Note: In production-grade applications, certificates are issued from renowned Certification Authorities (CA) to ensure that our application is a trusted entity.

However, as this is a demo, we will create a Self-Signed Certificate and use it in our application.

Java provides the keytool utility to create and manage certificates locally. It’s available with other JDK utilities in JDK_HOME/bin directory.

Step 1: Run the command prompt in administrator mode. Then execute this command line:

keytool -genkey -keyalg RSA -alias demo -keystore demo.jks -storepass password -validity 365 -keysize 4096 -storetype pkcs12

Here we are generating a certificate with the following options:

* Using the RSA algorithm
* Providing an alias name as demo
* Naming the Keystore file as demo.jks
* Validity for one year
* Once you hit this command, it will prompt a few details, and the certificate will be created.

Step 2: Next, we copy this certificate in the src/main/resources directory to be available at the classpath.

The endpoint exposed is protected by basic authentication. To access the API, use the following credentials:

* username: admin
* password: password

## License
Copyright © 2021 by Vinicio Flamini <focus.labs.it@gmail.com>
