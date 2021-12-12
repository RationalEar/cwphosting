package com.michael.cwphosting.auth.jwt.resource;

import java.io.Serializable;

public class JwtIdObjectRequest implements Serializable {
	private static final long serialVersionUID = -5656176897013108346L;

	private String id;

	JwtIdObjectRequest(){}

	public JwtIdObjectRequest(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
