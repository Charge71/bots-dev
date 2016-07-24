package com.charge71.model;


import java.util.Date;

import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="busmi-log")
public class BusMilanoLogEntry {
	
	private Date timestamp;
	
	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getStopId() {
		return stopId;
	}

	public void setStopId(String stopId) {
		this.stopId = stopId;
	}

	private String userId;
	
	private String stopId;

}
