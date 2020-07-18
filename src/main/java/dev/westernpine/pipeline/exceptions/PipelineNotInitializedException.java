package dev.westernpine.pipeline.exceptions;

public class PipelineNotInitializedException extends Exception {
	
	private static final long serialVersionUID = 1L;

	public PipelineNotInitializedException() {
		super("The Pipeline Handler has not been initialized yet!");
	}

}
