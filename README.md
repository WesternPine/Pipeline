
![Pipeline](https://github.com/WesternPine/Pipeline/raw/master/src/main/resources/pipeline.svg?raw=true)

[![](https://jitpack.io/v/WesternPine/Pipeline.jpg)](https://jitpack.io/#WesternPine/Pipeline)

# Pipeline

Messaging channel API that covers the most popular versions of modable Minecraft servers and proxies.

## Features

| Feature | Supported |
|---------|-----------|
| Single interface API | Yes |
| Custom Implementation Support | Yes |
| Communication With Various Proxies/Server Types | Yes |
| Requests For Responses | Yes |
| Multiple Any Object Messages | Yes |


## What Is Pipeline?

Pipeline, explained simply, is a simplified single interface that allows for servers and proxies to communicate with eachother using a player's connection in the network. This communication can take place between a proxy, and any varient of servers. Additionally, Pipeline also includes a request feature to request information from a proxy to a server or vice-versa. [Read more about requests here.](#What-Is-A-Request?) And in addition to all of that, you can write any Object that can be casted at the receiving end. And the best part... if Pipeline isn't supported on your build, you can create your own implementation [by reading more here.](#Creating-A-Custom-Implementation)

## Usage

**PLEASE** keep in mind that this project is intended to be used as a standalone plugin, and will be required as an active-running plugin on the Proxy/Server. If the Proxy/Server is not supported, you will need to run a custom implementation of Pipeline.

### Building With Pipeline

[![](https://jitpack.io/v/WesternPine/Pipeline.svg)](https://jitpack.io/#WesternPine/Pipeline)

If you are using a build automation tool, click the shield above to get more help on how to build with Pipeline. Otherwise, you can clone this repository to your projects folder and build the jar, or download the plugin itself to build with your project.

### Creating A Custom Implementation

Creating a custom implementation is rather easy, but it will require you to understand how to use messaging channels for the platform you plan to code this on. Because of the lengthy explination, I will not write a tutorial on how to do this. If you would like an example, feel free to check out any of the [live package classes containing the code for each supported base-platform.](https://github.com/WesternPine/Pipeline/tree/master/src/main/java/dev/westernpine/pipeline/live) The pre-written code should provide some great examples on how you can create your own implementation.

If your platform isn't supported, and you can't code your own implementation, or would like to see support for your platform, please see below on how to contact me.

## Working With Pipeline

Working with pipeline is coded to be extremely easy and versitile. You can check out the [Bukkit example snippits with explinations here](https://github.com/WesternPine/Pipeline/blob/master/src/main/java/dev/westernpine/pipeline/examples/UsageExample.md) to get an idea about how to use Pipeline in your workflow.

## What Is A Request?

In the default implementation, a request is basically just Pipeline sending a Message with a UUID attached. From there, Pipeline will wait for a response with a UUID matching the request UUID. If no response is recieved, it will execute the TimeoutHandler consumer, and if any other errors occured within any of that execution, it will execute the ExceptionHandler consumer.

On the other end of Pipeline, once a Request Message has been received, it executes all RequestHandlers that were specified. It is up to these request handlers to remember the request id UUID when it comes time to respond to the request. If the response does not reach the requesting pipeline before the timeout time from when it was requested, the request will time out.

# Closing

If you encounter any issues, or need to contact me for any reason, please feel free to contact me using the link in my profile, as it will be the fastest method of contact. This also applies for any suggestions, questions, comments, or concerns. I hope this helps!

# License

[MIT](https://choosealicense.com/)
