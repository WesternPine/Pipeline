package dev.westernpine.pipeline;

import java.util.UUID;

import dev.westernpine.pipeline.api.MessageReceiver;
import dev.westernpine.pipeline.api.MessageRequestReceiver;
import dev.westernpine.pipeline.api.PipelineHandler;
import dev.westernpine.pipeline.api.object.Message;
import dev.westernpine.pipeline.api.object.RequestProcess;
import lombok.Getter;
import lombok.NonNull;

public class Pipeline {
	public static long responseCacheTime = 5000;

	public static final String splitter = ":";
	public static final String requestPrefix = "request";
	public static final String responsePrefix = "response";
	public static final String namespace = "pipeline";
	public static final String proxyName = "proxy";
	public static final String serverName = "server";
	
	@Getter
	private static PipelineHandler handler;
	
	public static void setHandler(@NonNull PipelineHandler pipelineHandler) {
		handler = pipelineHandler;
	}

	public static void registerReceiver(@NonNull MessageReceiver receiver) {
		handler.registerReceiver(receiver);
	}

	public static void registerRequestReceiver(@NonNull MessageRequestReceiver receiver) {
		handler.registerRequestReceiver(receiver);
	}

	public static void sendAndForget(@NonNull Message message) {
		handler.sendAndForget(message);
	}

	public static RequestProcess request(long timeout, @NonNull Message message) {
		return handler.request(timeout, message);
	}

	public static void respond(@NonNull UUID requestId, @NonNull Message message) {
		handler.respond(requestId, message);
	}

}
