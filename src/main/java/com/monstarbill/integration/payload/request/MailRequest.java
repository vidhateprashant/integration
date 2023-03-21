package com.monstarbill.integration.payload.request;

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
public class MailRequest {
	private String toMail;
	private String ccMail;
	private String subject;
	private String body;
	private Long entityId;
}
