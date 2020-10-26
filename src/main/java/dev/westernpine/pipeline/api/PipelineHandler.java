package dev.westernpine.pipeline.api;

import java.util.UUID;

import dev.westernpine.pipeline.api.object.Message;
import dev.westernpine.pipeline.api.object.RequestProcess;
import lombok.NonNull;

public interface PipelineHandler {
	
	/**
	 * Registers a new receiver for general messages.
	 * 
	 * @param receiver The receiver to handle incoming messages.
	 */
	public void registerReceiver(@NonNull MessageReceiver receiver);
	
	/**
	 * Clears the registered receivers.
	 */
	public void clearRegisteredReceivers();

	/**
	 * Registers a new request receiver for requests.
	 * 
	 * @param receiver The receiver to handle incoming requests than need responses.
	 */
	public void registerRequestReceiver(@NonNull MessageRequestReceiver receiver);
	
	/**
	 * Clears the registered request receivers.
	 */
	public void clearRegisteredRequestReceivers();
	
	/**
	 * Sends a general message.
	 * 
	 * @param message The message to send.
	 */
	public void sendAndForget(@NonNull Message message);

	/**
	 * Sends the request and starts the listener for the response.
	 * 
	 * @param timeout The time in milliseconds before the request times out.
	 * @param message The request to send.
	 * @return The process to set up handling the listening process for the Request's response.
	 */
	public RequestProcess request(long timeout, @NonNull Message message);
	
	/**
	 * Responds to a request with the given request ID.
	 * 
	 * @param requestId The original request ID.
	 * @param message The response message.
	 */
	public void respond(@NonNull UUID requestId, @NonNull Message message);
	
}
