package dev.westernpine.pipelines.lib;

public class EmptyMessageException extends Exception {
	
	private static final long serialVersionUID = 1L;

	public EmptyMessageException() {
		super("The message payload is empty!");
	}

}
