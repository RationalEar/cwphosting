package com.michael.cwphosting;

import com.michael.cwphosting.auth.models.Address;
import com.michael.cwphosting.auth.models.Role;
import com.michael.cwphosting.auth.models.User;
import com.michael.cwphosting.auth.repository.RoleRepository;
import com.michael.cwphosting.auth.repository.UserRepository;
import com.michael.cwphosting.auth.services.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

@SpringBootApplication
@Slf4j
public class CwpHostingManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CwpHostingManagerApplication.class, args);
	}

	@Bean
	PasswordEncoder passwordEncoder(){
		return new BCryptPasswordEncoder();
	}

	@Bean
	CommandLineRunner runner(UserService userService, RoleRepository roleRepository){
		return args -> {

			String roleName = "USER";
			Optional<Role> role = roleRepository.findRoleByName(roleName);
			if(role.isEmpty()){
				roleRepository.insert(new Role(roleName));
				role = roleRepository.findRoleByName(roleName);
			}

			roleName = "ADMIN";
			Optional<Role> role2 = roleRepository.findRoleByName(roleName);
			if(role2.isEmpty()){
				roleRepository.insert(new Role(roleName));
				role2 = roleRepository.findRoleByName(roleName);
			}

			if(userService.countUsers()==0){
				Address address = new Address("Zimbabwe", "Harare", "Address Line One", "Address Line Two", "00263");
				User newUser = new User("Admin", "User", "admin@example.com", "0123456789", "password", address);
				if(role.isPresent()) newUser.addRole(role.get());
				if(role2.isPresent()) newUser.addRole(role2.get());
				userService.saveUser(newUser);
			}

		};
	}

	private void createUserAfterQuery(UserRepository repository, MongoTemplate mongoTemplate) {
		Query query = new Query();
		String email = "michael@zimall.co.zw";
		query.addCriteria(Criteria.where("email").is(email));
		List<User> users = mongoTemplate.find(query, User.class);
		if(users.isEmpty()){
			Address address = new Address("Zimbabwe", "Harare", "52 Friarsgate", "03 J. Tongogara Avenue", "00263");
			User user = new User("Michael", "Chiwere", email, "0775441383", "password", address);
			repository.insert(user);
		}
		else{
			System.out.println("Student already exists");
		}
	}

}
