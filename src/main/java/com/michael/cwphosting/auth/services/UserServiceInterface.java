package com.michael.cwphosting.auth.services;

import com.michael.cwphosting.auth.models.Role;
import com.michael.cwphosting.auth.models.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserServiceInterface {
	User saveUser(User user);
	User updateUser(User user);
	User signUpUser(User user);
	Role saveRole(Role role);
	boolean addRoleToUser(String username, String roleName);
	User getUser(String username);
	List<User> getUsers();
	List<User> getUsers(int start, int limit);
	boolean incrementLoginAttempt(String username, String ipAddress);
	boolean resetLoginAttempts(String username);
	boolean loginAttemptsExceeded(String username);
	int getWaitTime(String username);
	void sendConfirmationMail(String emailAddress, String token, String name);
	void sendPasswordResetMail(String emailAddress, String token, String name, LocalDateTime forgottenPasswordTokenExpire);
	boolean isValidPassword(String password);
	boolean isValidEmail(String email);
	User findUserByActivationToken(String token);
	User findUserByForgottenPasswordToken(String token);
	long countUsers();
	User findUserById(String id);
	void deleteUser(User user);

	User updateUserStatus(String id, Boolean status);
}
