package com.charge71.telegramapi;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.charge71.framework.RequestSender;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestTelegramMessage implements RequestSender<TelegramRequest, ObjectNode> {

	private final String token;

	private RestTemplate restTemplate = new RestTemplate();

	private static final String BASE_URL = "https://api.telegram.org/bot";

	TestTelegramMessage(String token) {
		this.token = token;
	}

	@Override
	public ResponseEntity<ObjectNode> sendRequest(TelegramRequest message) throws Exception {
		message.builder.uri(new URI(BASE_URL + token)).path(message.path);
		return restTemplate.getForEntity(message.builder.build().encode().toUri(), ObjectNode.class);
	}

	//

	public static void main(String[] args) throws Exception {
		TestTelegramMessage sender = new TestTelegramMessage("236804872:AAHa_Z0fdO_9CedIsBqfwEabwPJK5Lq1bow");

		TelegramRequest tm =
				TelegramRequest.sendMessage("148883640").text("Ciao")
		//.keyboard(Keyboard
		// .replyKeyboard()
		// .button(new String(Character.toChars(127468)) + new
		// String(Character.toChars(127463)) + " English")
		// .row()
		// .button(new String(Character.toChars(127470)) + new
		// String(Character.toChars(127481)) + " Italiano")
		// .resize());
		.hideKeyboard();
		// .parseModeMarkdown();

		//TelegramRequest tm = TelegramRequest.getUserProfilePhotos("148883640").limit(1);
		//String response = 
		sender.sendRequest(tm);

		//System.out.println(response);

		//System.out.println(JsonPath.with(response).get("result.photos.size() > 0"));
	}

}
