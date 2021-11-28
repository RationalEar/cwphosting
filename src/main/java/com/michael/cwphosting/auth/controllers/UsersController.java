package com.michael.cwphosting.auth.controllers;

import com.michael.cwphosting.auth.models.User;
import com.michael.cwphosting.auth.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
@AllArgsConstructor
public class UsersController {

	private final UserRepository userRepository;

	@GetMapping
	public List<User> getUsers(){
		List<User> users = userRepository.findAll();
		return users;
	}
}
