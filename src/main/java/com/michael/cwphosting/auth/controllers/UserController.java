package com.michael.cwphosting.auth.controllers;

import com.michael.cwphosting.auth.exceptions.InvalidPasswordException;
import com.michael.cwphosting.auth.exceptions.UserNotFoundAuthenticationException;
import com.michael.cwphosting.auth.jwt.resource.JwtMessageResponse;
import com.michael.cwphosting.auth.jwt.resource.JwtPasswordResetObjectRequest;
import com.michael.cwphosting.auth.jwt.resource.JwtUsernameObjectRequest;
import com.michael.cwphosting.auth.models.User;
import com.michael.cwphosting.auth.services.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/user")
@AllArgsConstructor
@Slf4j
public class UserController {
	private final UserService userService;
	private PasswordEncoder passwordEncoder;

	@Autowired
	public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
		this.passwordEncoder = passwordEncoder;
	}

	@GetMapping
	public ResponseEntity<?> getUserCreateFields(){
		try {
			return ResponseEntity.ok(new User());
		}
		catch (Exception e){
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JwtMessageResponse(e.getMessage()));
		}
	}

	@PostMapping
	public ResponseEntity<?> createUserAccount(HttpServletRequest request, @RequestBody User data) {
		try {
			User user = userService.signUpUser(data);
			URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(user.getId()).toUri();
			return ResponseEntity.created(uri).build();
		}
		catch (Exception e){
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JwtMessageResponse(e.getMessage()));
		}
	}

	@PostMapping("activation-token/resend")
	public ResponseEntity<?> resendActivationToken(@RequestBody JwtUsernameObjectRequest request){
		try {
			User user = userService.getUser(request.getUsername());
			if (user == null)
				throw new UserNotFoundAuthenticationException("A valid username/email address is required");
			userService.sendConfirmationMail(user.getEmail(), user.getActivationToken(), user.getFirstName());
			return ResponseEntity.ok(new JwtMessageResponse("An account activation email has been sent to your email address"));
		}
		catch (Exception e){
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JwtMessageResponse(e.getMessage()));
		}
	}

	@PostMapping("forgotten-password")
	public ResponseEntity<?> sendForgottenPasswordToken(@RequestBody JwtUsernameObjectRequest request){
		try {
			User user = userService.getUser(request.getUsername());
			if (user == null) throw new UserNotFoundAuthenticationException("A valid username/email address is required");
			String token = UUID.randomUUID().toString();
			user.setForgottenPasswordToken(token);
			user.setForgottenPasswordTokenExpire(LocalDateTime.now().plusSeconds(userService.getForgottenPasswordExpire()));
			userService.updateUser(user);
			userService.sendPasswordResetMail(user.getEmail(), token, user.getFirstName(), user.getForgottenPasswordTokenExpire());
			return ResponseEntity.ok(new JwtMessageResponse("A password reset email has been sent to your email address."));
		}
		catch (Exception e){
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JwtMessageResponse(e.getMessage()));
		}
	}

	@GetMapping("confirm-account")
	public ResponseEntity<?> confirmAccount(@RequestParam(name = "token", required = true) String token){
		try {
			if (token == null || token.isEmpty()) throw new IOException("A valid activation token is required");
			User user = userService.findUserByActivationToken(token);
			if (user == null) throw new UserNotFoundAuthenticationException("Unable to find user with submitted activation token.");
			if (user.getActivationToken().equals(token)) {
				user.setActivationToken(null);
				userService.updateUser(user);
				return ResponseEntity.ok(new JwtMessageResponse("Account activated successfully"));
			}
			else {
				throw new Exception("The submitted token is not valid");
			}
		}
		catch (Exception e){
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JwtMessageResponse(e.getMessage()));
		}
	}

	@PostMapping("reset-password")
	public ResponseEntity<?> resetPassword(@RequestBody JwtPasswordResetObjectRequest data){
		try {
			if (data.getToken() == null || data.getToken().isEmpty()) throw new IOException("A valid forgotten password token is required");
			User user = userService.findUserByForgottenPasswordToken(data.getToken());
			if (user == null) throw new UserNotFoundAuthenticationException("Unable to find user with submitted password reset token.");
			if (user.getForgottenPasswordToken().equals(data.getToken()) && user.getForgottenPasswordTokenExpire().isAfter(LocalDateTime.now()) ) {

				if( !data.getPassword().equals(data.getPasswordConfirm()) ) throw new InvalidPasswordException("The submitted passwords do not match");
				if(!userService.isValidPassword(data.getPassword())) throw new InvalidPasswordException(userService.getPasswordValidationMessage());

				user.setForgottenPasswordToken(null);
				user.setPassword( data.getPassword() );
				userService.updateUser(user);
				return ResponseEntity.ok(new JwtMessageResponse("Account password has been reset successfully"));
			}
			else {
				throw new Exception("The submitted token is not valid");
			}
		}
		catch (Exception e){
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JwtMessageResponse(e.getMessage()));
		}
	}

	@GetMapping("reset-password")
	public ResponseEntity<?> getPasswordResetFields(){
		try {
			return ResponseEntity.ok(new JwtPasswordResetObjectRequest());
		}
		catch (Exception e){
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JwtMessageResponse(e.getMessage()));
		}
	}

}
