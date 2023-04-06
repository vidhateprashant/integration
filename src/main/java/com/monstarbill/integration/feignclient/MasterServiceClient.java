package com.monstarbill.integration.feignclient;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.monstarbill.integration.models.Account;
import com.monstarbill.integration.models.Bank;
import  com.monstarbill.integration.models.Employee;
import com.monstarbill.integration.models.Item;
import com.monstarbill.integration.models.Location;
import com.monstarbill.integration.models.Supplier;
import com.monstarbill.integration.models.SupplierSubsidiary;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@FeignClient(name = "masters-ws")
public interface MasterServiceClient {

	Logger logger = LoggerFactory.getLogger(MasterServiceClient.class);


	@GetMapping("/supplier/get")
	@Retry(name = "masters-ws")
	@CircuitBreaker(name = "masters-ws", fallbackMethod = "findSupplierByIdFallback")
	public Supplier findSupplierById(@RequestParam("id") Long id);

	default Supplier findSupplierByIdFallback(Long id, Throwable exception) {
		logger.error("Getting exception from MS to find the supplier");
		logger.error("Exception : " + exception.getLocalizedMessage());
		return null;
	}


	@PostMapping("/supplier/save")
	@Retry(name = "masters-ws")
	@CircuitBreaker(name = "masters-ws", fallbackMethod = "saveSupplierFallback")
	public Supplier saveSupplier(@Valid @RequestBody Supplier supplier);

	default Supplier saveSupplierFallback(Supplier supplier, Throwable exception) {
		logger.error("Getting exception from MS to save the supplier");
		logger.error("Exception : " + exception.getLocalizedMessage());
		return null;
	}

	@GetMapping("item/find-by-subsidiary-createdDate")
	@Retry(name = "masters-ws")
	@CircuitBreaker(name = "masters-ws", fallbackMethod = "findItemBySubsidiaryAndCreatedDateBetweenFallback")
	public List<Item> findItemBySubsidiaryAndCreatedDateBetween(@RequestParam("SubsidiaryId") Long SubsidiaryId, @RequestParam ("startDate")Date startDate,@RequestParam ("endDate")Date endDate);

	default List<Item> findItemBySubsidiaryAndCreatedDateBetweenFallback(Long SubsidiaryId, Date startDate, Date endDate, Throwable exception) {
		logger.error("Getting exception from MS to find the item");
		logger.error("Exception : " + exception.getLocalizedMessage());
		return null;
	}

	@GetMapping("item/get")
	@Retry(name = "masters-ws")
	@CircuitBreaker(name = "masters-ws", fallbackMethod = "findByIdFallback")
	public Item findById(@RequestParam ("id")Long id);

	default Item findByIdFallback(Long id, Throwable exception) {
		logger.error("Getting exception from MS to find the item");
		logger.error("Exception : " + exception.getLocalizedMessage());
		return null;
	}


	@PostMapping("item/save")
	@Retry(name = "masters-ws")
	@CircuitBreaker(name = "masters-ws", fallbackMethod = "saveItemFallback")
	public Item saveItem(@Valid @RequestBody Item item);

	default Item saveItemFallback(Item item, Throwable exception) {
		logger.error("Getting exception from MS to generating the item");
		logger.error("Exception : " + exception.getLocalizedMessage());
		return null;
	}

	@GetMapping("account/get")
	@Retry(name = "masters-ws")
	@CircuitBreaker(name = "masters-ws", fallbackMethod = "getAccountFallback")
	public Account getAccount(@RequestParam ("id")Long id);

	default Account getAccountFallback(Long id, Throwable exception) {
		logger.error("Getting exception from MS to find the Account");
		logger.error("Exception : " + exception.getLocalizedMessage());
		return null;
	}

	@GetMapping("employee/get-emp-by-subsidiary-Date")
	@Retry(name = "masters-ws")
	@CircuitBreaker(name = "masters-ws", fallbackMethod = "getEmplBySubIdFallback")
	public List<Employee> getEmplBySubId(@RequestParam ("SubsidiaryId")Long SubsidiaryId, @RequestParam ("startDate")Date startDate,@RequestParam ("EndDate")Date EndDate);

	default List<Employee> getEmplBySubIdFallback(Long SubsidiaryId, Date startDate, Date endDate, Throwable exception) {
		logger.error("Getting exception from MS to find the employee");
		logger.error("Exception : " + exception.getLocalizedMessage());
		return null;
	}

	@GetMapping("employee/get")
	@Retry(name = "masters-ws")
	@CircuitBreaker(name = "masters-ws", fallbackMethod = "findEmpByIdFallback")
	public Employee findEmpById(@RequestParam ("id")Long id);

