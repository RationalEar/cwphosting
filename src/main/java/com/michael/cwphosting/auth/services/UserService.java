package com.michael.cwphosting.auth.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.michael.cwphosting.auth.exceptions.InvalidEmailException;
import com.michael.cwphosting.auth.exceptions.InvalidPasswordException;
import com.michael.cwphosting.auth.exceptions.UserAlreadyExistAuthenticationException;
import com.michael.cwphosting.auth.models.Role;
import com.michael.cwphosting.auth.models.User;
import com.michael.cwphosting.auth.repository.RoleRepository;
import com.michael.cwphosting.auth.repository.UserRepository;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.validator.routines.EmailValidator;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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
	private MongoTemplate mongoTemplate;

	@Value("${spring.user.password.validation-message}")
	private String passwordValidationMessage;
	@Value("${spring.user.password.special-characters}")
	private String passwordSpecialChars;
	@Value("${spring.user.password.min-length}")
	private String passwordMinLength;
	@Value("${spring.user.password.max-length}")
	private String passwordMaxLength;

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

	@Autowired
	public void setMongoTemplate(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
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

	public User updateUserDetails(String id, User user){
		Optional<User> old = userRepository.findUserById(id);
		if(old.isPresent()){
			user.setCreated(null);
			user.setLastLoginAttempt(null);
			user.setForgottenPasswordTokenExpire(null);
			log.info("Last login attempt nullified...");
			ObjectMapper oMapper = new ObjectMapper();
			Map<String, Object> dataMap = oMapper.convertValue(user, Map.class);
			dataMap.values().removeIf(Objects::isNull);

			// skip roles, will update separately
			dataMap.remove("roles");

			Update update = new Update();
			dataMap.forEach(update::set);
			Query query = new Query();
			query.addCriteria(Criteria.where("_id").is(id));
			UpdateResult u = mongoTemplate.update(User.class).matching(query).apply(update).first();

			Optional<User> updated = userRepository.findUserById(user.getId());
			if(updated.isPresent()){
				User toUpdate = updated.get();

				List<Role> roles = new ArrayList<>();
				for(Role role: user.getRoles()){
					if(role.getId() != null) roles.add(role);
				}
				toUpdate.setRoles( roles );

				userRepository.save(toUpdate);
				updated = userRepository.findUserById(user.getId());
			}
			return updated.isPresent() ? updated.get() : null;
		}
		else return null;
	}

	@Override
	public User updateUserStatus(String id, Boolean status) {
		Optional<User> old = userRepository.findUserById(id);
		if(old.isPresent()){
			User user = old.get();
			user.setSuspended(status);
			userRepository.save(user);
			return user;
		}
		else return null;
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
	public User findUserById(String id) {
		Optional<User> user = userRepository.findUserById(id);
		return user.isPresent() ? user.get() : null;
	}

	@Override
	public void deleteUser(User user) {
		userRepository.delete(user);
	}

	@Override
	public List<User> getUsers() {
		return userRepository.findAll();
	}

	@Override
	public List<User> getUsers(int start, int limit) {
		return userRepository.findAll().subList(start, limit);
	}

	public Page<User> getUsers(Pageable pageable){
		return userRepository.findAll(pageable);
	}

	public Page<User> findUsers(String name, Pageable pageable){
//		return userRepository.findUserByFirstNameOrLastNameOrEmailContaining(name, pageable);
		return userRepository.findUserByFirstNameRegexOrLastNameRegexOrEmailRegex(".*"+name+".*", pageable);
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
		setAccountBasics(user);
		String pwd = user.getPassword();
		log.info("Password retrieved: {}", pwd);
		if(!isValidPassword(pwd)) throw new InvalidPasswordException(passwordValidationMessage);
		log.info("Password validated");
		String encodedPwd = passwordEncoder.encode(pwd);
		user.setPassword(encodedPwd);

		Optional<Role> role = roleRepository.findRoleByName("USER");
		if(role.isPresent()){
			List<Role> roles = new ArrayList<>();
			roles.add(role.get());
			user.setRoles(roles);
		}

		log.info("ready to create user");
		final User createdUser = userRepository.save(user);
		log.info("User created, send confirmation email");
		sendConfirmationMail( user.getEmail(), user.getActivationToken(), user.getFirstName() );
		log.info("Confirmation email sent");
		return user;
	}

	// Admin method for creating user account
	public User createUserAccount(User user) {

		setAccountBasics(user);
		String pwd = user.getPassword();
		user.setPassword(passwordEncoder.encode(pwd));

		log.info("ready to create user");
		final User createdUser = userRepository.save(user);
		log.info("User created, send confirmation email");
		sendConfirmationMail( user.getEmail(), user.getActivationToken(), user.getFirstName() );
		log.info("Confirmation email sent");
		return createdUser;
	}

	private void setAccountBasics(User user) {
		if(!isValidEmail(user.getEmail())) throw new InvalidEmailException("The submitted email address is not valid");

		Optional<User> emailUsed = userRepository.findUserByEmail(user.getEmail());
		if( emailUsed.isPresent() ){
			throw new UserAlreadyExistAuthenticationException("User with given email already exists");
		}
		user.setSuspended(suspendByDefault);
		user.setCreated(LocalDateTime.now());
		String activationToken = UUID.randomUUID().toString();
		user.setActivationToken(activationToken);
	}

	@Override
	public boolean isValidPassword(String password) {
		if(password==null) return false;
		StringBuilder sb = new StringBuilder();

		sb.append("^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[");
		sb.append(passwordSpecialChars);	// special characters
		sb.append("\"]).{");
		sb.append(passwordMinLength);	// min password length
		sb.append(",");
		sb.append(passwordMaxLength);	// max password length
		sb.append("}$");
		String regex = sb.toString();
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

			HtmlEmail htmlEmail = new HtmlEmail();
			emailSenderService.initialize(htmlEmail);

			htmlEmail.addTo(userMail);
			htmlEmail.setSubject("Please activate your account");
			htmlEmail.setFrom(emailFrom);

			StringBuilder builder = new StringBuilder();
			String link = String.format("%suser/confirm-account/%s", emailSenderService.getAppUrl(),token);

			builder.append(String.format("<h4>Dear %s</h4><p>Thank you for registering on %s.<br />", name, emailSenderService.getAppName()));
			builder.append("Please click on the link below to activate your account:<br />");
			builder.append(String.format("<a href=\"%s\"><b>Activate Account</b></a><br /><br />", link));
			builder.append(String.format("Alternatively, you can copy and paste this link in your browser:<br />%s <br /></p>", link));
			builder.append(String.format("<p>Regards, <br />%s</p>", emailSenderService.getAppName()));
			String body = builder.toString();

			htmlEmail.setHtmlMsg(body);
			String plain = Jsoup.parse(body).text();
			htmlEmail.setTextMsg(plain);
			htmlEmail.send();
		}
		catch (Exception e){
			log.warn(e.getMessage());
		}
	}

	@Override
	public void sendPasswordResetMail(String emailAddress, String token, String name, LocalDateTime forgottenPasswordTokenExpire) {
		try {

			DateTimeFormatter df = DateTimeFormatter.ofPattern("EEE, dd MMM uuuu, HH:mm");


			HtmlEmail htmlEmail = new HtmlEmail();
			emailSenderService.initialize(htmlEmail);
			htmlEmail.addTo(emailAddress);
			htmlEmail.setSubject("Forgotten password reset");
			htmlEmail.setFrom(emailFrom);

			StringBuilder builder = new StringBuilder();
			String link = String.format("%suser/reset-password/%s", emailSenderService.getAppUrl(),token);

			builder.append(String.format("<h4>Dear %s</h4><p>We received your request to reset your password on %s.<br />", name, emailSenderService.getAppName()));
			builder.append("Please click on the link below to reset your account password:<br />");
			builder.append(String.format("<a href=\"%s\"><b>Reset Password</b></a><br /><br />", link));
			builder.append(String.format("Alternatively, you can copy and paste this link in your browser:<br />%s <br /></p>", link));
			builder.append(String.format("<p>The activation link will expire on %s.</p>", forgottenPasswordTokenExpire.format(df)));
			builder.append("<p>If you did not send this request, someone else may have tried to access your account. To be safe, we recommend that you logout of all your active sessions.</p>");
			builder.append(String.format("<p>Regards, <br />%s</p>", emailSenderService.getAppName()));
			String body = builder.toString();

			htmlEmail.setHtmlMsg(body);
			String plain = Jsoup.parse(body).text();
			htmlEmail.setTextMsg(plain);
			htmlEmail.send();
		}
		catch (Exception e){
			log.warn(e.getMessage());
		}
	}
}
