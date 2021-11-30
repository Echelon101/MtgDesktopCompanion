package org.magic.api.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.List;

import org.magic.api.beans.enums.TransactionDirection;
import org.magic.api.beans.shop.Contact;
import org.magic.api.interfaces.MTGStockItem;

public class Announce implements Serializable {

	private static final long serialVersionUID = 1L;
	private int id=-1;
	private Contact contact;
	private List<GedEntry<Announce>> images;
	private Date startDate;
	private Date endDate;
	private Date expirationDate;
	private Double totalPrice;
	private Currency currency;
	private String title;
	private String description;
	private List<MTGStockItem> items;
	private TransactionDirection type;
	private boolean updated=false;
	
	public Announce() {
		images = new ArrayList<>();
		items = new ArrayList<>();
	}
	
	public TransactionDirection getType() {
		return type;
	}
	
	public void setType(TransactionDirection type) {
		this.type = type;
	}
	
	public boolean isUpdated() {
		return updated;
	}

	public void setUpdated(boolean updated) {
		this.updated = updated;
	}

	@Override
	public String toString() {
		return getTitle();
	}
	
	public void setId(int id) {
		this.id = id;
	}
	public int getId() {
		return id;
	}
	public Contact getContact() {
		return contact;
	}
	public void setContact(Contact contact) {
		this.contact = contact;
	}
	public List<GedEntry<Announce>> getImages() {
		return images;
	}
	public void setImages(List<GedEntry<Announce>> images) {
		this.images = images;
	}
	public Date getStartDate() {
		return startDate;
	}
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	public Date getEndDate() {
		return endDate;
	}
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
	public Date getExpirationDate() {
		return expirationDate;
	}
	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}
	public Double getTotalPrice() {
		return totalPrice;
	}
	public void setTotalPrice(Double totalPrice) {
		this.totalPrice = totalPrice;
	}
	public Currency getCurrency() {
		return currency;
	}
	public void setCurrency(Currency currency) {
		this.currency = currency;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public List<MTGStockItem> getItems() {
		return items;
	}
	public void setItems(List<MTGStockItem> items) {
		this.items = items;
	}
	
	
	
}
