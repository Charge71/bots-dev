package com.charge71.services;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.charge71.framework.ApiClient;
import com.charge71.model.BusMilanoFavorites;
import com.charge71.model.BusMilanoFavorites.BusMilanoStop;
import com.charge71.model.BusMilanotUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class BusMilanoBotService {

	private static Logger log = Logger.getLogger(BusMilanoBotService.class);

	@Autowired
	private MongoTemplate mongoTemplate;

	private RestTemplate restTemplate = new RestTemplate();

	public void createUser(ApiClient client, String userId, String chatId) {
		if (!mongoTemplate.exists(Query.query(Criteria.where("id").is(userId)), BusMilanotUser.class)) {
			ObjectNode json = client.getUserInfo(chatId);
			BusMilanotUser user = new BusMilanotUser();
			user.setId(userId);
			user.setFirstName(json.get("first_name").asText());
			user.setLastName(json.get("last_name").asText());
			mongoTemplate.save(user);
		}
	}

	public void startBot(ApiClient client, String chatId, String userFirstName, String userLastName, String username,
			String userId) {
		BusMilanotUser user = new BusMilanotUser();
		user.setId(userId);
		user.setChatId(chatId);
		user.setFirstName(userFirstName);
		if (userLastName != null) {
			user.setLastName(userLastName);
		}
		if (username != null) {
			user.setUsername(username);
		}
		mongoTemplate.save(user);
		client.sendButton(chatId,
				"Ciao " + userFirstName
						+ "! Invia il numero della fermata ATM che ti interessa per ricevere informazioni e tempi di attesa.",
				"/preferite");
	}

	public void help(ApiClient client, String chatId) {
		client.sendMessage(chatId,
				"Invia il numero della fermata ATM che ti interessa per ricevere informazioni e tempi di attesa. Premi il pulsante in basso o /preferite per visualizzare le fermate salvate come preferite.");
	}

	public void addFavorite(ApiClient client, String stopId, String chatId, String userId) {
		if (mongoTemplate.exists(Query.query(Criteria.where("id").is(userId)), BusMilanoFavorites.class)) {
			if (mongoTemplate.exists(Query.query(Criteria.where("id").is(userId).and("stops.id").is(stopId)),
					BusMilanoFavorites.class)) {
				client.sendMessage(chatId, "Fermata già inclusa nei preferiti.");
			} else if (mongoTemplate.exists(Query.query(Criteria.where("id").is(userId).and("stops.9").exists(true)),
					BusMilanoFavorites.class)) {
				client.sendMessage(chatId, "Non puoi avere più di 10 fermate preferite.");
			} else {
				try {
					Long.parseLong(stopId);
					ObjectNode info = getInfo(stopId);
					BusMilanoStop stop = new BusMilanoStop();
					stop.setId(stopId);
					stop.setName(info.get("StopPoint").get("Description").asText());
					mongoTemplate.updateFirst(Query.query(Criteria.where("id").is(userId)),
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
				favorites.setId(userId);
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

	public void removeFavorite(ApiClient client, String stopId, String chatId, String userId) {
		if (mongoTemplate.exists(Query.query(Criteria.where("id").is(userId)), BusMilanoFavorites.class)) {
			if (mongoTemplate.exists(Query.query(Criteria.where("id").is(userId).and("stops.id").is(stopId)),
					BusMilanoFavorites.class)) {
				BusMilanoStop stop = new BusMilanoStop();
				stop.setId(stopId);
				mongoTemplate.updateFirst(Query.query(Criteria.where("id").is(userId)),
						new Update().pull("stops", stop), BusMilanoFavorites.class);
				client.sendMessage(chatId, "Fermata rimossa dai preferiti.");
			} else {
				client.sendMessage(chatId, "Fermata non inclusa nei preferiti.");
			}
		} else {
			client.sendMessage(chatId, "Fermata non inclusa nei preferiti.");
		}
	}

	public void listFavorites(ApiClient client, String chatId, String userId) {
		if (mongoTemplate.exists(Query.query(Criteria.where("id").is(userId)), BusMilanoFavorites.class)) {
			BusMilanoFavorites favorites = mongoTemplate.findById(userId, BusMilanoFavorites.class);
			if (favorites.getStops() != null && favorites.getStops().length > 0) {
				StringBuilder message = new StringBuilder("Fermate preferite:");
				for (BusMilanoStop stop : favorites.getStops()) {
					message.append("\n" + stop.getName() + " /ferm" + stop.getId());
					message.append(" (rimuovi /unfav" + stop.getId() + ")");
				}
				client.sendMessage(chatId, message.toString());
				client.sendMarkdownMessage(chatId,
						"_Grazie di utilizzare Bus Milano Bot! Supportalo condividendolo con i tuoi amici o lasciando una valutazione a questo_ [link](https://storebot.me/bot/busmilanobot)");
			} else {
				client.sendMessage(chatId, "Non hai salvato fermate preferite.");
			}
		} else {
			client.sendMessage(chatId, "Non hai salvato fermate preferite.");
		}
	}

	public void sendStopInfoMessenger(ApiClient client, String chatId, String stopId, String userId) {
		try {
			Long.parseLong(stopId);
			ObjectNode response = getInfo(stopId);
			client.sendMessage(chatId, "Fermata " + response.get("StopPoint").get("Description"));
			ObjectNode message = getResponseMessageMessenger(response, stopId, userId);
			client.sentStructuredMessage(chatId, message);
		} catch (NumberFormatException e) {
			client.sendMessage(chatId,
					"Il codice inserito non è corretto. Inserisci il codice che vedi sulla palina della fermata, ad esempio 11111.");
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
				client.sendMessage(chatId,
						"Il codice inserito non è corretto. Inserisci il codice che vedi sulla palina della fermata, ad esempio 11111.");
			} else {
				log.error("Errore su codice: " + stopId, e);
				client.sendMessage(chatId,
						"Errore nell'elaborazione del codice. Inserisci il codice che vedi sulla palina della fermata, ad esempio 11111.");
			}
		} catch (RestClientException e) {
			log.error("Errore su codice: " + stopId, e);
			client.sendMessage(chatId,
					"Errore nell'elaborazione del codice. Inserisci il codice che vedi sulla palina della fermata, ad esempio 11111.");
		}
	}

	public void sendStopInfoTelegram(ApiClient client, String chatId, String stopId, String userId) {
		try {
			Long.parseLong(stopId);
			ObjectNode response = getInfo(stopId);
			List<String> list = getResponseMessage(response, stopId, userId);
			for (String message : list) {
				client.sendMarkdownMessage(chatId, message);
			}
		} catch (NumberFormatException e) {
			client.sendMessage(chatId,
					"Il codice inserito non è corretto. Inserisci il codice che vedi sulla palina della fermata, ad esempio 11111.");
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
				client.sendMessage(chatId,
						"Il codice inserito non è corretto. Inserisci il codice che vedi sulla palina della fermata, ad esempio 11111.");
			} else {
				log.error("Errore su codice: " + stopId, e);
				client.sendMessage(chatId,
						"Errore nell'elaborazione del codice. Inserisci il codice che vedi sulla palina della fermata, ad esempio 11111.");
			}
		} catch (RestClientException e) {
			log.error("Errore su codice: " + stopId, e);
			client.sendMessage(chatId,
					"Errore nell'elaborazione del codice. Inserisci il codice che vedi sulla palina della fermata, ad esempio 11111.");
		}
	}

	public void stopBot(String userId) {
		BusMilanoFavorites favorites = mongoTemplate.findById(userId, BusMilanoFavorites.class);
		if (favorites != null) {
			mongoTemplate.remove(favorites);
		}
		BusMilanotUser user = mongoTemplate.findById(userId, BusMilanotUser.class);
		if (user != null) {
			mongoTemplate.remove(user);
		}
	}

	//

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

	private ObjectNode getResponseMessageMessenger(ObjectNode json, String stopId, String id) {
		ObjectNode response = JsonNodeFactory.instance.objectNode();
		ObjectNode message = response.putObject("message");
		ObjectNode attachment = message.putObject("attachment");
		attachment.put("type", "template");
		ObjectNode payload = attachment.putObject("payload");
		payload.put("template_type", "generic");
		ArrayNode elements = payload.putArray("elements");
		for (JsonNode line : json.get("Lines")) {
			String lineCode = line.get("Line").get("LineCode").asText();
			String lineDescription = line.get("Line").get("LineDescription").asText();
			String waitMessage = line.get("WaitMessage").isNull() ? "-" : line.get("WaitMessage").asText();
			ObjectNode element = elements.addObject();
			element.put("title", "Linea " + lineCode + " " + lineDescription);
			element.put("subtitle", "Attesa: " + waitMessage);
		}
		return response;
	}
}
