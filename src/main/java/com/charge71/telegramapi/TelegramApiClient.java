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

	public ObjectNode sendLocationRequest(String chatId, String text) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL + token).path("/sendMessage")
				.queryParam("chat_id", chatId).queryParam("text", text).queryParam("reply_markup", locationRequest());
		return restTemplate.getForObject(builder.build().encode().toUri(), ObjectNode.class);
	}
	
	public ObjectNode sendPhoto(String chatId, String fileId, String caption) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL + token).path("/sendPhoto")
				.queryParam("chat_id", chatId).queryParam("photo", fileId).queryParam("caption", caption);
		return restTemplate.getForObject(builder.build().encode().toUri(), ObjectNode.class);
	}

	public ObjectNode getUserProfilePhoto(String userId) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL + token).path("/getUserProfilePhotos")
				.queryParam("user_id", userId).queryParam("limit", 1);
		return restTemplate.getForObject(builder.build().encode().toUri(), ObjectNode.class);
	}

	//

	private String locationRequest() {
		return "{\"keyboard\":[[{\"text\":\"Check In\",\"request_location\":true}]],\"one_time_keyboard\":true}";
	}
}
