package com.charge71.telegram.bots;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.charge71.framework.ApiClient;
import com.charge71.framework.PlatformApiAware;
import com.charge71.model.RssSubscriptions;
import com.charge71.model.RssSubscriptions.RssFeed;
import com.charge71.model.RssUser;
import com.charge71.services.RssService;
import com.charge71.services.RssService.RssHandler;
import com.charge71.telegramapi.annotations.BotCommand;
import com.charge71.telegramapi.annotations.TelegramBot;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
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
	public void handle(String chatId, String title, String link) {
		client.sendMarkdownMessage(chatId, "[" + title + "](" + link + ")", false);
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
		client.sendForceReply(chatId, messages.getMessage(user.getLang(), "add"));
	}

	@BotCommand("/remove")
	public void remove(ObjectNode json, String command) {
		log.debug("/remove start");
		String chatId = json.get("message").get("chat").get("id").asText();
		String userId = json.get("message").get("from").get("id").asText();
		RssUser user = mongoTemplate.findById(userId, RssUser.class);
		RssSubscriptions subs = mongoTemplate.findById(userId, RssSubscriptions.class);
		if (subs != null && subs.getFeeds() != null && subs.getFeeds().length > 0) {
			RssFeed[] feeds = subs.getFeeds();
			ObjectNode buttons = JsonNodeFactory.instance.objectNode();
			ArrayNode b1 = buttons.putArray("inline_keyboard");
			ArrayNode b2 = b1.addArray();
			for (int i = 0; i < feeds.length; i++) {
				ObjectNode btn = b2.addObject();
				btn.put("text", feeds[i].getName());
				btn.put("callback_data", "remove_" + i);
			}
			client.sendButtons(chatId, messages.getMessage(user.getLang(), "remove"), buttons.toString());
		}
	}

	@BotCommand("default")
	public void def(ObjectNode json, String command) {
		log.debug("default start");
		String chatId = json.get("message").get("chat").get("id").asText();
		String userId = json.get("message").get("from").get("id").asText();
		RssUser user = mongoTemplate.findById(userId, RssUser.class);
		if (json.get("message").get("reply_to_message") != null) {

			if (json.get("message").get("reply_to_message").get("text").asText()
					.equals(messages.getMessage(user.getLang(), "add"))) {

				// add
				String url = json.get("message").get("text").asText();
				try {
					RssFeed feed = RssService.initRss(url);
					RssSubscriptions subs = mongoTemplate.findById(userId, RssSubscriptions.class);
					if (subs != null) {
						mongoTemplate.updateFirst(Query.query(Criteria.where("id").is(userId)),
								new Update().push("feeds", feed), RssSubscriptions.class);
					} else {
						subs = new RssSubscriptions();
						subs.setId(userId);
						subs.setFeeds(new RssFeed[] { feed });
						mongoTemplate.save(subs);
					}
					log.debug("Added feed " + feed.getName());
					client.sendMessage(chatId, messages.getMessage(user.getLang(), "rssadd", feed.getName()));
				} catch (Exception e) {
					log.error("RSS init error.", e);
					client.sendMessage(chatId, messages.getMessage(user.getLang(), "rsserror"));
				}
			}
		}
	}
}
