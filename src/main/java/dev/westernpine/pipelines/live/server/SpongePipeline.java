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
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.spongepowered.api.Game;
import org.spongepowered.api.Platform;
import org.spongepowered.api.network.ChannelBinding.RawDataChannel;
import org.spongepowered.api.network.ChannelRegistrationException;
import org.spongepowered.api.network.PlayerConnection;

import dev.westernpine.pipelines.api.Message;
import dev.westernpine.pipelines.api.MessageType;
import dev.westernpine.pipelines.api.Pipeline;
import dev.westernpine.pipelines.api.Request;
import dev.westernpine.pipelines.api.Response;
import dev.westernpine.pipelines.lib.NoRoutablePathException;
import dev.westernpine.pipelines.lib.PipelineDefaults;
import dev.westernpine.pipelines.lib.ResponseListener;

public class SpongePipeline implements Pipeline {
	
	private Game game;
	
	private Set<Consumer<Message>> messageListeners = new HashSet<>();
	
	private Set<Consumer<Request>> requestListeners = new HashSet<>();
	
	private HashMap<UUID, ResponseListener> responseListeners = new HashMap<>();
	
	private RawDataChannel outgoingChannel;
	
	@SuppressWarnings("unused")
	private RawDataChannel incomingChannel;
	
	/**
	 * PLEASE refer to this class as a the {@link Pipeline} interface for documentation support.
	 * 
	 * @param game The Game initializing this pipeline.
	 * @param namespace The identifying group or plugin to create these pipelines under.
	 * @param outgoing The outgoing channel identifier for this pipeline.
	 * @param incoming The incoming channel identifier for this pipeline.
	 * 
	 * @throws ChannelRegistrationException - Thrown if the channel name is too long, or reserved.
	 */
	@SuppressWarnings("unchecked")
	public SpongePipeline(Game game, String namespace, String outgoing, String incoming) {
		this.game = game;
		String outgoingChannel = namespace + PipelineDefaults.SPLITTER + outgoing;
		String incomingChannel = namespace + PipelineDefaults.SPLITTER + incoming;
		this.outgoingChannel = game.getChannelRegistrar().createRawChannel(game, outgoingChannel);
		(this.incomingChannel = game.getChannelRegistrar().createRawChannel(game, incomingChannel)).addListener(Platform.Type.SERVER, (buffer, connection, side) -> {
			try (ByteArrayInputStream bytes = new ByteArrayInputStream(buffer.readBytes(buffer.available()))) {
				try (ObjectInputStream input = new ObjectInputStream(bytes)) {
					MessageType type = (MessageType) input.readObject();
					LinkedList<Object> payload = (LinkedList<Object>) input.readObject();
					switch (type) {
					case REQUEST:
						Request request = new Request(((PlayerConnection)connection).getPlayer().getUniqueId(), payload, (UUID) input.readObject());
						requestListeners.forEach(listener -> listener.accept(request));
						break;
					case RESPONSE:
						Response response = new Response(((PlayerConnection)connection).getPlayer().getUniqueId(), payload, (UUID) input.readObject());
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
						Message message = new Message(((PlayerConnection)connection).getPlayer().getUniqueId(), payload);
						messageListeners.forEach(listener -> listener.accept(message));
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		game.getScheduler().createTaskBuilder().interval(1, TimeUnit.SECONDS).execute(() -> {
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
		}).submit(game);
	}
	
	public void onMessage(Consumer<Message> messageHandler) {
		messageListeners.add(messageHandler);
	}
	
	public void onRequest(Consumer<Request> requestHandler) {
		requestListeners.add(requestHandler);
	}
	
	public void send(Message message) {
		if(game.getServer().getOnlinePlayers().size() < 1)
			throw new RuntimeException(new NoRoutablePathException());
		outgoingChannel.sendTo(game.getServer().getOnlinePlayers().iterator().next(), buffer -> buffer.writeBytes(message.toByteArray()));
	}
	
	public CompletableFuture<Optional<Response>> request(Request request) {
		if(game.getServer().getOnlinePlayers().size() < 1)
			throw new RuntimeException(new NoRoutablePathException());
		outgoingChannel.sendTo(game.getServer().getOnlinePlayers().iterator().next(), buffer -> buffer.writeBytes(request.toByteArray()));
		responseListeners.put(request.getUuid(), new ResponseListener(new CompletableFuture<>()));
		return responseListeners.get(request.getUuid()).getResponseHandler();
	}
	
	public void respond(Response response) {
		if(game.getServer().getOnlinePlayers().size() < 1)
			throw new RuntimeException(new NoRoutablePathException());
		outgoingChannel.sendTo(game.getServer().getOnlinePlayers().iterator().next(), buffer -> buffer.writeBytes(response.toByteArray()));
	}

}