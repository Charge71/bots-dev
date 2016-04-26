package com.charge71.bots;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.charge71.telegramapi.TelegramApiAware;
import com.charge71.telegramapi.annotations.BotCommand;
import com.charge71.telegramapi.annotations.TelegramBot;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@TelegramBot("204159588:AAF3Y-4eKSheRYFlfOPhZ_Xvn1AcZLDgvqA")
public class BusMilanoBot extends TelegramApiAware {

	private static Logger log = Logger.getLogger(BusMilanoBot.class);

	private RestTemplate restTemplate = new RestTemplate();

	@BotCommand("/start")
	public void start(ObjectNode json, String command) {
		log.debug("/start start");
		String chatId = json.get("message").get("chat").get("id").asText();
		String name = json.get("message").get("from").get("first_name").asText();
		client.sendMessage(chatId, "Ciao " + name
				+ "! Invia il numero della fermata ATM che ti interessa per ricevere informazioni e tempi di attesa.");
	}

	@BotCommand("/help")
	public void help(ObjectNode json, String command) {
		log.debug("/start start");
		String chatId = json.get("message").get("chat").get("id").asText();
		client.sendMessage(chatId,
				"Invia il numero della fermata ATM che ti interessa per ricevere informazioni e tempi di attesa.");
	}

	@BotCommand("default")
	public void def(ObjectNode json, String command) {
		log.debug("default start");
		String chatId = json.get("message").get("chat").get("id").asText();
		String text = json.get("message").get("text").asText();
		try {
			Long.parseLong(text);
			ObjectNode response = getInfo(text);
			List<String> list = getResponseMessage(response);
			for (String message : list) {
				client.sendMarkdownMessage(chatId, message);
			}
		} catch (NumberFormatException e) {
			client.sendMessage(chatId, "Il codice inserito non è corretto.");
		} catch (RestClientException e) {
			log.error("Errore su codice: " + text, e);
			client.sendMessage(chatId, "Errore nell'elaborazione del codice.");
		}
	}

	//

	private ObjectNode getInfo(String code) {
		UriComponentsBuilder builder = UriComponentsBuilder
				.fromHttpUrl("http://giromilano.atm.it/TPPortalBackEnd/tpl/stops/").path(code).path("/linesummary");
		return restTemplate.getForObject(builder.build().encode().toUri(), ObjectNode.class);
	}

	private List<String> getResponseMessage(ObjectNode json) {
		List<String> result = new ArrayList<>(json.get("Lines").size() + 1);
		result.add("*Fermata " + json.get("StopPoint").get("Description") + "*");
		for (JsonNode line : json.get("Lines")) {
			String lineCode = line.get("Line").get("LineCode").asText();
			String lineDescription = line.get("Line").get("LineDescription").asText();
			String waitMessage = line.get("WaitMessage").isNull() ? "-" : line.get("WaitMessage").asText();
			String bookletUrl = line.get("BookletUrl").asText();
			result.add("Linea " + lineCode + " " + lineDescription + "\nAttesa: " + waitMessage + " ([orari]("
					+ bookletUrl + "))");
		}
		return result;
	}

}
