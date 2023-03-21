package com.monstarbill.integration.feignclient;

import java.sql.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.monstarbill.integration.models.Invoice;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@FeignClient(name = "finance-ws")
public interface FinanceServiceClient {

	Logger logger = LoggerFactory.getLogger(MasterServiceClient.class);

	@GetMapping("invoice/get-subsidiary-id-and-date-between")
	@Retry(name = "finance-ws")
	@CircuitBreaker(name = "finance-ws", fallbackMethod = "findByIdAndCreatedDateBetweenFallback")
	public List<Invoice> findByIdAndIntegratedIdAndCreatedDateBetween(@RequestParam ("subsidiaryId")Long subsidiaryId, @RequestParam ("startDate")Date startDate, @RequestParam ("endDate")Date endDate);

	default List<Invoice> findByIdAndCreatedDateBetweenFallback(Long subsidiaryId, Date startDate, Date endDate ,Throwable exception) {
		logger.error("Getting exception from MS to find the Invoice");
		logger.error("Exception : " + exception.getLocalizedMessage());
		return null;
	}

	@GetMapping("/invoice/get")
	@Retry(name = "finance-ws")
	@CircuitBreaker(name = "finance-ws", fallbackMethod = "getInvoiceByIdFallback")
	public Invoice getInvoiceById(@RequestParam ("invoiceId")Long invoiceId);

	default Invoice getInvoiceByIdFallback(Long invoiceId,Throwable exception) {
		logger.error("Getting exception from MS to find the Invoice");
		logger.error("Exception : " + exception.getLocalizedMessage());
		return null;
	}

	@PostMapping("/invoice/save")
	@Retry(name = "finance-ws")
	@CircuitBreaker(name = "finance-ws", fallbackMethod = "saveInvoiceFallback")
	public Invoice saveInvoice(@RequestBody Invoice invoice);

	default Invoice saveInvoiceFallback(Invoice invoice,Throwable exception) {
		logger.error("Getting exception from MS to save the Invoice");
		logger.error("Exception : " + exception.getLocalizedMessage());
		return null;
	}
}
