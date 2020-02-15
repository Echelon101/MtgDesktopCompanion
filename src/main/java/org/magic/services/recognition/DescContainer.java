package org.magic.services.recognition;

public class DescContainer implements Comparable<DescContainer>{
	

	private ImageDesc descData;
	private String stringData;
	private double match = 0;

	
	public DescContainer(ImageDesc descData, String stringData) {
		this.descData = descData;
		this.stringData = stringData;
	}
	
	
	@Override
	public int compareTo(DescContainer dc) {
		return Double.compare(dc.getMatch(),match);
	}
	
	public String getName()
	{
		return stringData.split("\\|")[0];
	}

	public ImageDesc getDescData() {
		return descData;
	}
	
	public String getStringData() {
		return stringData;
	}

	public void setMatch(double d) {
		match=d;
	}
	
	public double getMatch() {
		return match;
	}
}
