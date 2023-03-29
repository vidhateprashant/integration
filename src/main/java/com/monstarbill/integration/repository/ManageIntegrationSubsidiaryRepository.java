package com.monstarbill.integration.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.monstarbill.integration.models.ManageIntegrationSubsidiary;



@Repository
public interface ManageIntegrationSubsidiaryRepository extends JpaRepository<ManageIntegrationSubsidiary, String> {


	public List<ManageIntegrationSubsidiary> findByIntigrationIdAndIsDeleted(Long id, boolean isDeleted);

	public List<ManageIntegrationSubsidiary> findByIntigrationId(Long manageIntegrationId);
	
	public Optional<ManageIntegrationSubsidiary> findBySubsidiaryId(Long subsidiaryId);


}
