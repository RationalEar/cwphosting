package com.michael.cwphosting.auth.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document(collection = "refresh_tokens")
@AllArgsConstructor
@NoArgsConstructor
public class RefreshToken {
	@Id
	String id;
	String username;
	@Indexed(unique = true)
	String token;
	Date created;
	Date expires;

	public RefreshToken(String username, String token, Date created, Date expires) {
		this.username = username;
		this.token = token;
		this.created = created;
		this.expires = expires;
	}
}
