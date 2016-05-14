package com.charge71.framework;

import com.charge71.lang.MessageHelper;

public class PlatformApiAware {

	public ApiClient client;

	public MessageHelper messages;

	public void setClient(ApiClient client) {
		this.client = client;
	}

	public void setMessages(MessageHelper messages) {
		this.messages = messages;
	}

}
