package com.infa.groot.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.infa.groot.dto.GrootResponseDto;
import com.infa.groot.dto.GrootScanDto;
import com.infa.groot.dto.GrootScanResultFilters;
import com.infa.groot.repo.GrootScanRecord;

/**
 * 
 * @author pujain
 *
 */
public interface GrootService {

	public GrootResponseDto saveNewScan(GrootScanDto grootScanDto);

	public GrootResponseDto getScanStatus(Long scanId);

	public GrootResponseDto fetchResult(Long scanId, GrootScanResultFilters grootScanResultFilters, Integer page,
			Integer pageLimit);

	public boolean retryScan(Long scanId);

	public boolean deleteScan(Long scanId);

	public long fetchResultCount(Long scanId, GrootScanResultFilters grootScanResultFilters);

	public Optional<GrootScanRecord> findScanById(Long scanId);

	public List<GrootResponseDto> fetchFlawCount(List<Long> scanIds);
}
