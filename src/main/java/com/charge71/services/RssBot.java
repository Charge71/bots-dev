package com.charge71.services;

import org.springframework.beans.factory.annotation.Autowired;

import com.charge71.framework.ApiClient;
import com.charge71.framework.PlatformApiAware;
import com.charge71.services.RssService.RssHandler;
import com.charge71.telegramapi.annotations.TelegramBot;

@TelegramBot("test")
public class RssBot extends PlatformApiAware implements RssHandler {

	@Autowired
	private RssService rssService;

	@Override
	public void setClient(ApiClient client) {
		super.setClient(client);
		rssService.start(this);
	}

	@Override
	public void handle(String link) {
		// TODO Auto-generated method stub

	}

}
