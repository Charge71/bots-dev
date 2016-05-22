package com.charge71.telegram.bots;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.charge71.framework.PlatformApiAware;
import com.charge71.model.MeetLocation;
import com.charge71.model.MeetRequests;
import com.charge71.model.MeetUser;
import com.charge71.telegramapi.annotations.BotCommand;
import com.charge71.telegramapi.annotations.TelegramBot;
import com.fasterxml.jackson.databind.node.ObjectNode;

@TelegramBot("204887014:AAFpLXo_Sh-cLRl_XOfJf_KFOVTuxK4H-s0")
public class MeetAroundBot extends PlatformApiAware {

	private static Logger log = Logger.getLogger(MeetAroundBot.class);

	@Autowired
	private MongoTemplate mongoTemplate;

	@BotCommand("/start")
	public void start(ObjectNode json, String command) {
		log.debug("/start start");
		String id = json.get("message").get("from").get("id").asText();
		String chatId = json.get("message").get("chat").get("id").asText();
		MeetUser user = mongoTemplate.findById(id, MeetUser.class);
		String name = json.get("message").get("from").get("first_name").asText();
		if (user == null) {
			user = new MeetUser();
			user.setId(id);
			log.debug("/start new user " + id);
		} else {
			log.debug("/start old user " + id);
		}
		boolean ok = json.get("message").get("from").get("username") != null;
		String message = messages.getMessage(user.getLang(), "welcome", name);
		if (ok) {
			message += " " + messages.getMessage(user.getLang(), "intro");
			if (json.get("message").get("from").get("last_name") != null) {
				String last = json.get("message").get("from").get("last_name").asText();
				user.setLastName(last);
			}
			user.setFirstName(name);
			user.setChatId(chatId);
			user.setUsername(json.get("message").get("from").get("username").asText());
			mongoTemplate.save(user);
			client.sendLocationRequest(chatId, message, messages.getMessage(user.getLang(), "sendloc"));
		} else {
			message += " " + messages.getMessage(user.getLang(), "nousername");
			client.sendMessage(chatId, message);
		}
	}

	@BotCommand("/settings")
	public void settings(ObjectNode json, String command) {
		log.debug("/settings start");
		String chatId = json.get("message").get("chat").get("id").asText();
		client.sendLanguageButtons(chatId, "Select your language/Seleziona lingua");
	}

	@BotCommand("/help")
	public void help(ObjectNode json, String command) {
		log.debug("/help start");
		String id = json.get("message").get("from").get("id").asText();
		String chatId = json.get("message").get("chat").get("id").asText();
		MeetUser user = mongoTemplate.findById(id, MeetUser.class);
		client.sendMessage(chatId, messages.getMessage(user == null ? "en" : user.getLang(), "help"));
	}

	@BotCommand("/stop")
	public void stop(ObjectNode json, String command) {
		log.debug("/stop start");
		String id = json.get("message").get("from").get("id").asText();
		MeetLocation location = mongoTemplate.findById(id, MeetLocation.class);
		if (location != null) {
			mongoTemplate.remove(location);
		}
		MeetUser user = mongoTemplate.findById(id, MeetUser.class);
		if (user != null) {
			mongoTemplate.remove(user);
		}
	}

	@BotCommand(value = "/connect", isPrefix = true)
	public void connect(ObjectNode json, String command) {
		log.debug("/connect start");
		String chatId = json.get("message").get("chat").get("id").asText();
		String id = json.get("message").get("from").get("id").asText();
		MeetUser myself = mongoTemplate.findById(id, MeetUser.class);
		if (json.get("message").get("from").get("username") == null) {
			client.sendMessage(chatId, messages.getMessage(myself.getLang(), "nousername"));
			return;
		}
		String connectToId = command.substring(8);
		MeetUser connectToUser = mongoTemplate.findById(connectToId, MeetUser.class);
		if (connectToUser == null) {
			client.sendMessage(chatId, messages.getMessage(myself.getLang(), "nouser"));
		} else {
			boolean requested = false;
			MeetRequests meetRequests;
			if (mongoTemplate.exists(Query.query(Criteria.where("id").is(id)), MeetRequests.class)) {
				if (mongoTemplate.exists(Query.query(Criteria.where("id").is(id).and("requests").is(connectToId)),
						MeetRequests.class)) {
					requested = true;
				} else {
					mongoTemplate.updateFirst(Query.query(Criteria.where("id").is(id)),
							new Update().push("requests", connectToId), MeetRequests.class);
				}
			} else {
				meetRequests = new MeetRequests();
				meetRequests.setId(id);
				meetRequests.setRequests(new String[] { connectToId });
				mongoTemplate.save(meetRequests);
			}
			if (!requested) {
				client.sendMessage(connectToUser.getChatId(),
						messages.getMessage(connectToUser.getLang(), "wishes", "@" + myself.getUsername()));
				client.sendMessage(chatId,
						messages.getMessage(myself.getLang(), "request", connectToUser.getFirstName()));
			} else {
				client.sendMessage(chatId,
						messages.getMessage(myself.getLang(), "requested", connectToUser.getFirstName()));
			}
		}
	}

