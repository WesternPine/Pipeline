package dev.westernpine.pipelines.live.proxy;

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
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent.ForwardResult;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import dev.westernpine.pipelines.api.Message;
import dev.westernpine.pipelines.api.MessageType;
import dev.westernpine.pipelines.api.Pipeline;
import dev.westernpine.pipelines.api.Request;
import dev.westernpine.pipelines.api.Response;
import dev.westernpine.pipelines.lib.NoRoutablePathException;
import dev.westernpine.pipelines.lib.ResponseListener;

public class VelocityPipeline implements Pipeline {
	
	@SuppressWarnings("unused")
	private Object plugin;
	
	private ProxyServer proxyServer;
	
	private Set<Consumer<Message>> messageListeners = new HashSet<>();
	
	private Set<Consumer<Request>> requestListeners = new HashSet<>();
	
	private HashMap<UUID, ResponseListener> responseListeners = new HashMap<>();
	
	private MinecraftChannelIdentifier outgoingChannel;
	
	private MinecraftChannelIdentifier incomingChannel;
	
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
	public VelocityPipeline(Object plugin, ProxyServer proxyServer, String namespace, String outgoing, String incoming) {
		this.plugin = plugin;
		this.proxyServer = proxyServer;
		proxyServer.getChannelRegistrar().register(outgoingChannel = MinecraftChannelIdentifier.create(namespace, outgoing));
		proxyServer.getChannelRegistrar().register(incomingChannel = MinecraftChannelIdentifier.create(namespace, incoming));
		
		proxyServer.getEventManager().register(plugin, this);
		
		proxyServer.getScheduler().buildTask(plugin, () -> {
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
		}).repeat(1, TimeUnit.SECONDS).schedule();
	}
	
	@SuppressWarnings("unchecked")
	@Subscribe
	public void onPluginMessage(PluginMessageEvent event) {
		if(event.getIdentifier().getId().equals(incomingChannel.getId())) {
			event.setResult(ForwardResult.handled());
			try (ByteArrayInputStream bytes = new ByteArrayInputStream(event.getData())) {
				try (ObjectInputStream input = new ObjectInputStream(bytes)) {
					MessageType type = (MessageType) input.readObject();
					LinkedList<Object> payload = (LinkedList<Object>) input.readObject();
					switch (type) {
					case REQUEST:
						Request request = new Request(((Player)event.getTarget()).getUniqueId(), payload, (UUID) input.readObject());
						requestListeners.forEach(listener -> listener.accept(request));
						break;
					case RESPONSE:
						Response response = new Response(((Player)event.getTarget()).getUniqueId(), payload, (UUID) input.readObject());
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
						Message message = new Message(((Player)event.getTarget()).getUniqueId(), payload);
						messageListeners.forEach(listener -> listener.accept(message));
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void onMessage(Consumer<Message> messageHandler) {
		messageListeners.add(messageHandler);
	}
	
	public void onRequest(Consumer<Request> requestHandler) {
		requestListeners.add(requestHandler);
	}
	
	public void send(Message message) {
		if(proxyServer.getAllPlayers().size() < 1)
			throw new RuntimeException(new NoRoutablePathException());
		proxyServer.getPlayer(message.getCarrier()).get().getCurrentServer().ifPresent(server -> server.sendPluginMessage(outgoingChannel, message.toByteArray()));
	}
	
	public CompletableFuture<Optional<Response>> request(Request request) {
		if(proxyServer.getAllPlayers().size() < 1)
			throw new RuntimeException(new NoRoutablePathException());
		proxyServer.getPlayer(request.getCarrier()).get().getCurrentServer().ifPresent(server -> server.sendPluginMessage(outgoingChannel, request.toByteArray()));
		responseListeners.put(request.getUuid(), new ResponseListener(new CompletableFuture<>()));
		return responseListeners.get(request.getUuid()).getResponseHandler();
	}
	
	public void respond(Response response) {
		if(proxyServer.getAllPlayers().size() < 1)
			throw new RuntimeException(new NoRoutablePathException());
		proxyServer.getPlayer(response.getCarrier()).get().getCurrentServer().ifPresent(server -> server.sendPluginMessage(outgoingChannel, response.toByteArray()));
	}

}