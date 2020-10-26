package dev.westernpine.pipeline.live.server;

import java.time.Instant;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import dev.westernpine.pipeline.Pipeline;
import dev.westernpine.pipeline.api.MessageReceiver;
import dev.westernpine.pipeline.api.MessageRequestReceiver;
import dev.westernpine.pipeline.api.PipelineHandler;
import dev.westernpine.pipeline.api.object.Message;
import dev.westernpine.pipeline.api.object.RequestProcess;
import dev.westernpine.pipeline.exceptions.NoRoutablePathException;
import lombok.SneakyThrows;

public class BukkitPipeline extends JavaPlugin implements PipelineHandler {
	
	private BukkitPipeline instance;
	
	private ExecutorService service;
	
	private String outgoingChannel = Pipeline.namespace + Pipeline.splitter + Pipeline.proxyName;
	private String outgoingRequestChannel = Pipeline.requestPrefix + Pipeline.namespace + Pipeline.splitter + Pipeline.proxyName;
	private String outgoingResponseChannel = Pipeline.responsePrefix + Pipeline.namespace + Pipeline.splitter + Pipeline.proxyName;
	
	private Set<MessageReceiver> messageReceivers = new HashSet<>();

	private Set<MessageRequestReceiver> messageRequestReceivers = new HashSet<>();
	
	private ConcurrentHashMap<Message, Instant> responses = new ConcurrentHashMap<>();
	
	public BukkitPipeline() {
		instance = this;
		Pipeline.setHandler(this);
	}
	
	public void start() {
		getServer().getMessenger().registerOutgoingPluginChannel(this, outgoingChannel);
        getServer().getMessenger().registerIncomingPluginChannel(this, Pipeline.namespace + Pipeline.splitter + Pipeline.serverName, new MessageReceivedListener());
        
        getServer().getMessenger().registerOutgoingPluginChannel(this, outgoingRequestChannel);
        getServer().getMessenger().registerIncomingPluginChannel(this, Pipeline.requestPrefix + Pipeline.namespace + Pipeline.splitter + Pipeline.serverName, new RequestReceivedListener());
        
        getServer().getMessenger().registerOutgoingPluginChannel(this, outgoingResponseChannel);
        getServer().getMessenger().registerIncomingPluginChannel(this, Pipeline.responsePrefix + Pipeline.namespace + Pipeline.splitter + Pipeline.serverName, new ResponseReceivedListener());
        
        startCleaner();
	}
	
	public void stop() {
		clearRegisteredReceivers();
		clearRegisteredRequestReceivers();
		service.shutdownNow();
	}
	
	@Override
	public void onLoad() {
		
	}
	
	@Override
	public void onEnable() {
        start();
	}
	
	@Override
	public void onDisable() {
		stop();
	}
	
	class MessageReceivedListener implements PluginMessageListener {
		@Override
	    public void onPluginMessageReceived(String channel, Player player, byte[] data) {
	    	Message message = new Message(data);
	        messageReceivers.forEach(receiver -> receiver.onMessageReceived(message.clone()));
	    }
	}
	
	class RequestReceivedListener implements PluginMessageListener {
		@Override
        public void onPluginMessageReceived(String channel, Player player, byte[] data) {
			Message message = new Message(data);
			if(!message.hasContent())
				return;
			Object first = message.read();
			if(!(first instanceof UUID))
				return;
        	UUID requestId = (UUID) first;
        	messageRequestReceivers.forEach(receiver -> receiver.onRequestReceived(requestId, message.clone()));
        }
	}
	
	class ResponseReceivedListener implements PluginMessageListener {
		@Override
        public void onPluginMessageReceived(String channel, Player player, byte[] data) {
        	synchronized (responses) {
        		responses.put(new Message(data), Instant.now().plusMillis(Pipeline.responseCacheTime));
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
								if(entry.getValue().isBefore(Instant.now())) {
									it.remove();
								}
							}
						}
					} catch (Exception e) {
						System.out.println("Failed to identify responses.");
						e.printStackTrace();
					}
					
					try {Thread.sleep(1);} catch (InterruptedException e) {e.printStackTrace();}
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
		if (getServer().getOnlinePlayers().size() > 0) {
            Player player = (Player) getServer().getPlayer(message.getCarrier());
            if (player != null) {
            	player.sendPluginMessage(instance, outgoingChannel, message.toByteArray());
				return;
            }
		}
		throw new NoRoutablePathException();
	}
	
	@SneakyThrows
	@Override
	public RequestProcess request(long timeout, Message message) {
		if (getServer().getOnlinePlayers().size() > 0) {
			Player player = (Player) getServer().getPlayer(message.getCarrier());
			if (player != null) {
				final UUID requestId = UUID.randomUUID();
				
				Future<Message> responseListener =  Executors.newSingleThreadExecutor().submit(() -> {
					player.sendPluginMessage(this, outgoingRequestChannel, message.inject(requestId).toByteArray());
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
		if (getServer().getOnlinePlayers().size() > 0) {
			Player player = (Player) getServer().getPlayer(message.getCarrier());
			if (player != null) {
				player.sendPluginMessage(this, outgoingResponseChannel, message.inject(requestId).toByteArray());
				return;
			}
		}
		throw new NoRoutablePathException();
	}
}