	@BotCommand("location")
	public void location(ObjectNode json, String command) {
		log.debug("location start");
		String id = json.get("message").get("from").get("id").asText();
		MeetUser myself = mongoTemplate.findById(id, MeetUser.class);
		String chatId = json.get("message").get("chat").get("id").asText();
		if (json.get("message").get("from").get("username") == null) {
			client.sendMessage(chatId, messages.getMessage(myself == null ? "en" : myself.getLang(), "nousername"));
			return;
		}
		MeetLocation oldloc = mongoTemplate.findById(id, MeetLocation.class);
		if (oldloc != null) {
			long oldtime = oldloc.getCreated().getTime();
			long nowtime = System.currentTimeMillis();
			if (nowtime < oldtime + 300000) { // 5 mins
				client.sendMessage(chatId, messages.getMessage(myself.getLang(), "wait"));
				return;
			}
		}
		double latitude = json.get("message").get("location").get("latitude").asDouble();
		double longitude = json.get("message").get("location").get("longitude").asDouble();
		Date date = new Date(Long.parseLong(json.get("message").get("date").asText() + "000"));
		GeoJsonPoint point = new GeoJsonPoint(longitude, latitude);
		MeetLocation location = new MeetLocation();
		location.setId(id);
		location.setCreated(date);
		location.setLocation(new GeoJsonPoint(longitude, latitude));
		mongoTemplate.save(location);
		MeetRequests requests = mongoTemplate.findById(id, MeetRequests.class);
		int count = 0;
		if (requests != null) {
			count = requests.getRequests().length;
		}
		Criteria criteria = Criteria.where("location").nearSphere(point).maxDistance(200 + (count * 10)).and("id").ne(id);
		List<MeetLocation> list = mongoTemplate.find(Query.query(criteria), MeetLocation.class);
		if (list.isEmpty()) {
			client.sendMessage(chatId, messages.getMessage(myself.getLang(), "nochecks", String.valueOf(count)));
		} else {
			List<String> ids = new ArrayList<>(list.size());
			for (MeetLocation meet : list) {
				ids.add(meet.getId());
			}
			criteria = Criteria.where("id").in(ids);
			List<MeetUser> users = mongoTemplate.find(Query.query(criteria), MeetUser.class);
			client.sendMessage(chatId, messages.getMessage(myself.getLang(), "nearby"));
			for (MeetUser user : users) {
				sendConnection(chatId, user, myself.getLang());
				client.sendMessage(user.getChatId(), messages.getMessage(user.getLang(), "checked"));
				sendConnection(user.getChatId(), myself, user.getLang());
				log.info("**** HIT " + myself.getId() + " - " + user.getId());
			}
		}
	}

	@BotCommand("callback")
	public void callback(ObjectNode json, String command) {
		log.debug("callback start");
		String chatId = json.get("callback_query").get("message").get("chat").get("id").asText();
		String fromId = json.get("callback_query").get("from").get("id").asText();
		String data = json.get("callback_query").get("data").asText();
		if (data.equals("lang_en")) {
			mongoTemplate.findAndModify(Query.query(Criteria.where("id").is(fromId)), new Update().set("lang", "en"),
					MeetUser.class);
			client.sendLocationRequest(chatId, messages.getMessage("en", "lang"), messages.getMessage("en", "sendloc"));
		} else if (data.equals("lang_it")) {
			mongoTemplate.findAndModify(Query.query(Criteria.where("id").is(fromId)), new Update().set("lang", "it"),
					MeetUser.class);
			client.sendLocationRequest(chatId, messages.getMessage("it", "lang"), messages.getMessage("it", "sendloc"));
		}
	}

	@BotCommand("default")
	public void def(ObjectNode json, String command) {
		log.debug("default start");
		String id = json.get("message").get("from").get("id").asText();
		String chatId = json.get("message").get("chat").get("id").asText();
		MeetUser user = mongoTemplate.findById(id, MeetUser.class);
		client.sendMessage(chatId, messages.getMessage(user == null ? "en" : user.getLang(), "unknown"));
	}

	//

	private void sendConnection(String chatId, MeetUser user, String lang) {
		ObjectNode photoJson = client.getUserProfilePhoto(user.getId());
		if (photoJson.get("result").get("total_count").asInt() > 0) {
			String photoId = photoJson.get("result").get("photos").get(0).get(0).get("file_id").asText();
			client.sendPhoto(chatId, photoId,
					messages.getMessage(lang, "connect", user.getFirstName()) + " /connect" + user.getId());
		} else {
			client.sendMessage(chatId,
					messages.getMessage(lang, "connect", user.getFirstName()) + " /connect" + user.getId());
		}
	}
}
