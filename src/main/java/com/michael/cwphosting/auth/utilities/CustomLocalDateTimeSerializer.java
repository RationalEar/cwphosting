package com.michael.cwphosting.auth.utilities;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class CustomLocalDateTimeSerializer extends StdSerializer<LocalDateTime> {

	//private SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
	private DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
	//private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-ddThh-mm-ssz");


	public CustomLocalDateTimeSerializer() {
		this(null);
	}

	public CustomLocalDateTimeSerializer(Class t) {
		super(t);
	}

	@Override
	public void serialize (LocalDateTime value, JsonGenerator gen, SerializerProvider arg2) throws IOException, JsonProcessingException {
		ZoneId zoneId = formatter.getZone();
		String tz = zoneId != null ? zoneId.toString() : "";
		gen.writeString( formatter.format(value) + tz );
	}
}
