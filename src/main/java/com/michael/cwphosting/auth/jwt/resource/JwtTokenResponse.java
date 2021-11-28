package com.michael.cwphosting.auth.jwt.resource;

import java.io.Serializable;

public class JwtTokenResponse implements Serializable {

	private static final long serialVersionUID = 8317676219297719109L;

	private final String accessToken;
	private final String refreshToken;

	public JwtTokenResponse(String token) {
		this.accessToken = token;
		this.refreshToken = null;
	}

	public JwtTokenResponse(String accessToken, String refreshToken) {
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
	}

	public String getAccessToken() {
		return this.accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}
}
