package com.charge71.lang;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

public class MessageHelper {

	private Properties props = new Properties();

	public MessageHelper(String token) throws IOException {
		InputStream in = this.getClass().getClassLoader().getResourceAsStream(token + ".props");
		props.load(in);
	}

	public String getMessage(String lang, String label, String... args) {
		String message = props.getProperty(lang + "." + label);
		return processArgs(message, args);
	}

	//

	private String processArgs(String message, String... args) {
		for (String arg : args) {
			message = StringUtils.replaceOnce(message, "{}", arg);
		}
		return message;
	}

}
