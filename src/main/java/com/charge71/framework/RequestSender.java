package com.charge71.framework;

import org.springframework.http.ResponseEntity;

public interface RequestSender<T, R> {

	public ResponseEntity<R> sendRequest(T request) throws Exception;

}
