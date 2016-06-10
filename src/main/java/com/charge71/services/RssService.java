package com.charge71.services;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.charge71.model.RssSubscriptions;
import com.charge71.model.RssSubscriptions.RssFeed;
import com.charge71.model.RssUser;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

public class RssService {

	private static Logger log = Logger.getLogger(RssService.class);

	@Autowired
	private MongoTemplate mongoTemplate;

	private RssHandler rssHandler;

	private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	public static interface RssHandler {
		public void handle(String chatId, String title, String link);
	}

	private class RssChecker implements Runnable {

		@Override
		public void run() {
			//log.info("RssChecker starting...");
			List<RssUser> users = getUsers();
			for (RssUser user : users) {
				RssFeed[] feeds = getFeeds(user.getId());
				for (RssFeed feed : feeds) {
					try {
						//log.debug("Checking " + feed.getUrl());
						Date date = updateRss(user.getChatId(), feed.getUrl(), feed.getLast(), rssHandler);
						if (date.after(feed.getLast())) {
							updateDate(user.getId(), feed.getUrl(), date);
						}
						//log.debug("Done " + feed.getUrl());
					} catch (Exception e) {
						log.error("Error checking " + feed.getUrl(), e);
					}
				}
			}
			//log.info("RssChecker finished.");
		}

	}

	public void start(RssHandler rssHandler) {
		this.rssHandler = rssHandler;
		scheduler.scheduleAtFixedRate(new RssChecker(), 0, 1, TimeUnit.MINUTES);
		log.info("Started RSS service.");
	}

	public void destroy() {
		scheduler.shutdown();
		log.info("Stopped RSS service.");
	}

	public List<RssUser> getUsers() {
		return mongoTemplate.findAll(RssUser.class);
	}

	public RssFeed[] getFeeds(String userId) {
		RssSubscriptions subs = mongoTemplate.findById(userId, RssSubscriptions.class);
		return subs != null ? subs.getFeeds() : new RssFeed[0];
	}

	public void updateDate(String id, String url, Date date) {
		mongoTemplate.updateFirst(Query.query(Criteria.where("id").is(id).and("feeds.url").is(url)),
				Update.update("feeds.$.last", date), RssSubscriptions.class);
	}

	public static RssFeed initRss(String url) throws Exception {

		Date last = null;

		try (InputStream is = new URL(url).openStream()) {
			SyndFeedInput input = new SyndFeedInput();
			SyndFeed feed = input.build(new XmlReader(is));

			for (SyndEntry entry : feed.getEntries()) {
				if (last == null || entry.getPublishedDate().after(last)) {
					last = entry.getPublishedDate();
				}
			}

			RssFeed rssfeed = new RssFeed();
			rssfeed.setLast(last);
			rssfeed.setName(feed.getTitle());
			rssfeed.setUrl(url);
			return rssfeed;
		}

	}

	public static Date updateRss(String chatId, String url, Date last, RssHandler handler) throws Exception {

		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		con.setConnectTimeout(10000);
		con.setReadTimeout(10000);
		con.connect();
		try (InputStream is = con.getInputStream()) {
			SyndFeedInput input = new SyndFeedInput();
			SyndFeed feed = input.build(new XmlReader(is));

			for (SyndEntry entry : feed.getEntries()) {
				if (entry.getPublishedDate().after(last)) {
					last = entry.getPublishedDate();
					handler.handle(chatId, entry.getTitle(), entry.getLink());
				}
			}

			return last;
		} finally {
			con.disconnect();
		}

	}

}
