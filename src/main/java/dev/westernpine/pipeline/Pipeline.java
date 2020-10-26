package dev.westernpine.pipeline;

import java.util.UUID;

import dev.westernpine.pipeline.api.MessageReceiver;
import dev.westernpine.pipeline.api.MessageRequestReceiver;
import dev.westernpine.pipeline.api.PipelineHandler;
import dev.westernpine.pipeline.api.object.Message;
import dev.westernpine.pipeline.api.object.RequestProcess;
import dev.westernpine.pipeline.exceptions.PipelineNotInitializedException;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;

public class Pipeline {
	
	/**
	 * The cache time in milliseconds for a response to be cached in memory.
	 * This only applies to default implementations, or if custom implementations that use this value.
	 */
	public static long responseCacheTime = 5000;

	public static final String splitter = ":";
	public static final String requestPrefix = "request";
	public static final String responsePrefix = "response";
	public static final String namespace = "pipeline";
	public static final String proxyName = "proxy";
	public static final String serverName = "server";
	
	/**
	 * The pipeline handler.
	 */
	@Getter
	private static PipelineHandler handler;
	
	/**
	 * Sets the pipeline handler.
	 * 
	 * @param pipelineHandler The pipeline handler.
	 */
	public static void setHandler(@NonNull PipelineHandler pipelineHandler) {
		handler = pipelineHandler;
	}

	/**
	 * Registers a new receiver for general messages.
	 * 
	 * @param receiver The receiver to handle incoming messages.
	 * 
	 * @throws PipelineNotInitializeException if the pipeline has not been initialized.
	 */
	@SneakyThrows
	public static void registerReceiver(@NonNull MessageReceiver receiver) {
		if(handler == null)
			throw new PipelineNotInitializedException();
		handler.registerReceiver(receiver);
	}
	
	/**
	 * Clears the registered receivers.
	 * 
	 * @throws PipelineNotInitializeException if the pipeline has not been initialized.
	 */
	@SneakyThrows
	public static void clearRegisteredReceivers() {
		if(handler == null)
			throw new PipelineNotInitializedException();
		handler.clearRegisteredReceivers();
	}

	/**
	 * Registers a new request receiver for requests.
	 * 
	 * @param receiver The receiver to handle incoming requests than need responses.
	 * 
	 * @throws PipelineNotInitializeException if the pipeline has not been initialized.
	 */
	@SneakyThrows
	public static void registerRequestReceiver(@NonNull MessageRequestReceiver receiver) {
		if(handler == null)
			throw new PipelineNotInitializedException();
		handler.registerRequestReceiver(receiver);
	}
	
	/**
	 * Clears the registered request receivers.
	 * 
	 * @throws PipelineNotInitializeException if the pipeline has not been initialized.
	 */
	@SneakyThrows
	public static void clearRegisteredRequestReceivers() {
		if(handler == null)
			throw new PipelineNotInitializedException();
		handler.clearRegisteredReceivers();
	}
	
	/**
	 * Sends a general message.
	 * 
	 * @param message The message to send.
	 * 
	 * @throws PipelineNotInitializeException if the pipeline has not been initialized.
	 */
	@SneakyThrows
	public static void sendAndForget(@NonNull Message message) {
		if(handler == null)
			throw new PipelineNotInitializedException();
		handler.sendAndForget(message);
	}

	/**
	 * Sends the request and starts the listener for the response.
	 * 
	 * @param timeout The timeout time in milliseconds.
	 * @param message The request to send.
	 * @return The process to set up handling the listening process for the Request's response.
	 * 
	 * @throws PipelineNotInitializeException if the pipeline has not been initialized.
	 */
	@SneakyThrows
	public static RequestProcess request(long timeout, @NonNull Message message) {
		if(handler == null)
			throw new PipelineNotInitializedException();
		return handler.request(timeout, message);
	}
	
	/**
	 * Responds to a request with the given request ID.
	 * 
	 * @param requestId The original request ID.
	 * @param message The response message.
	 * 
	 * @throws PipelineNotInitializeException if the pipeline has not been initialized.
	 */
	@SneakyThrows
	public static void respond(@NonNull UUID requestId, @NonNull Message message) {
		if(handler == null)
			throw new PipelineNotInitializedException();
		handler.respond(requestId, message);
	}

}
