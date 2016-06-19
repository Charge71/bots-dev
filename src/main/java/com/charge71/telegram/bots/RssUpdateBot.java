package com.charge71.telegram.bots;

import org.apache.commons.lang3.ArrayUtils;
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
import com.fasterxml.jackson.databind.JsonNode;
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
	public void handle(RssUser user, String feedTitle, String title, String link) {
		log.debug("Handle " + link);
		ObjectNode node = client.sendMarkdownMessage(user.getChatId(), feedTitle + "\n[" + title + "](" + link + ")",
				false);
		if (node == null) { // 403
			mongoTemplate.remove(Query.query(Criteria.where("id").is(user.getId())), RssSubscriptions.class);
			log.info("Removed subscriptions for " + user.getId());
		}
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
		if (mongoTemplate.exists(Query.query(Criteria.where("id").is(userId).and("feeds.3").exists(true)),
				RssSubscriptions.class)) {
			client.sendMessage(chatId, messages.getMessage(user.getLang(), "maxfav"));
		} else {
			client.sendForceReply(chatId, messages.getMessage(user.getLang(), "add"));
		}
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
			for (int i = 0; i < feeds.length; i++) {
				ObjectNode btn = b1.addArray().addObject();
				btn.put("text", feeds[i].getName());
				btn.put("callback_data", "remove_" + i);
			}
			client.sendButtons(chatId, messages.getMessage(user.getLang(), "remove"), buttons.toString());
		} else {
			client.sendMessage(chatId, messages.getMessage(user.getLang(), "nofav", user.getFirstName()));
		}
	}

	@BotCommand("/help")
	public void help(ObjectNode json, String command) {
		log.debug("/help start");
		String chatId = json.get("message").get("chat").get("id").asText();
		String userId = json.get("message").get("from").get("id").asText();
		RssUser user = mongoTemplate.findById(userId, RssUser.class);
		if (user != null) {
			client.sendMessage(chatId, messages.getMessage(user.getLang(), "help"));
		} else {
			client.sendMessage(chatId, messages.getMessage("en", "restart"));
		}
	}

	@BotCommand("/stop")
	public void stop(ObjectNode json, String command) {
		log.debug("/stop start");
		String id = json.get("message").get("from").get("id").asText();
		RssSubscriptions subs = mongoTemplate.findById(id, RssSubscriptions.class);
		if (subs != null) {
			mongoTemplate.remove(subs);
		}
		RssUser user = mongoTemplate.findById(id, RssUser.class);
		if (user != null) {
			mongoTemplate.remove(user);
		}
	}
	
	@BotCommand("/test")
	public void test(ObjectNode json, String command) {
		String chatId = json.get("message").get("chat").get("id").asText();
		String userId = json.get("message").get("from").get("id").asText();
		RssUser user = mongoTemplate.findById(userId, RssUser.class);
		RssSubscriptions subs = mongoTemplate.findById(userId, RssSubscriptions.class);
		if (subs != null && subs.getFeeds() != null && subs.getFeeds().length > 0) {
			RssFeed[] feeds = subs.getFeeds();
			ObjectNode buttons = JsonNodeFactory.instance.objectNode();
			ArrayNode b1 = buttons.putArray("keyboard");
			for (int i = 0; i < feeds.length; i++) {
				ObjectNode btn = b1.addArray().addObject();
				btn.put("text", i + " " + feeds[i].getName());
			}
			buttons.put("resize_keyboard", true);
			client.sendButtons(chatId, messages.getMessage(user.getLang(), "remove"), buttons.toString());
		} else {
			client.sendMessage(chatId, messages.getMessage(user.getLang(), "nofav", user.getFirstName()));
		}
	}

	@BotCommand("callback")
	public void callback(ObjectNode json, String command) {
		log.debug("callback start");
		String chatId = json.get("callback_query").get("message").get("chat").get("id").asText();
		String fromId = json.get("callback_query").get("from").get("id").asText();
		RssUser user = mongoTemplate.findById(fromId, RssUser.class);
		String data = json.get("callback_query").get("data").asText();
		if (data.startsWith("remove_")) {
			int index = Integer.parseInt(data.substring(7));
			RssSubscriptions subs = mongoTemplate.findById(fromId, RssSubscriptions.class);
			RssFeed[] feeds = subs.getFeeds();
			RssFeed feed = subs.getFeeds()[index];
			subs.setFeeds(ArrayUtils.remove(feeds, index));
			mongoTemplate.save(subs);
			client.sendMessage(chatId, messages.getMessage(user.getLang(), "rssremove", feed.getName()));
			log.debug("Removed " + feed.getUrl() + " from " + fromId);
		}
	}

	@BotCommand("default")
	public void def(ObjectNode json, String command) {
		log.debug("default start");
		String userId = json.get("message").get("from").get("id").asText();
		RssUser user = mongoTemplate.findById(userId, RssUser.class);
		if (json.get("message").get("reply_to_message") != null) {

			if (json.get("message").get("reply_to_message").get("text").asText()
					.equals(messages.getMessage(user.getLang(), "add"))) {
				String url = json.get("message").get("text").asText();
				add(url, user);
			}
		} else if (json.get("message").get("entities") != null) {
			for (JsonNode entity : json.get("message").get("entities")) {
				if (entity.get("type").asText().equals("url")) {
					int offset = entity.get("offset").asInt();
					int length = entity.get("length").asInt();
					String url = json.get("message").get("text").asText().substring(offset, length + offset);
					add(url, user);
					return;
				}
			}
		}
	}

	//

	private void add(String url, RssUser user) {

		// add
		if (mongoTemplate.exists(Query.query(Criteria.where("id").is(user.getId()).and("feeds.url").is(url)),
				RssSubscriptions.class)) {
			client.sendMessage(user.getChatId(), messages.getMessage(user.getLang(), "already", url));
			return;
		}
		try {
			RssFeed feed = RssService.initRss(url);
			RssSubscriptions subs = mongoTemplate.findById(user.getId(), RssSubscriptions.class);
			if (subs != null) {
				mongoTemplate.updateFirst(Query.query(Criteria.where("id").is(user.getId())),
						new Update().push("feeds", feed), RssSubscriptions.class);
			} else {
				subs = new RssSubscriptions();
				subs.setId(user.getId());
				subs.setFeeds(new RssFeed[] { feed });
				mongoTemplate.save(subs);
			}
			log.debug("Added feed " + feed.getName() + " to " + user.getId());
			client.sendMessage(user.getChatId(), messages.getMessage(user.getLang(), "rssadd", feed.getName()));
		} catch (Exception e) {
			log.error("RSS init error.", e);
			client.sendMessage(user.getChatId(), messages.getMessage(user.getLang(), "rsserror"));
		}

	}
}
