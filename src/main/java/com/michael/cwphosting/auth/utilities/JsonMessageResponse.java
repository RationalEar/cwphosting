package com.michael.cwphosting.auth.utilities;

import java.io.Serializable;

public class JsonMessageResponse implements Serializable {
	private static final long serialVersionUID = 8317676219297719106L;

	private final String message;

	public JsonMessageResponse(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}
