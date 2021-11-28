package com.michael.cwphosting.auth.jwt;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.michael.cwphosting.auth.utilities.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static java.util.Arrays.stream;

@Slf4j
@RequiredArgsConstructor
public class JwtTokenAuthorizationOncePerRequestFilter extends OncePerRequestFilter {

	private final TokenUtil tokenUtil;
	private final UserDetailsService userDetailsService;

	//@Value("${jwt.http.request.header}")
	private final String tokenHeader = "Authorization";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		log.info("Authentication Request For '{}'", request.getRequestURL());
		log.info(request.getServletPath());
		if(request.getServletPath().equals("/api/login")
				|| request.getServletPath().startsWith("/api/token/")
				|| request.getServletPath().startsWith("/api/user/")
		){
			filterChain.doFilter(request, response);
		}
		else {
			try {
				final String requestTokenHeader = request.getHeader(this.tokenHeader);
				log.info("Token Header = {}", requestTokenHeader);
				if (requestTokenHeader == null || !requestTokenHeader.startsWith("Bearer ")) {
					throw new Exception("A valid access token is required");
				}
				String jwtToken = requestTokenHeader.substring(7);
				DecodedJWT decodedToken = tokenUtil.getDecodedToken(jwtToken);
				String username = decodedToken.getSubject();
				UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
				if (tokenUtil.validateToken(decodedToken, userDetails)) {
					UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(username, null, userDetails.getAuthorities());
					usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
					filterChain.doFilter(request,response);
				}
				else{
					throw new Exception("Unable to verify token. Please request a new token");
				}
			}
			catch (Exception e){
				log.error(e.getMessage());
				ResponseBuilder.sendMessageResponse(response, e);
			}
		}
	}
}
