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
	
	@Getter
	private final UUID carrier;
	LinkedList<Object> content = new LinkedList<>();
	int index = 0;
	
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
	
	public Message(@NonNull UUID carrier) {
		this.carrier = carrier;
	}
	
	public Message(@NonNull UUID carrier, Collection<Object> content) {
		this.carrier = carrier;
		this.content.addAll(content);
	}
	
	public Message inject(Object object) {
		content.addFirst(object);
		return this;
	}
	
	public Message write(Object object) {
		content.add(object);
		return this;
	}
	
	@SneakyThrows
	public Object read() {
        if (content.isEmpty())
            throw new Exception("Index " + index + " out of range.");
        index++;
        return content.remove(0);
    }
	
	@SneakyThrows
    public <T> T read(Class<T> clazz) {
        Object object = read();
        if (object.getClass().isAssignableFrom(clazz))
            return clazz.cast(object);
        throw new Exception("Could not cast " + object + " to class: " + clazz.getSimpleName());
    }
	
	public Message clone() {
		return new Message(carrier, content);
	}
	
	public boolean hasContent() {
		return !this.content.isEmpty();
	}
	
	public List<Object> getContent() {
		return new LinkedList<>(this.content);
	}
	
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
