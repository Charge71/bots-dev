package com.charge71.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
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
import com.charge71.model.BusMilanoLogEntry;
import com.charge71.model.BusMilanotUser;
import com.charge71.telegramapi.TelegramRequest;
import com.charge71.telegramapi.TelegramRequest.Keyboard;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class BusMilanoBotService {

	private static final String BUS_STOP = new String(Character.toChars(128655));
	private static final String PLUS_SIGN = new String(Character.toChars(10133));
	private static final String MINUS_SIGN = new String(Character.toChars(10134));

	private static final Logger log = Logger.getLogger(BusMilanoBotService.class);

	private static final FastDateFormat formatter = FastDateFormat.getInstance("yyyyMMddHHmmss");

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

	public void listFavoritesTelegram(ApiClient client, String chatId, String userId) {
		if (mongoTemplate.exists(Query.query(Criteria.where("id").is(userId)), BusMilanoFavorites.class)) {
			BusMilanoFavorites favorites = mongoTemplate.findById(userId, BusMilanoFavorites.class);
			if (favorites.getStops() != null && favorites.getStops().length > 0) {
				Keyboard buttons = Keyboard.replyKeyboard().resize();
				for (int i = 0; i < favorites.getStops().length; i++) {
					if (i > 0) {
						buttons.row();
					}
					BusMilanoStop stop = favorites.getStops()[i];
					buttons.button(BUS_STOP + stop.getId() + " " + stop.getName());
				}
				TelegramRequest req = TelegramRequest.sendMessage(chatId)
						.text("_Grazie di utilizzare Bus Milano Bot! Supportalo condividendolo con i tuoi amici o lasciando una valutazione a questo_ [link](https://storebot.me/bot/busmilanobot)")
						.disableWebPagePreview().parseModeMarkdown().keyboard(buttons);
				try {
					client.sendRequest(req);
				} catch (Exception e) {
					log.error("Error sending message to " + chatId, e);
				}
			} else {
				client.sendMessage(chatId, "Non hai salvato fermate preferite.");
			}
		} else {
			client.sendMessage(chatId, "Non hai salvato fermate preferite.");
		}
	}

	// public void listFavoritesTelegram(ApiClient client, String chatId, String
	// userId) {
	// if (mongoTemplate.exists(Query.query(Criteria.where("id").is(userId)),
	// BusMilanoFavorites.class)) {
	// BusMilanoFavorites favorites = mongoTemplate.findById(userId,
	// BusMilanoFavorites.class);
	// if (favorites.getStops() != null && favorites.getStops().length > 0) {
	// StringBuilder message = new StringBuilder("Fermate preferite:");
	// for (BusMilanoStop stop : favorites.getStops()) {
	// message.append("\n" + stop.getName() + " /ferm" + stop.getId());
	// message.append(" (rimuovi /unfav" + stop.getId() + ")");
	// }
	// client.sendMessage(chatId, message.toString());
	// client.sendMarkdownMessage(chatId,
	// "_Grazie di utilizzare Bus Milano Bot! Supportalo condividendolo con i
	// tuoi amici o lasciando una valutazione a questo_
	// [link](https://storebot.me/bot/busmilanobot)",
	// true);
	// } else {
	// client.sendMessage(chatId, "Non hai salvato fermate preferite.");
	// }
	// } else {
	// client.sendMessage(chatId, "Non hai salvato fermate preferite.");
	// }
	// }

	public void listFavoritesMessenger(ApiClient client, String chatId, String userId) {
		if (mongoTemplate.exists(Query.query(Criteria.where("id").is(userId)), BusMilanoFavorites.class)) {
			BusMilanoFavorites favorites = mongoTemplate.findById(userId, BusMilanoFavorites.class);
			if (favorites.getStops() != null && favorites.getStops().length > 0) {
				ObjectNode response = JsonNodeFactory.instance.objectNode();
				ObjectNode message = response.putObject("message");
				ObjectNode attachment = message.putObject("attachment");
				attachment.put("type", "template");
				ObjectNode payload = attachment.putObject("payload");
				payload.put("template_type", "generic");
				ArrayNode elements = payload.putArray("elements");
				for (BusMilanoStop stop : favorites.getStops()) {
					ObjectNode element = elements.addObject();
					element.put("title", stop.getName());
					ArrayNode buttons = element.putArray("buttons");
					ObjectNode button1 = buttons.addObject();
					button1.put("type", "postback");
					button1.put("title", "info");
					button1.put("payload", "stop" + stop.getId());
				}
				client.sentStructuredMessage(chatId, response);
				// client.sendMessage(chatId,
				// "Grazie di utilizzare Bus Milano Bot! Supportalo
				// condividendolo con i tuoi amici!");
			} else {
				client.sendMessage(chatId, "Non hai salvato fermate preferite.");
			}
		} else {
			client.sendMessage(chatId, "Non hai salvato fermate preferite.");
		}
	}

	public void sendInfoMessenger(ApiClient client, String chatId) {
		client.sendMessage(chatId, "Inserisci solo il codice che vedi sulla palina della fermata, ad esempio 11871.");
		client.sentStructuredMessage(chatId, getImageMessage("http://dev-charge71.rhcloud.com/static/palina.png"));
	}

	public void sendInfoTelegram(ApiClient client, String chatId) {
		client.sendMessage(chatId, "Inserisci solo il codice che vedi sulla palina della fermata, ad esempio 11871.");
	}

	public void sendStopInfoMessenger(ApiClient client, String chatId, String stopId, String userId) {
		try {
			if (stopId.length() > 5) {
				stopId = stopId.substring(0, 5);
			}
			Long.parseLong(stopId);
			ObjectNode response = getInfo(stopId);
			client.sendMessage(chatId, "Fermata " + response.get("StopPoint").get("Description"));
			ObjectNode message = getResponseMessageMessenger(response, stopId, userId);
			client.sentStructuredMessage(chatId, message);
			message = getResponseButtonsMessenger(response, stopId, userId);
			client.sentStructuredMessage(chatId, message);
		} catch (NumberFormatException e) {
			client.sendMessage(chatId,
					"Il codice inserito non è corretto. Inserisci solo il codice che vedi sulla palina della fermata, ad esempio 11871.");
			client.sentStructuredMessage(chatId, getImageMessage("http://dev-charge71.rhcloud.com/static/palina.png"));
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
				client.sendMessage(chatId,
						"Il codice inserito non è corretto. Inserisci solo il codice che vedi sulla palina della fermata, ad esempio 11871.");
				client.sentStructuredMessage(chatId,
						getImageMessage("http://dev-charge71.rhcloud.com/static/palina.png"));
			} else {
				log.error("Errore su codice: " + stopId, e);
				client.sendMessage(chatId,
						"Errore nell'elaborazione del codice. Inserisci solo il codice che vedi sulla palina della fermata, ad esempio 11871.");
				client.sentStructuredMessage(chatId,
						getImageMessage("http://dev-charge71.rhcloud.com/static/palina.png"));
			}
		} catch (RestClientException e) {
			log.error("Errore su codice: " + stopId, e);
			client.sendMessage(chatId,
					"Errore nell'elaborazione del codice. Inserisci solo il codice che vedi sulla palina della fermata, ad esempio 11871.");
			client.sentStructuredMessage(chatId, getImageMessage("http://dev-charge71.rhcloud.com/static/palina.png"));
		}
	}

	public void sendStopInfoTelegram(ApiClient client, String chatId, String text, String userId) {
		if (text.startsWith(PLUS_SIGN)) {
			String stopId = text.substring(1, 6);
			addFavorite(client, stopId, chatId, userId);
			listFavoritesTelegram(client, chatId, userId);
			return;
		}
		if (text.startsWith(MINUS_SIGN)) {
			String stopId = text.substring(1, 6);
			removeFavorite(client, stopId, chatId, userId);
			listFavoritesTelegram(client, chatId, userId);
			return;
		}
		if (text.startsWith(BUS_STOP)) {
			text = text.substring(2, 7);
		}
		try {
			if (text.length() > 5) {
				text = text.substring(0, 5);
			}
			Long.parseLong(text);
			ObjectNode response = getInfo(text);
			List<TelegramRequest> list = getResponseMessageTelegram(response, chatId, text, userId);
			for (TelegramRequest message : list) {
				try {
					client.sendRequest(message);
				} catch (Exception e) {
					log.error("Error sending request to " + chatId, e);
				}
			}
		} catch (NumberFormatException e) {
			client.sendMessage(chatId,
					"Il codice inserito non è corretto. Inserisci il codice che vedi sulla palina della fermata, ad esempio 11871.");
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
				client.sendMessage(chatId,
						"Il codice inserito non è corretto. Inserisci il codice che vedi sulla palina della fermata, ad esempio 11871.");
			} else {
				log.error("Errore su codice: " + text, e);
				client.sendMessage(chatId,
						"Errore nell'elaborazione del codice. Inserisci il codice che vedi sulla palina della fermata, ad esempio 11871.");
			}
		} catch (RestClientException e) {
			log.error("Errore su codice: " + text, e);
			client.sendMessage(chatId,
					"Errore nell'elaborazione del codice. Inserisci il codice che vedi sulla palina della fermata, ad esempio 11871.");
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

	public ObjectNode log(int offset, int limit) {
		Query query = new Query().with(new Sort(Direction.DESC, "timestamp")).skip(offset).limit(limit);
		List<BusMilanoLogEntry> entries = mongoTemplate.find(query, BusMilanoLogEntry.class);
		ObjectNode response = JsonNodeFactory.instance.objectNode();
		response.put("count", entries.size());
		ArrayNode array = response.putArray("items");
		for (BusMilanoLogEntry entry : entries) {
			array.addObject().put("timestamp", formatter.format(entry.getTimestamp())).put("userId", entry.getUserId())
					.put("stopId", entry.getStopId());
		}
		return response;
	}

	//

	private ObjectNode getInfo(String code) {
		UriComponentsBuilder builder = UriComponentsBuilder
				.fromHttpUrl("http://giromilano.atm.it/TPPortalBackEnd/tpl/stops/").path(code).path("/linesummary");
		return restTemplate.getForObject(builder.build().encode().toUri(), ObjectNode.class);
	}

	// private List<String> getResponseMessage(ObjectNode json, String stopId,
	// String id) {
	// List<String> result = new ArrayList<>(json.get("Lines").size() + 1);
	// result.add("*Fermata " + json.get("StopPoint").get("Description") + "*");
	// for (JsonNode line : json.get("Lines")) {
	// String lineCode = line.get("Line").get("LineCode").asText();
	// String lineDescription =
	// line.get("Line").get("LineDescription").asText();
	// String waitMessage = line.get("WaitMessage").isNull() ? "-" :
	// line.get("WaitMessage").asText();
	// String bookletUrl = line.get("BookletUrl").asText();
	// String message = "Linea " + lineCode + " " + lineDescription + "\nAttesa:
	// " + waitMessage + " ([orari]("
	// + bookletUrl + "))";
	// result.add(message);
	// }
	// if
	// (!mongoTemplate.exists(Query.query(Criteria.where("id").is(id).and("stops.id").is(stopId)),
	// BusMilanoFavorites.class)) {
	// result.add("Aggiungi fermata ai preferiti /fav" + stopId);
	// } else {
	// result.add("Rimuovi fermata dai preferiti /unfav" + stopId);
	// }
	// stopRequested(stopId);
	// return result;
	// }

	private List<TelegramRequest> getResponseMessageTelegram(ObjectNode json, String chatId, String stopId,
			String userId) {
		List<TelegramRequest> result = new ArrayList<>(json.get("Lines").size());
		result.add(TelegramRequest.sendMessage(chatId)
				.text("*Fermata " + json.get("StopPoint").get("Description") + "*").parseModeMarkdown());
		for (JsonNode line : json.get("Lines")) {
			String lineCode = line.get("Line").get("LineCode").asText();
			String lineDescription = line.get("Line").get("LineDescription").asText();
			String waitMessage = line.get("WaitMessage").isNull() ? "-" : line.get("WaitMessage").asText();
			String bookletUrl = line.get("BookletUrl").asText();
			String message = "Linea " + lineCode + " " + lineDescription + "\nAttesa: " + waitMessage + " ([orari]("
					+ bookletUrl + "))";
			result.add(TelegramRequest.sendMessage(chatId).text(message).parseModeMarkdown().disableWebPagePreview());
		}
		TelegramRequest first = result.get(0);
		if (!mongoTemplate.exists(Query.query(Criteria.where("id").is(userId).and("stops.id").is(stopId)),
				BusMilanoFavorites.class)) {
			first.keyboard(Keyboard.replyKeyboard()
					.button(PLUS_SIGN + stopId + " " + json.get("StopPoint").get("Description") + " ai preferiti").row()
					.button("/preferite").resize());
		} else {
			first.keyboard(Keyboard.replyKeyboard()
					.button(MINUS_SIGN + stopId + " " + json.get("StopPoint").get("Description") + " dai preferiti")
					.row().button("/preferite").resize());
		}
		stopRequested(userId, stopId);
		return result;
	}

	private void stopRequested(String userId, String stopId) {
		BusMilanoLogEntry entry = new BusMilanoLogEntry();
		entry.setStopId(stopId);
		entry.setUserId(userId);
		entry.setTimestamp(new Date());
		mongoTemplate.save(entry);
		log.info("STOP REQUESTED " + stopId);
	}

	private ObjectNode getImageMessage(String imageUrl) {
		ObjectNode response = JsonNodeFactory.instance.objectNode();
		ObjectNode message = response.putObject("message");
		ObjectNode attachment = message.putObject("attachment");
		attachment.put("type", "image");
		ObjectNode payload = attachment.putObject("payload");
		payload.put("url", imageUrl);
		return response;
	}

	private ObjectNode getResponseButtonsMessenger(ObjectNode json, String stopId, String id) {
		ObjectNode response = JsonNodeFactory.instance.objectNode();
		ObjectNode message = response.putObject("message");
		ObjectNode attachment = message.putObject("attachment");
		attachment.put("type", "template");
		ObjectNode payload = attachment.putObject("payload");
		payload.put("template_type", "button");
		payload.put("text", "Fermata " + json.get("StopPoint").get("Description"));
		ArrayNode buttons = payload.putArray("buttons");
		ObjectNode button1 = buttons.addObject();
		button1.put("type", "postback");
		if (!mongoTemplate.exists(Query.query(Criteria.where("id").is(id).and("stops.id").is(stopId)),
				BusMilanoFavorites.class)) {
			button1.put("title", "aggiungi preferita");
			button1.put("payload", "fav" + stopId);
		} else {
			button1.put("title", "rimuovi preferita");
			button1.put("payload", "unfav" + stopId);
		}
		ObjectNode button2 = buttons.addObject();
		button2.put("type", "postback");
		button2.put("title", "lista preferite");
		button2.put("payload", "favourites");
		return response;
	}

	private ObjectNode getResponseMessageMessenger(ObjectNode json, String stopId, String userId) {
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
			ArrayNode buttons = element.putArray("buttons");
			ObjectNode button1 = buttons.addObject();
			button1.put("type", "web_url");
			button1.put("title", "orari");
			button1.put("url", line.get("BookletUrl").asText());
		}
		stopRequested(userId, stopId);
		return response;
	}
}
