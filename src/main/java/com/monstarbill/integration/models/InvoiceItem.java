package com.monstarbill.integration.models;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;

import lombok.Data;

@Data
@Entity
@Audited
@Table(schema = "finance")
public class InvoiceItem implements Cloneable {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long invoiceItemId;

	private Long itemId, grnId, taxGroupId, invoiceId, poId;
	private double billQty, rate, amount, taxAmount, totalAmount;
	private String department, itemDescription, itemUom;
	
	private String createdBy, lastModifiedBy;
	
	@CreationTimestamp
    private Date createdDate;

    @UpdateTimestamp
    private Date lastModifiedDate;
	
    @Transient
	private String itemName, description, uom, discount, netAmount;
    
    @Transient
	private String grnNumber;
    
    @Override
    public Object clone() throws CloneNotSupportedException {
    	return super.clone();
    }
    
}
