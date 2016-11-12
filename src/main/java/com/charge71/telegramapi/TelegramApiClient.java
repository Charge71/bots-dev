package com.charge71.telegramapi;

import java.net.URI;

import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.charge71.framework.ApiClient;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TelegramApiClient implements ApiClient<TelegramRequest, ObjectNode> {

	private static Logger log = Logger.getLogger(TelegramApiClient.class);

	private static final String BASE_URL = "https://api.telegram.org/bot";

	private final String token;

	private RestTemplate restTemplate = new RestTemplate();

	TelegramApiClient(String token) {
		this.token = token;
	}

	@Override
	public ResponseEntity<ObjectNode> sendRequest(TelegramRequest message) throws Exception {
		message.builder.uri(new URI(BASE_URL + token)).path(message.path);
		return restTemplate.getForEntity(message.builder.build().encode().toUri(), ObjectNode.class);
	}

	@Override
	public ObjectNode sendMessage(String chatId, String text) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL + token).path("/sendMessage")
				.queryParam("chat_id", chatId).queryParam("text", text);
		try {
			return restTemplate.getForObject(builder.build().encode().toUri(), ObjectNode.class);
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
				log.warn("FOBIDDEN chatId: " + chatId);
				return null;
			} else if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
				log.warn("BAD_REQUEST text: " + text);
				return null;
			} else {
				throw e;
			}
		}
	}

	@Override
	public ObjectNode sendMarkdownMessage(String chatId, String text, boolean disablePreview) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL + token).path("/sendMessage")
				.queryParam("chat_id", chatId).queryParam("text", text).queryParam("parse_mode", "Markdown")
				.queryParam("disable_web_page_preview", disablePreview);
		try {
			return restTemplate.getForObject(builder.build().encode().toUri(), ObjectNode.class);
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
				log.warn("FOBIDDEN chatId: " + chatId);
				return JsonNodeFactory.instance.objectNode().put("errorCode", HttpStatus.FORBIDDEN.value());
			} else if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
				log.warn("BAD_REQUEST text: " + text);
				return JsonNodeFactory.instance.objectNode().put("errorCode", HttpStatus.BAD_REQUEST.value());
			} else {
				throw e;
			}
		}
	}

	@Override
	public ObjectNode sendLocationRequest(String chatId, String text, String request) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL + token).path("/sendMessage")
				.queryParam("chat_id", chatId).queryParam("text", text)
				.queryParam("reply_markup", locationRequest(request));
		return restTemplate.getForObject(builder.build().encode().toUri(), ObjectNode.class);
	}

	@Override
	public ObjectNode sendForceReply(String chatId, String text) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL + token).path("/sendMessage")
				.queryParam("chat_id", chatId).queryParam("text", text).queryParam("reply_markup", forceReply());
		return restTemplate.getForObject(builder.build().encode().toUri(), ObjectNode.class);
	}

	@Override
	public ObjectNode sendButton(String chatId, String text, String request) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL + token).path("/sendMessage")
				.queryParam("chat_id", chatId).queryParam("text", text).queryParam("reply_markup", button(request));
		return restTemplate.getForObject(builder.build().encode().toUri(), ObjectNode.class);
	}

	@Override
	public ObjectNode sendPhoto(String chatId, String fileId, String caption) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL + token).path("/sendPhoto")
				.queryParam("chat_id", chatId).queryParam("photo", fileId).queryParam("caption", caption);
		return restTemplate.getForObject(builder.build().encode().toUri(), ObjectNode.class);
	}

	@Override
	public ObjectNode sendLanguageButtons(String chatId, String text) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL + token).path("/sendMessage")
				.queryParam("chat_id", chatId).queryParam("text", text).queryParam("reply_markup", languageRequest());
		return restTemplate.getForObject(builder.build().encode().toUri(), ObjectNode.class);
	}

	@Override
	public ObjectNode sendButtons(String chatId, String text, String buttons) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL + token).path("/sendMessage")
				.queryParam("chat_id", chatId).queryParam("text", text).queryParam("reply_markup", buttons);
		return restTemplate.getForObject(builder.build().encode().toUri(), ObjectNode.class);
	}

	@Override
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

	@Override
	public ObjectNode sendSettings(ObjectNode objectNode) {
		// TODO Auto-generated method stub
		return null;
	}

}
