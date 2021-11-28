package com.michael.cwphosting.auth.exceptions;

import org.springframework.security.core.AuthenticationException;

public class UserNotFoundAuthenticationException extends AuthenticationException {

	public UserNotFoundAuthenticationException(final String msg) {
		super(msg);
	}

}
