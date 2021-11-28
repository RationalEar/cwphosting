package com.michael.cwphosting.auth.exceptions;

import org.springframework.security.core.AuthenticationException;

public class AccountSuspendedAuthenticationException extends AuthenticationException {

	public AccountSuspendedAuthenticationException(final String msg) {
		super(msg);
	}

}
