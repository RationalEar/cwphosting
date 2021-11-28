package com.michael.cwphosting.auth.repository;

import com.michael.cwphosting.auth.models.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {
	Optional<RefreshToken> findRefreshTokenByToken(String token);
	List<RefreshToken> findRefreshTokensByUsername(String username);
}
