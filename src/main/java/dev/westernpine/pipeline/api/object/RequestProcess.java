package dev.westernpine.pipeline.api.object;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import lombok.NonNull;

public class RequestProcess {
	
	private Thread safeThread;
	
	private Thread task;
	
	private Consumer<Message> responseHandler = message -> {};
	private Consumer<TimeoutException> timeoutHandler = timeout -> {};
	private Consumer<Exception> exceptionHandler = exception -> {};
	
	public RequestProcess (long timeoutTime, @NonNull Future<Message> response) {
		this.safeThread = Thread.currentThread();
		this.task = new Thread(() -> {
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
	
	public RequestProcess onCompletion(@NonNull Consumer<Message> responseHandler) {
		this.responseHandler = responseHandler;
		return this;
	}
	
	public RequestProcess onTimeout(@NonNull Consumer<TimeoutException> timeoutHandler) {
		this.timeoutHandler = timeoutHandler;
		return this;
	}
	
	public RequestProcess onException(@NonNull Consumer<Exception> exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
		return this;
	}
	
	public void start() {
		task.start();
	}
	
	public void interrupt() {
		task.interrupt();
	}

}
