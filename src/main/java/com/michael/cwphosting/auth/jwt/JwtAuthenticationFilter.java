package com.michael.cwphosting.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.michael.cwphosting.auth.exceptions.AccountSuspendedAuthenticationException;
import com.michael.cwphosting.auth.exceptions.LoginAttemptsExeededException;
import com.michael.cwphosting.auth.exceptions.UserNotFoundAuthenticationException;
import com.michael.cwphosting.auth.models.User;
import com.michael.cwphosting.auth.services.RefreshTokenService;
import com.michael.cwphosting.auth.services.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

	private final AuthenticationManager authenticationManager;
	private final TokenUtil tokenUtil;
	private final UserService userDetailsService;
	private final RefreshTokenService refreshTokenService;

	public JwtAuthenticationFilter(AuthenticationManager authenticationManager, TokenUtil tokenUtil, UserService userDetailService, RefreshTokenService refreshTokenService) {
		this.authenticationManager = authenticationManager;
		this.tokenUtil = tokenUtil;
		this.userDetailsService = userDetailService;
		this.refreshTokenService = refreshTokenService;
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {

		String username, password;
		try {
			Map<String, String> requestMap = new ObjectMapper().readValue(request.getInputStream(), Map.class);
			username = requestMap.get("username");
			password = requestMap.get("password");
		}
		catch (IOException e) {
			throw new AuthenticationServiceException(e.getMessage(), e);
		}

		log.info("Attempt Authentication: {}, {}", username, password);
		if( userDetailsService.loginAttemptsExceeded(username) ){
			int minutes = userDetailsService.getWaitTime(username);
			String s = minutes==1?"minute":"minutes";
			throw new LoginAttemptsExeededException("Too many login attempts, you need to wait for least "+minutes+" "+s+" before you can try again.");
		}

		User user = userDetailsService.getUser(username);
		if(user==null) throw new UserNotFoundAuthenticationException("User account not found");

		JwtUserDetails userDetails = new JwtUserDetails(user);
		if(!userDetails.isEnabled()) throw new AccountSuspendedAuthenticationException("Unable to login. Account is suspended");

		userDetails.setActivationGracePeriod(tokenUtil.getAccountActivationExpire());
		if(!userDetails.isAccountNonExpired()) throw new AccountSuspendedAuthenticationException("Unable to login. You need to activate your account first.");

		log.info("Authenticating, username: {}, password: {}", username, password);
		UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
		return authenticationManager.authenticate(authenticationToken);

	}

	@Override
	protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
		org.springframework.security.core.userdetails.User user = (org.springframework.security.core.userdetails.User) authResult.getPrincipal();
		String accessToken = tokenUtil.generateToken( user, false );
		String refreshToken = tokenUtil.generateToken(user, true);
		String username = user.getUsername();
		if(username !=null){
			userDetailsService.resetLoginAttempts(username);
			refreshTokenService.saveRefreshToken(username, refreshToken);
		}
		response.setHeader("access_token", accessToken);
		response.setHeader("refresh_token", refreshToken);
		Map<String,String> tokens = new HashMap<>();
		tokens.put("accessToken", accessToken);
		tokens.put("refreshToken", refreshToken);
		response.setContentType(APPLICATION_JSON_VALUE);
		new ObjectMapper().writeValue(response.getOutputStream(), tokens);
	}

	@Override
	protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
		String username = request.getParameter("username");
		if(username !=null){
			userDetailsService.incrementLoginAttempt(username, request.getRemoteAddr());
		}
		log.warn("Unable to authenticate {}, {}", username, failed.getMessage());

		SecurityContextHolder.clearContext();
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setHeader("message", failed.getMessage());
		response.setContentType(APPLICATION_JSON_VALUE);
		Map<String,String> messages = new HashMap<>();
		messages.put("message", failed.getMessage());
		new ObjectMapper().writeValue(response.getOutputStream(), messages);
		//super.unsuccessfulAuthentication(request, response, failed);
	}
}
