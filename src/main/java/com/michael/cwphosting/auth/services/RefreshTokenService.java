package com.michael.cwphosting.auth.services;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.michael.cwphosting.auth.exceptions.InvalidTokenException;
import com.michael.cwphosting.auth.jwt.TokenUtil;
import com.michael.cwphosting.auth.models.RefreshToken;
import com.michael.cwphosting.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.sql.Ref;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenService implements RefreshTokenServiceInterface{

	private final TokenUtil tokenUtil;
	private final RefreshTokenRepository repository;
	private final UserDetailsService userDetailsService;

	@Override
	public boolean saveRefreshToken(String username, String token) {
		try {
			DecodedJWT decodedJWT = tokenUtil.getDecodedToken(token);
			RefreshToken refreshToken = new RefreshToken(username, token, decodedJWT.getIssuedAt(), decodedJWT.getExpiresAt() );
			repository.save(refreshToken);
			return true;
		}
		catch (Exception e){
			log.error(e.getMessage());
		}
		return false;
	}

	@Override
	public boolean revokeRefreshToken(String token) {
		Optional<RefreshToken> refreshToken = repository.findRefreshTokenByToken(token);
		if(refreshToken.isPresent()){
			repository.delete(refreshToken.get());
			return true;
		}
		return false;
	}

	@Override
	public boolean revokeAllTokens(String username) {
		List<RefreshToken> tokens = repository.findRefreshTokensByUsername(username);
		if(!tokens.isEmpty()){
			for(RefreshToken token: tokens){
				repository.delete(token);
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean validateRefreshToken(String username, String token) {
		Optional<RefreshToken> refreshToken = repository.findRefreshTokenByToken(token);
		return refreshToken.isPresent() && refreshToken.get().getExpires().after(new Date()) && refreshToken.get().getUsername().equals(username);
	}

	@Override
	public String refreshToken(String token) throws InvalidTokenException {
		DecodedJWT decodedJWT = tokenUtil.getDecodedToken(token);
		String username = decodedJWT.getSubject();
		UserDetails userDetails = userDetailsService.loadUserByUsername(username);
		if( validateRefreshToken(username, token) ){
			return tokenUtil.generateToken(userDetails);
		}
		throw new InvalidTokenException("Could not generate new token. Your refresh token was not accepted");
	}
}
