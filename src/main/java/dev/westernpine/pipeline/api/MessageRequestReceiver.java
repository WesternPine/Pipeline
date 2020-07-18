package dev.westernpine.pipeline.api;

import java.util.UUID;

import dev.westernpine.pipeline.api.object.Message;

public interface MessageRequestReceiver {
	
	/**
	 * The method called when a request message is received.
	 * 
	 * @param requestId The request ID.
	 * @param message The request message.
	 */
	public void onRequestReceived(UUID requestId, Message message);

}
