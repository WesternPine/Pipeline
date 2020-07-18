package dev.westernpine.pipeline.api;

import dev.westernpine.pipeline.api.object.Message;

public interface MessageReceiver {
	
	public void onMessageReceived(Message message);

}
