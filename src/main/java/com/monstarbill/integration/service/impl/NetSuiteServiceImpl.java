package com.monstarbill.integration.service.impl;

import static com.netsuite.suitetalk.client.v2022_1.utils.Utils.createRecordRef;
import static com.netsuite.webservices.samples.Messages.ERROR_OCCURRED;
import static com.netsuite.webservices.samples.Messages.INVALID_WS_URL;
import static com.netsuite.webservices.samples.Messages.WRONG_PROPERTIES_FILE;
import static com.netsuite.webservices.samples.utils.PrintUtils.printError;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.axis.AxisFault;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.monstarbill.integration.commons.CustomException;
import com.monstarbill.integration.enums.FormNames;
import com.monstarbill.integration.feignclient.FinanceServiceClient;
import com.monstarbill.integration.feignclient.MasterServiceClient;
import com.monstarbill.integration.feignclient.SetupServiceClient;
import com.monstarbill.integration.models.Account;
import com.monstarbill.integration.models.Employee;
import com.monstarbill.integration.models.EmployeeAccounting;
import com.monstarbill.integration.models.EmployeeAddress;
import com.monstarbill.integration.models.EmployeeContact;
import com.monstarbill.integration.models.Invoice;
import com.monstarbill.integration.models.InvoiceItem;
import com.monstarbill.integration.models.Item;
import com.monstarbill.integration.models.Location;
import com.monstarbill.integration.models.ManageIntegration;
import com.monstarbill.integration.models.Subsidiary;
import com.monstarbill.integration.models.Supplier;
import com.monstarbill.integration.models.SupplierAddress;
import com.monstarbill.integration.models.SupplierSubsidiary;
import com.monstarbill.integration.models.TaxGroup;
import com.monstarbill.integration.payload.request.NetsuiteValueReturn;
import com.monstarbill.integration.repository.ManageIntegrationRepository;
import com.monstarbill.integration.service.NetSuiteService;
import com.netsuite.suitetalk.client.v2022_1.WsClient;
import com.netsuite.suitetalk.proxy.v2022_1.lists.accounting.NonInventoryResaleItem;
import com.netsuite.suitetalk.proxy.v2022_1.lists.employees.EmployeeAddressbook;
import com.netsuite.suitetalk.proxy.v2022_1.lists.employees.EmployeeAddressbookList;
import com.netsuite.suitetalk.proxy.v2022_1.lists.relationships.Vendor;
import com.netsuite.suitetalk.proxy.v2022_1.lists.relationships.VendorAddressbook;
import com.netsuite.suitetalk.proxy.v2022_1.lists.relationships.VendorAddressbookList;
import com.netsuite.suitetalk.proxy.v2022_1.platform.common.Address;
import com.netsuite.suitetalk.proxy.v2022_1.platform.common.types.Country;
import com.netsuite.suitetalk.proxy.v2022_1.platform.core.BaseRef;
import com.netsuite.suitetalk.proxy.v2022_1.platform.core.CustomFieldList;
import com.netsuite.suitetalk.proxy.v2022_1.platform.core.CustomFieldRef;
import com.netsuite.suitetalk.proxy.v2022_1.platform.core.GetSelectValueFieldDescription;
import com.netsuite.suitetalk.proxy.v2022_1.platform.core.LongCustomFieldRef;
import com.netsuite.suitetalk.proxy.v2022_1.platform.core.RecordRef;
import com.netsuite.suitetalk.proxy.v2022_1.platform.core.RecordRefList;
import com.netsuite.suitetalk.proxy.v2022_1.platform.core.StringCustomFieldRef;
import com.netsuite.suitetalk.proxy.v2022_1.platform.core.types.RecordType;
import com.netsuite.suitetalk.proxy.v2022_1.platform.messages.WriteResponse;
import com.netsuite.suitetalk.proxy.v2022_1.transactions.purchases.VendorBill;
import com.netsuite.suitetalk.proxy.v2022_1.transactions.purchases.VendorBillItem;
import com.netsuite.suitetalk.proxy.v2022_1.transactions.purchases.VendorBillItemList;
import com.netsuite.webservices.samples.Properties;
import com.netsuite.webservices.samples.WsClientFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NetSuiteServiceImpl implements NetSuiteService {

	@Autowired
	private MasterServiceClient masterServiceClient;

	@Autowired
	private SetupServiceClient setupServiceClient;

	@Autowired
	private FinanceServiceClient financeServiceClient;

	private WsClient client;

	@Autowired
	private ManageIntegrationRepository integrationRepository;

	public void setClient() {
		try {
			Properties properties = new Properties();
			Long id = 1L;
			Optional<ManageIntegration> optIntegration = integrationRepository.findByIdAndIsDeleted(id, false);
			if (optIntegration.isPresent()) {
				ManageIntegration integration = optIntegration.get();
				properties.setNSProperty(integration);
				client = WsClientFactory.getWsClient(properties, null);
			}
		} catch (MalformedURLException e) {
			printError(INVALID_WS_URL, e.getMessage());
			System.exit(2);
		} catch (AxisFault e) {
			printError(ERROR_OCCURRED, e.getFaultString());
			System.exit(3);
		} catch (IOException e) {
			printError(WRONG_PROPERTIES_FILE, e.getMessage());
			System.exit(1);
		}
	}

	@Override
	public List<Supplier> sendSupplier(ArrayList<Long> supplierIds) {
		setClient();
		List<Supplier> suppliers = new ArrayList<Supplier>();
		int size = supplierIds.size();
		for (int position = 0; position < size; position++) {
			Supplier savedSupplier = savedSupplier(supplierIds.get(position));
			suppliers.add(savedSupplier);
		}
		return suppliers;
	}

	@Transactional
	Supplier savedSupplier(Long id) {
		Supplier savedsuSupplier = null;
		Supplier supplier = masterServiceClient.findSupplierById(id);
		if (supplier == null) {
			log.info("Supplier is not found");
			throw new CustomException("Supplier is not found id:: " + id);
		}
		Vendor vendor = new Vendor();
		vendor.setCompanyName(supplier.getName());
		vendor.setEntityId(supplier.getVendorNumber());
		vendor.setLegalName(supplier.getLegalName());
		GetSelectValueFieldDescription fieldDescription = new GetSelectValueFieldDescription();
		fieldDescription.setRecordType(RecordType.vendor);
		fieldDescription.setField("category");
		try {
			List<BaseRef> values = client.getSelectValue(fieldDescription);
			for (BaseRef baseRef : values) {
				RecordRef recordRef = (RecordRef) baseRef;
				if (supplier.getVendorType().equals(recordRef.getName())) {
					vendor.setCategory(recordRef);
					break;
				}
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		fieldDescription.setField("terms");
		try {
			List<BaseRef> values = client.getSelectValue(fieldDescription);
			for (BaseRef baseRef : values) {
				RecordRef recordRef = (RecordRef) baseRef;
				if (supplier.getPaymentTerm().equals(recordRef.getName())) {
					vendor.setTerms(recordRef);
					break;
				}
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		vendor.setIsInactive(!supplier.isActive());
		List<SupplierSubsidiary> supplierSubsidiaries = supplier.getSupplierSubsidiary();
		if (!CollectionUtils.isEmpty(supplierSubsidiaries)) {
			Long subsidiaryId = supplierSubsidiaries.get(0).getSubsidiaryId();
			Subsidiary subResponseEntity = setupServiceClient.findSubsidiaryById(subsidiaryId);
			if (subResponseEntity != null)
				vendor.setSubsidiary(createRecordRef(subResponseEntity.getIntegratedId()));
		}
		// vendor.setEmail("tanmoy28@gmail.com");
		vendor.setCustomForm(createRecordRef("312")); // 53

		VendorAddressbookList addressbookList = new VendorAddressbookList();
		List<SupplierAddress> supplierAddresses = supplier.getSupplierAddresses();
		VendorAddressbook[] addressBooks = new VendorAddressbook[supplierAddresses.size()];
		for (int i = 0; i < supplierAddresses.size(); ++i) {
			SupplierAddress supplierAddress = supplierAddresses.get(i);
			VendorAddressbook addressBook = new VendorAddressbook();
			addressBook.setDefaultBilling(supplierAddress.isDefaultBilling());
			addressBook.setDefaultShipping(supplierAddress.isDefaultShipping());
			// addressBook.setLabel("Test Address");
			Address address = new Address();
			// address.setAddressee("T. Sarkar");
			// address.setAttention("T. P. Sarkar");
			address.setAddr1(supplierAddress.getAddress1());
			address.setAddr2(supplierAddress.getAddress2());
			address.setCity(supplierAddress.getCity());
			address.setZip(supplierAddress.getPin());
			address.setState(supplierAddress.getState());
			// address.setCountry(Country.fromValue(supplierAddress.getCountry()));
			addressBook.setAddressbookAddress(address);
			addressBooks[i] = addressBook;
		}
		addressbookList.setAddressbook(addressBooks);
		vendor.setAddressbookList(addressbookList);
		WriteResponse response = null;
		try {
			response = client.callAddRecord(vendor);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		if (!response.getStatus().isIsSuccess()) {
			supplier.setNsMessage((response.getStatus().getStatusDetail()[0].getMessage()));
			supplier.setNsStatus("Disputed");
		} else {
			supplier.setNsStatus("Exported");
			String internalId = ((RecordRef) response.getBaseRef()).getInternalId();
			supplier.setIntegratedId(internalId);
		}

		try {
			savedsuSupplier = masterServiceClient.saveSupplier(supplier);
		} catch (Exception e) {
			log.error("Error while saving the Supplier :: " + e.getMessage());
			// throw new CustomException("Error while saving the Invoice: " +
			// e.getMostSpecificCause());
		}

		return savedsuSupplier;
	}

	@Override
	public List<Item> sendItems(ArrayList<Long> itemIds) {
		setClient();
		List<Item> items = new ArrayList<Item>();
		int size = itemIds.size();
		for (int position = 0; position < size; position++) {
			Item savedItems = sendItem(itemIds.get(position));
			items.add(savedItems);
		}
		return items;
	}

	@Transactional
	Item sendItem(Long id) {
		Item savedItems = null;
		log.info("Find item started from Master is started by it's id ::" + id);
		Item item = masterServiceClient.findById(id);
		log.info("Find item from Master is completed");
		if (item != null) {
			NonInventoryResaleItem nonInventoryResaleItem = new NonInventoryResaleItem();
			nonInventoryResaleItem.setItemId(item.getName());
			log.info("Find subsidiary started from Setup by it's id ::" + item.getSubsidiaryId());
			Subsidiary subResponseEntity = setupServiceClient.findSubsidiaryById(item.getSubsidiaryId());
			log.info("Find subsidiary from Setup is completed");
			if (subResponseEntity != null) {
				RecordRefList recordRefList = new RecordRefList();
				RecordRef[] recordRefs = new RecordRef[1];
				recordRefs[0] = createRecordRef(subResponseEntity.getIntegratedId());
				recordRefList.setRecordRef(recordRefs);
				nonInventoryResaleItem.setSubsidiaryList(recordRefList);
			}
			
			// For Custom Field Mblid
			CustomFieldList customFieldList = new CustomFieldList();
			LongCustomFieldRef customFieldRef = new LongCustomFieldRef();
			customFieldRef.setInternalId("3786");
			customFieldRef.setValue(item.getId());
			CustomFieldRef[] customFieldRefa = new CustomFieldRef[1];
			customFieldRefa[0] = customFieldRef;
			customFieldList.setCustomField(customFieldRefa);
			nonInventoryResaleItem.setCustomFieldList(customFieldList);
			nonInventoryResaleItem.setIsInactive(!item.isActive());
			nonInventoryResaleItem.setPurchaseDescription(item.getDescription());
			GetSelectValueFieldDescription fieldDescription = new GetSelectValueFieldDescription();
			fieldDescription.setRecordType(RecordType.nonInventoryResaleItem);
			fieldDescription.setField("expenseaccount");
			log.info("Find account for expense started from Master by it's id ::" + item.getExpenseAccountId());
			Account acccount = masterServiceClient.getAccount(item.getExpenseAccountId());
			log.info("Find account for expense from Master completed");
			if (acccount != null) {
				nonInventoryResaleItem.setExpenseAccount(createRecordRef(acccount.getIntegratedId()));
			}
			fieldDescription.setField("incomeaccount");
			Long incomeId = Long.parseLong(item.getIncomeAccount());
			log.info("Find account for Income started from Master by it's id :: " + incomeId);
			Account incomeAccount = masterServiceClient.getAccount(incomeId);
			log.info("Find account for Income from Master completed");
			if (incomeAccount != null) {
				nonInventoryResaleItem.setIncomeAccount((createRecordRef(incomeAccount.getIntegratedId())));
			}
			fieldDescription.setField("unitstype");
			try {
				List<BaseRef> values = client.getSelectValue(fieldDescription);
				for (BaseRef baseRef : values) {
					RecordRef recordRef = (RecordRef) baseRef;
					if (item.getUom().equals(recordRef.getName())) {
						nonInventoryResaleItem.setUnitsType(recordRef);
						break;
					}
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			WriteResponse response = null;
			try {
				response = client.callAddRecord(nonInventoryResaleItem);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			// For message set
			if (!response.getStatus().isIsSuccess()) {
				item.setNsMessage((response.getStatus().getStatusDetail()[0].getMessage()));
				log.info("UnSuccessfull");
				item.setNsStatus("Disputed");
				log.info(response.toString());
			} else {
				item.setNsStatus("Exported");
				log.info("Successfull");
				item.setNsMessage("Item send to netsuite successfully");
				String internalId = ((RecordRef) response.getBaseRef()).getInternalId();
				item.setIntegratedId(internalId);
			}

			try {
				log.info("Item save to Master is started");
				savedItems = masterServiceClient.saveItem(item);
				log.info("Item save to Master is now completed");

			} catch (Exception e) {
				log.error("Error while saving the Item :: " + e.getMessage());
			}
		}
		return savedItems;
	}

	@Override
	public List<Employee> sendEmployees(ArrayList<Long> employeeIds) {
		setClient();
		List<Employee> employees = new ArrayList<Employee>();
		int size = employeeIds.size();
		for (int position = 0; position < size; position++) {
			Employee savedEmployee = sendEmployee(employeeIds.get(position));
			employees.add(savedEmployee);
		}
		return employees;
	}

	@Transactional
	Employee sendEmployee(Long id) {
		Employee savedEmployees = null;
		log.info("Find employee started from Master by it's id ::" + id);
		Employee employee = masterServiceClient.findByemployeeIdId(id);
		log.info("Find employee complted from Master ");
		if (employee != null) {
			com.netsuite.suitetalk.proxy.v2022_1.lists.employees.Employee employeen = new com.netsuite.suitetalk.proxy.v2022_1.lists.employees.Employee();
			GetSelectValueFieldDescription fieldDescription = new GetSelectValueFieldDescription();
			log.info("Find subsidiary started from Setup by it's id ::" + employee.getSubsidiaryId());
			Subsidiary subsidiary = setupServiceClient.findSubsidiaryById(employee.getSubsidiaryId());
			log.info("Find subsidiary from Setup is completed");
			if (subsidiary != null) {
				employeen.setSubsidiary(createRecordRef(subsidiary.getIntegratedId()));
			}
			CustomFieldList customFieldList = new CustomFieldList();
			StringCustomFieldRef customFieldRef = new StringCustomFieldRef();
			customFieldRef.setInternalId("3789");
			customFieldRef.setValue(employee.getEmployeeNumber());
			CustomFieldRef[] customFieldRefa = new CustomFieldRef[1];
			customFieldRefa[0] = customFieldRef;
			customFieldList.setCustomField(customFieldRefa);
			employeen.setCustomFieldList(customFieldList);

			employeen.setFirstName(employee.getFirstName());
			employeen.setLastName(employee.getLastName());
			fieldDescription.setRecordType(RecordType.employee);
			fieldDescription.setField("department");
			try {
				List<BaseRef> values = client.getSelectValue(fieldDescription);
				for (BaseRef baseRef : values) {
					RecordRef recordRef = (RecordRef) baseRef;
					if (employee.getDepartment().equals(recordRef.getName())) {
						employeen.setDepartment(recordRef);
						break;
					}
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}

			fieldDescription.setField("supervisor");
			try {
				List<BaseRef> values = client.getSelectValue(fieldDescription);
				for (BaseRef baseRef : values) {
					RecordRef recordRef = (RecordRef) baseRef;
					if (employee.getSupervisor().equals(recordRef.getName())) {
						employeen.setSupervisor(recordRef);
						break;
					}
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			employeen.setEmail(employee.getEmail());
			EmployeeAccounting employeeAccount = employee.getEmployeeAccounting();
			Long DefaultLiabilityAccount = employeeAccount.getDefaultLiabilityAccount();
			String Account = Long.toString(DefaultLiabilityAccount);
			employeen.setAccountNumber(Account);
			EmployeeContact employeeContact = employee.getEmployeeContact();
			employeen.setPhone(employeeContact.getMobile());
			employeen.setOfficePhone(employeeContact.getOfficeNumber());
			EmployeeAddressbookList addressbookList = new EmployeeAddressbookList();
			List<EmployeeAddress> employeeAddresses = employee.getEmployeeAddresses();
			EmployeeAddressbook[] addressBooks = new EmployeeAddressbook[employeeAddresses.size()];
			for (int i = 0; i < employeeAddresses.size(); ++i) {
				EmployeeAddress employeeAddress = employeeAddresses.get(i);
				EmployeeAddressbook addressBook = new EmployeeAddressbook();
				Address address = new Address();
				address.setAddr1(employeeAddress.getAddress1());
				address.setAddr2(employeeAddress.getAddress2());
				address.setCity(employeeAddress.getCity());
				String pin = Integer.toString(employeeAddress.getPin());
				address.setZip(pin);
				address.setState(employeeAddress.getState());
				address.setCountry(Country.fromValue(employeeAddress.getCountry()));
				addressBook.setAddressbookAddress(address);
				addressBooks[i] = addressBook;
			}
			addressbookList.setAddressbook(addressBooks);
			employeen.setAddressbookList(addressbookList);
			CustomFieldList customFieldList1 = new CustomFieldList();
			LongCustomFieldRef customFieldRef1 = new LongCustomFieldRef();
			customFieldRef1.setInternalId("4826");
			customFieldRef1.setValue(employee.getId());
			CustomFieldRef[] customFieldRefa1 = new CustomFieldRef[1];
			customFieldRefa1[0] = customFieldRef1;
			customFieldList1.setCustomField(customFieldRefa1);
			employeen.setCustomFieldList(customFieldList);
			WriteResponse response = null;
			try {
				response = client.callAddRecord(employeen);

			} catch (RemoteException e) {
				e.printStackTrace();
			}
			if (!response.getStatus().isIsSuccess()) {
				employee.setNsMessage((response.getStatus().getStatusDetail()[0].getMessage()));
				employee.setNsStatus("Disputed");
			} else {
				employee.setNsStatus("Exported");
				String internalId = ((RecordRef) response.getBaseRef()).getInternalId();
				employee.setIntegratedId(internalId);
			}

			try {
				log.info("Employee save to Master is started");
				savedEmployees = masterServiceClient.saveEmployee(employee);
				log.info("Employee save to Master is now completed");

			} catch (Exception e) {
				log.error("Error while saving the employee :: " + e.getMessage());
			}
		}
		return savedEmployees;
	}

	@Transactional
	@Override
	public List<Invoice> sendInvoice(ArrayList<Long> invoiceIds) {
		setClient();
		List<Invoice> invoices = new ArrayList<Invoice>();
		int size = invoiceIds.size();
		for (int position = 0; position < size; position++) {
			Invoice savedEmployee = sendInvoice(invoiceIds.get(position));
			invoices.add(savedEmployee);
		}
		return invoices;
	}

	@Transactional
	Invoice sendInvoice(Long id) {
		Invoice savedInvoice = null, invoice = financeServiceClient.getInvoiceById(id);
		if (invoice != null) {
			VendorBill vendorBill = new VendorBill();
			Supplier supResponseEntity = masterServiceClient.findSupplierById(invoice.getSupplierId());
			if (supResponseEntity != null) {
				vendorBill.setEntity(createRecordRef(supResponseEntity.getIntegratedId()));
			}
			// vendorBill.setTransactionNumber("Test14112022");
			vendorBill.setTranId(invoice.getInvoiceNo());
			Subsidiary subResponseEntity = setupServiceClient.findSubsidiaryById(invoice.getSubsidiaryId());
			if (subResponseEntity != null)
				vendorBill.setSubsidiary(createRecordRef(subResponseEntity.getIntegratedId()));
			Location locResponseEntity = masterServiceClient.findLocationById(invoice.getLocationId());
			if (locResponseEntity != null)
				vendorBill.setLocation(createRecordRef(locResponseEntity.getIntegratedId()));
			vendorBill.setTranDate(DateUtils.toCalendar(invoice.getInvoiceDate()));
			GetSelectValueFieldDescription fieldDescription = new GetSelectValueFieldDescription();
			fieldDescription.setRecordType(RecordType.vendorBill);
			fieldDescription.setField("terms");
			try {
				List<BaseRef> values = client.getSelectValue(fieldDescription);
				for (BaseRef baseRef : values) {
					RecordRef recordRef = (RecordRef) baseRef;
					if (invoice.getPaymentTerm().equals(recordRef.getName())) {
						vendorBill.setTerms(recordRef); // System.out.println(invoice.getPaymentTerm());
						break;
					}
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			/*
			 * Optional<Currency> optCurrency =
			 * currencyRepository.findByNameAndIsDeleted(invoice.getCurrency(), false);
			 * if(optCurrency.isPresent())
			 * vendorBill.setCurrency(createRecordRef(optCurrency.get().getIntegratedId()));
			 */
			// vendorBill.setExchangeRate(invoice.getFxRate());
			// RecordRef approvalStatus = new RecordRef();
			// approvalStatus.setName(invoice.getInvStatus());
			vendorBill.setApprovalStatus(createRecordRef("2"));
			VendorBillItemList vendorBillItemList = new VendorBillItemList();
			List<InvoiceItem> invoiceItems = invoice.getInvoiceItems();
			VendorBillItem[] vendorBillItems = new VendorBillItem[invoiceItems.size()];
			for (int i = 0; i < invoiceItems.size(); ++i) {
				InvoiceItem invoiceItem = invoiceItems.get(i);
				VendorBillItem vendorBillItem = new VendorBillItem();
				Item itemResponseEntity = masterServiceClient.findItemById(invoiceItem.getItemId());
				if (itemResponseEntity != null) {
					vendorBillItem.setItem(createRecordRef(itemResponseEntity.getIntegratedId()));
					vendorBillItem.setDescription(itemResponseEntity.getDescription());
				}
				vendorBillItem.setQuantity(invoiceItem.getBillQty());
				vendorBillItem.setRate(String.valueOf(invoiceItem.getRate()));
				// vendorBillItem.setAmount(500.3);
				TaxGroup taxResponseEntity = setupServiceClient.findTaxGroupById(invoiceItem.getTaxGroupId());
				if (taxResponseEntity != null)
					vendorBillItem.setTaxCode(createRecordRef(taxResponseEntity.getIntegratedId()));
				// vendorBillItem.setTaxAmount(25.2);
				fieldDescription.setField("department");
				try {
					List<BaseRef> values = client.getSelectValue(fieldDescription);
					for (BaseRef baseRef : values) {
						RecordRef recordRef = (RecordRef) baseRef;
						if (invoiceItem.getDepartment().equals(recordRef.getName())) {
							vendorBill.setDepartment(recordRef);
							System.out.println(invoiceItem.getDepartment());
							break;
						}
					}
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				vendorBillItems[i] = vendorBillItem;
			}
			vendorBillItemList.setItem(vendorBillItems);
			vendorBill.setItemList(vendorBillItemList);

			WriteResponse response = null;
			try {
				response = client.callAddRecord(vendorBill);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			if (!response.getStatus().isIsSuccess()) {
				invoice.setNsMessage((response.getStatus().getStatusDetail()[0].getMessage()));
				invoice.setNsStatus("Disputed");
			} else {
				invoice.setNsStatus("Exported");
				String internalId = ((RecordRef) response.getBaseRef()).getInternalId();
				invoice.setIntegratedId(internalId);
			}
			String internalId = ((RecordRef) response.getBaseRef()).getInternalId();
			invoice.setIntegratedId(internalId);
			try {
				log.info("Invoice save is started to finance");
				savedInvoice = financeServiceClient.saveInvoice(invoice);
				log.info("Invoice save to finance is completed");
			} catch (Exception e) {
				log.error("Error while saving the Invoice :: " + e.getMessage());
				// throw new CustomException("Error while saving the Invoice: " +
				// e.getMostSpecificCause());
			}
		}

		return savedInvoice;
	}

	@Transactional
	@Override
	public List<NetsuiteValueReturn> getObject(Long subsidiaryId, String type, Date startDate, Date endDate) {
		String ItemType = FormNames.ITEM.getFormName();
		String EmployeeType = FormNames.EMPLOYEE.getFormName();
		String supplierType = FormNames.SUPPLIER.getFormName();
		String apInvoiceType = FormNames.INVOICE.getFormName();
		List<NetsuiteValueReturn> objects = new ArrayList<NetsuiteValueReturn>();
		if (type.equalsIgnoreCase(ItemType)) {
			log.info("Find item started from Master respect to subsidiaryId :: " + subsidiaryId
					+ " created Date between ::" + startDate + " and " + endDate);
			List<Item> items = masterServiceClient.findItemBySubsidiaryAndCreatedDateBetween(subsidiaryId, startDate,
					endDate);
			if (CollectionUtils.isEmpty(items)) {
				log.info("Item is not found");
				throw new CustomException("Item is not found ");
			}
			log.info("Item find complted from Master");
			items.forEach(item -> {
				NetsuiteValueReturn netsuiteValueReturn = new NetsuiteValueReturn();
				netsuiteValueReturn.setMblId(item.getId());
				netsuiteValueReturn.setType(ItemType);
				netsuiteValueReturn.setCreateddate(item.getCreatedDate());
				netsuiteValueReturn.setRemarks(item.getNsMessage());
				netsuiteValueReturn.setExportedStatus(item.getNsStatus());
				log.info("Find subsidiary from Setup respect to subsidiaryId :: " + item.getSubsidiaryId());
				Subsidiary subResponseEntity = setupServiceClient.findSubsidiaryById(item.getSubsidiaryId());
				log.info("Find subsidiary from setup completed");
				if (subResponseEntity != null) {
					netsuiteValueReturn.setName(subResponseEntity.getName());
				} else {
					log.error("Subsidiary is not found against id: " + item.getSubsidiaryId());
				}
				objects.add(netsuiteValueReturn);
			});
		}
		if (type.equalsIgnoreCase(EmployeeType)) {
			log.info("Find Employee started from Master respect to subsidiaryId :: " + subsidiaryId
					+ " created Date between ::" + startDate + " and " + endDate);
			List<Employee> employees = masterServiceClient.getEmplBySubId(subsidiaryId, startDate, endDate);
			log.info("Item Employee complted from Master");
			if (!CollectionUtils.isEmpty(employees)) {
				employees.forEach(employee -> {
					NetsuiteValueReturn netsuiteValueReturn = new NetsuiteValueReturn();
					netsuiteValueReturn.setMblId(employee.getId());
					netsuiteValueReturn.setType(EmployeeType);
					netsuiteValueReturn.setCreateddate(employee.getCreatedDate());
					netsuiteValueReturn.setRemarks(employee.getNsMessage());
					netsuiteValueReturn.setExportedStatus(employee.getNsStatus());
					netsuiteValueReturn.setReferenceNumber(employee.getEmployeeNumber());
					log.info("Find subsidiary from Setup respect to subsidiaryId :: " + employee.getSubsidiaryId());
					Subsidiary subResponseEntity = setupServiceClient.findSubsidiaryById(employee.getSubsidiaryId());
					log.info("Find subsidiary from setup completed");
					if (subResponseEntity != null) {
						netsuiteValueReturn.setName(subResponseEntity.getName());
					} else {
						log.error("Subsidiary is not found against id: " + employee.getSubsidiaryId());
					}
					objects.add(netsuiteValueReturn);
				});

			}
		}
		if (type.equalsIgnoreCase(supplierType)) {
			List<SupplierSubsidiary> supplierSubsidiary = masterServiceClient
					.findSupplierSubsidiaryBySubsidiaryId(subsidiaryId);
			if (!CollectionUtils.isEmpty(supplierSubsidiary)) {
				supplierSubsidiary.forEach(supplierSubsidiariary -> {
					Optional<Supplier> optSupplier = masterServiceClient
							.findByIdAndCreatedDateBetween(supplierSubsidiariary.getSupplierId(), startDate, endDate);
					if (optSupplier.isPresent()) {
						Supplier supplier = optSupplier.get();
						NetsuiteValueReturn netsuiteValueReturn = new NetsuiteValueReturn();
						netsuiteValueReturn.setReferenceNumber(supplier.getVendorNumber());
						netsuiteValueReturn.setMblId(supplier.getId());
						netsuiteValueReturn.setType(supplierType);
						netsuiteValueReturn.setCreateddate(supplier.getCreatedDate());
						netsuiteValueReturn.setRemarks(supplier.getNsMessage());
						netsuiteValueReturn.setExportedStatus(supplier.getNsStatus());
						Subsidiary OptSubsidiary = setupServiceClient
								.findSubsidiaryById(supplierSubsidiariary.getSubsidiaryId());

						if (OptSubsidiary != null) {
							netsuiteValueReturn.setName(OptSubsidiary.getName());
						}

						objects.add(netsuiteValueReturn);
					} else {
						log.error("Supplier not found");
					}
				});
			} else {
				log.error("Subsidiary is not found against id: " + subsidiaryId);
			}
			return objects;
		}

		if (type.equalsIgnoreCase(apInvoiceType)) {
			List<Invoice> invoices = financeServiceClient.findByIdAndIntegratedIdAndCreatedDateBetween(subsidiaryId,
					startDate, endDate);
			if (!CollectionUtils.isEmpty(invoices)) {
				invoices.forEach(invoice -> {
					NetsuiteValueReturn netsuiteValueReturn = new NetsuiteValueReturn();
					netsuiteValueReturn.setMblId(invoice.getInvoiceId());
					netsuiteValueReturn.setType(apInvoiceType);
					netsuiteValueReturn.setCreateddate(invoice.getCreatedDate());
					netsuiteValueReturn.setRemarks(invoice.getNsMessage());
					netsuiteValueReturn.setExportedStatus(invoice.getNsStatus());
					Subsidiary OptSubsidiary = setupServiceClient.findSubsidiaryById(invoice.getSubsidiaryId());

					if (OptSubsidiary != null) {
						netsuiteValueReturn.setName(OptSubsidiary.getName());
					} else {
						log.error("Subsidiary is not found against id: " + invoice.getSubsidiaryId());
					}
					objects.add(netsuiteValueReturn);
				});
			} else {
				log.error("invoice not found");
			}
			return objects;
		}
		return objects;
	}
}
