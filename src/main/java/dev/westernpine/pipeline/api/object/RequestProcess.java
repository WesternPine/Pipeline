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
	
	/**
	 * A request process that will be represented by a new thread.
	 * The new thread will act as a wrapper for the request's Future response result.
	 * If the request has been responded to, it will complete any current responseHandler consumer.
	 * If the request times out, then it will complete any current timeoutHandler consumer.
	 * And if the request encounters any other exception, then it will complete any current exceptionHandler consumer.
	 * 
	 * @param timeoutTime The time in milliseconds before the request response listener times out.
	 * @param response The future response, if completed before timing out.
	 */
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
	
	/**
	 * What should happen if the response returns before the timeout.
	 * 
	 * @param responseHandler How to handle the response message.
	 * @return The current process for streamlining of code.
	 * 
	 * @throws AlreadyStartedListenerException If the listener is running.
	 */
	@SneakyThrows
	public RequestProcess onCompletion(@NonNull Consumer<Message> responseHandler) {
		if(listener.isAlive())
			throw new AlreadyStartedListenerException();
		this.responseHandler = responseHandler;
		return this;
	}

	/**
	 * What should happen if the request times out.
	 * 
	 * @param timeoutHandler How to handle the timeout.
	 * @return The current process for streamlining of code.
	 * 
	 * @throws AlreadyStartedListenerException If the listener is running.
	 */
	@SneakyThrows
	public RequestProcess onTimeout(@NonNull Consumer<TimeoutException> timeoutHandler) {
		if(listener.isAlive())
			throw new AlreadyStartedListenerException();
		this.timeoutHandler = timeoutHandler;
		return this;
	}
	
	/**
	 * What should happen if the request encounters an exception.
	 * 
	 * @param exceptionHandler How to handle the exception.
	 * @return The current process for streamlining of code.
	 * 
	 * @throws AlreadyStartedListenerException If the listener is running.
	 */
	@SneakyThrows
	public RequestProcess onException(@NonNull Consumer<Exception> exceptionHandler) {
		if(listener.isAlive())
			throw new AlreadyStartedListenerException();
		this.exceptionHandler = exceptionHandler;
		return this;
	}
	
	/**
	 * Start listening for the response.
	 * 
	 * @throws AlreadyStartedListenerException If the listener is running.
	 */
	@SneakyThrows
	public void start() {
		if(listener.isAlive())
			throw new AlreadyStartedListenerException();
		listener.start();
	}
	
	/**
	 * Cancel the response listener.
	 */
	public void interrupt() {
		listener.interrupt();
	}

}
