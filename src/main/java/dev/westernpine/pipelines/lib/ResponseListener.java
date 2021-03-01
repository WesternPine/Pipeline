package dev.westernpine.pipelines.lib;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import dev.westernpine.pipelines.api.Response;

public class ResponseListener {
	
	private CompletableFuture<Optional<Response>> responseHandler;
	
	private Instant creation;
	
	public ResponseListener(CompletableFuture<Optional<Response>> responseHandler) {
		this.responseHandler = responseHandler;
		this.creation = Instant.now();
	}
	
	public CompletableFuture<Optional<Response>> getResponseHandler() {
		return this.responseHandler;
	}
	
	public boolean isExpired() {
		return this.creation.isBefore(Instant.now().minusMillis(PipelineDefaults.CACHE_TIME));
	}
	
	public void handle() {
		this.responseHandler.complete(Optional.empty());
	}
	
	public void handle(Response response) {
		this.responseHandler.complete(Optional.ofNullable(response));
	}

}
