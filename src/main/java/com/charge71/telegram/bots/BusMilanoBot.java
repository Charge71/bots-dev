package com.charge71.telegram.bots;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.charge71.framework.AdsProvider;
import com.charge71.framework.PlatformApiAware;
import com.charge71.services.BusMilanoBotService;
import com.charge71.telegramapi.TelegramRequest;
import com.charge71.telegramapi.annotations.BotCommand;
import com.charge71.telegramapi.annotations.TelegramBot;
import com.fasterxml.jackson.databind.node.ObjectNode;

@TelegramBot("204159588:AAF3Y-4eKSheRYFlfOPhZ_Xvn1AcZLDgvqA")
public class BusMilanoBot extends PlatformApiAware<TelegramRequest, ObjectNode> implements AdsProvider {

	private static Logger log = Logger.getLogger(BusMilanoBot.class);

	@Autowired
	private BusMilanoBotService service;

	@BotCommand("/start")
	public void start(ObjectNode json, String command) {
		log.debug("/start start");
		String chatId = json.get("message").get("chat").get("id").asText();
		String userFirstName = json.get("message").get("from").get("first_name").asText();
		String userId = json.get("message").get("from").get("id").asText();
		String userLastName = null;
		String username = null;
		if (json.get("message").get("from").get("last_name") != null) {
			userLastName = json.get("message").get("from").get("last_name").asText();
		}
		if (json.get("message").get("from").get("username") != null) {
			username = json.get("message").get("from").get("username").asText();
		}
		service.startBot(client, chatId, userFirstName, userLastName, username, userId);
	}

	@BotCommand("/help")
	public void help(ObjectNode json, String command) {
		log.debug("/help start");
		String chatId = json.get("message").get("chat").get("id").asText();
		service.help(client, chatId);
	}

	@BotCommand(value = "/fav", isPrefix = true)
	public void fav(ObjectNode json, String command) {
		log.debug("/fav start");
		String userId = json.get("message").get("from").get("id").asText();
		String chatId = json.get("message").get("chat").get("id").asText();
		String stopId = command.substring(4);
		service.addFavorite(client, stopId, chatId, userId);
	}

	@BotCommand(value = "/unfav", isPrefix = true)
	public void unfav(ObjectNode json, String command) {
		log.debug("/unfav start");
		String userId = json.get("message").get("from").get("id").asText();
		String chatId = json.get("message").get("chat").get("id").asText();
		String stopId = command.substring(6);
		service.removeFavorite(client, stopId, chatId, userId);
	}

	@BotCommand("/preferite")
	public void favorites(ObjectNode json, String command) {
		log.debug("/preferite start");
		String userId = json.get("message").get("from").get("id").asText();
		String chatId = json.get("message").get("chat").get("id").asText();
		service.listFavoritesTelegram(client, chatId, userId);
	}

	@BotCommand("default")
	public void def(ObjectNode json, String command) {
		log.debug("default start");
		String userId = json.get("message").get("from").get("id").asText();
		String chatId = json.get("message").get("chat").get("id").asText();
		if (json.get("message").get("text") != null) {
			String stopId = json.get("message").get("text").asText();
			service.sendStopInfoTelegram(client, chatId, stopId, userId);
		} else {
			service.sendInfoTelegram(client, chatId);
		}
	}

	@BotCommand(value = "/ferm", isPrefix = true)
	public void ferm(ObjectNode json, String command) {
		log.debug("/ferm start");
		String userId = json.get("message").get("from").get("id").asText();
		String chatId = json.get("message").get("chat").get("id").asText();
		String stopId = command.substring(5);
		service.sendStopInfoTelegram(client, chatId, stopId, userId);
	}

	@BotCommand("/stop")
	public void stop(ObjectNode json, String command) {
		log.debug("/stop start");
		String userId = json.get("message").get("from").get("id").asText();
		service.stopBot(userId);
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response) {
		try {
			response.sendRedirect("http://www.eidosmedia.com");
		} catch (IOException e) {
			log.error("Ads error", e);
		}
	}

}
