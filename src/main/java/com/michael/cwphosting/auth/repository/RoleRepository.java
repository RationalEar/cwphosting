package com.michael.cwphosting.auth.repository;

import com.michael.cwphosting.auth.models.Role;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RoleRepository extends MongoRepository<Role, Long> {
	Optional<Role> findRoleByName(String name);
}
