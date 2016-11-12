package com.charge71.messenger.bots;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.charge71.framework.AdsProvider;
import com.charge71.framework.PlatformApiAware;
import com.charge71.messengerapi.MessengerRequest;
import com.charge71.messengerapi.annotations.BotMessage;
import com.charge71.messengerapi.annotations.BotPostback;
import com.charge71.messengerapi.annotations.BotStartup;
import com.charge71.messengerapi.annotations.MessengerBot;
import com.charge71.services.BusMilanoBotService;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

@MessengerBot(name = "busmilanobot", token = "EAADKP2ZBkov0BAAV8pkwkJmzaiO0j1WRMUEYEZA89wO4ZCY8WRrz2U2knVZC4BWJAZCrNBkgEfYmZCUxP8zgZC0iGUq9hdniZBaHubBFJkOZAOe4kGBw1OY0HxO1TkZBZBsxFoigAOz0MgX8Le29fIeYkk64oKFSuFfFo3lluYYmY3wKwZDZD")
public class BusMilanoBot extends PlatformApiAware<MessengerRequest, ObjectNode> implements AdsProvider {

	private static Logger log = Logger.getLogger(BusMilanoBot.class);

	@Autowired
	private BusMilanoBotService service;

	private String adsBaseUrl;

	@BotMessage
	public void message(ObjectNode json) {
		log.debug("message start");
		String chatId = json.get("entry").get(0).get("messaging").get(0).get("sender").get("id").asText();
		String userId = "M" + chatId;
		service.createUser(client, userId, chatId);
		if (json.get("entry").get(0).get("messaging").get(0).get("message").get("text") != null) {
			String text = json.get("entry").get(0).get("messaging").get(0).get("message").get("text").asText();
			service.sendStopInfoMessenger(client, chatId, text, userId, adsBaseUrl);
		} else {
			service.sendInfoMessenger(client, chatId);
		}
	}

	@BotPostback(value = "stop", isPrefix = true)
	public void stop(ObjectNode json, String postback) {
		log.debug("message start");
		String text = postback.substring(4);
		String chatId = json.get("entry").get(0).get("messaging").get(0).get("sender").get("id").asText();
		String userId = "M" + chatId;
		service.createUser(client, userId, chatId);
		service.sendStopInfoMessenger(client, chatId, text, userId, adsBaseUrl);
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

	@BotStartup
	public void startup() {
		log.debug("startup start");
		ObjectNode node = JsonNodeFactory.instance.objectNode();
		node.put("setting_type", "call_to_actions");
		node.put("thread_state", "existing_thread");
		ArrayNode array = node.putArray("call_to_actions");
		ObjectNode button = array.addObject();
		button.put("type", "postback");
		button.put("title", "lista preferite");
		button.put("payload", "favourites");
		client.sendSettings(node);
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response) {
		try {
			String stopId = request.getParameter("stopId");
			String url = service.getAdUrl(stopId);
			if (url != null) {
				response.sendRedirect(url);
				log.info("Handled AD " + stopId + " to " + url);
			}
		} catch (IOException e) {
			log.error("Ads error", e);
		}
	}

	@Override
	public void setAdsBaseUrl(String adsBaseUrl) {
		this.adsBaseUrl = adsBaseUrl;
	}
}
