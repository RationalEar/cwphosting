package com.michael.cwphosting.auth.exceptions;

import org.springframework.security.core.AuthenticationException;

public class InvalidPasswordException extends AuthenticationException {
	public InvalidPasswordException(String msg){
		super(msg);
	}
}
