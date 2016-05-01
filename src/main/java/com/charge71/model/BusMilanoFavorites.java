package com.charge71.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "busmi-favorites")
public class BusMilanoFavorites {

	public static class BusMilanoStop {

		private String id;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		private String name;

	}

	@Id
	private String id;

	private BusMilanoStop[] stops;

	public BusMilanoStop[] getStops() {
		return stops;
	}

	public void setStops(BusMilanoStop[] stops) {
		this.stops = stops;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
