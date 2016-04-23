package com.charge71.bots;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

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
		if (user == null) {
			client.sendMessage(chatId, "Welcome " + name + "!");
			user = new MeetUser();
			user.setId(id);
			log.debug("/start new user " + id);
		} else {
			client.sendMessage(chatId, "Welcome back " + name + "!");
			log.debug("/start old user " + id);
		}
		String last = json.get("message").get("from").get("last_name").asText();
		ObjectNode photoJson = client.getUserProfilePhoto(id);
		if (photoJson.get("result").get("total_count").asInt() > 0) {
			String photoId = photoJson.get("result").get("photos").get(0).get(0).get("file_id").asText();
			user.setPhotoId(photoId);
		} else {
			//TODO
		}
		user.setFirstName(name);
		user.setLastName(last);
		user.setChatId(chatId);
		mongoTemplate.save(user);
		log.debug("/start end");
	}

}
