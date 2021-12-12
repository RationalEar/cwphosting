package com.michael.cwphosting.auth.models;

import com.fasterxml.jackson.annotation.JacksonAnnotation;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.michael.cwphosting.auth.utilities.CustomDateSerializer;
import com.michael.cwphosting.auth.utilities.CustomLocalDateTimeSerializer;
import com.michael.cwphosting.auth.utilities.Md5Digest;
import com.mongodb.lang.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

@Data
@Document(collection = "users")
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

	private static final long serialVersionUID = 8217676219297719109L;

	@Id
	private String id;
	private String firstName;
	private String lastName;
	@Indexed(unique = true)
	private String email;
	private String phoneNumber;
	@Nullable
	private Address address;

	@JsonSerialize(using = LocalDateTimeSerializer.class)
	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	private LocalDateTime created;

	@JsonSerialize(using = LocalDateTimeSerializer.class)
	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	private LocalDateTime lastLogin;

	private boolean suspended;

	@Nullable @JsonIgnore
	private String activationToken;

	@Nullable @JsonIgnore
	private String forgottenPasswordToken;

	@Nullable @JsonIgnore
	private LocalDateTime forgottenPasswordTokenExpire;

	@JsonIgnore
	private String password;

	private int loginAttempts;

	@Nullable
	@JsonSerialize(using = LocalDateTimeSerializer.class)
	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	private LocalDateTime lastLoginAttempt;

	@Nullable
	private String failedLoginIpAddress;

	private Collection<Role> roles = new ArrayList<>();

	public User(String firstName, String lastName, String email, String phoneNumber, String password, Address address) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.phoneNumber = phoneNumber;
		this.password = password;
		this.address = address;
		created = LocalDateTime.now();
		suspended = false;
		loginAttempts = 0;
		activationToken = Md5Digest.digest((firstName+password+lastName).getBytes() );
		log.info(activationToken);
	}

	public boolean addRole(Role role){
		if(!roles.contains(role)) return roles.add(role);
		return false;
	}

	public String getUsername(){
		return email;
	}
}
