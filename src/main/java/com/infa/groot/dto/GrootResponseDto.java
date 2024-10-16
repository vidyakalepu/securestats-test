package com.infa.groot.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.infa.groot.repo.GrootScanResult;
import com.infa.groot.repo.GrootScanStatus;
import com.infa.groot.repo.PromptSeverity;

public class GrootResponseDto implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Long scanId;

	private GrootScanStatus scanStatus;

	private List<GrootScanResult> results;

	private String message;

	private Map<PromptSeverity, Long> severityCount;

	public Long getScanId() {
		return scanId;
	}

	public void setScanId(Long scanId) {
		this.scanId = scanId;
	}

	public GrootScanStatus getScanStatus() {
		return scanStatus;
	}

	public void setScanStatus(GrootScanStatus scanStatus) {
		this.scanStatus = scanStatus;
	}

	public List<GrootScanResult> getResults() {
		return results;
	}

	public void setResults(List<GrootScanResult> results) {
		this.results = results;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Map<PromptSeverity, Long> getSeverityCount() {
		return severityCount;
	}

	public void setSeverityCount(Map<PromptSeverity, Long> severityCount) {
		this.severityCount = severityCount;
	}

	@Override
	public String toString() {
		return "GrootResponseDto{" +
				"scanId=" + scanId +
				", scanStatus=" + scanStatus +
				", results=" + results +
				", message='" + message + '\'' +
				", severityCount=" + severityCount +
				'}';
	}
}
