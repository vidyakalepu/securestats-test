package com.infa.groot.repo;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.DynamicInsert;

import com.fasterxml.jackson.annotation.JsonManagedReference;

/**
 * 
 * @author pujain
 *
 */
@Entity
@Table(name ="groot_scan_record")
@DynamicInsert
public class GrootScanRecord {
	
	@Id
	@SequenceGenerator(name = "GROOT_SCAN_ID_GENERATOR", sequenceName = "GROOT_SCAN_SEQUENCE", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GROOT_SCAN_ID_GENERATOR" )
	@Column(name = "id")
	private Long id;
	
	@Column(name = "product", nullable = false, length = 255)
	private String product;
	
	@Column(name = "microservice", nullable = false, length = 500)
	private String microservice;
	
	@Column(name = "submitted_date")
	private Date submittedDate;
	
	@Column(name = "prompt_categories", length = 2000, nullable = false)
	private String promptCategories;
	
	@Column(name = "prompt_file_path", nullable = false, length = 1000)
	private String promptFilePath;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 50)
	private GrootScanStatus status;
	
	@Column(name = "started_date")
	private Date startedDate;
	
	@Column(name = "completed_date")
	private Date completedDate;
	
	@Column(name = "comments", columnDefinition = "CLOB")
	@Lob
	private String comment;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "last_status", length =50)
	private GrootScanStatus lastStatus;
	
	@Column(name = "conversation_id", length = 50)
	private String conversationId;
	
	@Column(name = "scan_type", length = 50, nullable = false)
	private String scanType;
	
	@OneToMany(mappedBy = "scanRecord", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@JsonManagedReference
	private List<GrootScanResult> scanResults = new ArrayList<>();


	
	
	public GrootScanRecord() {
		super();
	}

	public GrootScanRecord(String product, String microservice, Date submittedDate, String promptCategories,
			String promptFilePath, GrootScanStatus status, String comment, String scanType) {
		super();
		this.product = product;
		this.microservice = microservice;
		this.submittedDate = submittedDate;
		this.promptCategories = promptCategories;
		this.promptFilePath = promptFilePath;
		this.status = status;
		this.comment = comment;
		this.scanType = scanType;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getProduct() {
		return product;
	}

	public void setProduct(String product) {
		this.product = product;
	}

	public String getMicroservice() {
		return microservice;
	}

	public void setMicroservice(String microservice) {
		this.microservice = microservice;
	}

	public Date getSubmittedDate() {
		return submittedDate;
	}

	public void setSubmittedDate(Date submittedDate) {
		this.submittedDate = submittedDate;
	}

	public String getPromptCategories() {
		return promptCategories;
	}

	public void setPromptCategories(String promptCategories) {
		this.promptCategories = promptCategories;
	}

	public String getPromptFilePath() {
		return promptFilePath;
	}

	public void setPromptFilePath(String promptFilePath) {
		this.promptFilePath = promptFilePath;
	}

	public GrootScanStatus getStatus() {
		return status;
	}

	public void setStatus(GrootScanStatus status) {
		this.status = status;
	}

	public Date getStartedDate() {
		return startedDate;
	}

	public void setStartedDate(Date startedDate) {
		this.startedDate = startedDate;
	}

	public Date getCompletedDate() {
		return completedDate;
	}

	public void setCompletedDate(Date completedDate) {
		this.completedDate = completedDate;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public GrootScanStatus getLastStatus() {
		return lastStatus;
	}

	public void setLastStatus(GrootScanStatus lastStatus) {
		this.lastStatus = lastStatus;
	}

	public String getConversationId() {
		return conversationId;
	}

	public void setConversationId(String conversationId) {
		this.conversationId = conversationId;
	}

	public String getScanType() {
		return scanType;
	}

	public void setScanType(String scanType) {
		this.scanType = scanType;
	}

	public List<GrootScanResult> getScanResults() {
		return scanResults;
	}

	public void setScanResults(List<GrootScanResult> scanResults) {
		this.scanResults = scanResults;
	}

	@Override
	public String toString() {
		return "GrootScanRecord [id=" + id + ", product=" + product + ", microservice=" + microservice
				+ ", submittedDate=" + submittedDate + ", promptCategories=" + promptCategories + ", promptFilePath="
				+ promptFilePath + ", status=" + status + ", startedDate=" + startedDate + ", completedDate="
				+ completedDate + ", comment=" + comment + ", lastStatus=" + lastStatus + ", conversationId="
				+ conversationId + ", scanType=" + scanType + ", scanResults=" + scanResults + "]";
	}
	
}
