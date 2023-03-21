package com.monstarbill.integration.service;

import java.util.List;

import com.monstarbill.integration.models.ManageIntegration;
import com.monstarbill.integration.payload.request.PaginationRequest;
import com.monstarbill.integration.payload.response.PaginationResponse;



public interface ManageIntegrationService {
	
public	List<ManageIntegration> save(List<ManageIntegration> manageIntegrations);

public ManageIntegration getManageIntegrationService(Long id);

public PaginationResponse getManageIntegrationServices(PaginationRequest paginationRequest);

}
