package com.charge71.controller;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.charge71.messengerapi.MessengerBotDispatcher;
import com.charge71.telegramapi.TelegramBotDispatcher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
public class WebhookController {

	private static Logger log = Logger.getLogger(WebhookController.class);

	@Autowired
	private TelegramBotDispatcher botDispatcher;

	@Autowired
	private MessengerBotDispatcher messengerDispatcher;

	private File updateFile = new File(System.getProperty("OPENSHIFT_DATA_DIR") + "/update.props");

	public WebhookController() {
		if (!updateFile.exists()) {
			try {
				FileUtils.touch(updateFile);
			} catch (IOException e) {
				log.error("Cannot create update file", e);
			}
		}
	}

	@RequestMapping(value = "/test", method = RequestMethod.GET)
	public String test() {
		return "OK";
	}

	@RequestMapping(value = "/webhook/{token}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> webhook(@PathVariable("token") String token, @RequestBody ObjectNode json) {
		long updateId = json.get("update_id").asLong();
		if (!newUpdate(updateId, token)) {
			log.debug("Skipped old update " + updateId);
			return new ResponseEntity<Void>(HttpStatus.OK);
		}
		log.info(token);
		log.info(json);
		String command = null;
		if (json.get("message") != null && json.get("message").get("entities") != null) {
			for (JsonNode entity : json.get("message").get("entities")) {
				if (entity.get("type").asText().equals("bot_command")) {
					int offset = entity.get("offset").asInt();
					int length = entity.get("length").asInt();
					command = json.get("message").get("text").asText().substring(offset, offset + length);
				}
			}
		}
		if (command == null) {
			if (json.get("message") != null && json.get("message").get("location") != null) {
				command = "location";
			} else if (json.get("callback_query") != null) {
				command = "callback";
			}
		}
		if (command != null) {
			log.info("Command: " + command);
			botDispatcher.exec(token, command, json);
		} else {
			log.info("Default command");
			botDispatcher.exec(token, "default", json);
		}
		return new ResponseEntity<Void>(HttpStatus.OK);
	}

	@RequestMapping(value = "/messenger/{name}", method = RequestMethod.GET)
	public String messenger(@PathVariable("name") String name, @RequestParam("hub.verify_token") String token,
			@RequestParam("hub.challenge") String challenge) {
		if (token.equals(name)) {
			return challenge;
		} else {
			return "error";
		}
	}

	@RequestMapping(value = "/messenger/{name}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> messenger(@PathVariable("name") String name, @RequestBody ObjectNode json) {
		log.debug(name);
		log.debug(json);
		if (json.get("entry").get(0).get("messaging").get(0).get("message") != null) {
			messengerDispatcher.exec(name, null, json);
			log.info("Default message");
		} else if (json.get("entry").get(0).get("messaging").get(0).get("postback") != null) {
			String postback = json.get("entry").get(0).get("messaging").get(0).get("postback").asText();
			messengerDispatcher.exec(name, postback, json);
			log.info("Postback: " + postback);
		}
		return new ResponseEntity<Void>(HttpStatus.OK);
	}

	//

	private synchronized boolean newUpdate(long updateId, String token) {
		Properties props = new Properties();
		try {
			props.load(FileUtils.openInputStream(updateFile));
			long oldUpdate = Long.parseLong(props.getProperty(token, "0"));
			if (updateId > oldUpdate) {
				props.setProperty(token, String.valueOf(updateId));
				props.store(FileUtils.openOutputStream(updateFile), "");
				return true;
			}
			return false;
		} catch (IOException e) {
			log.warn("Cannot check update file", e);
			return true;
		}
	}
}
