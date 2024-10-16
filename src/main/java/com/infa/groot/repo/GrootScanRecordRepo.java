package com.infa.groot.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 
 * @author pujain
 *
 */
@Repository
public interface GrootScanRecordRepo extends JpaRepository<GrootScanRecord, Long>, JpaSpecificationExecutor<GrootScanRecord>{
	
	Optional<GrootScanRecord> findById(Long id);
	
	List<GrootScanRecord> findByStatus(GrootScanStatus status);

	
}
