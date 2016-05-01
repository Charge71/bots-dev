package com.charge71.bots;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.charge71.model.BusMilanoFavorites;
import com.charge71.model.BusMilanoFavorites.BusMilanoStop;
import com.charge71.model.BusMilanotUser;
import com.charge71.telegramapi.TelegramApiAware;
import com.charge71.telegramapi.annotations.BotCommand;
import com.charge71.telegramapi.annotations.TelegramBot;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@TelegramBot("204159588:AAF3Y-4eKSheRYFlfOPhZ_Xvn1AcZLDgvqA")
public class BusMilanoBot extends TelegramApiAware {

	private static Logger log = Logger.getLogger(BusMilanoBot.class);

	@Autowired
	private MongoTemplate mongoTemplate;

	private RestTemplate restTemplate = new RestTemplate();

	@BotCommand("/start")
	public void start(ObjectNode json, String command) {
		log.debug("/start start");
		String chatId = json.get("message").get("chat").get("id").asText();
		String name = json.get("message").get("from").get("first_name").asText();
		String id = json.get("message").get("from").get("id").asText();
		BusMilanotUser user = new BusMilanotUser();
		user.setId(id);
		user.setChatId(chatId);
		user.setFirstName(name);
		if (json.get("message").get("from").get("last_name") != null) {
			String last = json.get("message").get("from").get("last_name").asText();
			user.setLastName(last);
		}
		if (json.get("message").get("from").get("username") != null) {
			user.setUsername(json.get("message").get("from").get("username").asText());
		}
		mongoTemplate.save(user);
		client.sendMessage(chatId, "Ciao " + name
				+ "! Invia il numero della fermata ATM che ti interessa per ricevere informazioni e tempi di attesa.");
	}

	@BotCommand("/help")
	public void help(ObjectNode json, String command) {
		log.debug("/help start");
		String chatId = json.get("message").get("chat").get("id").asText();
		client.sendMessage(chatId,
				"Invia il numero della fermata ATM che ti interessa per ricevere informazioni e tempi di attesa.");
	}

	@BotCommand(value = "/fav", isPrefix = true)
	public void fav(ObjectNode json, String command) {
		log.debug("/fav start");
		String id = json.get("message").get("from").get("id").asText();
		String chatId = json.get("message").get("chat").get("id").asText();
		String stopId = command.substring(4);
		if (mongoTemplate.exists(Query.query(Criteria.where("id").is(id)), BusMilanoFavorites.class)) {
			if (mongoTemplate.exists(Query.query(Criteria.where("id").is(id).and("stops.id").is(stopId)),
					BusMilanoFavorites.class)) {
				client.sendMessage(chatId, "Fermata già inclusa nei preferiti.");
			} else {
				try {
					Long.parseLong(stopId);
					ObjectNode info = getInfo(stopId);
					BusMilanoStop stop = new BusMilanoStop();
					stop.setId(stopId);
					stop.setName(info.get("StopPoint").get("Description").asText());
					mongoTemplate.updateFirst(Query.query(Criteria.where("id").is(id)),
							new Update().push("stops", stop), BusMilanoFavorites.class);
					client.sendMessage(chatId, "Fermata " + stop.getName() + " aggiunta ai preferiti.");
				} catch (NumberFormatException e) {
					client.sendMessage(chatId, "Il codice inserito non è corretto.");
				} catch (RestClientException e) {
					log.error("Errore su codice: " + stopId, e);
					client.sendMessage(chatId, "Errore nell'elaborazione del codice.");
				}
			}
		} else {
			try {
				Long.parseLong(stopId);
				ObjectNode info = getInfo(stopId);
				BusMilanoStop stop = new BusMilanoStop();
				stop.setId(stopId);
				stop.setName(info.get("StopPoint").get("Description").asText());
				BusMilanoFavorites favorites = new BusMilanoFavorites();
				favorites.setId(id);
				favorites.setStops(new BusMilanoStop[] { stop });
				mongoTemplate.save(favorites);
				client.sendMessage(chatId, "Fermata " + stop.getName() + " aggiunta ai preferiti.");
			} catch (NumberFormatException e) {
				client.sendMessage(chatId, "Il codice inserito non è corretto.");
			} catch (RestClientException e) {
				log.error("Errore su codice: " + stopId, e);
				client.sendMessage(chatId, "Errore nell'elaborazione del codice.");
			}
		}
	}

	@BotCommand("/pref")
	public void favorites(ObjectNode json, String command) {
		log.debug("/preferiti start");
		String id = json.get("message").get("from").get("id").asText();
		String chatId = json.get("message").get("chat").get("id").asText();
		if (mongoTemplate.exists(Query.query(Criteria.where("id").is(id)), BusMilanoFavorites.class)) {
			BusMilanoFavorites favorites = new BusMilanoFavorites();
			StringBuilder message = new StringBuilder("Fermate preferite:");
			for (BusMilanoStop stop : favorites.getStops()) {
				message.append("\n" + stop.getName() + " /stop" + stop.getId());
			}
			client.sendMessage(chatId, message.toString());
		} else {
			client.sendMessage(chatId, "Non hai salvato fermate preferite.");
		}
	}

	@BotCommand("default")
	public void def(ObjectNode json, String command) {
		log.debug("default start");
		String chatId = json.get("message").get("chat").get("id").asText();
		String stopId = json.get("message").get("text").asText();
		stop(chatId, stopId);
	}

	@BotCommand(value = "/stop", isPrefix = true)
	public void stop(ObjectNode json, String command) {
		log.debug("/stop start");
		String chatId = json.get("message").get("chat").get("id").asText();
		String stopId = command.substring(5);
		stop(chatId, stopId);
	}

	//

	private void stop(String chatId, String stopId) {
		try {
			Long.parseLong(stopId);
			ObjectNode response = getInfo(stopId);
			List<String> list = getResponseMessage(response);
			for (String message : list) {
				client.sendMarkdownMessage(chatId, message);
			}
		} catch (NumberFormatException e) {
			client.sendMessage(chatId, "Il codice inserito non è corretto.");
		} catch (RestClientException e) {
			log.error("Errore su codice: " + stopId, e);
			client.sendMessage(chatId, "Errore nell'elaborazione del codice.");
		}
	}

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
