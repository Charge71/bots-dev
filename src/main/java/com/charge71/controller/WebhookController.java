package com.charge71.controller;

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

	@RequestMapping(value = "/test", method = RequestMethod.GET)
	public String test() {
		return "OK";
	}

	@RequestMapping(value = "/webhook/{token}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> webhook(@PathVariable("token") String token, @RequestBody ObjectNode json) {
		log.info(token);
		log.info(json);
		String command = null;
		for (JsonNode entity : json.get("message").get("entities")) {
			if (entity.get("type").asText().equals("bot_command")) {
				int offset = entity.get("offset").asInt();
				int length = entity.get("length").asInt();
				command = json.get("message").get("text").asText().substring(offset, offset + length);
			}
		}
		log.info(command);
		botDispatcher.exec(token, command, json);
		return new ResponseEntity<Void>(HttpStatus.OK);
	}
}
