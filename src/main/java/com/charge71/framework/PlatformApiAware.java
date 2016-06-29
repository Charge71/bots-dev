package com.charge71.framework;

import com.charge71.lang.MessageHelper;

public class PlatformApiAware<T, R> {

	public ApiClient<T, R> client;

	public MessageHelper messages;

	public void setClient(ApiClient<T, R> client) {
		this.client = client;
	}

	public void setMessages(MessageHelper messages) {
		this.messages = messages;
	}

}
