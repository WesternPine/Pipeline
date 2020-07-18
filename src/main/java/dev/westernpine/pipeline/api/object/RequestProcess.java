package dev.westernpine.pipeline.api.object;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import dev.westernpine.pipeline.exceptions.AlreadyStartedListenerException;
import lombok.NonNull;
import lombok.SneakyThrows;

public class RequestProcess {
	
	private Thread safeThread;
	
	private Thread listener;
	
	private Consumer<Message> responseHandler = message -> {};
	private Consumer<TimeoutException> timeoutHandler = timeout -> {};
	private Consumer<Exception> exceptionHandler = exception -> {};
	
	public RequestProcess (long timeoutTime, @NonNull Future<Message> response) {
		this.safeThread = Thread.currentThread();
		this.listener = new Thread(() -> {
			try {
				try {
					Message message = response.get(timeoutTime, TimeUnit.MILLISECONDS);
					synchronized (safeThread) {
						responseHandler.accept(message);
					}
				} catch (TimeoutException timeoutException) {
					response.cancel(true);
					synchronized (safeThread) {
						timeoutHandler.accept(timeoutException);
					}
					return;
				}
			} catch (InterruptedException interrupted){
				response.cancel(true);
			}catch (Exception wrapperException) {
				synchronized (safeThread) {
					exceptionHandler.accept(wrapperException);
				}
			}
		});
	}

	@SneakyThrows
	public RequestProcess onCompletion(@NonNull Consumer<Message> responseHandler) {
		if(listener != null)
			throw new AlreadyStartedListenerException();
		this.responseHandler = responseHandler;
		return this;
	}

	@SneakyThrows
	public RequestProcess onTimeout(@NonNull Consumer<TimeoutException> timeoutHandler) {
		if(listener != null)
			throw new AlreadyStartedListenerException();
		this.timeoutHandler = timeoutHandler;
		return this;
	}
	
	@SneakyThrows
	public RequestProcess onException(@NonNull Consumer<Exception> exceptionHandler) {
		if(listener != null)
			throw new AlreadyStartedListenerException();
		this.exceptionHandler = exceptionHandler;
		return this;
	}

	@SneakyThrows
	public void startListening() {
		if(listener.isAlive())
			throw new AlreadyStartedListenerException();
		listener.start();
	}
	
	public void interrupt() {
		listener.interrupt();
	}

}
