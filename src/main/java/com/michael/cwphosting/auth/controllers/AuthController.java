package com.michael.cwphosting.auth.controllers;

import com.michael.cwphosting.auth.jwt.resource.JwtMessageResponse;
import com.michael.cwphosting.auth.jwt.resource.JwtTokenResponse;
import com.michael.cwphosting.auth.jwt.resource.JwtUsernameObjectRequest;
import com.michael.cwphosting.auth.services.RefreshTokenService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RestController
@AllArgsConstructor
@Slf4j
@RequestMapping("/token")
public class AuthController {

	private final RefreshTokenService refreshTokenService;

	@GetMapping("/refresh")
	public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
			String authorizationHeader = request.getHeader(AUTHORIZATION);
			if(authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) throw new Exception("A valid autorization header is required");
			String token = authorizationHeader.substring("Bearer ".length());
			String accessToken = refreshTokenService.refreshToken(token);
			response.setHeader("access_token", accessToken);
			return ResponseEntity.ok(new JwtTokenResponse(accessToken));
		}
		catch (Exception e){
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JwtMessageResponse(e.getMessage()));
		}
	}

	@GetMapping("/revoke")
	public ResponseEntity<?> revokeToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
			String authorizationHeader = request.getHeader(AUTHORIZATION);
			if(authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) throw new IOException("A valid autrhorization token is required");
			String token = authorizationHeader.substring("Bearer ".length());
			boolean revoked = refreshTokenService.revokeRefreshToken(token);
			if(!revoked) throw new Exception("Unable to validate token. No token revoked.");
			return ResponseEntity.ok(new JwtMessageResponse("Token revoked successfully"));
		}
		catch (Exception e){
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JwtMessageResponse(e.getMessage()));
		}
	}

	@PostMapping("/revokeall")
	public ResponseEntity<?> revokeAllTokens(@RequestBody JwtUsernameObjectRequest logoutAllRequest, HttpServletResponse response) throws IOException {
		try {
			if(logoutAllRequest.getUsername().length()>0) {
				boolean revoked = refreshTokenService.revokeAllTokens(logoutAllRequest.getUsername());
				if (!revoked) throw new Exception("You do not have any active refresh tokens.");
				return ResponseEntity.ok(new JwtMessageResponse("All access tokens have been revoked"));
			}
			else{
				throw new Exception("A valid username is required");
			}
		}
		catch (Exception e){
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JwtMessageResponse(e.getMessage()));
		}
	}

}
