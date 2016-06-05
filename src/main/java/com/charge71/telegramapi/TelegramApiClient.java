package com.charge71.telegramapi;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.charge71.framework.ApiClient;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TelegramApiClient implements ApiClient {

	private static final String BASE_URL = "https://api.telegram.org/bot";

	private final String token;

	private RestTemplate restTemplate = new RestTemplate();

	TelegramApiClient(String token) {
		this.token = token;
	}

	public ObjectNode sendMessage(String chatId, String text) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL + token).path("/sendMessage")
				.queryParam("chat_id", chatId).queryParam("text", text);
		try {
			return restTemplate.getForObject(builder.build().encode().toUri(), ObjectNode.class);
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
				return null;
			} else {
				throw e;
			}
		}
	}

	public ObjectNode sendMarkdownMessage(String chatId, String text, boolean disablePreview) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL + token).path("/sendMessage")
				.queryParam("chat_id", chatId).queryParam("text", text).queryParam("parse_mode", "Markdown")
				.queryParam("disable_web_page_preview", disablePreview);
		return restTemplate.getForObject(builder.build().encode().toUri(), ObjectNode.class);
	}

	public ObjectNode sendLocationRequest(String chatId, String text, String request) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL + token).path("/sendMessage")
				.queryParam("chat_id", chatId).queryParam("text", text)
				.queryParam("reply_markup", locationRequest(request));
		return restTemplate.getForObject(builder.build().encode().toUri(), ObjectNode.class);
	}

	public ObjectNode sendForceReply(String chatId, String text) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL + token).path("/sendMessage")
				.queryParam("chat_id", chatId).queryParam("text", text).queryParam("reply_markup", forceReply());
		return restTemplate.getForObject(builder.build().encode().toUri(), ObjectNode.class);
	}

	public ObjectNode sendButton(String chatId, String text, String request) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL + token).path("/sendMessage")
				.queryParam("chat_id", chatId).queryParam("text", text).queryParam("reply_markup", button(request));
		return restTemplate.getForObject(builder.build().encode().toUri(), ObjectNode.class);
	}

	public ObjectNode sendPhoto(String chatId, String fileId, String caption) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL + token).path("/sendPhoto")
				.queryParam("chat_id", chatId).queryParam("photo", fileId).queryParam("caption", caption);
		return restTemplate.getForObject(builder.build().encode().toUri(), ObjectNode.class);
	}

	public ObjectNode sendLanguageButtons(String chatId, String text) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL + token).path("/sendMessage")
				.queryParam("chat_id", chatId).queryParam("text", text).queryParam("reply_markup", languageRequest());
		return restTemplate.getForObject(builder.build().encode().toUri(), ObjectNode.class);
	}

	public ObjectNode sendButtons(String chatId, String text, String buttons) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL + token).path("/sendMessage")
				.queryParam("chat_id", chatId).queryParam("text", text).queryParam("reply_markup", buttons);
		return restTemplate.getForObject(builder.build().encode().toUri(), ObjectNode.class);
	}

	public ObjectNode getUserProfilePhoto(String userId) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL + token).path("/getUserProfilePhotos")
				.queryParam("user_id", userId).queryParam("limit", 1);
		return restTemplate.getForObject(builder.build().encode().toUri(), ObjectNode.class);
	}

	@Override
	public ObjectNode getUserInfo(String userId) {
		// unused
		return null;
	}
	//

	private String forceReply() {
		return "{\"force_reply\":true}";
	}

	private String locationRequest(String request) {
		return "{\"keyboard\":[[{\"text\":\"" + request + "\",\"request_location\":true}]],\"resize_keyboard\":true}";
	}

	private String button(String request) {
		return "{\"keyboard\":[[{\"text\":\"" + request + "\"}]],\"resize_keyboard\":true}";
	}

	private String languageRequest() {
		return "{\"inline_keyboard\":[[{\"text\":\"English\",\"callback_data\":\"lang_en\"},{\"text\":\"Italiano\",\"callback_data\":\"lang_it\"}]]}";
	}

	@Override
	public ObjectNode sentStructuredMessage(String chatId, ObjectNode objectNode) {
		// unused
		return null;
	}

}
