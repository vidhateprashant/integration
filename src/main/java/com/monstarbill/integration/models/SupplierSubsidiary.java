package com.monstarbill.integration.models;

import java.sql.Timestamp;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(schema="setup",name = "supplier_subsidiary")
@ToString
@Audited
@AuditTable("supplier_subsidiary_aud")
@EqualsAndHashCode
public class SupplierSubsidiary implements Cloneable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull(message = "Supplier Id is mandatory")
	@Column(name = "supplier_id")
	private Long supplierId;

	@NotNull(message = "Subsidiary Id is mandatory")
	@Column(name = "subsidiary_id")
	private Long subsidiaryId;

	private String currency;
	
	@NotBlank(message = "Supplier currency is mandetory")
	@Column(name = "supplier_currency")
	private String supplierCurrency;
	
	@Column(name="is_preferred_currency", columnDefinition = "boolean default false")
	private boolean isPreferredCurrency;
	
	@Column(name="is_deleted", columnDefinition = "boolean default false")
	private boolean isDeleted;

	@CreationTimestamp
	@Column(name = "created_date", updatable = false)
	private Date createdDate;

	@Column(name = "created_by", updatable = false)
	private String createdBy;

	@UpdateTimestamp
	@Column(name = "last_modified_date")
	private Timestamp lastModifiedDate;

	@Column(name = "last_modified_by")
	private String lastModifiedBy;

	@Transient
	private String subsidiaryName;

	public SupplierSubsidiary(Long id, Long supplierId, Long subsidiaryId, String name, String currency, String supplierCurrency, boolean isPreferredCurrency) {
		this.id = id;
		this.supplierId = supplierId;
		this.subsidiaryId = subsidiaryId;
		this.subsidiaryName = name;
		this.currency = currency;
		this.supplierCurrency = supplierCurrency;
		this.isPreferredCurrency = isPreferredCurrency;
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
}
