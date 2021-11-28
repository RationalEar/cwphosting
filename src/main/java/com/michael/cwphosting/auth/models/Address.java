package com.michael.cwphosting.auth.models;

import com.mongodb.lang.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Address {
	private String country;
	private String city;
	private String addressLine1;
	@Nullable
	private String addressLine2;
	@Nullable
	private String postCode;
}
