package com.charge71.telegram.bots;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;

import com.charge71.framework.ApiClient;
import com.charge71.framework.PlatformApiAware;
import com.charge71.model.RssSubscriptions;
import com.charge71.model.RssSubscriptions.RssFeed;
import com.charge71.model.RssUser;
import com.charge71.services.RssService;
import com.charge71.services.RssService.RssHandler;
import com.charge71.telegramapi.TelegramRequest;
import com.charge71.telegramapi.TelegramRequest.Keyboard;
import com.charge71.telegramapi.annotations.BotCommand;
import com.charge71.telegramapi.annotations.TelegramBot;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

@TelegramBot("236804872:AAHa_Z0fdO_9CedIsBqfwEabwPJK5Lq1bow")
public class RssUpdateBot extends PlatformApiAware<TelegramRequest, ObjectNode> implements RssHandler {

	private static Logger log = Logger.getLogger(RssUpdateBot.class);

	private static String ENG_FLAG = new String(Character.toChars(127468)) + new String(Character.toChars(127463));
	private static String ITA_FLAG = new String(Character.toChars(127470)) + new String(Character.toChars(127481));

	private static Keyboard LANG_KEYBOARD = Keyboard.replyKeyboard().resize().button(ENG_FLAG + " English").row()
			.button(ITA_FLAG + " Italiano").row();

	@Autowired
	private RssService rssService;

	@Autowired
	private MongoTemplate mongoTemplate;

	@Override
	public void setClient(ApiClient<TelegramRequest, ObjectNode> client) {
		super.setClient(client);
		rssService.start(this);
	}

