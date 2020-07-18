package dev.westernpine.pipeline.exceptions;

public class AlreadyStartedListenerException extends Exception {
	
	private static final long serialVersionUID = 1L;

	public AlreadyStartedListenerException() {
		super("The listener has already been started for this request process.");
	}

}
