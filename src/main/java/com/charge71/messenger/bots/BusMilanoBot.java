package com.charge71.messenger.bots;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.charge71.framework.PlatformApiAware;
import com.charge71.messengerapi.annotations.BotMessage;
import com.charge71.messengerapi.annotations.BotPostback;
import com.charge71.messengerapi.annotations.MessengerBot;
import com.charge71.services.BusMilanoBotService;
import com.fasterxml.jackson.databind.node.ObjectNode;

@MessengerBot(name = "busmilanobot", token = "EAADKP2ZBkov0BAAV8pkwkJmzaiO0j1WRMUEYEZA89wO4ZCY8WRrz2U2knVZC4BWJAZCrNBkgEfYmZCUxP8zgZC0iGUq9hdniZBaHubBFJkOZAOe4kGBw1OY0HxO1TkZBZBsxFoigAOz0MgX8Le29fIeYkk64oKFSuFfFo3lluYYmY3wKwZDZD")
public class BusMilanoBot extends PlatformApiAware {

	private static Logger log = Logger.getLogger(BusMilanoBot.class);

	@Autowired
	private BusMilanoBotService service;

	@BotMessage
	public void message(ObjectNode json) {
		log.debug("message start");
		String text = json.get("entry").get(0).get("messaging").get(0).get("message").get("text").asText();
		String chatId = json.get("entry").get(0).get("messaging").get(0).get("sender").get("id").asText();
		String userId = "M" + chatId;
		service.createUser(client, userId, chatId);
		service.sendStopInfoMessenger(client, chatId, text, userId);
	}

	@BotPostback(value = "stop", isPrefix = true)
	public void stop(ObjectNode json, String postback) {
		log.debug("message start");
		String text = postback.substring(4);
		String chatId = json.get("entry").get(0).get("messaging").get(0).get("sender").get("id").asText();
		String userId = "M" + chatId;
		service.createUser(client, userId, chatId);
		service.sendStopInfoMessenger(client, chatId, text, userId);
	}
	
	@BotPostback(value = "fav", isPrefix = true)
	public void fav(ObjectNode json, String postback) {
		log.debug("fav start");
		String chatId = json.get("entry").get(0).get("messaging").get(0).get("sender").get("id").asText();
		String userId = "M" + chatId;
		String stopId = postback.substring(3);
		service.addFavorite(client, stopId, chatId, userId);
	}

	@BotPostback(value = "unfav", isPrefix = true)
	public void unfav(ObjectNode json, String postback) {
		log.debug("unfav start");
		String chatId = json.get("entry").get(0).get("messaging").get(0).get("sender").get("id").asText();
		String userId = "M" + chatId;
		String stopId = postback.substring(5);
		service.removeFavorite(client, stopId, chatId, userId);
	}

	@BotPostback("favourites")
	public void favourites(ObjectNode json, String postback) {
		log.debug("favourites start");
		String chatId = json.get("entry").get(0).get("messaging").get(0).get("sender").get("id").asText();
		String userId = "M" + chatId;
		service.listFavoritesMessenger(client, chatId, userId);
	}
}
