package com.michael.cwphosting.auth.jwt.resource;

import java.io.Serializable;

public class JwtMessageResponse implements Serializable {

	private static final long serialVersionUID = 8317676219297719109L;

	private final String message;

	public JwtMessageResponse(String message) {
		this.message = message;
	}

	public String getMessage() {
		return this.message;
	}
}
