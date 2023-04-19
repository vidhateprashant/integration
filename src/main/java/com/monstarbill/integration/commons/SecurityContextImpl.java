package com.monstarbill.integration.commons;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class SecurityContextImpl {

	public String getCurrentUserName() {
		HttpServletRequest request = getServletRequest();
		return request.getHeader("X-APP-USER");
	}
	
	private HttpServletRequest getServletRequest() {
		return RequestContextHolder.getRequestAttributes() != null
				? ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest()
				: null;
	}
	
}
