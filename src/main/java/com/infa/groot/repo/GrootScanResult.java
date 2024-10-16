package com.infa.groot.repo;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.DynamicInsert;

import com.fasterxml.jackson.annotation.JsonBackReference;


/**
 * 
 * @author pujain
 *
 */
@Entity
@Table(name = "groot_scan_result")
@DynamicInsert
public class GrootScanResult {
	@Id
	@SequenceGenerator(name = "GROOT_RESULT_ID_GENERATOR", sequenceName = "GROOT_RESULT_SEQUENCE", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GROOT_RESULT_ID_GENERATOR" )
	@Column(name = "id")	
	private Long id;
	
	@ManyToOne
    @JoinColumn(name = "scan_id", nullable = false)
	@JsonBackReference
    private GrootScanRecord scanRecord;
	
	@Column(name = "prompt", columnDefinition = "CLOB")
	@Lob
	private String prompt;
	
	@Column(name = "response", columnDefinition = "CLOB")
	@Lob
	private String response;
	
	@Column(name = "prompt_category", length = 100)
	private String promptCategory;

	@Enumerated(EnumType.STRING)
	@Column(name = "severity", length = 50)
	private PromptSeverity severity;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "responseStatus", length = 50)
	private ResponseStatus responseStatus;
	
	@Column(name = "conversation_id", length = 50, nullable = false)
	private String conversationId;
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public GrootScanRecord getScanRecord() {
		return scanRecord;
	}

	public void setScanRecord(GrootScanRecord scanRecord) {
		this.scanRecord = scanRecord;
	}

	public String getPrompt() {
		return prompt;
	}

	public void setPrompt(String prompt) {
		this.prompt = prompt;
	}

	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
	}

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

	public String getConversationId() {
		return conversationId;
	}

	public void setConversationId(String conversationId) {
		this.conversationId = conversationId;
	}

	@Override
	public String toString() {
		return "GrootScanResult [id=" + id + ", scanRecord=" + scanRecord + ", prompt=" + prompt + ", response="
				+ response + ", promptCategory=" + promptCategory + ", severity=" + severity + ", responseStatus="
				+ responseStatus + ", conversationId=" + conversationId + "]";
	}
	
}
