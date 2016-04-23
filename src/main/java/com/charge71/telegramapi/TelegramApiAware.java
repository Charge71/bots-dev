package com.charge71.telegramapi;

public class TelegramApiAware {

	public TelegramApiClient client;

	protected void setClient(TelegramApiClient client) {
    	this.client = client;
    }

}
