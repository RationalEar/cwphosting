package com.michael.cwphosting.auth.exceptions;

import org.springframework.security.core.AuthenticationException;

public class LoginAttemptsExeededException extends AuthenticationException {
	public LoginAttemptsExeededException(String msg) {
		super(msg);
	}
}