	default Employee findEmpByIdFallback(Long id, Throwable exception) {
		logger.error("Getting exception from MS to find the Employee");
		logger.error("Exception : " + exception.getLocalizedMessage());
		return null;
	}


	@PostMapping("employee/save")
	@Retry(name = "masters-ws")
	@CircuitBreaker(name = "masters-ws", fallbackMethod = "saveEmployeeFallback")
	public Employee saveEmployee(@Valid @RequestBody Employee employee);

	default Employee saveEmployeeFallback(Employee employee, Throwable exception) {
		logger.error("Getting exception from MS to generating the Employee");
		logger.error("Exception : " + exception.getLocalizedMessage());
		return null;
	}

	@GetMapping("supplier/get-supplier-id-and-date-between")
	@Retry(name = "masters-ws")
	@CircuitBreaker(name = "masters-ws", fallbackMethod = "findByIdAndCreatedDateBetweenFallback")
	public Optional<Supplier> findByIdAndCreatedDateBetween(@RequestParam ("id")Long id, @RequestParam ("startDate")Date startDate, @RequestParam ("endDate")Date endDate);

	default Optional<Supplier> findByIdAndCreatedDateBetweenFallback(Long id, Date startDate, Date endDate ,Throwable exception) {
		logger.error("Getting exception from MS to find the supplier");
		logger.error("Exception : " + exception.getLocalizedMessage());
		return null;
	}
	@GetMapping("supplier/get-supplier-subsidiary-subsidiary-id")
	@Retry(name = "masters-ws")
	@CircuitBreaker(name = "masters-ws", fallbackMethod = "findSupplierSubsidiaryBySubsidiaryIdFallback")
	public List<SupplierSubsidiary> findSupplierSubsidiaryBySubsidiaryId(@RequestParam ("subsidiaryId")Long subsidiaryId);

	default List<SupplierSubsidiary> findSupplierSubsidiaryBySubsidiaryIdFallback(Long subsidiaryId,Throwable exception) {
		logger.error("Getting exception from MS to find the SupplierSubsidiary");
		logger.error("Exception : " + exception.getLocalizedMessage());
		return null;
	}

	@GetMapping("/location/get")
	@Retry(name = "masters-ws")
	@CircuitBreaker(name = "masters-ws", fallbackMethod = "findLocationByIdFallback")
	public Location findLocationById(@RequestParam ("id")Long id);

	default Location findLocationByIdFallback(Long id,Throwable exception) {
		logger.error("Getting exception from MS to find the Location");
		logger.error("Exception : " + exception.getLocalizedMessage());
		return null;
	}

	@GetMapping("/item/get")
	@Retry(name = "masters-ws")
	@CircuitBreaker(name = "masters-ws", fallbackMethod = "findItemByIdFallback")
	public Item findItemById(@RequestParam ("id")Long id);

	default Item findItemByIdFallback(Long id,Throwable exception) {
		logger.error("Getting exception from MS to find the Item");
		logger.error("Exception : " + exception.getLocalizedMessage());
		return null;
	}

	@GetMapping("/supplier/get-supplier-by-integrated-id")
	@Retry(name = "masters-ws")
	@CircuitBreaker(name = "masters-ws", fallbackMethod = "findSupplierByIntegratedIdAndIsDeletedFallback")
	public Supplier findSupplierByIntegratedIdAndIsDeleted(@RequestParam("integratedId") String integratedId);

	default Supplier findSupplierByIntegratedIdAndIsDeletedFallback(String integratedId, Throwable exception) {
		logger.error("Getting exception from MS to find the supplier");
		logger.error("Exception : " + exception.getLocalizedMessage());
		return null;
	}

	@GetMapping("/account/get-account-by-integrated-id")
	@Retry(name = "masters-ws")
	@CircuitBreaker(name = "masters-ws", fallbackMethod = "getAccountByIntegratedIdFallback")
	public Account getAccountByIntegratedId(@RequestParam ("integratedId")String integratedId);

	default Account getAccountByIntegratedIdFallback(String integratedId, Throwable exception) {
		logger.error("Getting exception from MS to find the Account by integratedId ");
		logger.error("Exception : " + exception.getLocalizedMessage());
		return null;
	}

	@GetMapping("/bank/get-by-gl-bank")
	@Retry(name = "masters-ws")
	@CircuitBreaker(name = "masters-ws", fallbackMethod = "findBankByglBankFallback")
	public Bank findBankByglBank(@RequestParam ("glBank") String glBank);

	default Bank findBankByglBankFallback(String glBank,Throwable exception) {
		logger.error("Getting exception from MS to find the Bank");
		logger.error("Exception : " + exception.getLocalizedMessage());
		return null;
	}

}
