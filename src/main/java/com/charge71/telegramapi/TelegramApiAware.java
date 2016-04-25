package com.charge71.telegramapi;

import com.charge71.lang.MessageHelper;

public class TelegramApiAware {

	public TelegramApiClient client;
	
	public MessageHelper messages;

	protected void setClient(TelegramApiClient client) {
    	this.client = client;
    }
	
	protected void setMessages(MessageHelper messages) {
		this.messages = messages;
	}

}
