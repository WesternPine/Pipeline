The following code was using the Bukkit platform, but differences are so minute that you should be able to figure it out.

The following code you must execute in the "onProxyInitializationEvent" for Velocity, "onEnable" for BungeeCord and Bukkit, "onGamePreInitializationEvent" for sponge, and so on.
Performing these actions in the post loading, enabling methods will ensure that both Pipeline and your code has been configured properly and is ready to be run.

If you plan on creating your own implementation, you will need the line of code below.
```Pipeline.setHandler(new CustomPipelineHandler());```

Registering receivers should also take place in the post loading, inside the enable methods.

```
//Register a regular message receiver.
Pipeline.registerReceiver(new MessageReceiver() {
	@Override
	public void onMessageReceived(Message message) {
		System.out.println(message.read().toString());
	}
});


//Register a response receiver.
Pipeline.registerRequestReceiver(new MessageRequestReceiver() {
	@Override
	public void onRequestReceived(UUID requestId, Message message) {
		Pipeline.respond(requestId, new Message(message.getCarrier()).write("Pong!"));
	}
});
```
		
Below is the code used to send messages and make requests. 
In the examples below, a random player is used as the carrier to send messages using it's connection to the proxy/server.

```
//Send a message
Bukkit.getOnlinePlayers().stream().findAny().ifPresent(player -> Pipeline.sendAndForget(new Message(player.getUniqueId()).write("Test")));

//Send a request to timeout after 5000 ms
Bukkit.getOnlinePlayers().stream().findAny().ifPresent(player -> Pipeline.request(5000, new Message(player.getUniqueId()).write("Test"))
	.onCompletion(message -> System.out.println(message.read().toString())) //Optional
	.onTimeout(timeout -> System.out.println("Request timed out!"))//Optional
	.onException(exception -> exception.printStackTrace())//Optional
	.start()); //NEEDED AT END
```
				
If handled correctly, you can just use the proxy as a router for your messages and send messages and requests directly to other servers with ease.