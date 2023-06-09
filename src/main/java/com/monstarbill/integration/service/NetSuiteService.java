package com.monstarbill.integration.service;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import com.monstarbill.integration.models.Employee;
import com.monstarbill.integration.models.Invoice;
import com.monstarbill.integration.models.Item;
import com.monstarbill.integration.models.MakePayment;
import com.monstarbill.integration.models.Supplier;
import com.monstarbill.integration.payload.request.NetsuiteValueReturn;

public interface NetSuiteService {

	public List<Supplier> sendSupplier(ArrayList<Long> supplierIds,Long subsidiaryId);
	
	/**
	 * @param itemId
	 * @return item
	 */
	List<Item> sendItems(ArrayList<Long> itemIds,Long subsidiaryId);
	
	/**
	 * @param employeeIds
	 * @return employee
	 */
	List<Employee> sendEmployees(ArrayList<Long> employeeIds,Long subsidiaryId);
	
	List<Invoice> sendInvoice(ArrayList<Long> invoiceIds,Long subsidiaryId);

	List<MakePayment> importPaymentXML(Long subsidiaryId);

	/**
	 * @param subsidiaryId
	 * @param type
	 * @param startDate
	 * @param endDate
	 * @return NetsuiteValueReturn
	 */
	public List<NetsuiteValueReturn> getObject(Long subsidiaryId, String type, Date startDate, Date endDate);

}
