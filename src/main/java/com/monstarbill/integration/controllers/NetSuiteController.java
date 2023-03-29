package com.monstarbill.integration.controllers;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.monstarbill.integration.models.Employee;
import com.monstarbill.integration.models.Invoice;
import com.monstarbill.integration.models.Item;
import com.monstarbill.integration.models.Supplier;
import com.monstarbill.integration.payload.request.NetsuiteValueReturn;
import com.monstarbill.integration.service.NetSuiteService;

@RestController
@RequestMapping("/netsuite")
public class NetSuiteController {

	@Autowired
	private NetSuiteService netSuiteService;

	/** Sending the list of supplier to Netsuite
	 * @param supplierIds
	 * @return
	 */
	@GetMapping("/send-supplier")
	public List<Supplier> sendSupplier(@RequestBody ArrayList<Long> supplierIds,@RequestParam Long subsidiaryId)
	{
		return netSuiteService.sendSupplier(supplierIds,subsidiaryId);
	}


	/** Sending the list of item to Netsuite
	 * @param itemIds
	 * @return
	 */
	@GetMapping("/send-item")
	public List<Item> sendItem(@RequestBody ArrayList<Long> itemIds,@RequestParam Long subsidiaryId)
	{
		return netSuiteService.sendItems(itemIds,subsidiaryId);
	}

	/** Sending the list of employee to Netsuite
	 * @param employeeIds
	 * @return
	 */
	@GetMapping("/send-employee")
	public List<Employee> sendEmployee(@RequestBody ArrayList<Long> employeeIds,@RequestParam Long subsidiaryId)
	{
		return netSuiteService.sendEmployees(employeeIds,subsidiaryId);
	}
	
	/** Sending the list of invoice to Netsuite
	 * @param invoiceIds
	 * @return
	 */
	@GetMapping("/send-invoice")
    public List<Invoice> sendInvoice(@RequestBody ArrayList<Long>invoiceIds,@RequestParam Long subsidiaryId)
    {
        return netSuiteService.sendInvoice(invoiceIds,subsidiaryId);
    }
	/** Get the list of item or employee or invoice or supplier as a form netsuiteValueReturn object based on type, subsidiaryId, cretedDate between
	 * @param subsidiaryId
	 * @param type
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	@GetMapping("/get-object")
	public ResponseEntity<List<NetsuiteValueReturn>> getSubsidiaryByType(@RequestParam Long subsidiaryId, @RequestParam String type,
			@RequestParam Date startDate, @RequestParam Date endDate) {
		List<NetsuiteValueReturn> netsuiteValueReturn = new ArrayList<NetsuiteValueReturn>();
		netsuiteValueReturn = netSuiteService.getObject(subsidiaryId, type, startDate, endDate);
		if (netsuiteValueReturn == null) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		return ResponseEntity.ok(netsuiteValueReturn);
	}

	@GetMapping("/get")
	public String get()	{
		return "hello";
	}
}
