package com.michael.cwphosting.auth.services;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.HtmlEmail;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Getter
public class EmailSenderService extends HtmlEmail {

	private String appName;
	private String appUrl;

	public EmailSenderService(@Value("${spring.mail.host}")String hostName, @Value("${spring.mail.port}") int port, @Value("${spring.mail.username}") String username,
							  @Value("${spring.mail.password}") String password,@Value("${spring.mail.ssl}") boolean ssl, @Value("${spring.application.name}") String appName,
							  @Value("${spring.application.base-url}") String appUrl) {
		super();
		setHostName(hostName);
		setSmtpPort(port);
		setAuthenticator(new DefaultAuthenticator(username, password));
		setSSLOnConnect(ssl);
		this.appName = appName;
		this.appUrl = appUrl;
	}

	public void initialize(HtmlEmail htmlEmail){
		htmlEmail.setHostName(this.getHostName());
		htmlEmail.setSmtpPort(Integer.parseInt(this.getSmtpPort()));
		htmlEmail.setAuthenticator( this.authenticator );
		htmlEmail.setSSLOnConnect(this.isSSLOnConnect());
	}
}