package com.monstarbill.integration.feignclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.monstarbill.integration.models.Subsidiary;
import com.monstarbill.integration.models.TaxGroup;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@FeignClient(name = "setup-ws")
public interface SetupServiceClient {

	Logger logger = LoggerFactory.getLogger(MasterServiceClient.class);
	@GetMapping("/subsidiary/get")
	@Retry(name = "setup-ws")
	@CircuitBreaker(name = "setup-ws", fallbackMethod = "findSubsidiaryByIdFallback")
	public Subsidiary findSubsidiaryById(@RequestParam ("id")Long id);

	default Subsidiary findSubsidiaryByIdFallback(Long id, Throwable exception) {
		logger.error("Getting exception from MS to find the subsidiary");
		logger.error("Exception : " + exception.getLocalizedMessage());
		return null;
	}
	
	@GetMapping("/tax-group/get")
	@Retry(name = "setup-ws")
	@CircuitBreaker(name = "setup-ws", fallbackMethod = "findTaxGroupByIdFallback")
	public TaxGroup findTaxGroupById(@RequestParam ("id") Long id);
	
	default TaxGroup findTaxGroupByIdFallback(Long id,Throwable exception) {
		logger.error("Getting exception from MS to find the TaxGroup");
		logger.error("Exception : " + exception.getLocalizedMessage());
		return null;
	}
}
