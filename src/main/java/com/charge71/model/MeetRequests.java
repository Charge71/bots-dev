package com.charge71.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "meet-requests")
public class MeetRequests {

	@Id
	private String id;

	private String[] requests;

	public String getId() {
		return id;
	}

	public String[] getRequests() {
		return requests;
	}

	public void setRequests(String[] requests) {
		this.requests = requests;
	}

	public void setId(String id) {
		this.id = id;
	}
}
