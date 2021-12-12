package com.michael.cwphosting.auth.controllers;

import com.michael.cwphosting.auth.jwt.resource.JwtIdObjectRequest;
import com.michael.cwphosting.auth.jwt.resource.JwtMessageDataResponse;
import com.michael.cwphosting.auth.jwt.resource.JwtMessageResponse;
import com.michael.cwphosting.auth.jwt.resource.JwtStatusObjectRequest;
import com.michael.cwphosting.auth.models.User;
import com.michael.cwphosting.auth.repository.UserRepository;
import com.michael.cwphosting.auth.services.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.*;

@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
@Slf4j
public class UsersController {

	private final UserRepository userRepository;
	private final UserService userService;

	@GetMapping
	public ResponseEntity<?> getUsers(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int limit,
									  @RequestParam(defaultValue = "created:asc") String sort, @RequestParam(required = false) String filter){
		try {
			String[] parts = sort.split(":");
			Pageable paging;
			if(parts.length==2){
				String field = parts[0];
				Sort asc;
				if(field.equals("name")){
					asc = parts[1].equals("asc") ? Sort.by("firstName").and(Sort.by("lastName"))
							: Sort.by("firstName").descending().and(Sort.by("lastName").descending());
				}
				else{
					asc = parts[1].equals("asc") ? Sort.by(field).ascending() : Sort.by(field).descending();
				}
				paging = PageRequest.of(page, limit, asc );
				log.info("{}",paging);
			}
			else{
				paging = PageRequest.of(page, limit);
			}
			log.info("Filter by {}", filter);
			Page<User> pageUsers = filter == null || filter.trim().length()==0 ? userService.getUsers(paging) : userService.findUsers(filter.trim(), paging);

			Map<String, Object> response = new HashMap<>();
			response.put("items", pageUsers.getContent());
			response.put("currentPage", pageUsers.getNumber());
			response.put("totalItems", pageUsers.getTotalElements());
			response.put("totalPages", pageUsers.getTotalPages());
			log.info("sending out response");
			ResponseEntity<?> r = new ResponseEntity<>(response, HttpStatus.OK);
			return r;
		}
		catch (Exception e){
			log.warn("Unable to process request");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new JwtMessageResponse(e.getMessage()));
		}
	}

	@PostMapping
	public ResponseEntity<?> createUser(@RequestBody User data) {
		try {
			log.info("User: {}", data);
			String password = UUID.randomUUID().toString();
			data.setPassword(password);
			User user = userService.createUserAccount(data);
			URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(user.getId()).toUri();
			return ResponseEntity.created(uri).body(new JwtMessageResponse("User account created successfully"));
		}
		catch (Exception e){
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JwtMessageResponse(e.getMessage()));
		}
	}

	@PutMapping("/{id}")
	public ResponseEntity<?> updateUser(@PathVariable(name = "id") String id, @RequestBody User data) {
		try {
			log.info("User: {}", data);
			User user = userService.updateUserDetails(id, data);
			return ResponseEntity.ok(new JwtMessageDataResponse<>("User account updated successfully", user));
		}
		catch (Exception e){
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JwtMessageResponse(e.getMessage()));
		}
	}

	@PutMapping("/{id}/status")
	public ResponseEntity<?> updateUserStatus(@PathVariable(name = "id") String id, @RequestBody JwtStatusObjectRequest status) {
		try {
			log.info("User Status: {}", status);
			User user = userService.updateUserStatus(id, status.getStatus());
			return ResponseEntity.ok(new JwtMessageDataResponse<>("User account status updated successfully", user));
		}
		catch (Exception e){
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JwtMessageResponse(e.getMessage()));
		}
	}

	@DeleteMapping
	public ResponseEntity<?> removeUser(@RequestParam(name = "id") String id) {
		try {
			if(id==null || id.isEmpty()) throw new Exception("A valid user ID is required");
			User user = userService.findUserById(id);
			if(user==null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new JwtMessageResponse("User not found"));
			userService.deleteUser(user);
			return ResponseEntity.ok(new JwtMessageResponse("User account deleted successfully"));
		}
		catch (Exception e){
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JwtMessageResponse(e.getMessage()));
		}
	}

}
