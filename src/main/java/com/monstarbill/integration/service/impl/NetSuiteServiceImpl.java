package com.monstarbill.integration.service.impl;

import static com.netsuite.suitetalk.client.v2022_1.utils.Utils.createRecordRef;
import static com.netsuite.webservices.samples.Messages.ERROR_OCCURRED;
import static com.netsuite.webservices.samples.Messages.INVALID_WS_URL;
import static com.netsuite.webservices.samples.Messages.WRONG_PROPERTIES_FILE;
import static com.netsuite.webservices.samples.utils.PrintUtils.printError;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.axis.AxisFault;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.monstarbill.integration.commons.CommonUtils;
import com.monstarbill.integration.commons.CustomException;
import com.monstarbill.integration.commons.CustomMessageException;
import com.monstarbill.integration.enums.FormNames;
import com.monstarbill.integration.feignclient.FinanceServiceClient;
import com.monstarbill.integration.feignclient.MasterServiceClient;
import com.monstarbill.integration.feignclient.SetupServiceClient;
import com.monstarbill.integration.models.Account;
import com.monstarbill.integration.models.Bank;
import com.monstarbill.integration.models.Employee;
import com.monstarbill.integration.models.EmployeeAddress;
import com.monstarbill.integration.models.EmployeeContact;
import com.monstarbill.integration.models.Invoice;
import com.monstarbill.integration.models.InvoiceItem;
import com.monstarbill.integration.models.InvoicePayment;
import com.monstarbill.integration.models.Item;
import com.monstarbill.integration.models.Location;
import com.monstarbill.integration.models.MakePayment;
import com.monstarbill.integration.models.MakePaymentList;
import com.monstarbill.integration.models.ManageIntegration;
import com.monstarbill.integration.models.ManageIntegrationSubsidiary;
import com.monstarbill.integration.models.Subsidiary;
import com.monstarbill.integration.models.Supplier;
import com.monstarbill.integration.models.SupplierAddress;
import com.monstarbill.integration.models.SupplierSubsidiary;
import com.monstarbill.integration.models.TaxGroup;
import com.monstarbill.integration.payload.request.NetsuiteValueReturn;
import com.monstarbill.integration.repository.ManageIntegrationRepository;
import com.monstarbill.integration.repository.ManageIntegrationSubsidiaryRepository;
import com.monstarbill.integration.service.NetSuiteService;
import com.netsuite.suitetalk.client.v2022_1.WsClient;
import com.netsuite.suitetalk.proxy.v2022_1.documents.filecabinet.File;
import com.netsuite.suitetalk.proxy.v2022_1.lists.accounting.InventoryItem;
import com.netsuite.suitetalk.proxy.v2022_1.lists.accounting.NonInventoryResaleItem;
import com.netsuite.suitetalk.proxy.v2022_1.lists.employees.EmployeeAddressbook;
import com.netsuite.suitetalk.proxy.v2022_1.lists.employees.EmployeeAddressbookList;
import com.netsuite.suitetalk.proxy.v2022_1.lists.relationships.Vendor;
import com.netsuite.suitetalk.proxy.v2022_1.lists.relationships.VendorAddressbook;
import com.netsuite.suitetalk.proxy.v2022_1.lists.relationships.VendorAddressbookList;
import com.netsuite.suitetalk.proxy.v2022_1.platform.common.Address;
import com.netsuite.suitetalk.proxy.v2022_1.platform.common.FileSearchBasic;
import com.netsuite.suitetalk.proxy.v2022_1.platform.core.BaseRef;
import com.netsuite.suitetalk.proxy.v2022_1.platform.core.CustomFieldList;
import com.netsuite.suitetalk.proxy.v2022_1.platform.core.CustomFieldRef;
import com.netsuite.suitetalk.proxy.v2022_1.platform.core.GetSelectValueFieldDescription;
import com.netsuite.suitetalk.proxy.v2022_1.platform.core.LongCustomFieldRef;
import com.netsuite.suitetalk.proxy.v2022_1.platform.core.RecordRef;
import com.netsuite.suitetalk.proxy.v2022_1.platform.core.RecordRefList;
import com.netsuite.suitetalk.proxy.v2022_1.platform.core.SearchMultiSelectField;
import com.netsuite.suitetalk.proxy.v2022_1.platform.core.StringCustomFieldRef;
import com.netsuite.suitetalk.proxy.v2022_1.platform.core.types.RecordType;
import com.netsuite.suitetalk.proxy.v2022_1.platform.core.types.SearchMultiSelectFieldOperator;
import com.netsuite.suitetalk.proxy.v2022_1.platform.messages.ReadResponse;
import com.netsuite.suitetalk.proxy.v2022_1.platform.messages.WriteResponse;
import com.netsuite.suitetalk.proxy.v2022_1.transactions.purchases.VendorBill;
import com.netsuite.suitetalk.proxy.v2022_1.transactions.purchases.VendorBillItem;
import com.netsuite.suitetalk.proxy.v2022_1.transactions.purchases.VendorBillItemList;
import com.netsuite.suitetalk.proxy.v2022_1.transactions.purchases.VendorPayment;
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

	@Autowired
	private ManageIntegrationSubsidiaryRepository manageIntegrationSubsidiaryRepository;

	private WsClient client;

	@Autowired
	private ManageIntegrationRepository integrationRepository;

	public void setClient(Long subsidiaryId) {
		Long integrationId = null;
		Optional<ManageIntegrationSubsidiary> optManageIntegrationSubsidiary = manageIntegrationSubsidiaryRepository.findBySubsidiaryId(subsidiaryId);
		if(optManageIntegrationSubsidiary.isEmpty()) {
			log.info("ManageIntegration not found against subsidiaryId "+subsidiaryId);	
			throw new CustomException("ManageIntegration not found against subsidiaryId "+subsidiaryId);
		}
		integrationId = optManageIntegrationSubsidiary.get().getIntigrationId();
		try {
			Properties properties = new Properties();
			log.info("Id of netsuite credentials "+integrationId);
			Optional<ManageIntegration> optIntegration = integrationRepository.findByIdAndIsDeleted(integrationId, false);
			if (optIntegration.isPresent()) {
				ManageIntegration integration = optIntegration.get();
				log.info("Integration credentials found "+integration);
				properties.setNSProperty(integration);
				client = WsClientFactory.getWsClient(properties, null);
				log.info("Client is created");;
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
	public List<Supplier> sendSupplier(ArrayList<Long> supplierIds,Long subsidiaryId) {
		setClient(subsidiaryId);
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
		}
		Vendor vendor = new Vendor();
		log.info("Set CompanyName, EntityId and LegalName started");
		vendor.setCompanyName(supplier.getName());
		vendor.setEntityId(supplier.getVendorNumber());
		vendor.setLegalName(supplier.getLegalName());
		log.info("Set CompanyName, EntityId and LegalName finished "+supplier.getName()+" , "+supplier.getVendorNumber()+" and "+supplier.getLegalName());
		CustomFieldList customFieldList = new CustomFieldList();
		LongCustomFieldRef customFieldRef = new LongCustomFieldRef();
		customFieldRef.setInternalId("4826");
		customFieldRef.setValue(supplier.getId());
		CustomFieldRef[] customFieldRefa = new CustomFieldRef[1];
		customFieldRefa[0] = customFieldRef;
		customFieldList.setCustomField(customFieldRefa);
		vendor.setCustomFieldList(customFieldList);
		GetSelectValueFieldDescription fieldDescription = new GetSelectValueFieldDescription();
		fieldDescription.setRecordType(RecordType.vendor);
		fieldDescription.setField("category");
		try {
			List<BaseRef> values = client.getSelectValue(fieldDescription);
			for (BaseRef baseRef : values) {
				RecordRef recordRef = (RecordRef) baseRef;
				if (supplier.getVendorType().equals(recordRef.getName())) {
					log.info("Supplier vendor type matched");	
					vendor.setCategory(recordRef);
					break;
				}else {
					log.info("Supplier type not matched");				}
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
					log.info("Supplier paymentTerm matched with netsuite terms");
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
			Subsidiary subsidiary = setupServiceClient.findSubsidiaryById(subsidiaryId);
			if (subsidiary != null)
				log.info("Subsidiary found aginst id "+subsidiaryId);
			vendor.setSubsidiary(createRecordRef(subsidiary.getIntegratedId()));
		}
		// vendor.setEmail("tanmoy28@gmail.com");
		vendor.setCustomForm(createRecordRef("159")); // 53

		VendorAddressbookList addressbookList = new VendorAddressbookList();
		List<SupplierAddress> supplierAddresses = supplier.getSupplierAddresses();
		VendorAddressbook[] addressBooks = new VendorAddressbook[supplierAddresses.size()];
		for (int i = 0; i < supplierAddresses.size(); ++i) {
			SupplierAddress supplierAddress = supplierAddresses.get(i);
			log.info("Supplier address save started "+supplierAddress);
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
			log.info("Sending to netsuite "+vendor);
			response = client.callAddRecord(vendor);
			log.info("Response from netsuite "+response);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		if (!response.getStatus().isIsSuccess()) {
			supplier.setNsMessage((response.getStatus().getStatusDetail()[0].getMessage()));
			supplier.setNsStatus("Disputed");
			log.info("Supplier not send because "+supplier.getNsMessage());
		} else {
			supplier.setNsStatus("Exported");
			supplier.setNsMessage((response.getStatus().getStatusDetail()[0].getMessage()));
			String internalId = ((RecordRef) response.getBaseRef()).getInternalId();
			supplier.setIntegratedId(internalId);
			log.info("Supplier send to Netsuite ");
		}

		try {
			savedsuSupplier = masterServiceClient.saveSupplier(supplier);
			log.info("Supplier saved successfully in mbl "+savedsuSupplier);
		} catch (Exception e) {
			log.error("Error while saving the Supplier :: " + e.getMessage());
		}

		return savedsuSupplier;
	}

	@Override
	public List<Item> sendItems(ArrayList<Long> itemIds,Long subsidiaryId) {
		setClient(subsidiaryId);
		List<Item> items = new ArrayList<Item>();
		int size = itemIds.size();
		for (int position = 0; position < size; position++) {
			Item savedItems = sendItem(itemIds.get(position));
			log.info("Item id "+itemIds.get(position));
			items.add(savedItems);
		}
		//		for (Long id : itemIds) {
		//			Item savedItems = sendItem(id);
		//			items.add(savedItems);
		//		}
		return items;
	}

	@Transactional
	Item sendItem(Long id) {
		Item savedItems = null;
		log.info("Find item started from Master is started by it's id ::" + id);
		Item item = masterServiceClient.findById(id);
		log.info("Find item from Master is completed");
		if (item != null) {
			GetSelectValueFieldDescription fieldDescription = new GetSelectValueFieldDescription();
			String category = "Inventory Item";
			if(item.getCategory().equals(category)){
				log.info("In inventory item");
				InventoryItem inventoryItem = new InventoryItem();
				fieldDescription.setRecordType(RecordType.inventoryItem);
				fieldDescription.setField("assetaccount");
				log.info("Find account for asset started from Master by it's id ::" + item.getAssetAccountId());
				Account acccount = masterServiceClient.getAccount(item.getAssetAccountId());
				log.info("Find account for asset from Master completed");
				if (acccount != null) {
					inventoryItem.setAssetAccount(createRecordRef(acccount.getIntegratedId()));
				}
				inventoryItem.setItemId(item.getName());
				log.info("Find subsidiary started from Setup by it's id ::" + item.getSubsidiaryId());
				Subsidiary subsidiary = setupServiceClient.findSubsidiaryById(item.getSubsidiaryId());
				log.info("Find subsidiary from Setup is completed");
				if (subsidiary != null) {
					RecordRefList recordRefList = new RecordRefList();
					RecordRef[] recordRefs = new RecordRef[1];
					recordRefs[0] = createRecordRef(subsidiary.getIntegratedId());
					recordRefList.setRecordRef(recordRefs);
					inventoryItem.setSubsidiaryList(recordRefList);
					log.info("Subsidiary set completed, integratedId "+subsidiary.getIntegratedId());
				}
				// For Custom Field Mblid
				CustomFieldList customFieldList = new CustomFieldList();
				LongCustomFieldRef customFieldRef = new LongCustomFieldRef();
				customFieldRef.setInternalId("4827");
				customFieldRef.setValue(item.getId());
				CustomFieldRef[] customFieldRefa = new CustomFieldRef[1];
				customFieldRefa[0] = customFieldRef;
				customFieldList.setCustomField(customFieldRefa);
				inventoryItem.setCustomFieldList(customFieldList);
				inventoryItem.setTaxSchedule(createRecordRef("1"));
				inventoryItem.setIsInactive(!item.isActive());
				inventoryItem.setPurchaseDescription(item.getDescription());
				WriteResponse response = null;
				try {
					log.info("Send item to netsuite "+inventoryItem.toString());
					response = client.callAddRecord(inventoryItem);
					log.info("Getting response from netsuite"+response.toString());	
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				// For message set
				if (!response.getStatus().isIsSuccess()) {
					item.setNsMessage((response.getStatus().getStatusDetail()[0].getMessage()));
					log.info("UnSuccessfull because "+item.getNsMessage());
					item.setNsStatus("Disputed");
					log.info(response.toString());
				} else {
					item.setNsStatus("Exported");
					log.info("Successfull");
					item.setNsMessage("Item send to netsuite successfully");
					String internalId = ((RecordRef) response.getBaseRef()).getInternalId();
					item.setIntegratedId(internalId);
				}
			}
			String category2 = "Service Item";
			if(item.getCategory().equals(category2)) {
				log.info("In non inventory item");
				NonInventoryResaleItem nonInventoryResaleItem = new NonInventoryResaleItem();
				fieldDescription.setRecordType(RecordType.nonInventoryResaleItem);
				fieldDescription.setField("expenseaccount");
				log.info("Find account for expense started from Master by it's id ::" + item.getExpenseAccountId());
				Account acccount = masterServiceClient.getAccount(item.getExpenseAccountId());
				log.info("Find account for expense from Master completed");
				if (acccount != null) {
					nonInventoryResaleItem.setExpenseAccount(createRecordRef(acccount.getIntegratedId()));
				}
				nonInventoryResaleItem.setItemId(item.getName());
				log.info("Find subsidiary started from Setup by it's id ::" + item.getSubsidiaryId());
				Subsidiary subsidiary = setupServiceClient.findSubsidiaryById(item.getSubsidiaryId());
				log.info("Find subsidiary from Setup is completed");
				if (subsidiary != null) {
					RecordRefList recordRefList = new RecordRefList();
					RecordRef[] recordRefs = new RecordRef[1];
					recordRefs[0] = createRecordRef(subsidiary.getIntegratedId());
					recordRefList.setRecordRef(recordRefs);
					nonInventoryResaleItem.setSubsidiaryList(recordRefList);
					log.info("Subsidiary set completed, integratedId "+subsidiary.getIntegratedId());
				}
				// For Custom Field Mblid
				CustomFieldList customFieldList = new CustomFieldList();
				LongCustomFieldRef customFieldRef = new LongCustomFieldRef();
				customFieldRef.setInternalId("4827");
				customFieldRef.setValue(item.getId());
				CustomFieldRef[] customFieldRefa = new CustomFieldRef[1];
				customFieldRefa[0] = customFieldRef;
				customFieldList.setCustomField(customFieldRefa);
				nonInventoryResaleItem.setCustomFieldList(customFieldList);
				nonInventoryResaleItem.setTaxSchedule(createRecordRef("1"));
				nonInventoryResaleItem.setIsInactive(!item.isActive());
				nonInventoryResaleItem.setPurchaseDescription(item.getDescription());
				WriteResponse response = null;
				try {
					log.info("Send item to netsuite "+nonInventoryResaleItem.toString());
					response = client.callAddRecord(nonInventoryResaleItem);
					log.info("Getting response from netsuite"+response.toString());	
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				// For message set
				if (!response.getStatus().isIsSuccess()) {
					item.setNsMessage((response.getStatus().getStatusDetail()[0].getMessage()));
					log.info("UnSuccessfull because "+item.getNsMessage());
					item.setNsStatus("Disputed");
					log.info(response.toString());
				} else {
					item.setNsStatus("Exported");
					log.info("Successfull");
					item.setNsMessage("Item send to netsuite successfully");
					String internalId = ((RecordRef) response.getBaseRef()).getInternalId();
					item.setIntegratedId(internalId);
				}
			}

			try {
				log.info("Item save to Master is started "+item);
				savedItems = masterServiceClient.saveItem(item);
				log.info("Item save to Master is now completed "+savedItems);

			} catch (Exception e) {
				log.error("Error while saving the Item :: " + e.getMessage());
			}
		}
		return savedItems;
	}

	@Override
	public List<Employee> sendEmployees(ArrayList<Long> employeeIds,Long subsidiaryId) {
		setClient(subsidiaryId);
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
		Employee employee = masterServiceClient.findEmpById(id);
		log.info("Find employee complted from Master ");
		if (employee != null) {
			com.netsuite.suitetalk.proxy.v2022_1.lists.employees.Employee employeen = new com.netsuite.suitetalk.proxy.v2022_1.lists.employees.Employee();
			GetSelectValueFieldDescription fieldDescription = new GetSelectValueFieldDescription();
			log.info("Find subsidiary started from Setup by it's id ::" + employee.getSubsidiaryId());
			Subsidiary subsidiary = setupServiceClient.findSubsidiaryById(employee.getSubsidiaryId());
			log.info("Find subsidiary from Setup is completed");
			if (subsidiary != null) {
				employeen.setSubsidiary(createRecordRef(subsidiary.getIntegratedId()));
				log.info("Set subsidiary completed which integratedId is"+subsidiary.getIntegratedId());
			}
			CustomFieldList customFieldList = new CustomFieldList();
			LongCustomFieldRef customFieldRef = new LongCustomFieldRef();
			customFieldRef.setInternalId("4826");
			//customFieldRef.setValue(employee.getEmployeeNumber());
			customFieldRef.setValue(employee.getId());
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
						log.info("Set department completed "+recordRef);
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
					if(employee.getSupervisor()!=null) {
						if (employee.getSupervisor().equals(recordRef.getName())) {
							employeen.setSupervisor(recordRef);
							log.info("Set supervisor completed "+recordRef);
							break;
						}
					}
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			employeen.setEmail(employee.getEmail());
			//			EmployeeAccounting employeeAccount = employee.getEmployeeAccounting();
			//			Long DefaultLiabilityAccount = employeeAccount.getDefaultLiabilityAccount();
			//			if(DefaultLiabilityAccount!=null) {
			//				String Account = Long.toString(DefaultLiabilityAccount);
			//				employeen.setAccountNumber(Account);
			//				log.info("Set account number is completed "+Account);
			//			}
			EmployeeContact employeeContact = employee.getEmployeeContact();
			employeen.setPhone(employeeContact.getMobile());
			employeen.setOfficePhone(employeeContact.getOfficeNumber());
			EmployeeAddressbookList addressbookList = new EmployeeAddressbookList();
			List<EmployeeAddress> employeeAddresses = employee.getEmployeeAddresses();
			EmployeeAddressbook[] addressBooks = new EmployeeAddressbook[employeeAddresses.size()];
			for (int i = 0; i < employeeAddresses.size(); ++i) {
				EmployeeAddress employeeAddress = employeeAddresses.get(i);
				log.info("Employee address completed "+employeeAddress);
				EmployeeAddressbook addressBook = new EmployeeAddressbook();
				Address address = new Address();
				address.setAddr1(employeeAddress.getAddress1());
				address.setAddr2(employeeAddress.getAddress2());
				address.setCity(employeeAddress.getCity());
				String pin = Integer.toString(employeeAddress.getPin());
				address.setZip(pin);
				address.setState(employeeAddress.getState());
				//address.setCountry(Country.fromValue(employeeAddress.getCountry()));
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
				log.info("Sending the employee netsuite "+employeen);
				response = client.callAddRecord(employeen);
				log.info("Getting response from "+response);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			if (!response.getStatus().isIsSuccess()) {
				employee.setNsMessage((response.getStatus().getStatusDetail()[0].getMessage()));
				employee.setNsStatus("Disputed");
				log.info("Employee not send to netsuite "+employee.getNsMessage());
			} else {
				employee.setNsStatus("Exported");
				employee.setNsMessage("Employee send to netsuite successfully");
				String internalId = ((RecordRef) response.getBaseRef()).getInternalId();
				employee.setIntegratedId(internalId);
				log.info("Employee send to netsuite successfully");
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
	public List<Invoice> sendInvoice(ArrayList<Long> invoiceIds,Long subsidiaryId) {
		setClient(subsidiaryId);
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
			CustomFieldList customFieldList = new CustomFieldList();
			LongCustomFieldRef customFieldRef = new LongCustomFieldRef();
			customFieldRef.setInternalId("193");
			customFieldRef.setValue(invoice.getInvoiceId());
			CustomFieldRef[] customFieldRefa = new CustomFieldRef[1];
			customFieldRefa[0] = customFieldRef;
			customFieldList.setCustomField(customFieldRefa);
			vendorBill.setCustomFieldList(customFieldList);
			// vendorBill.setTransactionNumber("Test14112022");
			vendorBill.setTranId(invoice.getInvoiceNo());
			Subsidiary subResponseEntity = setupServiceClient.findSubsidiaryById(invoice.getSubsidiaryId());
			log.info("subsidiary fetch " +subResponseEntity );

			if (subResponseEntity != null)
				vendorBill.setSubsidiary(createRecordRef(subResponseEntity.getIntegratedId()));
			Location locResponseEntity = masterServiceClient.findLocationById(invoice.getLocationId());
			if (locResponseEntity != null) {
				log.info("Location  fetch " +locResponseEntity );
				vendorBill.setLocation(createRecordRef(locResponseEntity.getIntegratedId()));
			}
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
			//	vendorBill.setApprovalStatus(createRecordRef("2"));
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
				log.info("Tax group fetch "+taxResponseEntity);
				if (taxResponseEntity != null)
					vendorBillItem.setTaxCode(createRecordRef(taxResponseEntity.getIntegratedId()));
				//vendorBillItem.setTaxg
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

	/**
	 * Import MakePayment XML and DB Insert
	 * 
	 * @return Saved MakePayments
	 */
	@Override
	public List<MakePayment> importPaymentXML(Long subsidiaryId) {
		setClient(subsidiaryId);
		List<MakePayment> savedMakePayments = new ArrayList<>();
		FileSearchBasic fileSearchBasic = new FileSearchBasic();
		SearchMultiSelectField searchMultiSelectField = new SearchMultiSelectField(
				new RecordRef[] { createRecordRef("860") }, SearchMultiSelectFieldOperator.anyOf);
		fileSearchBasic.setFolder(searchMultiSelectField);
		com.netsuite.suitetalk.proxy.v2022_1.platform.core.SearchResult searchResult = null;
		try {
			searchResult = client.callSearch(fileSearchBasic);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		com.netsuite.suitetalk.proxy.v2022_1.platform.core.Record[] records = searchResult.getRecordList().getRecord();
		if (records != null)
			for (com.netsuite.suitetalk.proxy.v2022_1.platform.core.Record record : records) {
				log.info(((File) record).getInternalId());
				String fileId = ((File) record).getInternalId();
				MakePayment savedMakePayment = saveMakePayment(fileId);
				savedMakePayments.add(savedMakePayment);
			}
		return savedMakePayments;
	}

	@Transactional
	MakePayment saveMakePayment(String fileId) {
		MakePayment savedMakePayment = null;
		Document document = null;
		try {
			ReadResponse response = client.callGetRecord(fileId, RecordType.file);
			File file = (File) response.getRecord();
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			document = db.parse(new ByteArrayInputStream(file.getContent()));
		} catch (IOException | ParserConfigurationException | SAXException e) {
			e.printStackTrace();
		}
		document.getDocumentElement().normalize();
		log.info("Import XML started.");
		MakePayment makePayment = new MakePayment();
		NodeList nList = document.getElementsByTagName("SubsidiaryID");
		Subsidiary subsidiary = setupServiceClient.getSubsidiaryByIntegratedId(nList.item(0).getTextContent(), false);
		makePayment.setSubsidiaryId(subsidiary.getId());
		nList = document.getElementsByTagName("Date");
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		try {
			makePayment.setPaymentDate(new Date(sdf.parse(nList.item(0).getTextContent()).getTime()));
		} catch (DOMException | ParseException e) {
			e.printStackTrace();
		}
		nList = document.getElementsByTagName("VendorId");
		Supplier supplier = masterServiceClient.findSupplierByIntegratedIdAndIsDeleted(nList.item(0).getTextContent());
		makePayment.setSupplierId(supplier.getId());
		nList = document.getElementsByTagName("BankAccountId");
		Account account = masterServiceClient.getAccountByIntegratedId(nList.item(0).getTextContent());
		log.info("This is account"+account);
		makePayment.setAccountId(account.getId());
		String glBank = account.getCode();
		Bank bank = masterServiceClient.findBankByglBank(glBank);
		log.info("THis is bank "+bank);
		makePayment.setBankId(bank.getId());
		nList = document.getElementsByTagName("TotalAmount");
		makePayment.setAmount(Double.parseDouble(nList.item(0).getTextContent()));
		nList = document.getElementsByTagName("Currency");
		makePayment.setCurrency(nList.item(0).getTextContent());
		nList = document.getElementsByTagName("BillPmtID");
		String netsuiteId = nList.item(0).getTextContent();
		makePayment.setNetsuiteId(netsuiteId);
		makePayment.setCreatedBy(CommonUtils.getLoggedInUsername());
		makePayment.setLastModifiedBy(CommonUtils.getLoggedInUsername());
		String transactionalDate = CommonUtils.convertDateToFormattedString(makePayment.getPaymentDate());
		String documentSequenceNumber = setupServiceClient.getDocumentSequenceNames(transactionalDate,
				makePayment.getSubsidiaryId(), FormNames.MAKE_PAYMENT.getFormName(), false);
		if (StringUtils.isEmpty(documentSequenceNumber)) {
			throw new CustomMessageException("Please validate your configuration to generate the Payment Number");
		}
		makePayment.setPaymentNumber(documentSequenceNumber);
		makePayment.setType(FormNames.MAKE_PAYMENT.getFormName());
		String message = "Success";
		List<CustomFieldRef> customFieldRefs = new ArrayList<>();
		if (makePayment != null) {
			nList = document.getElementsByTagName("MblId");
			NodeList pmtList = document.getElementsByTagName("PmtAmount");
			List<MakePaymentList> makePaymentLists = new ArrayList<>();
			List<InvoicePayment> invoicePaymentList = new ArrayList<>();
			for (int i = 0; i < nList.getLength(); i++) {
				MakePaymentList makePaymentList = new MakePaymentList();
				long invoiceId = Long.parseLong(nList.item(i).getTextContent());
				makePaymentList.setInvoiceId(invoiceId);
				Invoice invoice = financeServiceClient.getInvoiceById(invoiceId);
				makePaymentList.setAmountDue(invoice.getAmountDue());
				Double totalAmountPaid = financeServiceClient.getTotalAmountById(invoiceId);
				log.info("Total amount "+totalAmountPaid);
				totalAmountPaid = (totalAmountPaid!=null? totalAmountPaid : 0) + Double.parseDouble(pmtList.item(i).getTextContent());
				log.info("Total amountPaid "+totalAmountPaid);	
				invoice.setAmountDue(invoice.getTotalAmount() - totalAmountPaid);
				makePaymentList.setPaidAmount(Double.parseDouble(pmtList.item(i).getTextContent()));
				makePaymentList.setPaymentAmount(Double.parseDouble(pmtList.item(i).getTextContent()));
				makePaymentList.setPaymentNumber(documentSequenceNumber);
				makePaymentList.setCreatedBy(CommonUtils.getLoggedInUsername());
				makePaymentList.setLastModifiedBy(CommonUtils.getLoggedInUsername());
				makePaymentLists.add(makePaymentList);
				InvoicePayment invoicePayment = new InvoicePayment();
				invoicePayment.setInvoiceId(invoiceId);
				invoicePayment.setAmount(Double.parseDouble(pmtList.item(i).getTextContent()));
				invoicePayment.setType(FormNames.MAKE_PAYMENT.getFormName());
				invoicePaymentList.add(invoicePayment);
			}
			if (makePaymentLists != null) {
				makePayment.setMakePaymentList(makePaymentLists);
			}
			try {
				savedMakePayment =	financeServiceClient.save(makePayment);
			} catch (DataAccessException e) {
				log.error("Error while update the Payment :: " + e.getMostSpecificCause());
				message = e.getMostSpecificCause().getMessage();
			}
			if (savedMakePayment != null) {
				File copyFile = new File();
				copyFile.setInternalId(fileId);
				log.info("Make payment list not null");
				copyFile.setFolder(createRecordRef("861"));
				try {
					client.callUpdateRecord(copyFile);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				LongCustomFieldRef customFieldRef = new LongCustomFieldRef();
				customFieldRef.setInternalId("4830");
				customFieldRef.setValue(savedMakePayment.getId());
				customFieldRefs.add(customFieldRef);
				log.info("Imported XML: " + savedMakePayment);
				StringCustomFieldRef customFieldRef1 = new StringCustomFieldRef();
				customFieldRef1.setInternalId("860");
				customFieldRef1.setValue(message);
				customFieldRefs.add(customFieldRef);
				VendorPayment vendorPayment = new VendorPayment();
				vendorPayment.setInternalId(netsuiteId);
				CustomFieldList customFieldList = new CustomFieldList();
				customFieldList.setCustomField(customFieldRefs.toArray(new CustomFieldRef[customFieldRefs.size()]));
				vendorPayment.setCustomFieldList(customFieldList);
				try {
					client.callUpdateRecord(vendorPayment);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}
		return savedMakePayment;
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
				netsuiteValueReturn.setReferenceNumber(item.getName());
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
					netsuiteValueReturn.setReferenceNumber(invoice.getInvoiceNo());
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
