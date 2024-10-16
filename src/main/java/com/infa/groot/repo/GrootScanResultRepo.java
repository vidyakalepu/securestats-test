package com.infa.groot.repo;

import java.util.Date;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 
 * @author pujain
 *
 */
@Repository
public interface GrootScanResultRepo extends JpaRepository<GrootScanResult, Long>, JpaSpecificationExecutor<GrootScanResult> {
	
    List<GrootScanResult> findByScanRecord_IdAndConversationId(Long scanId, String conversationId);
    
    @Transactional
    @Modifying
    @Query("DELETE FROM GrootScanResult r WHERE r.conversationId NOT IN (SELECT s.conversationId FROM GrootScanRecord s) AND r.scanRecord.completedDate < :dateLimit")
    int purgeOutdatedResults(@Param("dateLimit") Date dateLimit);
     
    @Query("SELECT r.scanRecord.id AS scanId, " +
    	       "COUNT(CASE WHEN r.severity = 'LOW' THEN 1 END) AS lowCount, " +
    	       "COUNT(CASE WHEN r.severity = 'MEDIUM' THEN 1 END) AS mediumCount, " +
    	       "COUNT(CASE WHEN r.severity = 'HIGH' THEN 1 END) AS highCount, " +
    	       "COUNT(CASE WHEN r.severity = 'CRITICAL' THEN 1 END) AS criticalCount " +
    	       "FROM GrootScanResult r " +
    	       "JOIN r.scanRecord s " +
    	       "ON r.conversationId = s.conversationId " +
    	       "WHERE r.scanRecord.id IN :scanIds " +
    	       "AND r.responseStatus = :responseStatus " +
    	       "GROUP BY r.scanRecord.id")
	List<Object[]> fetchFlawCountByScanIds(@Param("scanIds") List<Long> scanIds,
	                                        @Param("responseStatus") ResponseStatus responseStatus);


}
