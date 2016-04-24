package com.charge71.bots;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.charge71.model.MeetLocation;
import com.charge71.model.MeetUser;
import com.charge71.telegramapi.TelegramApiAware;
import com.charge71.telegramapi.annotations.BotCommand;
import com.charge71.telegramapi.annotations.TelegramBot;
import com.fasterxml.jackson.databind.node.ObjectNode;

@TelegramBot("204887014:AAFpLXo_Sh-cLRl_XOfJf_KFOVTuxK4H-s0")
public class MeetAroundBot extends TelegramApiAware {

	private static Logger log = Logger.getLogger(MeetAroundBot.class);

	@Autowired
	private MongoTemplate mongoTemplate;

	@BotCommand("/start")
	public void start(ObjectNode json) {
		log.debug("/start start");
		String id = json.get("message").get("from").get("id").asText();
		String chatId = json.get("message").get("chat").get("id").asText();
		MeetUser user = mongoTemplate.findById(id, MeetUser.class);
		String name = json.get("message").get("from").get("first_name").asText();
		boolean newUser = true;
		if (user == null) {
			user = new MeetUser();
			user.setId(id);
			log.debug("/start new user " + id);
		} else {
			newUser = false;
			log.debug("/start old user " + id);
		}
		boolean ok = json.get("message").get("from").get("username") != null;
		String message = "Welcome " + (newUser ? "" : "back ") + name + "! ";
		if (ok) {
			message += "To start click /meet to check in or /help for the list of commands.";
			String last = json.get("message").get("from").get("last_name").asText();
			user.setFirstName(name);
			user.setLastName(last);
			user.setChatId(chatId);
			user.setUsername(json.get("message").get("from").get("username").asText());
			mongoTemplate.save(user);
		} else {
			message += "Please note that to use this bot you need to set a username in the Telegram settings. When done click /start.";
		}
		client.sendMessage(chatId, message);
		// ObjectNode photoJson = client.getUserProfilePhoto(id);
		// if (photoJson.get("result").get("total_count").asInt() > 0) {
		// String photoId =
		// photoJson.get("result").get("photos").get(0).get(0).get("file_id").asText();
		// user.setPhotoId(photoId);
		// } else {
		// // TODO
		// }
		log.debug("/start end");
	}

	@BotCommand("/help")
	public void help(ObjectNode json) {
		log.debug("/help start");
		String chatId = json.get("message").get("chat").get("id").asText();
		client.sendMessage(chatId,
				"Available commands:\n/meet to check in and meet people around.\n/stop to exit this bot.\n/help to show this help.");
		log.debug("/help end");
	}

	@BotCommand("/meet")
	public void meet(ObjectNode json) {
		log.debug("/meet start");
		String chatId = json.get("message").get("chat").get("id").asText();
		if (json.get("message").get("from").get("username") == null) {
			client.sendMessage(chatId,
					"Please note that to use this bot you need to set a username in the Telegram settings. When done click /start.");
			return;
		}
		client.sendLocationRequest(chatId, "Please check in to meet people around you.");
		log.debug("/meet end");
	}

	@BotCommand("location")
	public void location(ObjectNode json) {
		log.debug("location start");
		String chatId = json.get("message").get("chat").get("id").asText();
		if (json.get("message").get("from").get("username") == null) {
			client.sendMessage(chatId,
					"Please note that to use this bot you need to set a username in the Telegram settings. When done click /start.");
			return;
		}
		String id = json.get("message").get("from").get("id").asText();
		// String chatId = json.get("message").get("chat").get("id").asText();
		double latitude = json.get("message").get("location").get("latitude").asDouble();
		double longitude = json.get("message").get("location").get("longitude").asDouble();
		Date date = new Date(Long.parseLong(json.get("message").get("date").asText() + "000"));
		GeoJsonPoint point = new GeoJsonPoint(longitude, latitude);
		MeetLocation location = new MeetLocation();
		location.setId(id);
		location.setCreated(date);
		location.setLocation(new GeoJsonPoint(longitude, latitude));
		mongoTemplate.save(location);
		Criteria criteria = Criteria.where("location").nearSphere(point).maxDistance(100).and("id").ne(id);
		List<MeetLocation> list = mongoTemplate.find(Query.query(criteria), MeetLocation.class);
		if (list.isEmpty()) {
			client.sendMessage(chatId,
					"It seems no one checked in nearby lately. Why don't you share this bot to increase the chance to meet people?");
		} else {
			List<String> ids = new ArrayList<>(list.size());
			for (MeetLocation meet : list) {
				ids.add(meet.getId());
			}
			criteria = Criteria.where("id").in(ids);
			List<MeetUser> users = mongoTemplate.find(Query.query(criteria), MeetUser.class);
			client.sendMessage(chatId, "These users checked in nearby lately:");
			for (MeetUser user : users) {
				ObjectNode photoJson = client.getUserProfilePhoto(user.getId());
				if (photoJson.get("result").get("total_count").asInt() > 0) {
					String photoId = photoJson.get("result").get("photos").get(0).get(0).get("file_id").asText();
					String caption = user.getFirstName();
					client.sendPhoto(chatId, photoId, caption + "\n/connect " + user.getId());
				} else {
					client.sendMessage(chatId, user.getFirstName() + "\n/connect " + user.getId());
				}
			}
		}
		log.debug("location end");
	}
}
