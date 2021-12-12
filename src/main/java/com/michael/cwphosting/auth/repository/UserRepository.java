package com.michael.cwphosting.auth.repository;

import com.michael.cwphosting.auth.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
	Optional<User> findUserById(String id);
	Optional<User> findUserByEmail(String email);
	Optional<User> findUserByActivationToken(String token);
	Optional<User> findUserByForgottenPasswordToken(String token);
	Page<User> findUserByFirstNameOrLastNameOrEmailContaining(String name, Pageable pageable);
	Page<User> findUserByFirstNameContains(String name, Pageable pageable);

	@Query(value = "{ $or: [ { 'firstName' : {$regex:?0,$options:'i'} }, { 'lastName' : {$regex:?0,$options:'i'} }, { 'email' : {$regex:?0,$options:'i'} } ] }")
	Page<User> findUserByFirstNameRegexOrLastNameRegexOrEmailRegex(String name, Pageable pageable);
}
