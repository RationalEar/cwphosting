package com.michael.cwphosting.auth.jwt.resource;

import java.io.Serializable;

public class JwtUsernameObjectRequest implements Serializable {
	private static final long serialVersionUID = -5656176897013108345L;

	private String username;

	JwtUsernameObjectRequest(){}

	public JwtUsernameObjectRequest(String username) {
		this.username = username;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
}
