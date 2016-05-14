package com.charge71.telegramapi;

import com.charge71.framework.ApiClient;
import com.charge71.lang.MessageHelper;

public class PlatformApiAware {

	public ApiClient client;

	public MessageHelper messages;

	protected void setClient(ApiClient client) {
		this.client = client;
	}

	protected void setMessages(MessageHelper messages) {
		this.messages = messages;
	}

}
