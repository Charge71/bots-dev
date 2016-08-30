package com.charge71.framework;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface AdsProvider {

	public void handle(HttpServletRequest request, HttpServletResponse response);
	
	public void setAdsBaseUrl(String adsBaseUrl);

}
