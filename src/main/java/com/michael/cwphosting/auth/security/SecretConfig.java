package com.michael.cwphosting.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SecretConfig {
	@Value("${jwt.signing.key.secret}")
	private String secret;

	public String getSecret() {
		return secret;
	}

	public byte[] getBytes(){
		return secret.getBytes();
	}
}
