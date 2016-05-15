package com.charge71.messengerapi;

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
import com.charge71.messengerapi.annotations.BotMessage;
import com.charge71.messengerapi.annotations.BotPostback;
import com.charge71.messengerapi.annotations.MessengerBot;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class MessengerBotDispatcher {

	private static Logger log = Logger.getLogger(MessengerBotDispatcher.class);

	@Autowired
	private AutowireCapableBeanFactory beanFactory;

	private List<String> botClasses;

	private Map<String, Object> bots = new HashMap<String, Object>();
	private Map<String, Method> messages = new HashMap<String, Method>();
	private Map<String, Map<String, Method>> methods = new HashMap<String, Map<String, Method>>();
	private Map<String, Map<String, Method>> prefixMethods = new HashMap<String, Map<String, Method>>();

	public void init() throws IOException {

		for (String botClass : botClasses) {
			try {
				Class<?> cbot = Class.forName(botClass);
				MessengerBot mbot = cbot.getAnnotation(MessengerBot.class);
				if (mbot == null) {
					continue;
				}
				String token = mbot.token();
				String botname = mbot.name();
				Object bot = cbot.newInstance();
				bots.put(botname, bot);
				Map<String, Method> postbacks = new HashMap<String, Method>();
				Map<String, Method> prefixPostbacks = new HashMap<String, Method>();
				methods.put(botname, postbacks);
				prefixMethods.put(botname, prefixPostbacks);
				Method[] ma = cbot.getMethods();
				for (Method m : ma) {
					BotMessage bm = m.getAnnotation(BotMessage.class);
					if (bm != null) {
						messages.put(botname, m);
					} else {
						BotPostback bp = m.getAnnotation(BotPostback.class);
						if (bp != null) {
							if (!bp.isPrefix()) {
								postbacks.put(bp.value(), m);
							} else {
								prefixPostbacks.put(bp.value(), m);
							}
						}
					}
				}
				if (bot instanceof PlatformApiAware) {
					((PlatformApiAware) bot).setClient(new MessengerApiClient(token));
					((PlatformApiAware) bot).setMessages(new MessageHelper(token));
				}
				beanFactory.autowireBean(bot);
				log.info("BotDispatcher init ok for class " + botClass);
				log.debug("Message: " + messages.get(botname));
				log.debug("Postbacks: " + methods.get(botname).keySet());
				log.debug("Prefix postbacks: " + prefixMethods.get(botname).keySet());
			} catch (Exception e) {
				log.error("BotDispatcher error for class " + botClass, e);
			}

		}

	}

	public void exec(String botname, String postback, ObjectNode json) {
		Object bot = bots.get(botname);
		if (bot == null) {
			log.warn("Not found bot with token " + botname);
			return;
		}
		if (postback != null) {
			Method method = methods.get(botname).get(postback);
			if (method == null) {
				for (String prefixPostback : prefixMethods.get(botname).keySet()) {
					if (postback.startsWith(prefixPostback)) {
						method = prefixMethods.get(botname).get(prefixPostback);
					}
				}
				if (method == null) {
					log.debug("Not found postback " + postback + " for bot " + bot);
					return;
				}
				try {
					method.invoke(bot, json, postback);
				} catch (Exception e) {
					log.error("BotDispatcher error for postback " + postback, e);
				}
			}
		} else {
			Method method = messages.get(botname);
			if (method == null) {
				log.debug("Not found message for bot " + bot);
				return;
			}
			try {
				method.invoke(bot, json);
			} catch (Exception e) {
				log.error("BotDispatcher error for message", e);
			}
		}
	}

	public List<String> getBotClasses() {
		return botClasses;
	}

	public void setBotClasses(List<String> botClasses) {
		this.botClasses = botClasses;
	}

}
