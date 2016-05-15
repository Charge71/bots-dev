package com.charge71.messengerapi;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.charge71.framework.ApiClient;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class MessengerApiClient implements ApiClient {

	private static final String BASE_URL = "https://graph.facebook.com";

	private static final String VERSION = "v2.6";

	private final String token;

	private RestTemplate restTemplate = new RestTemplate();

	MessengerApiClient(String token) {
		this.token = token;
	}

	@Override
	public ObjectNode sendMessage(String chatId, String text) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL).path("/").path(VERSION)
				.path("/me/messages").queryParam("access_token", token);
		String message = "{\"recipient\":{\"id\":\"" + chatId + "\"},\"message\":{\"text\":\"" + text + "\"}}";
		return restTemplate.postForObject(builder.build().encode().toUri(), message, ObjectNode.class);
	}

	@Override
	public ObjectNode sendMarkdownMessage(String chatId, String text) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectNode sendLocationRequest(String chatId, String text, String request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectNode sendButton(String chatId, String text, String request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectNode sendPhoto(String chatId, String fileId, String caption) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectNode sendLanguageButtons(String chatId, String text) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectNode getUserProfilePhoto(String userId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectNode getUserInfo(String userId) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL).path("/").path(userId)
				.queryParam("access_token", token);
		return restTemplate.getForObject(builder.build().encode().toUri(), ObjectNode.class);
	}

}
