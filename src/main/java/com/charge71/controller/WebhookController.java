package com.charge71.controller;

import java.io.File;
import java.io.IOException;

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
import org.springframework.web.bind.annotation.RestController;

import com.charge71.telegramapi.BotDispatcher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
public class WebhookController {

	private static Logger log = Logger.getLogger(WebhookController.class);

	@Autowired
	private BotDispatcher botDispatcher;

	private File updateFile = new File(System.getProperty("OPENSHIFT_DATA_DIR") + "/update.txt");

	public WebhookController() {
		if (!updateFile.exists()) {
			try {
				FileUtils.write(updateFile, "0");
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
		if (!newUpdate(updateId)) {
			log.debug("Skipped old update " + updateId);
			return new ResponseEntity<Void>(HttpStatus.OK);
		}
		log.info(token);
		log.info(json);
		String command = null;
		if (json.get("message").get("entities") != null) {
			for (JsonNode entity : json.get("message").get("entities")) {
				if (entity.get("type").asText().equals("bot_command")) {
					int offset = entity.get("offset").asInt();
					int length = entity.get("length").asInt();
					command = json.get("message").get("text").asText().substring(offset, offset + length);
				}
			}
		}
		if (command == null) {
			if (json.get("message").get("location") != null) {
				command = "location";
			}
		}
		if (command != null) {
			log.info(command);
			botDispatcher.exec(token, command, json);
		} else {
			log.info("No command found");
		}
		return new ResponseEntity<Void>(HttpStatus.OK);
	}

	//

	private synchronized boolean newUpdate(long updateId) {
		try {
			long oldUpdate = Long.parseLong(FileUtils.readFileToString(updateFile));
			if (updateId > oldUpdate) {
				FileUtils.write(updateFile, String.valueOf(updateId));
				return true;
			}
			return false;
		} catch (IOException e) {
			log.warn("Cannot check update file", e);
			return true;
		}
	}
}
