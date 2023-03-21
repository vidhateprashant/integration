package com.monstarbill.integration.models;

import java.sql.Timestamp;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(schema="setup",	name = "subsidiary_address")
@ToString
@Audited
@AuditTable("subsidiary_address_aud")
public class SubsidiaryAddress implements Cloneable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "subsidiary_id", nullable = false)
	private Long subsidiaryId;

	@NotBlank(message = "Country is mandatory")
	private String country;

	private String attention;

	private String address;

	private String phone;

	@NotBlank(message = "Address1 is mandatory")
	private String address1;

	private String address2;

	private String city;

	private String state;

	private String zipcode;
	
	@Column(name="is_active", columnDefinition = "boolean default false")
	private boolean isActive;
	
	@Column(name="is_deleted", columnDefinition = "boolean default false")
	private boolean isDeleted;

	@Column(name = "registration_code")
	private String registrationCode;

	@Column(name = "registration_type")
	private String registrationType;
	
	@CreationTimestamp
	@Column(name="created_date", updatable = false)
	private Date createdDate;

	@Column(name="created_by")
	private String createdBy;

	@UpdateTimestamp
	@Column(name="last_modified_date")
	private Timestamp lastModifiedDate;

	@Column(name="last_modified_by")
	private String lastModifiedBy;

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

}