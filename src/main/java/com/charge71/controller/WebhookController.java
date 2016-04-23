package com.charge71.controller;

import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
public class WebhookController {

	private Logger log = Logger.getLogger(WebhookController.class);

	@RequestMapping(value = "/test", method = RequestMethod.GET)
	public String test() {
		return "OK";
	}

	@RequestMapping(value = "/webhook/{token}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> webhook(@PathVariable("token") String token, @RequestBody ObjectNode json) {
		log.info(token);
		log.info(json);
		return new ResponseEntity<Void>(HttpStatus.OK);
	}
}
