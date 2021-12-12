package com.michael.cwphosting.auth.jwt.resource;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
public class JwtPasswordResetObjectRequest implements Serializable {
	private static final long serialVersionUID = -5656176899813108345L;
	private String token;
	private String password;
	private String confirmPassword;
}
