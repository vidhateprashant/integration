package com.monstarbill.integration.payload.request;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Builder
public class NetsuiteValueReturn {


	private String name;
	private String type;
	private String referenceNumber;
	private String exportedStatus;
	private Date createddate;
	private Long mblId;
	private String remarks;

}
