package org.magic.api.beans;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

public class MagicFormat implements Serializable {

	public enum FORMATS {STANDARD, LEGACY, VINTAGE, MODERN, COMMANDER, PAUPER, PIONEER, BRAWL, FRONTIER}
	public enum AUTHORIZATION {LEGAL, RESTRICTED, BANNED,  NOT_LEGAL}
	
	
	public static String toString(FORMATS f)
	{
		return StringUtils.capitalize(f.name().toLowerCase());
	}

	private static final long serialVersionUID = 1L;
	private String format;

	private AUTHORIZATION formatLegality;

	public MagicFormat() {

	}
	
	public MagicFormat(String format, AUTHORIZATION legality)
	{
		this.format=format;
		formatLegality=legality;
	}
	
	public AUTHORIZATION getFormatLegality() {
		return formatLegality;
	}
	
	public void setFormatLegality(AUTHORIZATION formatLegality) {
		this.formatLegality = formatLegality;
	}
	
	
	public String getFormat() {
		return format;
	}
	


	public void setFormat(String format) {
		this.format = format;
	}
	
	public String toString() {
		return getFormat() + " " + formatLegality;
	}

	@Override
	public int hashCode() {
		return getFormat().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;

		if (this.getClass() != obj.getClass())
			return false;

		return getFormat().equalsIgnoreCase(((MagicFormat) obj).getFormat());

	}

	public void setFormat(FORMATS standard) {
		format = StringUtils.capitalize(standard.name().toLowerCase());
		
	}
}