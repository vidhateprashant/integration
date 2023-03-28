package com.netsuite.webservices.samples;

import static com.netsuite.webservices.samples.Messages.ADDRESS_BOOK_LABEL;
import static com.netsuite.webservices.samples.Messages.COMPANY_NAME;
import static com.netsuite.webservices.samples.Messages.CUSTOMER_NAME;
import static com.netsuite.webservices.samples.Messages.CUSTOM_RECORD_TYPE_ID;
import static com.netsuite.webservices.samples.Messages.DATE_CREATED;
import static com.netsuite.webservices.samples.Messages.EMAIL;
import static com.netsuite.webservices.samples.Messages.ENTITY_ID;
import static com.netsuite.webservices.samples.Messages.ENTITY_STATUS;
import static com.netsuite.webservices.samples.Messages.ERROR;
import static com.netsuite.webservices.samples.Messages.EXTERNAL_ID;
import static com.netsuite.webservices.samples.Messages.INTERNAL_ID;
import static com.netsuite.webservices.samples.Messages.IS_INACTIVE;
import static com.netsuite.webservices.samples.Messages.ITEM_NAME;
import static com.netsuite.webservices.samples.Messages.NAME;
import static com.netsuite.webservices.samples.Messages.PHONE;
import static com.netsuite.webservices.samples.Messages.QUANTITY;
import static com.netsuite.webservices.samples.Messages.STATUS_INTERNAL_ID;
import static com.netsuite.webservices.samples.Messages.TOTAL_AMOUNT;
import static com.netsuite.webservices.samples.Messages.TRANSACTION_ID;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import com.netsuite.suitetalk.proxy.v2022_1.lists.relationships.Customer;
import com.netsuite.suitetalk.proxy.v2022_1.platform.core.RecordRef;
import com.netsuite.suitetalk.proxy.v2022_1.platform.messages.WriteResponse;
import com.netsuite.suitetalk.proxy.v2022_1.setup.customization.CustomRecord;
import com.netsuite.suitetalk.proxy.v2022_1.transactions.sales.SalesOrder;
import com.netsuite.suitetalk.proxy.v2022_1.transactions.sales.SalesOrderItem;

/**
 * <p>This is type of map which converts particular objects to map of the fields. It can be simply printed
 * using {@link com.netsuite.webservices.samples.utils.PrintUtils#printMap(Map)} method.</p>
 * <p>© 2019 NetSuite Inc. All rights reserved.</p>
 */
@ParametersAreNonnullByDefault
public class Fields extends LinkedHashMap<String, String> {

    public Fields() {
        super();
    }

    public Fields(String internalId) {
        super();
        put(INTERNAL_ID, internalId);
    }

    public Fields(String internalId, String errorMessage) {
        this(internalId);
        put(ERROR, errorMessage);
    }

    public Fields(Customer customer) {
        super();
        put(INTERNAL_ID, customer.getInternalId());
        put(EXTERNAL_ID, customer.getExternalId());
        put(ENTITY_ID, customer.getEntityId());
        put(COMPANY_NAME, customer.getCompanyName());
        put(EMAIL, customer.getEmail());
        put(PHONE, customer.getPhone());
        put(ENTITY_STATUS, customer.getEntityStatus().getName());
        put(IS_INACTIVE, String.valueOf(customer.getIsInactive()));
        put(DATE_CREATED, customer.getDateCreated().getTime().toString());
    }

    public Fields(WriteResponse response, Customer customer) {
        super();
        put(INTERNAL_ID, ((RecordRef) response.getBaseRef()).getInternalId());
        put(EXTERNAL_ID, ((RecordRef) response.getBaseRef()).getExternalId());
        put(ENTITY_ID, customer.getEntityId());
        put(COMPANY_NAME, customer.getCompanyName());
        put(EMAIL, customer.getEmail());
        put(STATUS_INTERNAL_ID, getStatusKey(customer));
        put(ADDRESS_BOOK_LABEL, getAddressBookLabel(customer));
    }

    public Fields(SalesOrder salesOrder, boolean isEntityId) {
        super();
        put(INTERNAL_ID, salesOrder.getInternalId());
        put(TRANSACTION_ID, salesOrder.getTranId());
        if (isEntityId) {
            put(ENTITY_ID, salesOrder.getEntity().getInternalId());
        } else {
            put(CUSTOMER_NAME, salesOrder.getEntity().getName());
        }
        put(TOTAL_AMOUNT, String.valueOf(salesOrder.getTotal()));
        put(DATE_CREATED, salesOrder.getCreatedDate().getTime().toString());
    }

    public Fields(SalesOrderItem item) {
        super();
        put(ITEM_NAME, item.getItem().getName());
        put(QUANTITY, String.valueOf(item.getQuantity()));
    }

    public Fields(CustomRecord customRecord) {
        super();
        put(CUSTOM_RECORD_TYPE_ID, customRecord.getRecType().getInternalId());
        put(INTERNAL_ID, customRecord.getInternalId());
        put(NAME, customRecord.getName());
    }

    private static String getStatusKey(Customer customer) {
        if (customer.getEntityStatus() == null) {
            return null;
        }
        return customer.getEntityStatus().getInternalId();
    }

    private static String getAddressBookLabel(Customer customer) {
        final int i = 0;
        if (customer.getAddressbookList() == null || customer.getAddressbookList().getAddressbook() == null
                || customer.getAddressbookList().getAddressbook()[i] == null) {
            return null;
        }
        return customer.getAddressbookList().getAddressbook()[i].getLabel();
    }
}
