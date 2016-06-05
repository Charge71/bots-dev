package com.charge71.framework;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface ApiClient {

	public ObjectNode sendMessage(String chatId, String text);

	public ObjectNode sendMarkdownMessage(String chatId, String text, boolean disablePreview);

	public ObjectNode sendLocationRequest(String chatId, String text, String request);
	
	public ObjectNode sendForceReply(String chatId, String text);

	public ObjectNode sendButton(String chatId, String text, String request);

	public ObjectNode sendPhoto(String chatId, String fileId, String caption);

	public ObjectNode sendLanguageButtons(String chatId, String text);

	public ObjectNode getUserProfilePhoto(String userId);

	public ObjectNode getUserInfo(String userId);
	
	public ObjectNode sentStructuredMessage(String chatId, ObjectNode objectNode);
}
