package com.michael.cwphosting.auth.exceptions;

import org.springframework.security.core.AuthenticationException;

public class AccountLockedAuthenticationException extends AuthenticationException {

	public AccountLockedAuthenticationException(final String msg) {
		super(msg);
	}

}
