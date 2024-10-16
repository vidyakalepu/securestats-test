package com.infa.groot.dto;

import java.io.Serializable;

import com.infa.groot.repo.PromptSeverity;
import com.infa.groot.repo.ResponseStatus;

/**
 * 
 * @author pujain
 *
 */
public class GrootScanResultFilters implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String promptCategory;
	private PromptSeverity severity;
	private ResponseStatus responseStatus;
	
	
	public String getPromptCategory() {
		return promptCategory;
	}
	public void setPromptCategory(String promptCategory) {
		this.promptCategory = promptCategory;
	}
	public PromptSeverity getSeverity() {
		return severity;
	}
	public void setSeverity(PromptSeverity severity) {
		this.severity = severity;
	}
	public ResponseStatus getResponseStatus() {
		return responseStatus;
	}
	public void setResponseStatus(ResponseStatus responseStatus) {
		this.responseStatus = responseStatus;
	}
	
	
	@Override
	public String toString() {
		return "GrootScanResultFilters [promptCategory=" + promptCategory + ", severity=" + severity
				+ ", responseStatus=" + responseStatus + "]";
	}
	
	
}
