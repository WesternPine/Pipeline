package dev.westernpine.pipeline.live.proxy;

import java.time.Instant;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import dev.westernpine.pipeline.Pipeline;
import dev.westernpine.pipeline.api.MessageReceiver;
import dev.westernpine.pipeline.api.MessageRequestReceiver;
import dev.westernpine.pipeline.api.PipelineHandler;
import dev.westernpine.pipeline.api.object.Message;
import dev.westernpine.pipeline.api.object.RequestProcess;
import dev.westernpine.pipeline.exceptions.NoRoutablePathException;
import lombok.Getter;
import lombok.SneakyThrows;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

public class BungeeCordPipeline extends Plugin implements PipelineHandler, Listener {
	
	@Getter
	private BungeeCordPipeline instance;

	private String outgoingChannel = Pipeline.namespace + Pipeline.splitter + Pipeline.serverName;
	private String incomingChannel = Pipeline.namespace + Pipeline.splitter + Pipeline.proxyName;
	
	private String outgoingRequestChannel = Pipeline.requestPrefix + Pipeline.namespace + Pipeline.splitter + Pipeline.serverName;
	private String incomingRequestChannel = Pipeline.requestPrefix + Pipeline.namespace + Pipeline.splitter + Pipeline.proxyName;
	
	private String outgoingResponseChannel = Pipeline.responsePrefix + Pipeline.namespace + Pipeline.splitter + Pipeline.serverName;
	private String incomingResponseChannel = Pipeline.responsePrefix + Pipeline.namespace + Pipeline.splitter + Pipeline.proxyName;
	
	private Set<MessageReceiver> messageReceivers = new HashSet<>();

	private Set<MessageRequestReceiver> messageRequestReceivers = new HashSet<>();
	
	private ConcurrentHashMap<Message, Instant> responses = new ConcurrentHashMap<>();
	
	@Override
	public void onLoad() {
		instance = this;
		Pipeline.setHandler(this);
	}
	
	@Override
	public void onEnable() {
		getProxy().registerChannel(outgoingChannel);
		getProxy().registerChannel(incomingChannel);

		getProxy().registerChannel(outgoingRequestChannel);
		getProxy().registerChannel(incomingRequestChannel);

		getProxy().registerChannel(outgoingResponseChannel);
		getProxy().registerChannel(incomingResponseChannel);
		
		getProxy().getPluginManager().registerListener(this, this);
		
		startCleaner();
	}
	
	@EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
		if (event.getTag().equals(incomingChannel)) {
			Message message = new Message(event.getData());
			messageReceivers.forEach(receiver -> receiver.onMessageReceived(message.clone()));
		}
		
		if (event.getTag().equals(incomingRequestChannel)) {
			Message message = new Message(event.getData());
			if(!message.hasContent())
				return;
			Object first = message.read();
			if(!(first instanceof UUID))
				return;
        	UUID requestId = (UUID) first;
        	messageRequestReceivers.forEach(receiver -> receiver.onRequestReceived(requestId, message.clone()));
		}
		
		if (event.getTag().equals(incomingResponseChannel)) {
			synchronized (responses) {
        		responses.put(new Message(event.getData()), Instant.now().plusMillis(Pipeline.responseCacheTime));
        	}
		}
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
	public void registerRequestReceiver(MessageRequestReceiver receiver) {
		messageRequestReceivers.add(receiver);
		
	}
	
	@SneakyThrows
	@Override
	public void sendAndForget(Message message) {
		ProxiedPlayer player = getProxy().getPlayer(message.getCarrier());
        if (player != null) {
            player.getServer().sendData(outgoingChannel, message.toByteArray());
            return;
        }
		throw new NoRoutablePathException();
	}

	@SneakyThrows
	@Override
	public RequestProcess request(long timeout, Message message) {
		ProxiedPlayer player = getProxy().getPlayer(message.getCarrier());
        if (player != null) {
        	final UUID requestId = UUID.randomUUID();
            
            Future<Message> responseListener =  Executors.newSingleThreadExecutor().submit(() -> {
            	player.getServer().sendData(outgoingRequestChannel, message.inject(requestId).toByteArray());
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
		throw new NoRoutablePathException();
	}

	@SneakyThrows
	@Override
	public void respond(UUID requestId, Message message) {
		ProxiedPlayer player = getProxy().getPlayer(message.getCarrier());
        if (player != null) {
            player.getServer().sendData(outgoingResponseChannel, message.inject(requestId).toByteArray());
            return;
        }
		throw new NoRoutablePathException();
	}

}
