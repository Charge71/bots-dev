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
		client.sendButton(chatId,
				"Ciao " + name
						+ "! Invia il numero della fermata ATM che ti interessa per ricevere informazioni e tempi di attesa.",
				"/preferite");
	}

	@BotCommand("/help")
	public void help(ObjectNode json, String command) {
		log.debug("/help start");
		String chatId = json.get("message").get("chat").get("id").asText();
		client.sendMessage(chatId,
				"Invia il numero della fermata ATM che ti interessa per ricevere informazioni e tempi di attesa. Premi il pulsante in basso o /preferite per visualizzare le fermate salvate come preferite.");
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
			} else if (mongoTemplate.exists(Query.query(Criteria.where("id").is(id).and("stops.9").exists(true)),
					BusMilanoFavorites.class)) {
				client.sendMessage(chatId, "Non puoi avere più di 10 fermate preferite.");
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

	@BotCommand(value = "/unfav", isPrefix = true)
	public void unfav(ObjectNode json, String command) {
		log.debug("/unfav start");
		String id = json.get("message").get("from").get("id").asText();
		String chatId = json.get("message").get("chat").get("id").asText();
		String stopId = command.substring(6);
		if (mongoTemplate.exists(Query.query(Criteria.where("id").is(id)), BusMilanoFavorites.class)) {
			if (mongoTemplate.exists(Query.query(Criteria.where("id").is(id).and("stops.id").is(stopId)),
					BusMilanoFavorites.class)) {
				BusMilanoStop stop = new BusMilanoStop();
				stop.setId(stopId);
				mongoTemplate.updateFirst(Query.query(Criteria.where("id").is(id)), new Update().pull("stops", stop),
						BusMilanoFavorites.class);
				client.sendMessage(chatId, "Fermata rimossa dai preferiti.");
			} else {
				client.sendMessage(chatId, "Fermata non inclusa nei preferiti.");
			}
		} else {
			client.sendMessage(chatId, "Fermata non inclusa nei preferiti.");
		}
	}

	@BotCommand("/preferite")
	public void favorites(ObjectNode json, String command) {
		log.debug("/preferite start");
		String id = json.get("message").get("from").get("id").asText();
		String chatId = json.get("message").get("chat").get("id").asText();
		if (mongoTemplate.exists(Query.query(Criteria.where("id").is(id)), BusMilanoFavorites.class)) {
			BusMilanoFavorites favorites = mongoTemplate.findById(id, BusMilanoFavorites.class);
			if (favorites.getStops() != null && favorites.getStops().length > 0) {
				StringBuilder message = new StringBuilder("Fermate preferite:");
				for (BusMilanoStop stop : favorites.getStops()) {
					message.append("\n" + stop.getName() + " /ferm" + stop.getId());
					message.append(" (rimuovi /unfav" + stop.getId() + ")");
				}
				client.sendMessage(chatId, message.toString());
			} else {
				client.sendMessage(chatId, "Non hai salvato fermate preferite.");
			}
		} else {
			client.sendMessage(chatId, "Non hai salvato fermate preferite.");
		}
	}

	@BotCommand("default")
	public void def(ObjectNode json, String command) {
		log.debug("default start");
		String id = json.get("message").get("from").get("id").asText();
		String chatId = json.get("message").get("chat").get("id").asText();
		String stopId = json.get("message").get("text").asText();
		stop(chatId, stopId, id);
	}

	@BotCommand(value = "/ferm", isPrefix = true)
	public void ferm(ObjectNode json, String command) {
		log.debug("/ferm start");
		String id = json.get("message").get("from").get("id").asText();
		String chatId = json.get("message").get("chat").get("id").asText();
		String stopId = command.substring(5);
		stop(chatId, stopId, id);
	}

	@BotCommand("/stop")
	public void stop(ObjectNode json, String command) {
		log.debug("/stop start");
		String id = json.get("message").get("from").get("id").asText();
		BusMilanoFavorites favorites = mongoTemplate.findById(id, BusMilanoFavorites.class);
		if (favorites != null) {
			mongoTemplate.remove(favorites);
		}
		BusMilanotUser user = mongoTemplate.findById(id, BusMilanotUser.class);
		if (user != null) {
			mongoTemplate.remove(user);
		}
	}

	//

	private void stop(String chatId, String stopId, String userId) {
		try {
			Long.parseLong(stopId);
			ObjectNode response = getInfo(stopId);
			List<String> list = getResponseMessage(response, stopId, userId);
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

	private List<String> getResponseMessage(ObjectNode json, String stopId, String id) {
		List<String> result = new ArrayList<>(json.get("Lines").size() + 1);
		result.add("*Fermata " + json.get("StopPoint").get("Description") + "*");
		for (JsonNode line : json.get("Lines")) {
			String lineCode = line.get("Line").get("LineCode").asText();
			String lineDescription = line.get("Line").get("LineDescription").asText();
			String waitMessage = line.get("WaitMessage").isNull() ? "-" : line.get("WaitMessage").asText();
			String bookletUrl = line.get("BookletUrl").asText();
			String message = "Linea " + lineCode + " " + lineDescription + "\nAttesa: " + waitMessage + " ([orari]("
					+ bookletUrl + "))";
			result.add(message);
		}
		if (!mongoTemplate.exists(Query.query(Criteria.where("id").is(id).and("stops.id").is(stopId)),
				BusMilanoFavorites.class)) {
			result.add("Aggiungi fermata ai preferiti /fav" + stopId);
		} else {
			result.add("Rimuovi fermata dai preferiti /unfav" + stopId);
		}
		return result;
	}

}
