package dev.westernpine.pipelines.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.UUID;

import dev.westernpine.pipelines.lib.EmptyMessageException;

public class Response {
	
	/**
	 * Forms a response message linked to the request.
	 * @param request The request to form the response from.
	 * @return A new empty response message linked to the request.
	 */
	public static Response fromRequest(Request request) {
		return request.toResponse();
	}
	
	private UUID carrier;
	private UUID uuid;
	private LinkedList<Object> payload = new LinkedList<>();

	/**
	 * A generic response to be sent.
	 * 
	 * @param payload The payload of the response.
	 */
	public Response(UUID carrier, LinkedList<Object> payload, UUID uuid) {
		this.carrier = carrier;
		this.payload = payload;
		this.uuid = uuid;
	}
	
	/**
	 * A generic response to be sent.
	 * 
	 * @param UUID The uuid of the Request.
	 */
	public Response(UUID carrier, UUID uuid) {
		this.carrier = carrier;
		this.uuid = uuid;
	}
	
	/**
	 * @return The player carrying the message.
	 */
	public UUID getCarrier() {
		return this.carrier;
	}
	
	/**
	 * Sets the carrier of the message.
	 */
	public void setCarrier(UUID carrier) {
		this.carrier = carrier;
	}
	
	/**
	 * Get a copy of the message payload at it's current state.
	 * 
	 * @return A copy of the message payload at it's current state.
	 */
	public LinkedList<Object> getPayload() {
		return payload;
	}
	
	/**
	 * @return The UUID of the request.
	 */
	public UUID getUuid() {
		return this.uuid;
	}
	
	/**
	 * Injects data at the start of the payload.
	 * 
	 * @param object The object to write.
	 * @return The same mutable message object, used for stream-lining code.
	 */
	public Response inject(Object object) {
		payload.addFirst(object);
		return this;
	}

	/**
	 * Appends data to the end of the payload.
	 * 
	 * @param object The object to write.
	 * @return The same mutable message object, used for stream-lining code.
	 */
	public Response append(Object object) {
		payload.addLast(object);
		return this;
	}
	
	/**
	 * @return True if the payload has content remaining.
	 */
	public boolean hasNext() {
		return !payload.isEmpty();
	}

	/**
	 * Removes and reads the first object's data from the payload.
	 * 
	 * @return The data read.
	 * 
	 * @throws EmptyMessageException If the message has an empty payload (Nothing else to read).
	 */
	public Object read() {
		if(payload.isEmpty())
			throw new RuntimeException(new EmptyMessageException());
		return payload.remove(0);
	}

	/**
	 * Removes and reads the first object's data from the payload, and attempts to cast it.
	 * 
	 * @param <T> The class type.
	 * @param clazz The class.
	 * @return The data read casted to the object class.
	 * 
	 * @throws EmptyMessageException If the payload is empty (Nothing else to read).
	 * @throws ClassCastException If the class could not be cast to the object.
	 */
	public <T> T read(Class<T> clazz) {
		return clazz.cast(read());
	}
	
	/**
	 * @return An exact replica of this message.
	 */
	public Response clone() {
		return new Response(carrier, payload, uuid);
	}

	
	/**
	 * Get the byte array representation of this message.
	 * 
	 * @return The byte array representation of this message.
	 * 
	 * @throws IOException If an I/O error occurs while writing stream header.
	 */
	public byte[] toByteArray() {
		try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
			try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
				output.writeObject(MessageType.RESPONSE);
				output.writeObject(payload);
				output.writeObject(uuid);
				return bytes.toByteArray();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Sends this response.
	 * @param pipeline The pipeline to send over.
	 */
	public void send(Pipeline pipeline) {
		pipeline.respond(this);
	}

}
