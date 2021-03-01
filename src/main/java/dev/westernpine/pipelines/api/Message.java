package dev.westernpine.pipelines.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.UUID;

import dev.westernpine.pipelines.lib.EmptyMessageException;

public class Message {
	
	private UUID carrier;
	private LinkedList<Object> payload = new LinkedList<>();

	/**
	 * A generic message to be sent.
	 * 
	 * @param payload The payload of the message.
	 */
	public Message(UUID carrier, LinkedList<Object> payload) {
		this.carrier = carrier;
		this.payload = payload;
	}
	
	/**
	 * A generic message to be sent.
	 */
	public Message(UUID carrier) {
		this.carrier = carrier;
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
	 * Injects data at the start of the payload.
	 * 
	 * @param object The object to write.
	 * @return The same mutable message object, used for stream-lining code.
	 */
	public Message inject(Object object) {
		payload.addFirst(object);
		return this;
	}

	/**
	 * Appends data to the end of the payload.
	 * 
	 * @param object The object to write.
	 * @return The same mutable message object, used for stream-lining code.
	 */
	public Message append(Object object) {
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
	public Message clone() {
		return new Message(carrier, payload);
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
				output.writeObject(MessageType.MESSAGE);
				output.writeObject(payload);
				return bytes.toByteArray();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Send this message.
	 * @param pipeline The pipeline to send over.
	 */
	public void send(Pipeline pipeline) {
		pipeline.send(this);
	}

}
