package com.michael.cwphosting.auth.utilities;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class ResponseBuilder {
	public static void sendMessageResponse(HttpServletResponse response, Exception e) throws IOException {
		response.setHeader("message", e.getMessage());
		response.setStatus(FORBIDDEN.value());
		Map<String,String> message = new HashMap<>();
		message.put("message", e.getMessage());
		response.setContentType(APPLICATION_JSON_VALUE);
		new ObjectMapper().writeValue(response.getOutputStream(), message);
	}

	public static void addJsonMessage(HttpServletResponse response, Exception e) throws IOException {
		response.setHeader("message", e.getMessage());
		Map<String,String> message = new HashMap<>();
		message.put("message", e.getMessage());
		response.setContentType(APPLICATION_JSON_VALUE);
	}

	public static void addJsonMessage(HttpServletResponse response, String message) throws IOException {
		response.setHeader("message", message);
		Map<String,String> msg = new HashMap<>();
		msg.put("message", message);
		response.setContentType(APPLICATION_JSON_VALUE);
	}
}
