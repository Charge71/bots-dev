package com.charge71.framework;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface ApiClient {

	public ObjectNode sendMessage(String chatId, String text);

	public ObjectNode sendMarkdownMessage(String chatId, String text);

	public ObjectNode sendLocationRequest(String chatId, String text, String request);

	public ObjectNode sendButton(String chatId, String text, String request);

	public ObjectNode sendPhoto(String chatId, String fileId, String caption);

	public ObjectNode sendLanguageButtons(String chatId, String text);

	public ObjectNode getUserProfilePhoto(String userId);

}
