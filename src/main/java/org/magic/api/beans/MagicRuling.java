package org.magic.api.beans;

import java.io.Serializable;

public class MagicRuling implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String date;
	String text;

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	@Override
	public String toString() {
		return date + " :" + text + "\n";
	}

}
