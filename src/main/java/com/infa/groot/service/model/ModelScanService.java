package com.infa.groot.service.model;


/**
 * 
 * @author pujain
 *
 */
public interface ModelScanService {

	public void runModelScan(Long id);

	void cancelScan(Long scanId);

}
