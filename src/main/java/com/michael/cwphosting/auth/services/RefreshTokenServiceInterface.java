package com.michael.cwphosting.auth.services;

import com.michael.cwphosting.auth.exceptions.InvalidTokenException;

public interface RefreshTokenServiceInterface {
	boolean saveRefreshToken(String username, String token);
	boolean revokeRefreshToken(String token);
	boolean revokeAllTokens(String username);
	boolean validateRefreshToken(String username, String token);
	String refreshToken(String token) throws InvalidTokenException;
}
