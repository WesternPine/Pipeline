package dev.westernpine.pipelines.live.server;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import dev.westernpine.pipelines.api.Message;
import dev.westernpine.pipelines.api.MessageType;
import dev.westernpine.pipelines.api.Pipeline;
import dev.westernpine.pipelines.api.Request;
import dev.westernpine.pipelines.api.Response;
import dev.westernpine.pipelines.lib.NoRoutablePathException;
import dev.westernpine.pipelines.lib.PipelineDefaults;
import dev.westernpine.pipelines.lib.ResponseListener;

public class BukkitPipeline implements Pipeline {
	
	private Plugin plugin;
	
	private Set<Consumer<Message>> messageListeners = new HashSet<>();
	
	private Set<Consumer<Request>> requestListeners = new HashSet<>();
	
	private HashMap<UUID, ResponseListener> responseListeners = new HashMap<>();
	
	private String outgoingChannel;
	
	private String incomingChannel;
	
	/**
	 * PLEASE refer to this class as a the {@link Pipeline} interface for documentation support.
	 * 
	 * @param plugin The Plugin initializing this pipeline.
	 * @param namespace The identifying group or plugin to create these pipelines under.
	 * @param outgoing The outgoing channel identifier for this pipeline.
	 * @param incoming The incoming channel identifier for this pipeline.
	 * 
	 * @throws IllegalArgumentException - Thrown if plugin, channel or listener is null, or the listener is already registered for this channel.
	 */
	public BukkitPipeline(Plugin plugin, String namespace, String outgoing, String incoming) {
		this.plugin = plugin;
		outgoingChannel = namespace + PipelineDefaults.SPLITTER + outgoing;
		incomingChannel = namespace + PipelineDefaults.SPLITTER + incoming;
		plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, outgoingChannel);
		plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, incomingChannel, new PluginMessageListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onPluginMessageReceived(String channel, Player player, byte[] data) {
				try (ByteArrayInputStream bytes = new ByteArrayInputStream(data)) {
					try (ObjectInputStream input = new ObjectInputStream(bytes)) {
						MessageType type = (MessageType) input.readObject();
						LinkedList<Object> payload = (LinkedList<Object>) input.readObject();
						switch (type) {
						case REQUEST:
							Request request = new Request(player.getUniqueId(), payload, (UUID) input.readObject());
							requestListeners.forEach(listener -> listener.accept(request));
							break;
						case RESPONSE:
							Response response = new Response(player.getUniqueId(), payload, (UUID) input.readObject());
							Iterator<Entry<UUID, ResponseListener>> it = responseListeners.entrySet().iterator();
							while(it.hasNext()) {
								Entry<UUID, ResponseListener> entry = it.next();
								if(entry.getKey().equals(response.getUuid())) {
									it.remove();
									entry.getValue().handle(response);
									break;
								}
							}
							break;
						default:
							Message message = new Message(player.getUniqueId(), payload);
							messageListeners.forEach(listener -> listener.accept(message));
							break;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
			try {
				Iterator<Entry<UUID, ResponseListener>> it = responseListeners.entrySet().iterator();
				while(it.hasNext()) {
					Entry<UUID, ResponseListener> entry = it.next();
					if(entry.getValue().isExpired()) {
						entry.getValue().handle();
						it.remove();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, 0, 20);
	}
	
	public void onMessage(Consumer<Message> messageHandler) {
		messageListeners.add(messageHandler);
	}
	
	public void onRequest(Consumer<Request> requestHandler) {
		requestListeners.add(requestHandler);
	}
	
	public void send(Message message) {
		if(Bukkit.getOnlinePlayers().size() < 1)
			throw new RuntimeException(new NoRoutablePathException());
		Bukkit.getOnlinePlayers().iterator().next().sendPluginMessage(plugin, outgoingChannel, message.toByteArray());
	}
	
	public CompletableFuture<Optional<Response>> request(Request request) {
		if(Bukkit.getOnlinePlayers().size() < 1)
			throw new RuntimeException(new NoRoutablePathException());
		Bukkit.getOnlinePlayers().iterator().next().sendPluginMessage(plugin, outgoingChannel, request.toByteArray());
		responseListeners.put(request.getUuid(), new ResponseListener(new CompletableFuture<>()));
		return responseListeners.get(request.getUuid()).getResponseHandler();
	}
	
	public void respond(Response response) {
		if(Bukkit.getOnlinePlayers().size() < 1)
			throw new RuntimeException(new NoRoutablePathException());
		Bukkit.getOnlinePlayers().iterator().next().sendPluginMessage(plugin, outgoingChannel, response.toByteArray());
	}

}