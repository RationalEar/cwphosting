package com.michael.cwphosting.auth.jwt.resource;

import java.io.Serializable;

public class JwtStatusObjectRequest implements Serializable {
	private static final long serialVersionUID = -5656176897013108345L;

	private Boolean status;

	JwtStatusObjectRequest(){}

	public JwtStatusObjectRequest(Boolean status) {
		this.status = status;
	}

	public Boolean getStatus() {
		return status;
	}

	public void setStatus(Boolean status) {
		this.status = status;
	}
}
