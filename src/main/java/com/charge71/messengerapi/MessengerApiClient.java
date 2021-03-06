package com.charge71.messengerapi;

import java.nio.charset.Charset;

import org.apache.log4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.charge71.framework.ApiClient;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class MessengerApiClient implements ApiClient<MessengerRequest, ObjectNode> {

	private static final Logger log = Logger.getLogger(MessengerApiClient.class);

	private static final String BASE_URL = "https://graph.facebook.com";

	private static final String VERSION = "v2.7";

	private final String token;

	private final JsonNodeFactory factory = JsonNodeFactory.instance;

	private RestTemplate restTemplate = new RestTemplate();

	MessengerApiClient(String token) {
		this.token = token;
		restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
	}

	@Override
	public ObjectNode sendMessage(String chatId, String text) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL).path("/").path(VERSION)
				.path("/me/messages").queryParam("access_token", token);
		// String message = "{\"recipient\":{\"id\":\"" + chatId +
		// "\"},\"message\":{\"text\":\"" + text + "\"}}";
		ObjectNode msgNode = factory.objectNode();
		msgNode.putObject("recipient").put("id", chatId);
		msgNode.putObject("message").put("text", text);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>(msgNode.toString(), headers);
		return restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.POST, entity, ObjectNode.class)
				.getBody();
	}

	@Override
	public ObjectNode sendMarkdownMessage(String chatId, String text, boolean disablePreview) {
		// unused
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
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL).path("/").path(VERSION).path("/")
				.path(userId).queryParam("access_token", token);
		return restTemplate.getForObject(builder.build().encode().toUri(), ObjectNode.class);
	}

	@Override
	public ObjectNode sentStructuredMessage(String chatId, ObjectNode objectNode) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL).path("/").path(VERSION)
				.path("/me/messages").queryParam("access_token", token);
		objectNode.putObject("recipient").put("id", chatId);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>(objectNode.toString(), headers);
		log.info("Message url " + builder.build().encode().toUri());
		log.info("Message body: " + objectNode.toString());
		return restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.POST, entity, ObjectNode.class)
				.getBody();
	}

	@Override
	public ObjectNode sendForceReply(String chatId, String text) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectNode sendButtons(String chatId, String text, String buttons) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<ObjectNode> sendRequest(MessengerRequest request) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectNode sendSettings(ObjectNode objectNode) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL).path("/").path(VERSION)
				.path("/me/thread_settings").queryParam("access_token", token);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>(objectNode.toString(), headers);
		return restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.POST, entity, ObjectNode.class)
				.getBody();
	}

}
