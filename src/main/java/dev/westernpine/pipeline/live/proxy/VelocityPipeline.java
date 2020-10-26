package dev.westernpine.pipeline.live.proxy;

import java.time.Instant;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent.ForwardResult;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyReloadEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import dev.westernpine.pipeline.Pipeline;
import dev.westernpine.pipeline.api.MessageReceiver;
import dev.westernpine.pipeline.api.MessageRequestReceiver;
import dev.westernpine.pipeline.api.PipelineHandler;
import dev.westernpine.pipeline.api.object.Message;
import dev.westernpine.pipeline.api.object.RequestProcess;
import dev.westernpine.pipeline.exceptions.NoRoutablePathException;
import lombok.SneakyThrows;

@Plugin(id = "pipeline", name = "Pipeline", version = "26", authors = { "WesternPine" })
public class VelocityPipeline implements PipelineHandler {

	private ProxyServer server;
	
	private ExecutorService service;

	private MinecraftChannelIdentifier outgoingChannel;
	private MinecraftChannelIdentifier incomingChannel;

	private MinecraftChannelIdentifier outgoingRequestChannel;
	private MinecraftChannelIdentifier incomingRequestChannel;

	private MinecraftChannelIdentifier outgoingResponseChannel;
	private MinecraftChannelIdentifier incomingResponseChannel;

	private Set<MessageReceiver> messageReceivers = new HashSet<>();

	private Set<MessageRequestReceiver> messageRequestReceivers = new HashSet<>();
	
	private ConcurrentHashMap<Message, Instant> responses = new ConcurrentHashMap<>();
	
	@Inject
	private VelocityPipeline(ProxyServer server) {
		this.server = server;
		Pipeline.setHandler(this);
	}
	
	private void start() {
		server.getChannelRegistrar().register(outgoingChannel = MinecraftChannelIdentifier.create(Pipeline.namespace, Pipeline.serverName));
		server.getChannelRegistrar().register(incomingChannel = MinecraftChannelIdentifier.create(Pipeline.namespace, Pipeline.proxyName));

		server.getChannelRegistrar().register(outgoingRequestChannel = MinecraftChannelIdentifier.create(Pipeline.requestPrefix + Pipeline.namespace, Pipeline.serverName));
		server.getChannelRegistrar().register(incomingRequestChannel = MinecraftChannelIdentifier.create(Pipeline.requestPrefix + Pipeline.namespace, Pipeline.proxyName));

		server.getChannelRegistrar().register(outgoingResponseChannel = MinecraftChannelIdentifier.create(Pipeline.responsePrefix + Pipeline.namespace, Pipeline.serverName));
		server.getChannelRegistrar().register(incomingResponseChannel = MinecraftChannelIdentifier.create(Pipeline.responsePrefix + Pipeline.namespace, Pipeline.proxyName));
		
		startCleaner();
	}
	
	private void stop() {
		clearRegisteredReceivers();
		clearRegisteredRequestReceivers();
		service.shutdownNow();
	}

	@Subscribe
	public void onProxyInitialization(ProxyInitializeEvent event) {
		start();
	}
	
	@Subscribe
	public void onProxyShutdown(ProxyShutdownEvent event) {
		stop();
	}
	
	@Subscribe
	public void onProxyReload(ProxyReloadEvent event) {
		stop();
		start();
	}

	@Subscribe
	public void onPluginMessage(PluginMessageEvent event) {
		if (event.getIdentifier().getId().equals(incomingChannel.getId())) {
			Message message = new Message(event.getData());
			event.setResult(ForwardResult.handled());
			messageReceivers.forEach(receiver -> receiver.onMessageReceived(message.clone()));
		}
		
		if (event.getIdentifier().getId().equals(incomingRequestChannel.getId())) {
			Message message = new Message(event.getData());
			if(!message.hasContent())
				return;
			Object first = message.read();
			if(!(first instanceof UUID))
				return;
        	UUID requestId = (UUID) first;
        	event.setResult(ForwardResult.handled());
        	messageRequestReceivers.forEach(receiver -> receiver.onRequestReceived(requestId, message.clone()));
		}
		
		if (event.getIdentifier().getId().equals(incomingResponseChannel.getId())) {
			event.setResult(ForwardResult.handled());
			synchronized (responses) {
        		responses.put(new Message(event.getData()), Instant.now().plusMillis(Pipeline.responseCacheTime));
        	}
		}
	}
	
	private void startCleaner() {
		(service = Executors.newSingleThreadExecutor()).submit(new Runnable() {
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
	public void clearRegisteredReceivers() {
		messageReceivers.clear();
	}

	@Override
	public void registerRequestReceiver(MessageRequestReceiver receiver) {
		messageRequestReceivers.add(receiver);
		
	}

	@Override
	public void clearRegisteredRequestReceivers() {
		messageRequestReceivers.clear();
	}

	@SneakyThrows
	@Override
	public void sendAndForget(Message message) {
		Optional<Player> player = server.getPlayer(message.getCarrier());
		if (player.isPresent()) {
			Optional<ServerConnection> server = player.get().getCurrentServer();
			if (server.isPresent()) {
				server.get().sendPluginMessage(outgoingChannel, message.toByteArray());
				return;
			}
		}
		throw new NoRoutablePathException();
	}

	@SneakyThrows
	@Override
	public RequestProcess request(long timeout, Message message) {
		Optional<Player> player = server.getPlayer(message.getCarrier());
		if (player.isPresent()) {
			Optional<ServerConnection> server = player.get().getCurrentServer();
			if (server.isPresent()) {
				final UUID requestId = UUID.randomUUID();
				
				Future<Message> responseListener =  Executors.newSingleThreadExecutor().submit(() -> {
					server.get().sendPluginMessage(outgoingRequestChannel, message.inject(requestId).toByteArray());
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
		Optional<Player> player = server.getPlayer(message.getCarrier());
		if (player.isPresent()) {
			Optional<ServerConnection> server = player.get().getCurrentServer();
			if (server.isPresent()) {
				server.get().sendPluginMessage(outgoingResponseChannel, message.inject(requestId).toByteArray());
				return;
			}
		}
		throw new NoRoutablePathException();
	}

}
