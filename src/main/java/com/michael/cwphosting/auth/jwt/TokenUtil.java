package com.michael.cwphosting.auth.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Clock;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultClock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Date;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TokenUtil implements Serializable {
	static final String CLAIM_KEY_USERNAME = "sub";
	static final String CLAIM_KEY_CREATED = "iat";
	static final String CLAIM_KEY_ROLES = "roles";
	private static final long serialVersionUID = -3301605591108950415L;
	private Clock clock = DefaultClock.INSTANCE;

	@Value("${jwt.signing.key.secret}")
	private String secret;

	@Value("${jwt.token.expiration.in.seconds}")
	private Long tokenExpiration;

	@Value("${jwt.token.refresh.expiration.in.seconds}")
	private Long refreshTokenExpiration;

	@Value("${jwt.account.activation.expire}")
	private Long accountActivationExpire;

	public Long getAccountActivationExpire() {
		return accountActivationExpire;
	}

	public String getUsernameFromToken(String token) {
		return getClaimFromToken(token, Claims::getSubject);
	}

	public Date getIssuedAtDateFromToken(String token) {
		return getClaimFromToken(token, Claims::getIssuedAt);
	}

	public Date getExpirationDateFromToken(String token) {
		return getClaimFromToken(token, Claims::getExpiration);
	}

	public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = getAllClaimsFromToken(token);
		return claimsResolver.apply(claims);
	}

	public Claim getClaim(String token, String claim) {
		Algorithm algorithm = getAlgorithm();
		JWTVerifier verifier = JWT.require(algorithm).build();
		DecodedJWT decodedJWT = verifier.verify(token);
		return decodedJWT.getClaim(claim);
	}

	private Claims getAllClaimsFromToken(String token) {
		return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
	}

	private Boolean isTokenExpired(DecodedJWT token) {
		final Date expiration = token.getExpiresAt();
		return expiration.before(clock.now());
	}

	private Boolean ignoreTokenExpiration(String token) {
		// here you specify tokens, for that the expiration is ignored
		return false;
	}

	public String generateToken(UserDetails userDetails) {
		return generateToken(userDetails, false);
	}

	public DecodedJWT getDecodedToken(String token){
		Algorithm algorithm = getAlgorithm();
		JWTVerifier verifier = JWT.require(algorithm).build();
		return verifier.verify(token);
	}

	public String generateToken(UserDetails user, boolean refreshToken) {
		final Date createdDate = clock.now();
		Algorithm algorithm = getAlgorithm();
		if(refreshToken){
			final Date expirationDate = calculateExpirationDate(createdDate,refreshTokenExpiration);
			return JWT.create()
					.withSubject(user.getUsername())
					.withExpiresAt(expirationDate)
					.sign(algorithm);
		}
		else{
			final Date expirationDate = calculateExpirationDate(createdDate, tokenExpiration);
			return JWT.create()
					.withSubject(user.getUsername())
					.withExpiresAt(expirationDate)
					.withClaim("roles", user.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()))
					.sign(algorithm);
		}
	}

	/*public Boolean canTokenBeRefreshed(String token) {
		return (!isTokenExpired(token) || ignoreTokenExpiration(token));
	}*/

	public String refreshToken(String token) {
		final Date createdDate = clock.now();
		final Date expirationDate = calculateExpirationDate(createdDate, tokenExpiration);

		final Claims claims = getAllClaimsFromToken(token);
		claims.setIssuedAt(createdDate);
		claims.setExpiration(expirationDate);

		return Jwts.builder().setClaims(claims).signWith(SignatureAlgorithm.HS512, secret).compact();
	}

	public Boolean validateToken(DecodedJWT token, UserDetails user) {
		final String username = token.getSubject();
		return (username.equals(user.getUsername()) && !isTokenExpired(token));
	}

	private Date calculateExpirationDate(Date createdDate, Long expirationTime) {
		return new Date(createdDate.getTime() + expirationTime * 1000);
	}

	public Algorithm getAlgorithm(){
		return Algorithm.HMAC256(secret.getBytes());
	}

//	public
}
