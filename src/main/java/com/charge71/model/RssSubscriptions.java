package com.charge71.model;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "rss-subscriptions")
public class RssSubscriptions {

	public static class RssFeed {

		private String url;

		private String name;

		private Date last;

		public Date getLast() {
			return last;
		}

		public void setLast(Date last) {
			this.last = last;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	@Id
	private String id;

	private RssFeed[] feeds;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public RssFeed[] getFeeds() {
		return feeds;
	}

	public void setFeeds(RssFeed[] feeds) {
		this.feeds = feeds;
	}

}
