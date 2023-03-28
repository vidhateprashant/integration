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

	@GetMapping("/subsidiary/get-subsidiary-by-integrated-id")
	@Retry(name = "setup-ws")
	@CircuitBreaker(name = "setup-ws", fallbackMethod = "getSubsidiaryByIntegratedIdFallback")
	public Subsidiary getSubsidiaryByIntegratedId(@RequestParam ("integratedId")String integratedId,@RequestParam ("isDeleted") Boolean isDeleted);

	default Subsidiary getSubsidiaryByIntegratedIdFallback(String idintegratedId, Boolean isDeleted, Throwable exception) {
		logger.error("Getting exception from MS to find the subsidiary by integratedId");
		logger.error("Exception : " + exception.getLocalizedMessage());
		return null;
	}

	@GetMapping("/document-sequence/get-document-sequence-numbers")
	@Retry(name = "setup-ws")
	@CircuitBreaker(name = "setup-ws", fallbackMethod = "getDocumentSequenceNamesFallback")
	public String getDocumentSequenceNames(@RequestParam ("transactionalDate")String transactionalDate,@RequestParam ("subsidiaryId") Long subsidiaryId,@RequestParam("formName")String formName,@RequestParam("isDeleted")Boolean isDeleted);

	default String getDocumentSequenceNamesFallback(String transectionalDate, Long subsidiaryId,String formName,Boolean isDeleted, Throwable exception) {
		logger.error("Getting exception from MS to find the document sequence number");
		logger.error("Exception : " + exception.getLocalizedMessage());
		return null;
	}
}
