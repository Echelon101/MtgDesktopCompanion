package org.magic.api.beans;

import org.magic.api.beans.enums.EnumCondition;
import org.magic.api.beans.enums.EnumItems;
import org.magic.api.interfaces.abstracts.AbstractStockItem;

public class SealedStock extends AbstractStockItem<MTGSealedProduct>  {

	private static final long serialVersionUID = 1L;
	
	public SealedStock(){
		setTypeStock(EnumItems.SEALED);
		setCondition(EnumCondition.SEALED);
	}
	
	public SealedStock(MTGSealedProduct p)
	{
		setProduct(p);
	}
	
	@Override
	public void setProduct(MTGSealedProduct product) {
		this.product=product;
		edition = product.getEdition();
		url = product.getUrl();
		setTypeStock(EnumItems.SEALED);
		setCondition(EnumCondition.SEALED);
		setProductName(product.getType() +" "+  product.getEdition().getSet());
	}
	
	@Override
	public String toString() {
		return ""+getId();
	}
	

	
}
