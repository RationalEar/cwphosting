package com.michael.cwphosting.auth.jwt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.michael.cwphosting.auth.models.Role;
import com.michael.cwphosting.auth.models.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JwtUserDetails implements UserDetails {

	private static final long serialVersionUID = 5155720064139820502L;
	private final User user;
	private final String id;
	private final String username;
	private final String password;
	private final boolean suspended;
	private Long activationGracePeriod = 60*60*24*7l;
	private final List<SimpleGrantedAuthority> authorities = new ArrayList<SimpleGrantedAuthority>();

	public JwtUserDetails(User user) {
		this.user = user;
		this.id = user.getId();
		this.username = user.getEmail();
		this.password = user.getPassword();
		this.suspended = user.isSuspended();
		for(Role role : user.getRoles()){
			this.authorities.add(new SimpleGrantedAuthority(role.getName()));
		}
	}

	@JsonIgnore
	public void setActivationGracePeriod(Long activationGracePeriod) {
		this.activationGracePeriod = activationGracePeriod;
	}


	@JsonIgnore
	public String getId() {
		return id;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@JsonIgnore
	@Override
	public boolean isAccountNonExpired() {
		String activationToken = user.getActivationToken();
		if(activationToken==null || activationToken.length()==0) return true;
		LocalDateTime created = user.getCreated();
		LocalDateTime expire = created.plusSeconds(activationGracePeriod);
		return expire.isAfter(LocalDateTime.now());
	}

	@JsonIgnore
	@Override
	public boolean isAccountNonLocked() {
		return !suspended;
	}

	@JsonIgnore
	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@JsonIgnore
	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public boolean isEnabled() {
		return !suspended;
	}

}
