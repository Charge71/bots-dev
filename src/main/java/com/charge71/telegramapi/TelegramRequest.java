package com.charge71.telegramapi;

import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TelegramRequest {

	protected UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
	
	protected String path;

	public static class Keyboard {

		private ObjectNode obj = JsonNodeFactory.instance.objectNode();

		private ArrayNode current;

		public static Keyboard replyKeyboard() {
			Keyboard kb = new Keyboard();
			kb.current = kb.obj.putArray("keyboard").addArray();
			return kb;
		}

		public Keyboard row() {
			current = ((ArrayNode) obj.get("keyboard")).addArray();
			return this;
		}

		public Keyboard button(String text) {
			current.addObject().put("text", text);
			return this;
		}

		public Keyboard locationButton(String text) {
			current.addObject().put("text", text).put("request_location", true);
			return this;
		}
		
		public Keyboard resize() {
			obj.put("resize_keyboard", true);
			return this;
		}

		public Keyboard oneTime() {
			obj.put("one_time_keyboard", true);
			return this;
		}
	}

	private TelegramRequest(String method, String param, String value) {
		path = method;
		builder.queryParam(param, value);
	}

	public static TelegramRequest sendMessage(String chatId) {
		return new TelegramRequest("/sendMessage", "chat_id", chatId);
	}

	public static TelegramRequest sendPhoto(String chatId) {
		return new TelegramRequest("/sendPhoto", "chat_id", chatId);
	}

	public static TelegramRequest getUserProfilePhotos(String userId) {
		return new TelegramRequest("/getUserProfilePhotos", "user_id", userId);
	}

	public TelegramRequest offset(int offset) {
		builder.queryParam("offset", offset);
		return this;
	}

	public TelegramRequest limit(int limit) {
		builder.queryParam("limit", limit);
		return this;
	}

	public TelegramRequest text(String text) {
		builder.queryParam("text", text);
		return this;
	}

	public TelegramRequest caption(String caption) {
		builder.queryParam("caption", caption);
		return this;
	}

	public TelegramRequest photoId(String photoId) {
		builder.queryParam("photo", photoId);
		return this;
	}

	public TelegramRequest parseModeMarkdown() {
		builder.queryParam("parse_mode", "Markdown");
		return this;
	}

	public TelegramRequest parseModeHtml() {
		builder.queryParam("parse_mode", "HTML");
		return this;
	}

	public TelegramRequest disableWebPagePreview() {
		builder.queryParam("disable_web_page_preview", true);
		return this;
	}

	public TelegramRequest disableNotification() {
		builder.queryParam("disable_notification", true);
		return this;
	}

	public TelegramRequest keyboard(Keyboard keyboard) {
		builder.queryParam("reply_markup", keyboard.obj.toString());
		return this;
	}

	public TelegramRequest hideKeyboard() {
		ObjectNode obj = JsonNodeFactory.instance.objectNode();
		obj.put("hide_keyboard", true);
		builder.queryParam("reply_markup", obj.toString());
		return this;
	}

	public TelegramRequest forceReply() {
		ObjectNode obj = JsonNodeFactory.instance.objectNode();
		obj.put("force_reply", true);
		builder.queryParam("reply_markup", obj.toString());
		return this;
	}
}
