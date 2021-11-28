package com.michael.cwphosting.auth.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "user_roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role {
	@Id
	private String id;
	private String name;

	public Role(String roleName) {
		this.name = roleName;
	}
}
