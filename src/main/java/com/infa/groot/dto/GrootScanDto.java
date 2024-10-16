package com.infa.groot.dto;

import java.io.Serializable;
import java.util.List;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 
 * @author pujain
 *
 */
public class GrootScanDto implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5996507702448836463L;
	
	private String product;
	
	private String microservice;
	
	private List<String> promptCategories;
	
	private String promptFilePath;
	
	private String comment;
	
	private String scanType;
	

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

	public List<String> getPromptCategories() {
		return promptCategories;
	}

	public void setPromptCategories(List<String> promptCategories) {
		this.promptCategories = promptCategories;
	}

	public String getPromptFilePath() {
		return promptFilePath;
	}

	public void setPromptFilePath(String promptFilePath) {
		this.promptFilePath = promptFilePath;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getScanType() {
		return scanType;
	}

	public void setScanType(String scanType) {
		this.scanType = scanType;
	}

	@Override
	public String toString() {
		return "GrootScanDto [product=" + product + ", microservice=" + microservice + ", promptCategories="
				+ promptCategories + ", promptFilePath=" + promptFilePath + ", comment=" + comment + ", scanType="
				+ scanType + "]";
	}

	public Boolean isValid() {
		return !StringUtils.isEmpty(product) &&
				!StringUtils.isEmpty(microservice) &&
				!StringUtils.isEmpty(promptFilePath) &&
				!StringUtils.isEmpty(scanType) &&
				!CollectionUtils.isEmpty(promptCategories);
	}
	
	
	
	
	

}
