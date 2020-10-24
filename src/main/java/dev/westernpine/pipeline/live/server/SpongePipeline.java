package dev.westernpine.pipeline.live.server;

import java.time.Instant;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.spongepowered.api.Game;
import org.spongepowered.api.Platform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameConstructionEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.network.ChannelBinding.RawDataChannel;
import org.spongepowered.api.plugin.Plugin;

import com.google.inject.Inject;

import dev.westernpine.pipeline.Pipeline;
import dev.westernpine.pipeline.api.MessageReceiver;
import dev.westernpine.pipeline.api.MessageRequestReceiver;
import dev.westernpine.pipeline.api.PipelineHandler;
import dev.westernpine.pipeline.api.object.Message;
import dev.westernpine.pipeline.api.object.RequestProcess;
import dev.westernpine.pipeline.exceptions.NoRoutablePathException;
import lombok.SneakyThrows;

@Plugin(id = "pipeline", name = "Pipeline", version = "23", authors = { "WesternPine" })
public class SpongePipeline implements PipelineHandler {
	
    @Inject
    private Game game;
    
    private RawDataChannel outgoingChannel;
    private RawDataChannel outgoingRequestChannel;
    private RawDataChannel outgoingResponseChannel;

	private Set<MessageReceiver> messageReceivers = new HashSet<>();

	private Set<MessageRequestReceiver> messageRequestReceivers = new HashSet<>();
	
	private ConcurrentHashMap<Message, Instant> responses = new ConcurrentHashMap<>();
	
	@Listener
    public void onGameConstruction(GameConstructionEvent event) {
		Pipeline.setHandler(this);
	}
	
	 @Listener
	    public void onGamePreInitialization(GamePreInitializationEvent event) {
			this.outgoingChannel = game.getChannelRegistrar().createRawChannel(this, Pipeline.namespace + Pipeline.splitter + Pipeline.proxyName);
			game.getChannelRegistrar().createRawChannel(this, Pipeline.namespace + Pipeline.splitter + Pipeline.serverName).addListener(Platform.Type.SERVER,
					(buffer, connection, side) -> {
						Message message = new Message(buffer.readBytes(buffer.available()));
						messageReceivers.forEach(receiver -> receiver.onMessageReceived(message.clone()));
					});
			
			this.outgoingRequestChannel = game.getChannelRegistrar().createRawChannel(this, Pipeline.requestPrefix + Pipeline.namespace + Pipeline.splitter + Pipeline.proxyName);
			game.getChannelRegistrar().createRawChannel(this, Pipeline.requestPrefix + Pipeline.namespace + Pipeline.splitter + Pipeline.serverName).addListener(Platform.Type.SERVER,
					(buffer, connection, side) -> {
						Message message = new Message(buffer.readBytes(buffer.available()));
						if(!message.hasContent())
							return;
						Object first = message.read();
						if(!(first instanceof UUID))
							return;
			        	UUID requestId = (UUID) first;
			        	messageRequestReceivers.forEach(receiver -> receiver.onRequestReceived(requestId, message.clone()));
					});
			
			this.outgoingResponseChannel = game.getChannelRegistrar().createRawChannel(this, Pipeline.responsePrefix + Pipeline.namespace + Pipeline.splitter + Pipeline.proxyName);
			game.getChannelRegistrar().createRawChannel(this, Pipeline.responsePrefix + Pipeline.namespace + Pipeline.splitter + Pipeline.serverName).addListener(Platform.Type.SERVER,
					(buffer, connection, side) -> {
						synchronized (responses) {
			        		responses.put(new Message(buffer.readBytes(buffer.available())), Instant.now().plusMillis(Pipeline.responseCacheTime));
			        	}  
					});
	        
			startCleaner();
	 }
	
	
	
	private void startCleaner() {
		Executors.newSingleThreadExecutor().submit(new Runnable() {
			@Override
			public void run() {
				while(true) {
					try {
						synchronized (responses) {
							Iterator<Entry<Message, Instant>> it = responses.entrySet().iterator();
							while(it.hasNext()) {
								Entry<Message, Instant> entry = it.next();
								if(entry.getValue().isAfter(Instant.now())) {
									it.remove();
								}
							}
						}
					} catch (Exception e) {
						System.out.println("Failed to identify responses.");
						e.printStackTrace();
					}
					
					try {Thread.sleep(1L);} catch (InterruptedException e) {e.printStackTrace();}
				}
			}
        });
	}

	@Override
	public void registerReceiver(MessageReceiver receiver) {
		messageReceivers.add(receiver);
	}

	@Override
	public void registerRequestReceiver(MessageRequestReceiver receiver) {
		messageRequestReceivers.add(receiver);
	}

	@SneakyThrows
	@Override
	public void sendAndForget(Message message) {
		if (game.getServer().getOnlinePlayers().size() > 0) {
			Player player = (Player) game.getServer().getOnlinePlayers().toArray()[0];
			if (player != null) {
				outgoingChannel.sendTo(player, buffer -> buffer.writeBytes(message.toByteArray()));
				return;
			}
		}
		throw new NoRoutablePathException();
	}

	@SneakyThrows
	@Override
	public RequestProcess request(long timeout, Message message) {
		if (game.getServer().getOnlinePlayers().size() > 0) {
			Player player = (Player) game.getServer().getOnlinePlayers().toArray()[0];
			if (player != null) {
				final UUID requestId = UUID.randomUUID();
				
				Future<Message> responseListener =  Executors.newSingleThreadExecutor().submit(() -> {
					outgoingRequestChannel.sendTo(player, buffer -> buffer.writeBytes(message.inject(requestId).toByteArray()));
					while(true && !Thread.interrupted()) {
						synchronized (responses) {
							Iterator<Entry<Message, Instant>> it = responses.entrySet().iterator();
							while(it.hasNext()) {
								Entry<Message, Instant> entry = it.next();
								
								Message msg = entry.getKey();
								Message response = msg.clone();
								
								if(!response.hasContent()) {
									it.remove();
									continue;
								}
								
								Object first = response.read();
								if(!(first instanceof UUID)) {
									it.remove();
									continue;
								}
								
					        	UUID id = (UUID) first;
					        	
					        	if(id.equals(requestId)) {
									it.remove();
									return response;
								}
							}
						}
						Thread.sleep(1);
					}
					return null;
				});
				return new RequestProcess(timeout, responseListener);
				
			}
		}
		throw new NoRoutablePathException();
	}

	@SneakyThrows
	@Override
	public void respond(UUID requestId, Message message) {
		if (game.getServer().getOnlinePlayers().size() > 0) {
			Player player = (Player) game.getServer().getOnlinePlayers().toArray()[0];
			if (player != null) {
				outgoingResponseChannel.sendTo(player, buffer -> buffer.writeBytes(message.inject(requestId).toByteArray()));
				return;
			}
		}
		throw new NoRoutablePathException();
	}

}
