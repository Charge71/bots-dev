package com.charge71.telegramapi;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class TelegramApiClient {

	private static final String BASE_URL = "https://api.telegram.org/bot";

	private final String token;

	private RestTemplate restTemplate = new RestTemplate();

	TelegramApiClient(String token) {
		this.token = token;
	}

	public ObjectNode sendMessage(String chatId, String text) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL + token).path("/sendMessage")
				.queryParam("chat_id", chatId).queryParam("text", text);
		return restTemplate.getForObject(builder.build().encode().toUri(), ObjectNode.class);
	}
}
