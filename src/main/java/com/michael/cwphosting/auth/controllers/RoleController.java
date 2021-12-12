package com.michael.cwphosting.auth.controllers;

import com.michael.cwphosting.auth.models.Role;
import com.michael.cwphosting.auth.repository.RoleRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@AllArgsConstructor
@Slf4j
public class RoleController {

	private final RoleRepository roleRepository;

	@GetMapping
	ResponseEntity<?> getRoles(){
		List<Role> roles = roleRepository.findAll();
		return new ResponseEntity<>(roles, HttpStatus.OK);
	}

}
