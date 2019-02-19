package org.magic.console;

public class TextResponse extends AbstractResponse<String> {

	private String content;
	
	
	public TextResponse(String content) {
		this.content=content;
	}
	
	
	@Override
	public String show() {
		return content;
	}
}
