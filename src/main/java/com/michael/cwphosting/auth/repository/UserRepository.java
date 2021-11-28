package com.michael.cwphosting.auth.repository;

import com.michael.cwphosting.auth.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
	Optional<User> findUserByEmail(String email);
	Optional<User> findUserByActivationToken(String token);
	Optional<User> findUserByForgottenPasswordToken(String token);
}
