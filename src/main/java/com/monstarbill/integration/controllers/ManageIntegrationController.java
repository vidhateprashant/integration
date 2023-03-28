package com.monstarbill.integration.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.monstarbill.integration.commons.CustomException;
import com.monstarbill.integration.models.ManageIntegration;
import com.monstarbill.integration.payload.request.PaginationRequest;
import com.monstarbill.integration.payload.response.PaginationResponse;
import com.monstarbill.integration.service.ManageIntegrationService;

import lombok.extern.slf4j.Slf4j;

/**
 * All WS's of the Purchase Order and it's child components if any
 * 
 * @author Prithwish 23-12-2022
 */
@Slf4j
@RestController
@RequestMapping("/intigration")
public class ManageIntegrationController {

	@Autowired
	private ManageIntegrationService manageIntegrationService;

	/**
	 * Save/update the manage integration
	 * 
	 * @param manage integration
	 * @return
	 */
	@PostMapping("/save")
	public ResponseEntity<List<ManageIntegration>> save(@RequestBody List<ManageIntegration> manageIntegrations) {
		log.info("Saving the manage integration :: " + manageIntegrations.toString());
		try {
			manageIntegrations = manageIntegrationService.save(manageIntegrations);
		} catch (Exception e) {
			log.error("Error while saving the manage integration :: ");
			e.printStackTrace();
			throw new CustomException("Error while saving the manage integration " + e.toString());
		}
		log.info("Manage integration saved successfully");
		return ResponseEntity.ok(manageIntegrations);
	}	
	
	/**
	 * Get manage integration
	 * 
	 * @param id
	 * @return manage integration
	 */
	@GetMapping("/get")
    public ManageIntegration getManageIntegration(@RequestParam Long id)
    {
        return manageIntegrationService.getManageIntegrationService(id);
    }
	
	/**
	 * Get all manage intigrations
	 * 
	 * @param
	 * @return manage intigrations
	 */
	@PostMapping("/get/all")
    public PaginationResponse getManageIntigrations(@RequestBody PaginationRequest paginationRequest)
    {
		return manageIntegrationService.getManageIntegrationServices(paginationRequest);
    }
}
