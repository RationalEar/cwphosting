package com.michael.cwphosting.auth.services;

import com.michael.cwphosting.auth.exceptions.InvalidEmailException;
import com.michael.cwphosting.auth.exceptions.InvalidPasswordException;
import com.michael.cwphosting.auth.exceptions.UserAlreadyExistAuthenticationException;
import com.michael.cwphosting.auth.models.Role;
import com.michael.cwphosting.auth.models.User;
import com.michael.cwphosting.auth.repository.RoleRepository;
import com.michael.cwphosting.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.EmailValidator;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserDetailsService, UserServiceInterface {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private EmailSenderService emailSenderService;

	@Value("${spring.user.password.validation-message}")
	private String passwordValidationMessage;

	@Value("${spring.user.suspend-by-default}")
	private boolean suspendByDefault;

	@Value("${spring.user.forgotten-password.expire}")
	private Long forgottenPasswordExpire;

	@Value("${spring.mail.from}")
	private String emailFrom;

	@Autowired
	public void setEmailSenderService(EmailSenderService emailSenderService) {
		this.emailSenderService = emailSenderService;
	}

	public String getPasswordValidationMessage() {
		return passwordValidationMessage;
	}

	public Long getForgottenPasswordExpire(){
		return forgottenPasswordExpire;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Optional<User> opUser = userRepository.findUserByEmail(username);
		if(opUser.isEmpty()){
			log.warn("User with username \"{}\" not found.", username);
			throw new UsernameNotFoundException("User not found");
		}
		else{
			User user = opUser.get();
			Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
			user.getRoles().forEach(role -> {
				authorities.add(new SimpleGrantedAuthority(role.getName()));
			});
			return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), authorities);
		}
	}

	@Override
	public long countUsers() {
		return userRepository.count();
	}

	@Override
	public User saveUser(User user) {
		log.info("Saving new user {}", user.getUsername());
		user.setPassword( passwordEncoder.encode(user.getPassword()) );
		return userRepository.save(user);
	}

	@Override
	public User updateUser(User user) {
		log.info("Updating user {}", user.getUsername());
		User currentUser = getUser(user.getUsername());
		if(currentUser != null && user.getUsername().equals(currentUser.getUsername()) && !user.getPassword().equals(currentUser.getPassword()) ){
			log.info("Password for {} has been updated", user.getUsername());
			user.setPassword( passwordEncoder.encode(user.getPassword()) );
		}
		return userRepository.save(user);
	}

	@Override
	public Role saveRole(Role role) {
		return roleRepository.save(role);
	}

	@Override
	public boolean addRoleToUser(String username, String roleName) {
		Optional<User> user = userRepository.findUserByEmail(username);
		if(user.isEmpty()){
			log.warn("Unable to find user {} to add role {}", username, roleName);
			return false;
		}
		Optional<Role> role = roleRepository.findRoleByName(roleName);
		if(role.isEmpty()){
			log.warn("Unable to find role {}", roleName);
			return false;
		}
		log.info("Adding role {} to user {}", roleName, username);
		return user.get().getRoles().add(role.get());
	}

	@Override
	public User getUser(String username) {
		Optional<User> user = userRepository.findUserByEmail(username);
		return user.isPresent() ? user.get() : null;
	}

	@Override
	public User findUserByActivationToken(String token) {
		Optional<User> user = userRepository.findUserByActivationToken(token);
		return user.isPresent() ? user.get() : null;
	}

	@Override
	public User findUserByForgottenPasswordToken(String token) {
		Optional<User> user = userRepository.findUserByForgottenPasswordToken(token);
		return user.isPresent() ? user.get() : null;
	}

	@Override
	public List<User> getUsers() {
		return userRepository.findAll();
	}

	@Override
	public List<User> getUsers(int start, int limit) {
		return userRepository.findAll().subList(start, limit);
	}

	@Override
	public boolean incrementLoginAttempt(String username, String ipAddress) {
		User user = getUser(username);
		if(user != null){
			int attempts = user.getLoginAttempts();
			user.setLoginAttempts(++attempts);
			user.setLastLoginAttempt(LocalDateTime.now());
			if(ipAddress != null) user.setFailedLoginIpAddress(ipAddress);
			updateUser(user);
			return true;
		}
		return false;
	}

	@Override
	public boolean resetLoginAttempts(String username) {
		User user = getUser(username);
		if(user != null){
			user.setLoginAttempts(0);
			updateUser(user);
			return true;
		}
		return false;
	}

	@Override
	public boolean loginAttemptsExceeded(String username) {
		User user = getUser(username);
		if(user != null){
			int attempts = user.getLoginAttempts();
			if(attempts > 3){
				LocalDateTime lastLoginAttempt = user.getLastLoginAttempt();
				int x = attempts-3;
				Long wait = ((10*x) / 2) * 60l;
				LocalDateTime now = LocalDateTime.now();
				lastLoginAttempt = lastLoginAttempt.plusSeconds(wait);
				if( lastLoginAttempt.isAfter(now) ){
					log.info("Can't log you in for now, you need to wait a little bit longer");
					return true;
				}
			}
		}
		log.info("You're good to go");
		return false;
	}

	@Override
	public int getWaitTime(String username){
		User user = getUser(username);
		if(user != null){
			int attempts = user.getLoginAttempts();
				LocalDateTime lastLoginAttempt = user.getLastLoginAttempt();
				int x = attempts-3;
				Long wait = ((10*x) / 2) * 60l;
				Long now = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
				lastLoginAttempt = lastLoginAttempt.plusSeconds(wait);
				long target = lastLoginAttempt.toEpochSecond(ZoneOffset.UTC);
				wait = target - now;
				if(wait>0) {
					log.info("Seconds to wait: {}", wait);
					return (int) Math.ceil(wait/60);
				}
		}
		return 0;
	}

	public User signUpUser(User user) {

		if(!isValidEmail(user.getEmail())) throw new InvalidEmailException("The submitted email address is not valid");

		Optional<User> emailUsed = userRepository.findUserByEmail(user.getEmail());
		if( emailUsed.isPresent() ){
			throw new UserAlreadyExistAuthenticationException("User with given email already exists");
		}

		user.setSuspended(suspendByDefault);

		user.setCreated(LocalDateTime.now());

		/*
		char[] possibleCharacters = (new String("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789~`!@#$%^&()-_=+[{]}|;:,<>?")).toCharArray();
		String randomStr = RandomStringUtils.random( 64, 0, possibleCharacters.length-1, false, false, possibleCharacters, new SecureRandom() );
		user.setSalt(randomStr);*/

		String pwd = user.getPassword();
		log.info("Password retrieved: {}", pwd);
		if(!isValidPassword(pwd)) throw new InvalidPasswordException(passwordValidationMessage);
		log.info("Password validated");
		String encodedPwd = passwordEncoder.encode(pwd);
		user.setPassword(encodedPwd);

		String activationToken = UUID.randomUUID().toString();
		user.setActivationToken(activationToken);

		Role role = new Role("USER");
		List<Role> roles = new ArrayList<>();
		roles.add(role);
		user.setRoles(roles);

		log.info("ready to create user");
		final User createdUser = userRepository.save(user);
		log.info("User created, send confirmation email");
		sendConfirmationMail( user.getEmail(), user.getActivationToken(), user.getFirstName() );
		log.info("Confirmation email sent");
		return user;
	}

	@Override
	public boolean isValidPassword(String password) {
		if(password==null) return false;
		String regex = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%]).{8,20}$";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(password);
		return matcher.matches();
	}

	@Override
	public boolean isValidEmail(String email) {
		EmailValidator emailValidator = EmailValidator.getInstance();
		return emailValidator.isValid(email);
	}

	public void sendConfirmationMail(String userMail, String token, String name) {
		try {
			emailSenderService.addTo(userMail);
			emailSenderService.setSubject("Please activate your account");
			emailSenderService.setFrom(emailFrom);

			StringBuilder builder = new StringBuilder();
			String link = String.format("%suser/confirm-account?token=%s", emailSenderService.getAppUrl(),token);

			builder.append(String.format("<h4>Dear %s</h4><p>Thank you for registering on %s.<br />", name, emailSenderService.getAppName()));
			builder.append("Please click on the below link to activate your account:<br />");
			builder.append(String.format("<a href=\"%s\"><b>Activate Account</b></a><br /><br />", link));
			builder.append(String.format("Alternatively, you can copy and paste this link in your browser:<br />%s <br /></p>", link));
			builder.append(String.format("<p>Regards, <br />%s</p>", emailSenderService.getAppName()));
			String body = builder.toString();

			emailSenderService.setHtmlMsg(body);
			String plain = Jsoup.parse(body).text();
			emailSenderService.setTextMsg(plain);
			emailSenderService.send();

		}
		catch (Exception e){
			log.warn(e.getMessage());
		}
	}

	@Override
	public void sendPasswordResetMail(String emailAddress, String token, String name, LocalDateTime forgottenPasswordTokenExpire) {
		try {
			emailSenderService.addTo(emailAddress);
			emailSenderService.setSubject("Forgotten password reset");
			emailSenderService.setFrom(emailFrom);

			StringBuilder builder = new StringBuilder();
			String link = String.format("%suser/reset-password?token=%s", emailSenderService.getAppUrl(),token);

			builder.append(String.format("<h4>Dear %s</h4><p>We received your request to reset your password on %s.<br />", name, emailSenderService.getAppName()));
			builder.append("Please click on the below link to reset your account password:<br />");
			builder.append(String.format("<a href=\"%s\"><b>Reset Password</b></a><br /><br />", link));
			builder.append(String.format("Alternatively, you can copy and paste this link in your browser:<br />%s <br /></p>", link));
			builder.append(String.format("<p>The activation link will expire on %s.</p>", forgottenPasswordTokenExpire));
			builder.append("<p>If you did not send this request, someone else may have tried to access your account. To be safe, we recommend that you logout of all your active sessions.</p>");
			builder.append(String.format("<p>Regards, <br />%s</p>", emailSenderService.getAppName()));
			String body = builder.toString();

			emailSenderService.setHtmlMsg(body);
			String plain = Jsoup.parse(body).text();
			emailSenderService.setTextMsg(plain);
			emailSenderService.send();
		}
		catch (Exception e){
			log.warn(e.getMessage());
		}
	}
}
