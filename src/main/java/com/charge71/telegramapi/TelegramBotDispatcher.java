package com.charge71.telegramapi;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import com.charge71.framework.PlatformApiAware;
import com.charge71.lang.MessageHelper;
import com.charge71.telegramapi.annotations.BotCommand;
import com.charge71.telegramapi.annotations.TelegramBot;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TelegramBotDispatcher {

	private static Logger log = Logger.getLogger(TelegramBotDispatcher.class);

	@Autowired
	private AutowireCapableBeanFactory beanFactory;

	private List<String> botClasses;

	private Map<String, Object> bots = new HashMap<String, Object>();
	private Map<String, Map<String, Method>> methods = new HashMap<String, Map<String, Method>>();
	private Map<String, Map<String, Method>> prefixMethods = new HashMap<String, Map<String, Method>>();

	public void init() throws IOException {

		for (String botClass : botClasses) {
			try {
				Class<?> cbot = Class.forName(botClass);
				TelegramBot tbot = cbot.getAnnotation(TelegramBot.class);
				if (tbot == null) {
					continue;
				}
				String token = tbot.value();
				Object bot = cbot.newInstance();
				bots.put(token, bot);
				Map<String, Method> commands = new HashMap<String, Method>();
				Map<String, Method> prefixCommands = new HashMap<String, Method>();
				methods.put(token, commands);
				prefixMethods.put(token, prefixCommands);
				Method[] ma = cbot.getMethods();
				for (Method m : ma) {
					BotCommand bc = m.getAnnotation(BotCommand.class);
					if (bc != null) {
						if (!bc.isPrefix()) {
							commands.put(bc.value(), m);
						} else {
							prefixCommands.put(bc.value(), m);
						}
					}
				}
				if (bot instanceof PlatformApiAware) {
					((PlatformApiAware) bot).setClient(new TelegramApiClient(token));
					((PlatformApiAware) bot).setMessages(new MessageHelper(token));
				}
				beanFactory.autowireBean(bot);
				log.info("BotDispatcher init ok for class " + botClass);
				log.debug("Commands: " + methods.get(token).keySet());
				log.debug("Prefix commands: " + prefixMethods.get(token).keySet());
			} catch (Exception e) {
				log.error("BotDispatcher error for class " + botClass, e);
			}

		}

	}

	public void exec(String token, String command, ObjectNode json) {
		Object bot = bots.get(token);
		if (bot == null) {
			log.warn("Not found bot with token " + token);
			return;
		}
		Method method = methods.get(token).get(command);
		if (method == null) {
			for (String prefixCommand : prefixMethods.get(token).keySet()) {
				if (command.startsWith(prefixCommand)) {
					method = prefixMethods.get(token).get(prefixCommand);
				}
			}
			if (method == null) {
				log.debug("Not found command " + command + " for bot " + bot + " trying default");
				method = methods.get(token).get("default");
				if (method == null) {
					log.warn("Not found command " + command + " for bot " + bot);
					return;
				}
			}
		}
		try {
			method.invoke(bot, json, command);
		} catch (Exception e) {
			log.error("BotDispatcher error for command " + command, e);
		}
	}

	public List<String> getBotClasses() {
		return botClasses;
	}

	public void setBotClasses(List<String> botClasses) {
		this.botClasses = botClasses;
	}

}