	@Override
	public void handle(RssUser user, String feedTitle, String title, String link) {
		log.debug("Handle " + link);
		ObjectNode node = client.sendMarkdownMessage(user.getChatId(), feedTitle + "\n[" + title + "](" + link + ")",
				false);
		if (node.get("errorCode") != null && node.get("errorCode").asInt() == HttpStatus.FORBIDDEN.value()) { // 403
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
		RssUser user = mongoTemplate.findById(userId, RssUser.class);
		if (user == null) {
			user = new RssUser();
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
			TelegramRequest tr = TelegramRequest.sendMessage(chatId)
					.text(messages.getMessage(user.getLang(), "start", user.getFirstName())).keyboard(LANG_KEYBOARD);
			// client.sendMessage(chatId, messages.getMessage(user.getLang(),
			// "start", user.getFirstName()));
			try {
				client.sendRequest(tr);
			} catch (Exception e) {
				log.error("Request error", e);
			}
		}
		if (!"/start".equals(json.get("message").get("text").asText())) {
			String url;
			try {
				url = URLDecoder.decode(json.get("message").get("text").asText().substring(7), "UTF-8");
				add(url, user);
			} catch (UnsupportedEncodingException e) {
				log.error("Encoding error", e);
			}
		}
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

	@BotCommand("/help")
	public void help(ObjectNode json, String command) {
		log.debug("/help start");
		String chatId = json.get("message").get("chat").get("id").asText();
		String userId = json.get("message").get("from").get("id").asText();
		RssUser user = mongoTemplate.findById(userId, RssUser.class);
		if (user != null) {
			TelegramRequest tr = TelegramRequest.sendMessage(chatId).text(messages.getMessage(user.getLang(), "help"))
					.keyboard(LANG_KEYBOARD);
			// client.sendMessage(chatId, messages.getMessage(user.getLang(),
			// "help"));
			try {
				client.sendRequest(tr);
			} catch (Exception e) {
				log.error("Request error", e);
			}
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

	@BotCommand("/remove")
	public void remove(ObjectNode json, String command) {
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
				btn.put("text", "\u274C " + (i + 1) + " " + feeds[i].getName());
			}
			ObjectNode btn = b1.addArray().addObject();
			btn.put("text", "\u274C 0 Cancel");
			buttons.put("resize_keyboard", true);
			client.sendButtons(chatId, messages.getMessage(user.getLang(), "remove"), buttons.toString());
		} else {
			client.sendMessage(chatId, messages.getMessage(user.getLang(), "nofav", user.getFirstName()));
		}
	}

	@BotCommand("default")
	public void def(ObjectNode json, String command) {
		log.debug("default start");
		if (!json.hasNonNull("message")) {
			log.info("No message");
			return;
		}
		String userId = json.get("message").get("from").get("id").asText();
		String chatId = json.get("message").get("chat").get("id").asText();
		RssUser user = mongoTemplate.findById(userId, RssUser.class);
		if (json.get("message").get("text") != null && json.get("message").get("text").asText().startsWith("\u274C")) {
			int index = Integer.valueOf(json.get("message").get("text").asText().substring(2, 3)) - 1;
			if (index == -1) {
				ObjectNode buttons = JsonNodeFactory.instance.objectNode();
				buttons.put("hide_keyboard", true);
				client.sendButtons(chatId, "\u274C", buttons.toString());
				return;
			}
			RssSubscriptions subs = mongoTemplate.findById(userId, RssSubscriptions.class);
			RssFeed[] feeds = subs.getFeeds();
			RssFeed feed = subs.getFeeds()[index];
			subs.setFeeds(ArrayUtils.remove(feeds, index));
			mongoTemplate.save(subs);
			ObjectNode buttons = JsonNodeFactory.instance.objectNode();
			buttons.put("hide_keyboard", true);
			client.sendButtons(chatId, messages.getMessage(user.getLang(), "rssremove", feed.getName()),
					buttons.toString());
			log.debug("Removed " + feed.getUrl() + " from " + userId);
		} else if (json.get("message").get("text") != null
				&& json.get("message").get("text").asText().startsWith(ENG_FLAG)) {
			mongoTemplate.findAndModify(Query.query(Criteria.where("id").is(userId)), new Update().set("lang", "en"),
					RssUser.class);
			TelegramRequest tr = TelegramRequest.sendMessage(chatId)
					.text(messages.getMessage("en", "help", user.getFirstName())).hideKeyboard();
			try {
				client.sendRequest(tr);
			} catch (Exception e) {
				log.error("Request error", e);
			}
		} else if (json.get("message").get("text") != null
				&& json.get("message").get("text").asText().startsWith(ITA_FLAG)) {
			mongoTemplate.findAndModify(Query.query(Criteria.where("id").is(userId)), new Update().set("lang", "it"),
					RssUser.class);
			TelegramRequest tr = TelegramRequest.sendMessage(chatId)
					.text(messages.getMessage("it", "help", user.getFirstName())).hideKeyboard();
			try {
				client.sendRequest(tr);
			} catch (Exception e) {
				log.error("Request error", e);
			}
		} else if (json.get("message").get("reply_to_message") != null) {
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

	@BotCommand("/broadcast")
	public void broadcast(ObjectNode json, String command) {
		log.debug("broadcast start");
		String id = json.get("message").get("from").get("id").asText();
		String chatId = json.get("message").get("chat").get("id").asText();
		RssUser user = mongoTemplate.findById(id, RssUser.class);
		if (id.equals("148883640")) {
			List<RssSubscriptions> subs = mongoTemplate.findAll(RssSubscriptions.class);
			for (RssSubscriptions sub : subs) {
				if (sub.getFeeds() != null && sub.getFeeds().length > 0) {
					RssUser ruser = mongoTemplate.findById(sub.getId(), RssUser.class);
					TelegramRequest tr = TelegramRequest.sendMessage(ruser.getChatId())
							.text(messages.getMessage(user.getLang(), "broadcast"));
					try {
						client.sendRequest(tr);
					} catch (Exception e) {
						log.error("Error sending message to " + ruser.getChatId(), e);
					}
				}
			}
		} else {
			client.sendMessage(chatId, messages.getMessage(user == null ? "en" : user.getLang(), "unknown"));
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
