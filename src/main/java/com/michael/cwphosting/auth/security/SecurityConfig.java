package com.michael.cwphosting.auth.security;

import com.michael.cwphosting.auth.jwt.JwtAuthenticationFilter;
import com.michael.cwphosting.auth.jwt.JwtTokenAuthorizationOncePerRequestFilter;
import com.michael.cwphosting.auth.jwt.JwtUnAuthorizedResponseAuthenticationEntryPoint;
import com.michael.cwphosting.auth.jwt.TokenUtil;
import com.michael.cwphosting.auth.services.RefreshTokenService;
import com.michael.cwphosting.auth.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@EnableGlobalMethodSecurity(prePostEnabled = true)
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {
	private final PasswordEncoder encoder;
	private final UserService userService;
	private TokenUtil tokenUtil;
	SecretConfig secret;
	private JwtUnAuthorizedResponseAuthenticationEntryPoint jwtUnAuthorizedResponseAuthenticationEntryPoint;
	private RefreshTokenService refreshTokenService;

	@Value("${jwt.get.token.uri}")
	private String authenticationPath;

	@Value("/register")
	private String registerPath;

	@Autowired
	public void setJwtUnAuthorizedResponseAuthenticationEntryPoint(JwtUnAuthorizedResponseAuthenticationEntryPoint jwtUnAuthorizedResponseAuthenticationEntryPoint) {
		this.jwtUnAuthorizedResponseAuthenticationEntryPoint = jwtUnAuthorizedResponseAuthenticationEntryPoint;
	}

	@Autowired
	public void setTokenUtil(TokenUtil tokenUtil) {
		this.tokenUtil = tokenUtil;
	}

	@Autowired
	public void setSecret(SecretConfig secret) {
		this.secret = secret;
	}

	@Autowired
	public void setRefreshTokenService(RefreshTokenService refreshTokenService) {
		this.refreshTokenService = refreshTokenService;
	}

	@Bean
	@Override
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}


	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(userService).passwordEncoder(encoder);
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.csrf().disable()
			.exceptionHandling().authenticationEntryPoint(jwtUnAuthorizedResponseAuthenticationEntryPoint).and()
			.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

		http.authorizeRequests().antMatchers("/token/**").permitAll();
		http.authorizeRequests().antMatchers("/user/**").permitAll();
		http.authorizeRequests().antMatchers("/users/**").hasAnyAuthority("ADMIN");

		http.authorizeRequests().anyRequest().authenticated();

		JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(authenticationManagerBean(), tokenUtil, userService, refreshTokenService );
		http.addFilter(jwtAuthenticationFilter);

		JwtTokenAuthorizationOncePerRequestFilter jwtAuthenticationTokenFilter = new JwtTokenAuthorizationOncePerRequestFilter(tokenUtil, userService);
		http.addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);

		http.headers().cacheControl(); //disable caching
		//.frameOptions().sameOrigin()  //H2 Console Needs this setting

	}

	@Override
	public void configure(WebSecurity webSecurity) throws Exception {
		webSecurity
				.ignoring().antMatchers(HttpMethod.POST, authenticationPath)
				.antMatchers(HttpMethod.OPTIONS, "/**")
				.and().ignoring().antMatchers(HttpMethod.POST, registerPath)
				.and().ignoring().antMatchers(HttpMethod.GET, "/"); //Other Stuff You want to Ignore
				//.and().ignoring().antMatchers("/h2-console/**/**");//Should not be in Production!
	}

}
