package com.monstarbill.integration.models;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;

import lombok.Data;

@Data
@Entity
@Audited
@Table(schema = "finance", uniqueConstraints = {@UniqueConstraint(columnNames = {"subsidiaryId", "supplierId", "invoiceNo"})})
public class Invoice implements Cloneable {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long invoiceId;
	
	private Long supplierId, subsidiaryId, poId, locationId, billToId, shipToId;
	private String invoiceNo, invStatus, paymentTerm, integratedId, currency, billTo, shipTo, invoiceCode, invoiceSupplyNumber, taxRegNumber;
	private Date invoiceDate, dueDate;
	private double fxRate, amount, taxAmount, totalAmount, paymentAmount, amountDue;

	@Column(name = "external_id")
	private String externalId;
	
	@Transient
	private boolean hasError;
	
	@Column(name = "approved_by")
	private String approvedBy;
	
	@Column(name = "next_approver")
	private String nextApprover;
	
	@Column(name = "next_approver_role")
	private String nextApproverRole;

	// stores the next approver level i.e. L1,L2,L3 etc.
	@Column(name = "next_approver_level")
	private String nextApproverLevel;
	
	// store's the id of approver preference
	@Column(name = "approver_preference_id")
	private Long approverPreferenceId;
	
	// stores the approver sequence id (useful internally in order to change the approver)
	@Column(name = "approver_sequence_id")
	private Long approverSequenceId;
	
	// stores the max level to approve, after that change status to approve
	@Column(name = "approver_max_level")
	private String approverMaxLevel;
	
	@Column(name = "note_to_approver")
	private String noteToApprover;
	
	@Column(name = "ns_message")
	private String nsMessage;

	@Column(name = "ns_status")
	private String nsStatus;


	private String createdBy, lastModifiedBy;
	
	@CreationTimestamp
    private Date createdDate;

    @UpdateTimestamp
    private Date lastModifiedDate;
    
    @Transient
	private List<InvoiceItem> invoiceItems;
	
}
