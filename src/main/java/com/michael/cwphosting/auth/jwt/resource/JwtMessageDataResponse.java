package com.michael.cwphosting.auth.jwt.resource;

import java.io.Serializable;

public class JwtMessageDataResponse<T> implements Serializable {

	private static final long serialVersionUID = 8317676219297719109L;

	private final String message;
	private final T data;

	public JwtMessageDataResponse(String message, T data) {
		this.message = message;
		this.data = data;
	}

	public String getMessage() {
		return this.message;
	}

	public T getData() {
		return data;
	}
}
