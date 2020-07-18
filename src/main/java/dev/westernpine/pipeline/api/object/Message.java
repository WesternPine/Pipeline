package dev.westernpine.pipeline.api.object;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;

public class Message {
	
	/**
	 * The Player entity carrying the message, represented by a UUID.
	 */
	@Getter
	private final UUID carrier;
	
	private LinkedList<Object> content = new LinkedList<>();
	private int index = 0;
	
	/**
	 * A message object to decode the byte array, and craft for use elsewhere.
	 * 
	 * @param data The byte array to decode.
	 */
	@SuppressWarnings("unchecked")
	@SneakyThrows
	public Message(@NonNull byte[] data) {
		ByteArrayInputStream bytes = new ByteArrayInputStream(data);
        ObjectInputStream input = new ObjectInputStream(bytes);

        carrier = (UUID) input.readObject();
        content = (LinkedList<Object>) input.readObject();

        input.close();
        bytes.close();
	}
	
	/**
	 * The message object to be crafted, encoded, and sent elsewhere.
	 * 
	 * @param carrier The UUID representation of the player, whose connection should be used to send the message.
	 */
	public Message(@NonNull UUID carrier) {
		this.carrier = carrier;
	}
	
	/**
	 * The message object to be crafted, encoded, and sent elsewhere.
	 * 
	 * @param carrier The UUID representation of the player, whose connection should be used to send the message.
	 * @param content
	 */
	public Message(@NonNull UUID carrier, @NonNull Collection<Object> content) {
		this.carrier = carrier;
		this.content.addAll(content);
	}
	
	/**
	 * Writes data to the first slot.
	 * 
	 * @param object The object to write.
	 * @return The same mutable message object, used for stream-lining code.
	 */
	public Message inject(@NonNull Object object) {
		content.addFirst(object);
		return this;
	}
	
	/**
	 * Writes data to the last slot.
	 * 
	 * @param object The object to write.
	 * @return The same mutable message object, used for stream-lining code.
	 */
	public Message write(@NonNull Object object) {
		content.add(object);
		return this;
	}
	
	/**
	 * Reads data from the last slot, and removes it.
	 * 
	 * @return The data read.
	 * 
	 * @throws Exception If there is nothing else to read.
	 */
	@SneakyThrows
	public Object read() {
        if (content.isEmpty())
            throw new Exception("Index " + index + " out of range.");
        index++;
        return content.remove(0);
    }
	/**
	 * Removes and reads data from the last slot, and attempts to cast it.
	 * 
	 * @param <T> The class type.
	 * @param clazz The class.
	 * @return The data read.
	 * 
	 * @throws Exception If the class could not be cast to the object.
	 */
	@SneakyThrows
    public <T> T read(@NonNull Class<T> clazz) {
        Object object = read();
        if (object.getClass().isAssignableFrom(clazz))
            return clazz.cast(object);
        throw new Exception("Could not cast " + object + " to class: " + clazz.getSimpleName());
    }
	
	/**
	 * Clones the current message at it's current state to a new object.
	 */
	public Message clone() {
		return new Message(carrier, content);
	}
	
	/**
	 * If the message has readable content.
	 * 
	 * @return True if has content, false if empty.
	 */
	public boolean hasContent() {
		return !this.content.isEmpty();
	}
	
	/**
	 * Get a copy of the content in this message.
	 * 
	 * @return A copy of the content in this message.
	 */
	public List<Object> getContent() {
		return new LinkedList<>(this.content);
	}
	
	/**
	 * Get the byte array representation of this message.
	 * 
	 * @return The byte array representation of this message.
	 * 
	 * @throws IOException If an I/O error occurs while writing stream header.
	 */
	@SneakyThrows
	public byte[] toByteArray() {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream output = new ObjectOutputStream(bytes);

        output.writeObject(carrier);
        output.writeObject(content);

        output.close();
        bytes.close();
        
        return bytes.toByteArray();
	}

}
