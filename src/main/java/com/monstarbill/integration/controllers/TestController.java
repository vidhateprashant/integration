package com.monstarbill.integration.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/integration")
public class TestController {
	@GetMapping("/status/check")
	public String getStatus() {
		return "working good from integration...";
	}
	
//	@GetMapping("/status/check-comm")
//	public ResponseEntity<List<String>> getStatusComm() {
//		// 2. Communication using Feign Client (Declarative approach)
//		List<String> data = new ArrayList<String>();
//		data = this.setupServiceClient.getStatusList(1, "aaa");
//		return new ResponseEntity<List<String>>(data, HttpStatus.OK);
//	}
	
}
