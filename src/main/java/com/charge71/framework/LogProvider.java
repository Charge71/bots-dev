package com.charge71.framework;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface LogProvider {
	
	public ObjectNode getLog(int offset, int limit);

}
