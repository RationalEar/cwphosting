package com.michael.cwphosting.auth.exceptions;

import org.springframework.security.core.AuthenticationException;

public class InvalidEmailException extends AuthenticationException {
	public InvalidEmailException(String msg){
		super(msg);
	}
}
