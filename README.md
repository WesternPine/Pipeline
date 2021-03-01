![Pipeline](https://github.com/WesternPine/Pipeline/raw/master/src/main/resources/pipeline.jpg?raw=true)

[**Download Latest Release**](https://github.com/WesternPine/Pipeline/releases)

# Pipeline

Pipeline is a message channel API that covers the most popular versions of modable Minecraft servers and proxies.

## Features

| Feature | Supported |
|---------|-----------|
| Single interface API | Yes |
| Custom Implementation Support | Yes |
| Communication With Various Proxies/Server Types | Yes |
| Requests For Responses | Yes |
| Multiple Any Object Messages | Yes |


## What Is Pipeline?

Pipeline, is a simplified single interface that allows for servers and proxies to communicate with eachother using a player's connection in the network. This communication can take place between any proxy, and any variant of servers. Additionally, Pipeline also includes a request feature to request information from a proxy to a server or vice-versa. [Read more about requests here.](#What-Is-A-Request?) And in addition to all of that, you can write any Object that can be casted at the receiving end. And the best part... if Pipeline isn't supported on your build, you can create your own implementation [by reading more here.](#Creating-A-Custom-Implementation)

## Usage

As of version 2.0.0, Pipeline is no longer a stand-alone plugin!
Pipeline is required to be loaded into the JVM via inside your jar (shaded [Uber Jar]), or inside another plugin on the same JVM instance.
More details on how to do this are below.

### Building With Pipeline

[![](https://jitpack.io/v/WesternPine/Pipeline.svg)](https://jitpack.io/#WesternPine/Pipeline)

For gradle and maven users, you can visit JitPack above to find the dependency information you need. Please keep in mind that you need to have the jar present in the JVM, and that Pipeline is no longer a standalone plugin.

### Creating A Custom Implementation

Creating a custom implementation is rather easy, but it will require you to understand how to use messaging channels for the platform you plan to code this on. You can check out the premade live implementations to get an idea of how things are supposed to function.

If your platform isn't supported, and you can't code your own implementation, or would like to see support for your platform, please contact me via my github profile.

## Working With Pipeline

Working with pipeline is coded to be extremely easy and versatile.
Pipelines are NOT compatable with non-pipeline channels. Pipeline formats it's data in a particular maner with each message type.
To start, you need to register a new Pipeline object, when you're supposed to register plugin messaging channels for your platform.

 - Velocity -> `ProxyInitializeEvent`
 - BungeeCord -> `onEnable`
 - Sponge -> `GamePreInitializationEvent`
 - Bukkit -> `onEnable`

To register your a new Pipeline object, you must pass in the plugin instance, and for velocity, the proxyserver instance.

 - Velocity -> `Pipeline testLine = new VelocityPipeline(pluginObject, proxyServer, "myplugin", "outgoing", "incoming");`
 - BungeeCord -> `Pipeline testLine = new BungeeCordPipeline(plugin, "myplugin", "outgoing", "incoming");`
 - Sponge -> `Pipeline testLine = new SpongePipeline(game, "myplugin", "outgoing", "incoming");`
 - Bukkit -> `Pipeline testLine = new BukkitPipeline(plugin, "myplugin", "outgoing", "incoming");`

Please remember that to receive requests and responses, you must register the pipeline on both the server and the proxy.
Now that your pipelines are all set up and registered, it's easier than ever send and receive data.

 - Sending messages -> `new Message(player.getUniqueId()).append("Hello World!").send(testLine);`
 - Handling messages received -> `testLine.onMessage(message -> System.out.println(message.read(String.class)));`
 - Sending requests and handling responses -> `request.send(testLine).get().ifPresent(response -> System.out.println(response.read(String.class)));`
 - Handling requests received & sending responses -> `testLine.onRequest(request -> request.toResponse().append("Hello World!").send(testLine));`

Please note that Objects you write to messages MUST be serializable (use common sense for objects).
Obviously you won't be able to send a `Player` object in a plugin message.
UUID, List<String>, etc. can be written to and read from these messages though.

Notice the `#get()` method after sending a request. Sending requests returns a completable future, so you can wait for the response, or you can keep running code and handle the response later. CompletableFuture returns an Optional of the response. If the optional is empty, this means that the request timed out, otherwise your response should be there.

You can very easily scale this up to as far as needed. 
There are more methods in the Pipeline interface and message objects that can provide you with more options for sending and receiving, the code shown here was just to demonstrate the functionality availible to you.

## What Is A Request?

In the default implementation, a request is basically just Pipeline sending a Message with a UUID attached. From there, Pipeline will wait for a response with a UUID matching the request UUID. If no response is recieved, it will complete the response listener with an empty optional, otherwise it should complete with an optional of the response.

On the other end of Pipeline, once a Request Message has been received, all request handlers are executed for that pipeline. It is up to these request handlers to remember the request id UUID when it comes time to respond to the request. If the response does not reach the requesting pipeline before the timeout time from when the request was sent, the request will time out.

# Closing

If you encounter any issues, or need to contact me for any reason, please feel free to do so via my github profile, as it will be the fastest method of contact. This also applies for any suggestions, questions, comments, or concerns. I hope this helps!

# License

[MIT](https://choosealicense.com/)
