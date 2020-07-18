package dev.westernpine.pipeline.api;

import java.util.UUID;

import dev.westernpine.pipeline.api.object.Message;
import dev.westernpine.pipeline.api.object.RequestProcess;

public interface PipelineHandler {
	
	public void registerReceiver(MessageReceiver receiver);

	public void registerRequestReceiver(MessageRequestReceiver receiver);
	
	public void sendAndForget(Message message);
	
	public RequestProcess request(long timeout, Message message);
	
	public void respond(UUID requestId, Message message);
	
}
