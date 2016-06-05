package com.charge71.telegram.bots;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.charge71.framework.ApiClient;
import com.charge71.framework.PlatformApiAware;
import com.charge71.model.RssUser;
import com.charge71.services.RssService;
import com.charge71.services.RssService.RssHandler;
import com.charge71.telegramapi.annotations.BotCommand;
import com.charge71.telegramapi.annotations.TelegramBot;
import com.fasterxml.jackson.databind.node.ObjectNode;

@TelegramBot("236804872:AAHa_Z0fdO_9CedIsBqfwEabwPJK5Lq1bow")
public class RssUpdateBot extends PlatformApiAware implements RssHandler {

	private static Logger log = Logger.getLogger(RssUpdateBot.class);

	@Autowired
	private RssService rssService;

	@Autowired
	private MongoTemplate mongoTemplate;

	@Override
	public void setClient(ApiClient client) {
		super.setClient(client);
		rssService.start(this);
	}

	@Override
	public void handle(String link) {
		// TODO Auto-generated method stub

	}

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
		RssUser user = new RssUser();
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
		client.sendMessage(chatId, messages.getMessage(user.getLang(), "start", user.getFirstName()));
	}

	@BotCommand("/add")
	public void add(ObjectNode json, String command) {
		log.debug("/add start");
		String chatId = json.get("message").get("chat").get("id").asText();
		String userId = json.get("message").get("from").get("id").asText();
		RssUser user = mongoTemplate.findById(userId, RssUser.class);
		client.sendForceReply(chatId, messages.getMessage(user.getLang(), "add", user.getFirstName()));
	}
}
