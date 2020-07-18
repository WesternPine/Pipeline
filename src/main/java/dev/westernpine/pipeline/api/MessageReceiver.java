package dev.westernpine.pipeline.api;

import dev.westernpine.pipeline.api.object.Message;

public interface MessageReceiver {
	
	/**
	 * The method called when a general message is received.
	 * 
	 * @param message The message received.
	 */
	public void onMessageReceived(Message message);

}
