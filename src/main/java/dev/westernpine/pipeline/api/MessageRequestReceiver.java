package dev.westernpine.pipeline.api;

import java.util.UUID;

import dev.westernpine.pipeline.api.object.Message;

public interface MessageRequestReceiver {
	
	public void onRequestReceived(UUID requestId, Message message);

}
