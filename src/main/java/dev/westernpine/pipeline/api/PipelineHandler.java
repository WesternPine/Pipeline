package dev.westernpine.pipeline.api;

import java.util.UUID;

import dev.westernpine.pipeline.api.object.Message;
import dev.westernpine.pipeline.api.object.RequestProcess;
import lombok.NonNull;

public interface PipelineHandler {
	
	public void registerReceiver(@NonNull MessageReceiver receiver);

	public void registerRequestReceiver(@NonNull MessageRequestReceiver receiver);
	
	public void sendAndForget(@NonNull Message message);
	
	public RequestProcess request(long timeout, @NonNull Message message);
	
	public void respond(@NonNull UUID requestId, @NonNull Message message);
	
}
