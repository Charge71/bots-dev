package com.charge71.bots;

import org.apache.log4j.Logger;

import com.charge71.telegramapi.TelegramApiAware;
import com.charge71.telegramapi.annotations.BotCommand;
import com.charge71.telegramapi.annotations.TelegramBot;
import com.fasterxml.jackson.databind.node.ObjectNode;

@TelegramBot("204887014:AAFpLXo_Sh-cLRl_XOfJf_KFOVTuxK4H-s0")
public class MeetAroundBot extends TelegramApiAware {

	private static Logger log = Logger.getLogger(MeetAroundBot.class);

	@BotCommand("/start")
	public void start(ObjectNode json) {
		log.debug("/start start");
		String name = json.get("message").get("from").get("first_name").asText();
		String chatId = json.get("message").get("chat").get("id").asText();
		client.sendMessage(chatId, "Welcome " + name + "!");
		log.debug("/start end");
	}

}
