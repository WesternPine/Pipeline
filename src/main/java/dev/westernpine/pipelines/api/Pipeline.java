package dev.westernpine.pipelines.api;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface Pipeline {
	
	/**
	 * Adds a listener for messages received.
	 * @param messageHandler The handler.
	 */
	public void onMessage(Consumer<Message> messageHandler);
	
	/**
	 * Adds a listener for requests received.
	 * @param requestHandler The handler.
	 */
	public void onRequest(Consumer<Request> requestHandler);
	
	/**
	 * Send the message using the carrier's connection.
	 * @param message The message to be sent.
	 */
	public void send(Message message);
	
	/**
	 * Requests a response from the server/proxy the carrier is connected to/through.
	 * @param request The request to be sent.
	 * @return The response from the server/proxy.
	 */
	public CompletableFuture<Optional<Response>> request(Request request);
	
	/**
	 * Replies to a request with the same uuid.
	 * @param response The response to be sent.
	 */
	public void respond(Response response);

}
