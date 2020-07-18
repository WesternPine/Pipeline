package dev.westernpine.pipeline.exceptions;

public class NoRoutablePathException extends Exception {
	
	private static final long serialVersionUID = 1L;

	public NoRoutablePathException() {
		super("Failed to find a suitable path to route the message.");
	}

}
